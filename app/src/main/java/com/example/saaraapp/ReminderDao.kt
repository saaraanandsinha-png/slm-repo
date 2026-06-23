package com.example.saaraapp

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {

    // Get all reminders, newest first — auto updates when DB changes
    @Query("SELECT * FROM reminders ORDER BY time DESC")
    fun getAllReminders(): Flow<List<ReminderEntity>>

    // Insert or update a reminder
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ReminderEntity)

    // Delete a single reminder
    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteReminder(id: String)

    // Clear all reminders
    @Query("DELETE FROM reminders")
    suspend fun clearAll()

    // Get all reminders on a specific date (for deduplication + rescheduling)
    @Query("SELECT * FROM reminders WHERE reminderDate = :epochDay")
    suspend fun getRemindersOnDate(epochDay: Long): List<ReminderEntity>

    // Get all reminders with no date (for deduplication of dateless reminders)
    @Query("SELECT * FROM reminders WHERE reminderDate IS NULL")
    suspend fun getDatelessReminders(): List<ReminderEntity>

    // Dismiss the time reminder prompt for a specific reminder
    @Query("UPDATE reminders SET reminderPromptDismissed = 1 WHERE id = :id")
    suspend fun dismissReminderPrompt(id: String)

    // Set the scheduled alarm time for a specific reminder
    @Query("UPDATE reminders SET scheduledAlarmTime = :alarmTime WHERE id = :id")
    suspend fun setAlarmTime(id: String, alarmTime: Long)
}
