package com.example.saaraapp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SettingsRepository(application)

    /** null = follow system default */
    val darkTheme: StateFlow<Boolean?> = repository.darkTheme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val amoledMode: StateFlow<Boolean> = repository.amoledMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setDarkTheme(enabled: Boolean) {
        viewModelScope.launch { repository.setDarkTheme(enabled) }
    }

    fun setAmoledMode(enabled: Boolean) {
        viewModelScope.launch { repository.setAmoledMode(enabled) }
    }
}
