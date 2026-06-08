package com.example.saaraapp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NotificationViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ReminderRepository(
        ReminderDatabase.getDatabase(application).reminderDao()
    )

    // Reminders come from the database — survive app restarts!
    val reminders: StateFlow<List<ReminderItem>> = repository.allReminders
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun deleteReminder(id: String) {
        viewModelScope.launch { repository.deleteReminder(id) }
    }

    fun clearAll() {
        viewModelScope.launch { repository.clearAll() }
    }
}
