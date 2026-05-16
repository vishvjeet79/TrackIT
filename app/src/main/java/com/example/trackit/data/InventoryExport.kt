package com.example.trackit.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class InventoryExport(
    val items: List<InventoryItem>,
    val categories: List<Category>,
    val locations: List<Location>
)
