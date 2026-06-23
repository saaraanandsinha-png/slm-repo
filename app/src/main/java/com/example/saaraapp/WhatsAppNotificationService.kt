package com.example.saaraapp

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * A [NotificationListenerService] that intercepts WhatsApp notifications and
 * uses on-device AI to extract reminders from message text.
 *
 * ## How it works
 * 1. Android calls [onListenerConnected] when the user grants notification access.
 *    At that point we initialise [FunctionGemmaHelper] so the model is warm by the
 *    time the first message arrives.
 * 2. Every incoming notification is received in [onNotificationPosted].
 *    Non-WhatsApp and group-summary notifications are filtered out immediately.
 * 3. The message text is run through [FunctionGemmaHelper.analyze] to determine
 *    whether it's a reminder and to extract structured fields (date, time, category).
 * 4. A [ReminderItem] is built and checked for duplicates before being saved to
 *    the Room database via [ReminderDao].
 * 5. If the AI flags the message as a [ReminderCategory.SCHEDULE_CHANGE], the
 *    original reminder is found and updated or deleted instead of saving a new entry.
 *
 * ## Required permission
 * This service requires the `BIND_NOTIFICATION_LISTENER_SERVICE` permission and
 * must be declared in `AndroidManifest.xml`. The user must also manually grant
 * notification access in **Settings > Apps > Special app access > Notification access**.
 */
class WhatsAppNotificationService : NotificationListenerService() {

    // SupervisorJob ensures that if one coroutine fails (e.g. a DB write),
    // it doesn't cancel the entire scope and bring down all other in-flight work.
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val gemma by lazy { FunctionGemmaHelper(applicationContext) }

    override fun onListenerConnected() {
        super.onListenerConnected()
        // Try to load FunctionGemma. If it's in assets, initialize will copy it.
        serviceScope.launch {
            val modelFile = ModelDownloadManager.getModelFileIfExists(applicationContext)
                ?: File(applicationContext.filesDir, "functiongemma-270m-it-Q4_K_M.gguf")
            
            gemma.initialize(modelFile)
            Log.i("AloofService", "FunctionGemma initialization attempted")
        }
    }

    /**
     * Called by Android for every notification posted on the device.
     *
     * ## Pipeline
     * 1. **Filter** — ignore non-WhatsApp packages and group-summary notifications
     * 2. **Extract** — pull sender and message text from the notification extras
     * 3. **Analyse** — run [FunctionGemmaHelper.analyze] to classify the message
     * 4. **Route** — if it's a [ReminderCategory.SCHEDULE_CHANGE], delegate to [handleReschedule]
     * 5. **Deduplicate** — compare against existing reminders on the same date
     * 6. **Save** — insert the new [ReminderItem] into the Room database
     */
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        // com.whatsapp = standard WhatsApp, com.whatsapp.w4b = WhatsApp Business
        val isWhatsApp = sbn.packageName == "com.whatsapp" ||
                         sbn.packageName == "com.whatsapp.w4b"
        if (!isWhatsApp) return

        // Skip group summary notifications (the "X messages from Y chats" rollup)
        val isGroupSummary = sbn.notification.flags and android.app.Notification.FLAG_GROUP_SUMMARY != 0
        if (isGroupSummary) return

        val extras  = sbn.notification.extras
        val sender  = extras.getString("android.title") ?: "Unknown"
        val message = extras.getCharSequence("android.text")?.toString() ?: ""

        Log.d("AloofService", "Notification received from $sender: $message")

