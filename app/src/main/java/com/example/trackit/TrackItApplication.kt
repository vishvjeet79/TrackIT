package com.example.trackit

import android.app.Application
import com.example.trackit.data.InventoryDatabase
import com.example.trackit.data.InventoryRepository

class TrackItApplication : Application() {
    val database: InventoryDatabase by lazy { InventoryDatabase.getDatabase(this) }
    val repository: InventoryRepository by lazy { InventoryRepository(database.inventoryDao()) }
}
