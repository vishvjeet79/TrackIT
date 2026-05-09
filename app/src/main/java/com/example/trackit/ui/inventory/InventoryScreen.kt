package com.example.trackit.ui.inventory

import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import com.example.trackit.ui.TourStep
import com.example.trackit.ui.components.GuidedTourBubble
import coil.compose.AsyncImage
import com.example.trackit.R
import com.example.trackit.data.Category
import com.example.trackit.data.InventoryItem
import com.example.trackit.data.Location
import com.example.trackit.ui.AppViewModelProvider
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    onNavigateToAddItem: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: InventoryViewModel? = null,
    tourStep: TourStep? = null,
    onNextStep: () -> Unit = {},
    onSkipTour: () -> Unit = {}
) {
    if (LocalInspectionMode.current && viewModel == null) {
        InventoryScreenContent(
            uiState = InventoryUiState(),
            searchQuery = "",
            categoryList = emptyList(),
            locationList = emptyList(),
            onSearchQueryChange = {},
            onDeleteItem = {},
            onDeleteItems = {},
            onConsumeItem = { _, _ -> },
            onUpdateItem = {},
            onNavigateToAddItem = onNavigateToAddItem,
            modifier = modifier,
        )
    } else {
        val actualViewModel: InventoryViewModel = viewModel ?: viewModel(factory = AppViewModelProvider.Factory)
        val uiState by actualViewModel.inventoryUiState.collectAsStateWithLifecycle()
        val searchQuery by actualViewModel.searchQuery.collectAsStateWithLifecycle()
        val categoryList by actualViewModel.categories.collectAsStateWithLifecycle()
        val locationList by actualViewModel.locations.collectAsStateWithLifecycle()

        InventoryScreenContent(
            uiState = uiState,
            searchQuery = searchQuery,
            categoryList = categoryList,
            locationList = locationList,
            onSearchQueryChange = actualViewModel::onSearchQueryChange,
            onDeleteItem = actualViewModel::deleteItem,
            onDeleteItems = actualViewModel::deleteItems,
            onConsumeItem = actualViewModel::consumeItem,
            onUpdateItem = actualViewModel::updateItem,
            onNavigateToAddItem = onNavigateToAddItem,
            tourStep = tourStep,
            onNextStep = onNextStep,
            onSkipTour = onSkipTour,
            modifier = modifier,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreenContent(
    uiState: InventoryUiState,
    searchQuery: String,
    categoryList: List<Category>,
    locationList: List<Location>,
    onSearchQueryChange: (String) -> Unit,
    onDeleteItem: (InventoryItem) -> Unit,
    onDeleteItems: (List<InventoryItem>) -> Unit,
    onConsumeItem: (InventoryItem, Int) -> Unit,
    onUpdateItem: (InventoryItem) -> Unit,
    onNavigateToAddItem: () -> Unit,
    tourStep: TourStep? = null,
    onNextStep: () -> Unit = {},
    onSkipTour: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isSearchActiveState = rememberSaveable { mutableStateOf(value = false) }

    val itemToConsumeIdState = rememberSaveable { mutableStateOf<Int?>(null) }
    val itemToEditIdState = rememberSaveable { mutableStateOf<Int?>(null) }
    
    var selectedItems by rememberSaveable { mutableStateOf(setOf<Int>()) }
    val isInSelectionMode by remember { derivedStateOf { selectedItems.isNotEmpty() } }

    var fabCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var bodyCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }

    Scaffold(
        topBar = {
            if (isSearchActiveState.value) {
                SearchTopBar(
                    searchQuery = searchQuery,
                    onSearchQueryChange = onSearchQueryChange,
                ) {
                    onSearchQueryChange("")
                    isSearchActiveState.value = false
                }
            } else {
                TopAppBar(
                    title = {
                        if (isInSelectionMode) {
                            Text(stringResource(R.string.selected_count, selectedItems.size))
                        } else {
                            Text(stringResource(R.string.inventory_title))
                        }
                    },
                    navigationIcon = {
                        if (isInSelectionMode) {
                            IconButton(onClick = { selectedItems = emptySet() }) {
                                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.clear_selection))
                            }
                        }
                    },
                    actions = {
                        if (isInSelectionMode) {
                            IconButton(onClick = {
                                val itemsToDelete = uiState.itemList.filter { it.id in selectedItems }
                                onDeleteItems(itemsToDelete)
                                selectedItems = emptySet()
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete_selected))
                            }
                        } else {
                            IconButton(onClick = { isSearchActiveState.value = true }) {
                                Icon(Icons.Default.Search, contentDescription = stringResource(R.string.search))
                            }
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            if (!isInSelectionMode) {
                FloatingActionButton(
                    onClick = onNavigateToAddItem,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .padding(20.dp)
                        .onGloballyPositioned { fabCoordinates = it }
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.add_item_title)
                    )
                }
            }
        }
    ) { innerPadding ->
        InventoryBody(
            itemList = uiState.itemList,
            selectedItems = selectedItems,
            onItemClick = { item ->
                if (isInSelectionMode) {
                    selectedItems = if (item.id in selectedItems) {
                        selectedItems - item.id
                    } else {
                        selectedItems + item.id
                    }
                }
            },
            onItemLongClick = { item ->
                if (!isInSelectionMode) {
                    selectedItems = setOf(item.id)
                }
            },
            onDelete = { onDeleteItem(it) },
            onConsume = { itemToConsumeIdState.value = it.id },
            onEdit = { itemToEditIdState.value = it.id },
            modifier = modifier
                .padding(innerPadding)
                .onGloballyPositioned { bodyCoordinates = it }
        )

        if (tourStep == TourStep.INVENTORY_OVERVIEW) {
            GuidedTourBubble(
                text = "Welcome to your Inventory! All your tracked items will appear here.",
                onNext = onNextStep,
                onSkip = onSkipTour,
                targetCoordinates = bodyCoordinates
            )
        } else if (tourStep == TourStep.ADD_ITEM) {
            GuidedTourBubble(
                text = "Tap here to add a new item to your collection.",
                onNext = onNextStep,
                onSkip = onSkipTour,
                targetCoordinates = fabCoordinates
            )
        }

        itemToConsumeIdState.value?.let { id ->
            uiState.itemList.find { it.id == id }?.let { item ->
                ConsumeDialog(
                    item = item,
                    onDismiss = { itemToConsumeIdState.value = null },
                ) { amount ->
                    onConsumeItem(item, amount)
                    itemToConsumeIdState.value = null
                }
            } ?: run { itemToConsumeIdState.value = null }
        }

        itemToEditIdState.value?.let { id ->
            uiState.itemList.find { it.id == id }?.let { item ->
                EditItemDialog(
                    item = item,
                    onDismiss = { itemToEditIdState.value = null },
                    onConfirm = { updatedItem ->
                        onUpdateItem(updatedItem)
                        itemToEditIdState.value = null
                    },
                    categoryList = categoryList,
                    locationList = locationList
                )
            } ?: run { itemToEditIdState.value = null }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchTopBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onCloseClick: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        delay(100)
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    TopAppBar(
        title = {
            TextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text(stringResource(R.string.search_placeholder)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Search
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                )
            )
        },
        navigationIcon = {
            IconButton(onClick = onCloseClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
            }
        },
        actions = {
            if (searchQuery.isNotEmpty()) {
                IconButton(onClick = { onSearchQueryChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.clear))
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditItemDialog(
    item: InventoryItem,
    onDismiss: () -> Unit,
    onConfirm: (InventoryItem) -> Unit,
    categoryList: List<Category>,
    locationList: List<Location>
) {
    var name by rememberSaveable { mutableStateOf(item.name) }
    var location by rememberSaveable { mutableStateOf(item.location ?: "") }
    var category by rememberSaveable { mutableStateOf(item.category ?: "") }
    var quantity by rememberSaveable { mutableStateOf(item.quantity.toString()) }
    var expiryDate by rememberSaveable { mutableStateOf(item.expiryDate) }
    
    val expandedState = rememberSaveable { mutableStateOf(value = false) }
    val locationExpandedState = rememberSaveable { mutableStateOf(value = false) }
    
    val showDatePickerState = rememberSaveable { mutableStateOf(value = false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = item.expiryDate)

    if (showDatePickerState.value) {
        DatePickerDialog(
            onDismissRequest = { showDatePickerState.value = false },
            confirmButton = {
                TextButton(onClick = {
                    expiryDate = datePickerState.selectedDateMillis
                    showDatePickerState.value = false
                }) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerState.value = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_item_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.item_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { if (it.all { char -> char.isDigit() }) quantity = it },
                    label = { Text(stringResource(R.string.quantity)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                
                ExposedDropdownMenuBox(
                    expanded = expandedState.value,
                    onExpandedChange = { expandedState.value = !expandedState.value },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.category)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedState.value) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth(),
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedState.value,
                        onDismissRequest = { expandedState.value = false }
                    ) {
                        categoryList.forEach { categoryItem ->
                            DropdownMenuItem(
                                text = { Text(categoryItem.name) },
                                onClick = {
                                    category = categoryItem.name
                                    expandedState.value = false
                                }
                            )
                        }
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = locationExpandedState.value,
                    onExpandedChange = { locationExpandedState.value = !locationExpandedState.value },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text(stringResource(R.string.location)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = locationExpandedState.value) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryEditable)
                            .fillMaxWidth(),
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                    )
                    
                    val filteredOptions = locationList.filter { 
                        it.name.contains(location, ignoreCase = true) 
                    }
                    
                    if (filteredOptions.isNotEmpty()) {
                        ExposedDropdownMenu(
                            expanded = locationExpandedState.value,
                            onDismissRequest = { locationExpandedState.value = false }
                        ) {
                            filteredOptions.forEach { locationItem ->
                                DropdownMenuItem(
                                    text = { Text(locationItem.name) },
                                    onClick = {
                                        location = locationItem.name
                                        locationExpandedState.value = false
                                    }
                                )
                            }
                        }
                    }
                }
                
                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val dateText = expiryDate?.let { sdf.format(Date(it)) } ?: stringResource(R.string.no_expiry)
                
                OutlinedTextField(
                    value = dateText,
                    onValueChange = { },
                    label = { Text(stringResource(R.string.expiry_date)) },
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePickerState.value = true },
                    enabled = true,
                    trailingIcon = {
                        IconButton(onClick = { showDatePickerState.value = true }) {
                            Icon(Icons.Default.CalendarToday, contentDescription = stringResource(R.string.select_date))
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                    )
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(
                            item.copy(
                                name = name,
                                quantity = quantity.toIntOrNull() ?: item.quantity,
                                location = location.ifBlank { null },
                                category = category.ifBlank { null },
                                expiryDate = expiryDate
                            )
                        )
                    }
                }
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun ConsumeDialog(
    item: InventoryItem,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var consumeAmount by rememberSaveable { mutableStateOf("1") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.consume_title, item.name)) },
        text = {
            Column {
                Text(stringResource(R.string.available_quantity, item.quantity))
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = consumeAmount,
                    onValueChange = { if (it.all { char -> char.isDigit() }) consumeAmount = it },
                    label = { Text(stringResource(R.string.amount_to_consume)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = consumeAmount.isNotEmpty() && ((consumeAmount.toIntOrNull() ?: 0) > item.quantity),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val amount = consumeAmount.toIntOrNull() ?: 0
                    if ((amount > 0) && (amount <= item.quantity)) {
                        onConfirm(amount)
                    }
                }
            ) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun InventoryBody(
    itemList: List<InventoryItem>,
    selectedItems: Set<Int>,
    onItemClick: (InventoryItem) -> Unit,
    onItemLongClick: (InventoryItem) -> Unit,
    onDelete: (InventoryItem) -> Unit,
    onConsume: (InventoryItem) -> Unit,
    onEdit: (InventoryItem) -> Unit,
    modifier: Modifier = Modifier
) {
    if (itemList.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(R.string.no_items),
                style = MaterialTheme.typography.titleLarge
            )
        }
    } else {
        LazyColumn(modifier = modifier.fillMaxSize()) {
            items(items = itemList, key = { it.id }) { item ->
                InventoryItemCard(
                    item = item,
                    isSelected = item.id in selectedItems,
                    onClick = { onItemClick(item) },
                    onLongClick = { onItemLongClick(item) },
                    onDelete = { onDelete(item) },
                    onConsume = { onConsume(item) },
                    onEdit = { onEdit(item) },
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun InventoryItemCard(
    item: InventoryItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDelete: () -> Unit,
    onConsume: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = if (isSelected) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            item.imagePath?.let {
                AsyncImage(
                    model = it,
                    contentDescription = null,
                    modifier = Modifier
                        .size(80.dp)
                        .padding(end = 16.dp),
                    contentScale = ContentScale.Crop
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = item.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = stringResource(R.string.quantity) + ": ${item.quantity}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
                
                item.location?.let {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                item.category?.let {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Category,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
                
                item.expiryDate?.let {
                    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Timer,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Expires: ${sdf.format(Date(it))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            
            Column {
                IconButton(onClick = onEdit) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit")
                }
                IconButton(onClick = onConsume) {
                    Icon(imageVector = Icons.Default.RemoveCircleOutline, contentDescription = "Consume")
                }
                IconButton(onClick = onDelete) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete")
                }
            }
        }
    }
}
