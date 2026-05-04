package com.example.trackit.ui

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.trackit.TrackItApplication
import com.example.trackit.ui.inventory.InventoryViewModel

object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer {
            InventoryViewModel(trackItApplication().repository)
        }
    }
}

fun CreationExtras.trackItApplication(): TrackItApplication =
    (this[AndroidViewModelFactory.APPLICATION_KEY] as TrackItApplication)
