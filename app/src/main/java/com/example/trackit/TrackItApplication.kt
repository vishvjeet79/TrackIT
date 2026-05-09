package com.example.trackit

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.example.trackit.data.InventoryDatabase
import com.example.trackit.data.InventoryRepository
import com.example.trackit.data.UserPreferencesRepository

private const val LAYOUT_PREFERENCE_NAME = "user_preferences"
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = LAYOUT_PREFERENCE_NAME
)

class TrackItApplication : Application() {
    val database: InventoryDatabase by lazy { InventoryDatabase.getDatabase(this) }
    val repository: InventoryRepository by lazy { InventoryRepository(database.inventoryDao()) }
    val userPreferencesRepository: UserPreferencesRepository by lazy {
        UserPreferencesRepository(dataStore)
    }
}
