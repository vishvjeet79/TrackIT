package com.example.trackit.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trackit.data.UserPreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {
    val isDarkModeEnabled: StateFlow<Boolean> = userPreferencesRepository.isDarkModeEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = true
        )

    val isFirstLaunch: StateFlow<Boolean> = userPreferencesRepository.isFirstLaunch
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = false
        )

    fun toggleDarkMode(isEnabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.saveDarkModePreference(isEnabled)
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            userPreferencesRepository.setFirstLaunchCompleted()
        }
    }
}
