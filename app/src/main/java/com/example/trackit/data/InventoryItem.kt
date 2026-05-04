package com.example.trackit.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "inventory_items")
data class InventoryItem(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val quantity: Int,
    val imagePath: String? = null,
    val location: String? = null,
    val category: String? = null,
    val expiryDate: Long? = null,
    val timestamp: Long = System.currentTimeMillis()
)
