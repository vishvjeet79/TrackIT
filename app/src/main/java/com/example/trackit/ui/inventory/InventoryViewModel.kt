package com.example.trackit.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trackit.data.Category
import com.example.trackit.data.InventoryItem
import com.example.trackit.data.InventoryRepository
import com.example.trackit.data.Location
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class InventoryViewModel(private val repository: InventoryRepository) : ViewModel() {

    val categories: StateFlow<List<Category>> =
        repository.getAllCategoriesStream()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(TIMEOUT_MILLIS),
                initialValue = listOf()
            )

    val locations: StateFlow<List<Location>> =
        repository.getAllLocationsStream()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(TIMEOUT_MILLIS),
                initialValue = listOf()
            )

    val allItems: StateFlow<List<InventoryItem>> =
        repository.getAllItemsStream()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(TIMEOUT_MILLIS),
                initialValue = listOf()
            )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    val inventoryUiState: StateFlow<InventoryUiState> =
        combine(repository.getAllItemsStream(), _searchQuery) { items, query ->
            val filteredItems = if (query.isEmpty()) {
                items
            } else {
                items.filter {
                    it.name.contains(query, ignoreCase = true) ||
                            (it.location?.contains(query, ignoreCase = true) ?: false) ||
                            (it.category?.contains(query, ignoreCase = true) ?: false)
                }
            }
            InventoryUiState(filteredItems)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(TIMEOUT_MILLIS),
            initialValue = InventoryUiState()
        )

    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun addItem(name: String, quantity: Int, imagePath: String?, location: String?, category: String?, expiryDate: Long?) {
        viewModelScope.launch {
            repository.insertItem(
                InventoryItem(
                    name = name,
                    quantity = quantity,
                    imagePath = imagePath,
                    location = location,
                    category = category,
                    expiryDate = expiryDate
                )
            )
        }
    }

    fun deleteItem(item: InventoryItem) {
        viewModelScope.launch {
            repository.deleteItem(item)
        }
    }

    fun deleteItems(items: List<InventoryItem>) {
        viewModelScope.launch {
            items.forEach { repository.deleteItem(it) }
        }
    }

    fun consumeItem(item: InventoryItem, amount: Int) {
        viewModelScope.launch {
            if (item.quantity >= amount) {
                val updatedItem = item.copy(quantity = item.quantity - amount)
                if (updatedItem.quantity == 0) {
                    repository.deleteItem(item)
                } else {
                    repository.updateItem(updatedItem)
                }
            }
        }
    }

    fun updateItem(item: InventoryItem) {
        viewModelScope.launch {
            repository.updateItem(item)
        }
    }

    fun addCategory(categoryName: String) {
        viewModelScope.launch {
            repository.insertCategory(Category(name = categoryName))
        }
    }

    fun addLocation(locationName: String, parentName: String? = null) {
        viewModelScope.launch {
            repository.insertLocation(Location(name = locationName, parentName = parentName))
        }
    }

    fun deleteLocation(location: Location) {
        viewModelScope.launch {
            repository.deleteLocation(location)
        }
    }

    init {
        viewModelScope.launch {
            val categories = repository.getAllCategoriesStream().first()
            if (categories.isEmpty()) {
                val defaultCategories = listOf("Groceries", "Electronics", "Furniture", "Clothing", "Personal Care", "Books", "Other")
                defaultCategories.forEach {
                    repository.insertCategory(Category(it))
                }
            }
        }
        viewModelScope.launch {
            val locations = repository.getAllLocationsStream().first()
            if (locations.isEmpty()) {
                val defaultLocations = listOf("Kitchen", "Living Room", "Bedroom", "Bathroom", "Pantry", "Garage")
                defaultLocations.forEach {
                    repository.insertLocation(Location(it))
                }
            }
        }
    }

    companion object {
        private const val TIMEOUT_MILLIS = 5_000L
    }
}

data class InventoryUiState(val itemList: List<InventoryItem> = listOf())
