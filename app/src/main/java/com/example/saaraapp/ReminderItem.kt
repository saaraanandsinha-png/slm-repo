package com.example.saaraapp

enum class ReminderCategory(val label: String, val emoji: String) {
    ACADEMIC("Academic", "📚"),
    PERSONAL("Personal", "🏠"),
    EVENT("Event", "🎉"),
    INFO("Info", "💬"),
    OTHER("Other", "💬"),
    SCHEDULE_CHANGE("Schedule Change", "🔄"), // internal only — triggers reschedule logic, never saved to DB
}

data class ReminderItem(
    val id: String,
    val sender: String,
    val originalMessage: String,
    val tags: List<String>,
    val category: ReminderCategory,
    val time: Long,
    val reminderDate: java.time.LocalDate? = null,
    val reminderDateEnd: java.time.LocalDate? = null,
    val scheduledAlarmTime: Long? = null,
    val reminderPromptDismissed: Boolean = false
)
