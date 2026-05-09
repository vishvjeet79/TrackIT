package com.example.trackit.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trackit.data.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class TourStep {
    WAITING,
    INVENTORY_OVERVIEW,
    ADD_ITEM,
    LOCATIONS_TAB,
    SUB_LOCATIONS,
    FINISHED
}

class TourViewModel(
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {
    
    private val _currentStep = MutableStateFlow(TourStep.WAITING)
    val currentStep: StateFlow<TourStep> = _currentStep

    val showTour: StateFlow<Boolean> = combine(
        userPreferencesRepository.isFirstLaunch,
        _currentStep
    ) { isFirstLaunch, step ->
        isFirstLaunch && step != TourStep.FINISHED && step != TourStep.WAITING
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = false
    )

    fun nextStep() {
        val next = when (_currentStep.value) {
            TourStep.WAITING -> TourStep.INVENTORY_OVERVIEW
            TourStep.INVENTORY_OVERVIEW -> TourStep.ADD_ITEM
            TourStep.ADD_ITEM -> TourStep.LOCATIONS_TAB
            TourStep.LOCATIONS_TAB -> TourStep.SUB_LOCATIONS
            TourStep.SUB_LOCATIONS -> TourStep.FINISHED
            TourStep.FINISHED -> TourStep.FINISHED
        }
        _currentStep.value = next
        
        if (next == TourStep.FINISHED) {
            completeTour()
        }
    }

    fun skipTour() {
        _currentStep.value = TourStep.FINISHED
        completeTour()
    }

    private fun completeTour() {
        viewModelScope.launch {
            userPreferencesRepository.setFirstLaunchCompleted()
        }
    }
}
