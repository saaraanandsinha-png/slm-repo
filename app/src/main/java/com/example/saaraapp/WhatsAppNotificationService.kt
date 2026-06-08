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

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val isWhatsApp = sbn.packageName == "com.whatsapp" ||
                         sbn.packageName == "com.whatsapp.w4b"
        if (!isWhatsApp) return

        val extras  = sbn.notification.extras
        val sender  = extras.getString("android.title") ?: "Unknown"
        val message = extras.getCharSequence("android.text")?.toString() ?: ""

        if (!KeywordExtractor.isRelevant(message)) return

        val reminder = ReminderItem(
            id              = "${sbn.id}_${sbn.postTime}",
            sender          = sender,
            originalMessage = message,
            tags            = KeywordExtractor.extractTags(message),
            category        = KeywordExtractor.categorize(message),
            time            = sbn.postTime
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
