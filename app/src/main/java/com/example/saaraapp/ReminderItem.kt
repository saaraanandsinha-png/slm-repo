package com.example.saaraapp

enum class ReminderCategory(val label: String, val emoji: String) {
    DEADLINE("Deadline", "🗓️"),
    ASSIGNMENT("Assignment", "📝"),
    EXAM("Exam / Test", "📚"),
    MEETING("Meeting / Class", "📞"),
    REMINDER("Reminder", "⚠️"),
    SCHEDULE_CHANGE("Schedule Change", "🔄"),
    HOLIDAY("Holiday", "🎉"),
    OTHER("Note", "💬")
}

data class ReminderItem(
    val id: String,
    val sender: String,
    val originalMessage: String,
    val tags: List<String>,
    val category: ReminderCategory,
    val time: Long
)
