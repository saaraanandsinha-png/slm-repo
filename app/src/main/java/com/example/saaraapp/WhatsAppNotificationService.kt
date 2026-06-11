package com.example.saaraapp

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
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
        // Try to load FunctionGemma if the model is already downloaded
        serviceScope.launch {
            val modelFile = ModelDownloadManager.getModelFileIfExists(applicationContext)
            if (modelFile != null) {
                gemma.initialize(modelFile)
                Log.i("AloofService", "FunctionGemma ready ✅")
            } else {
                Log.i("AloofService", "Model not downloaded yet — using keyword fallback")
                // Download happens from MainActivity on first launch
            }
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

        // Fast rule-based pre-filter — avoids waking up the model for irrelevant messages
        if (!KeywordExtractor.isRelevant(message)) return

        // Save to DB on IO thread using FunctionGemma (or fallback if not ready)
        serviceScope.launch {
            val result = gemma.analyze(message)

            // If the model says it's not a reminder, skip it
            if (!result.isReminder) return@launch

            // Use Gemma's extracted date if available, otherwise fall back to DateParser
            val (reminderDate, reminderDateEnd) = if (result.dateText != null) {
                Pair(
                    DateParser.parse(result.dateText),
                    null
                )
            } else {
                DateParser.extractRangeFrom(message)
            }

            val reminder = ReminderItem(
                id              = "${sbn.packageName}_${(sender + message).hashCode()}",
                sender          = sender,
                originalMessage = message,
                tags            = result.tags.ifEmpty { KeywordExtractor.extractTags(message) },
                category        = result.category,
                time            = sbn.postTime,
                reminderDate    = reminderDate,
                reminderDateEnd = reminderDateEnd
            )

            ReminderDatabase.getDatabase(applicationContext)
                .reminderDao()
                .insertReminder(reminder.toEntity())
        }
    }

    // Reminders stay in the database even after notification is dismissed
    override fun onNotificationRemoved(sbn: StatusBarNotification) { }

    override fun onDestroy() {
        super.onDestroy()
        gemma.close()
        serviceScope.cancel()
    }
}