        // All DB and inference work is done off the main thread
        serviceScope.launch {
            val result = gemma.analyze(message)

            // Use Gemma's extracted date if available, otherwise fall back to DateParser
            val (reminderDate, reminderDateEnd) = if (result.dateText != null) {
                Pair(DateParser.parse(result.dateText), null)
            } else {
                DateParser.extractRangeFrom(message)
            }

            val reminder = ReminderItem(
                id              = "${sbn.packageName}_${System.currentTimeMillis()}_${(sender + message).hashCode()}",
                sender          = sender,
                originalMessage = message,
                tags            = if (result.fromFallback) KeywordExtractor.extractTags(message) else result.tags,
                category        = result.category,
                time            = sbn.postTime,
                reminderDate    = reminderDate,
                reminderDateEnd = reminderDateEnd
            )

            val dao = ReminderDatabase.getDatabase(applicationContext).reminderDao()

            // If this is a schedule change, try to find and update the original reminder
            if (result.category == ReminderCategory.SCHEDULE_CHANGE) {
                handleReschedule(
                    dao           = dao,
                    result        = result,
                    newDate       = reminderDate,
                    incomingTags  = reminder.tags
                )
                return@launch  // don't save the schedule-change message itself
            }

            // ── Deduplication ─────────────────────────────────────────────────
            // WhatsApp often sends multiple notifications for the same event
            // (e.g. a forwarded message and a direct reply). We compare the new
            // reminder against existing ones on the same date using
            // [ReminderDeduplicator.isSimilar] (tag + text overlap).
            //
            // If a duplicate is found, we keep whichever message has the higher
            // quality score (longer, more structured text wins). This way we
            // always store the most informative version of a reminder.
            val candidates = if (reminderDate != null)
                dao.getRemindersOnDate(reminderDate.toEpochDay())
            else
                dao.getDatelessReminders()

            val duplicate = candidates
                .map { it.toReminderItem() }
                .firstOrNull { ReminderDeduplicator.isSimilar(it, reminder) }

            if (duplicate != null) {
                val existingScore = ReminderDeduplicator.qualityScore(duplicate.originalMessage)
                val incomingScore = ReminderDeduplicator.qualityScore(reminder.originalMessage)

                if (incomingScore <= existingScore) {
                    // Existing is better or equal — skip the new one
                    Log.d("AloofService", "Duplicate skipped (existing is higher quality)")
                    return@launch
                } else {
                    // Incoming is better — replace the existing one
                    Log.d("AloofService", "Duplicate replaced (incoming is higher quality)")
                    dao.deleteReminder(duplicate.id)
                }
            }
            // ─────────────────────────────────────────────────────────────────

            dao.insertReminder(reminder.toEntity())
        }
    }

    // Reminders stay in the database even after notification is dismissed
    override fun onNotificationRemoved(sbn: StatusBarNotification) { }

    // ── Reschedule handler ────────────────────────────────────────────────────

    /**
     * Handles SCHEDULE_CHANGE messages:
     * - Finds the original reminder using [result.originalDateText] + tag overlap
     * - If new date exists: moves it to the new date
     * - If cancelled / no new date: deletes it
     * - If original not found: silently drops the message (nothing to reschedule)
     */
    private suspend fun handleReschedule(
        dao          : ReminderDao,
        result       : GemmaResult,
        newDate      : java.time.LocalDate?,
        incomingTags : List<String>
    ) {
        // Parse the original (old) date
        val originalDate = result.originalDateText?.let { DateParser.parse(it) }

        // Fetch candidates from the original date
        val candidates = if (originalDate != null)
            dao.getRemindersOnDate(originalDate.toEpochDay())
        else
            dao.getDatelessReminders()

        // Find the reminder whose tags overlap with the incoming message
        val incomingTagsLower = incomingTags.map { it.lowercase() }.toSet()
        val original = candidates.firstOrNull { entity ->
            entity.tags.map { it.lowercase() }.any { it in incomingTagsLower }
        }

        if (original == null) {
            Log.d("AloofService", "Reschedule: no matching original reminder found — dropping")
            return
        }

        // Delete the original entry
        dao.deleteReminder(original.id)
        Log.d("AloofService", "Reschedule: deleted original reminder '${original.originalMessage}'")

        if (newDate != null) {
            // Move to the new date
            val moved = original.copy(
                id           = "${original.id}_rescheduled_${newDate.toEpochDay()}",
                reminderDate = newDate.toEpochDay()
            )
            dao.insertReminder(moved)
            Log.d("AloofService", "Reschedule: moved to $newDate")
        } else {
            // Cancelled with no new date — just deleted above
            Log.d("AloofService", "Reschedule: cancelled with no new date — removed")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        gemma.close()
        serviceScope.cancel()
    }
}
