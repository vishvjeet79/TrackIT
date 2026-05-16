package com.example.trackit.data

import kotlinx.coroutines.flow.Flow

class InventoryRepository(private val inventoryDao: InventoryDao) {
    fun getAllItemsStream(): Flow<List<InventoryItem>> = inventoryDao.getAllItems()
    suspend fun getAllItems(): List<InventoryItem> = inventoryDao.getAllItemsSync()
    suspend fun insertItem(item: InventoryItem) = inventoryDao.insertItem(item)
    suspend fun insertItems(items: List<InventoryItem>) = inventoryDao.insertItems(items)
    suspend fun deleteItem(item: InventoryItem) = inventoryDao.deleteItem(item)
    suspend fun deleteItems(items: List<InventoryItem>) = inventoryDao.deleteItems(items)
    suspend fun deleteAllItems() = inventoryDao.deleteAllItems()
    suspend fun updateItem(item: InventoryItem) = inventoryDao.updateItem(item)

    fun getAllCategoriesStream(): Flow<List<Category>> = inventoryDao.getAllCategories()
    suspend fun getAllCategories(): List<Category> = inventoryDao.getAllCategoriesSync()
    suspend fun insertCategory(category: Category) = inventoryDao.insertCategory(category)
    suspend fun insertCategories(categories: List<Category>) = inventoryDao.insertCategories(categories)
    suspend fun deleteCategory(category: Category) = inventoryDao.deleteCategory(category)

    fun getAllLocationsStream(): Flow<List<Location>> = inventoryDao.getAllLocations()
    suspend fun getAllLocations(): List<Location> = inventoryDao.getAllLocationsSync()
    suspend fun insertLocation(location: Location) = inventoryDao.insertLocation(location)
    suspend fun insertLocations(locations: List<Location>) = inventoryDao.insertLocations(locations)
    suspend fun deleteLocation(location: Location) = inventoryDao.deleteLocation(location)
}
