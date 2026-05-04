package com.example.trackit.data

import kotlinx.coroutines.flow.Flow

class InventoryRepository(private val inventoryDao: InventoryDao) {
    fun getAllItemsStream(): Flow<List<InventoryItem>> = inventoryDao.getAllItems()

    suspend fun insertItem(item: InventoryItem) = inventoryDao.insertItem(item)

    suspend fun deleteItem(item: InventoryItem) = inventoryDao.deleteItem(item)

    suspend fun updateItem(item: InventoryItem) = inventoryDao.updateItem(item)

    fun getAllCategoriesStream(): Flow<List<Category>> = inventoryDao.getAllCategories()

    suspend fun insertCategory(category: Category) = inventoryDao.insertCategory(category)

    fun getAllLocationsStream(): Flow<List<Location>> = inventoryDao.getAllLocations()

    suspend fun insertLocation(location: Location) = inventoryDao.insertLocation(location)

    suspend fun deleteLocation(location: Location) = inventoryDao.deleteLocation(location)
}
