package com.example.trackit.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface InventoryDao {
    @Query("SELECT * FROM inventory_items ORDER BY timestamp DESC")
    fun getAllItems(): Flow<List<InventoryItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: InventoryItem)

    @Delete
    suspend fun deleteItem(item: InventoryItem)

    @Update
    suspend fun updateItem(item: InventoryItem)

    @Query("SELECT * FROM categories")
    fun getAllCategories(): Flow<List<Category>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategory(category: Category)

    @Query("SELECT * FROM locations")
    fun getAllLocations(): Flow<List<Location>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLocation(location: Location)

    @Delete
    suspend fun deleteLocation(location: Location)
}
