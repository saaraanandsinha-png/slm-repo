package com.example.saaraapp

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ReminderRepository(private val dao: ReminderDao) {

    // All reminders as ReminderItem list, live from database
    val allReminders: Flow<List<ReminderItem>> =
        dao.getAllReminders().map { entities ->
            entities.map { it.toReminderItem() }
        }

    suspend fun insertReminder(reminder: ReminderItem) {
        dao.insertReminder(reminder.toEntity())
    }

    suspend fun deleteReminder(id: String) {
        dao.deleteReminder(id)
    }

    suspend fun clearAll() {
        dao.clearAll()
    }
}
