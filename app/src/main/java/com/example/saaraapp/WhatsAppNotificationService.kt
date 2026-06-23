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

class WhatsAppNotificationService : NotificationListenerService() {

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

    override fun onNotificationPosted(sbn: StatusBarNotification) {
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

        // Save to DB on IO thread
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
            // Fetch existing reminders on the same date (or dateless ones)
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
