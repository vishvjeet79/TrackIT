package com.example.trackit.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "locations")
data class Location(
    @PrimaryKey
    val name: String,
    val parentName: String? = null,
    val imagePath: String? = null,
)
