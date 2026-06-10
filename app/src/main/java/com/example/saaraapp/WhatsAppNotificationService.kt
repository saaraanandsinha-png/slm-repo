package com.example.saaraapp

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class WhatsAppNotificationService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onListenerConnected() {
        super.onListenerConnected()
        // Service is bound and ready — Android called this after (re)connecting
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

        if (!KeywordExtractor.isRelevant(message)) return

        val (reminderDate, reminderDateEnd) = DateParser.extractRangeFrom(message)

        val reminder = ReminderItem(
            // Hash of sender + message text: same content = same ID (deduplicates
            // WhatsApp re-posting the same notification), different message = new entry
            id              = "${sbn.packageName}_${(sender + message).hashCode()}",
            sender          = sender,
            originalMessage = message,
            tags            = KeywordExtractor.extractTags(message),
            category        = KeywordExtractor.categorize(message),
            time            = sbn.postTime,
            reminderDate    = reminderDate,
            reminderDateEnd = reminderDateEnd
        )

        // Save to Room database on IO thread
        serviceScope.launch {
            ReminderDatabase.getDatabase(applicationContext)
                .reminderDao()
                .insertReminder(reminder.toEntity())
        }
    }

    // Reminders stay in the database even after notification is dismissed
    override fun onNotificationRemoved(sbn: StatusBarNotification) { }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
