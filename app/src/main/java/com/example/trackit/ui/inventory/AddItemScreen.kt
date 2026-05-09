package com.example.trackit.ui.inventory

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.trackit.R
import com.example.trackit.data.Category
import com.example.trackit.data.InventoryItem
import com.example.trackit.data.Location
import com.example.trackit.ui.AppViewModelProvider
import com.example.trackit.ui.components.CameraView
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemScreen(
    navigateBack: () -> Unit,
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: InventoryViewModel? = null,
) {
    if (LocalInspectionMode.current && viewModel == null) {
        AddItemContent(
            categoryList = emptyList(),
            locationList = emptyList(),
            allItems = emptyList(),
            onAddItem = { _, _, _, _, _, _ -> },
            onAddCategory = {},
            onAddLocation = {},
            navigateBack = navigateBack,
            onNavigateUp = onNavigateUp,
            modifier = modifier
        )
    } else {
        val actualViewModel: InventoryViewModel = viewModel ?: viewModel(factory = AppViewModelProvider.Factory)
        val categoryList by actualViewModel.categories.collectAsStateWithLifecycle()
        val locationList by actualViewModel.locations.collectAsStateWithLifecycle()
        val allItems by actualViewModel.allItems.collectAsStateWithLifecycle()

        AddItemContent(
            categoryList = categoryList,
            locationList = locationList,
            allItems = allItems,
            onAddItem = { name, qty, path, loc, cat, date ->
                actualViewModel.addItem(name, qty, path, loc, cat, date)
            },
            onAddCategory = { actualViewModel.addCategory(it) },
            onAddLocation = { actualViewModel.addLocation(it) },
            navigateBack = navigateBack,
            onNavigateUp = onNavigateUp,
            modifier = modifier
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemContent(
    categoryList: List<Category>,
    locationList: List<Location>,
    allItems: List<InventoryItem>,
    onAddItem: (String, Int, String?, String?, String?, Long?) -> Unit,
    onAddCategory: (String) -> Unit,
    onAddLocation: (String) -> Unit,
    navigateBack: () -> Unit,
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    
    var name by rememberSaveable { mutableStateOf("") }
    var quantity by rememberSaveable { mutableStateOf("") }
    var location by rememberSaveable { mutableStateOf("") }
    
    var category by rememberSaveable { mutableStateOf("") }
    
    // Set default category once categories are loaded
    LaunchedEffect(categoryList) {
        if (category.isEmpty() && categoryList.isNotEmpty()) {
            category = categoryList[0].name
        }
    }

    var imageUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    val showCameraState = rememberSaveable { mutableStateOf(value = false) }
    
    val showDatePickerState = rememberSaveable { mutableStateOf(value = false) }
    val datePickerState = rememberDatePickerState()
    var expiryDateLabel by rememberSaveable { mutableStateOf("") }
    val defaultExpiryLabel = stringResource(R.string.select_date)

    val nameExpandedState = rememberSaveable { mutableStateOf(value = false) }
    val expandedState = rememberSaveable { mutableStateOf(value = false) }
    val locationExpandedState = rememberSaveable { mutableStateOf(value = false) }
    val showAddCategoryDialogState = rememberSaveable { mutableStateOf(value = false) }
    val showAddLocationDialogState = rememberSaveable { mutableStateOf(value = false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        if (isGranted) {
            showCameraState.value = true
        }
    }

    if (showDatePickerState.value) {
        DatePickerDialog(
            onDismissRequest = { showDatePickerState.value = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDatePickerState.value = false
                        datePickerState.selectedDateMillis?.let {
                            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                            expiryDateLabel = sdf.format(Date(it))
                        }
                    },
                ) {
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

    if (showAddCategoryDialogState.value) {
        var newCategoryName by rememberSaveable { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddCategoryDialogState.value = false },
            title = { Text(stringResource(R.string.add_category)) },
            text = {
                OutlinedTextField(
                    value = newCategoryName,
                    onValueChange = { newCategoryName = it },
                    label = { Text(stringResource(R.string.category)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newCategoryName.isNotBlank()) {
                            onAddCategory(newCategoryName)
                            category = newCategoryName
                            showAddCategoryDialogState.value = false
                        }
                    }
                ) {
                    Text(stringResource(R.string.add))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategoryDialogState.value = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showAddLocationDialogState.value) {
        var newLocationName by rememberSaveable { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddLocationDialogState.value = false },
            title = { Text(stringResource(R.string.add_location)) },
            text = {
                OutlinedTextField(
                    value = newLocationName,
                    onValueChange = { newLocationName = it },
                    label = { Text(stringResource(R.string.location)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newLocationName.isNotBlank()) {
                            onAddLocation(newLocationName)
                            location = newLocationName
                            showAddLocationDialogState.value = false
                        }
                    }
                ) {
                    Text(stringResource(R.string.add))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddLocationDialogState.value = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.add_item_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { innerPadding ->
        if (showCameraState.value) {
            CameraView(
                onImageCaptured = { uri ->
                    imageUri = uri
                    showCameraState.value = false
                },
                onClose = { showCameraState.value = false },
                onError = { showCameraState.value = false },
                modifier = Modifier.padding(innerPadding)
            )
        } else {
            Column(
                modifier = modifier
                    .padding(innerPadding)
                    .padding(16.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (imageUri != null) {
                    AsyncImage(
                        model = imageUri,
                        contentDescription = null,
                        modifier = Modifier
                            .size(200.dp)
                            .padding(8.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Button(
                            onClick = {
                                val permissionCheckResult = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                                if (permissionCheckResult == PackageManager.PERMISSION_GRANTED) {
                                    showCameraState.value = true
                                } else {
                                    permissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            },
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.take_photo))
                        }
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = nameExpandedState.value,
                    onExpandedChange = { nameExpandedState.value = !nameExpandedState.value },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            nameExpandedState.value = it.isNotBlank()
                        },
                        label = { Text(stringResource(R.string.item_name)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryEditable),
                        singleLine = true
                    )

                    val filteredItems = allItems.asSequence().filter {
                        it.name.contains(name, ignoreCase = true)
                    }.distinctBy { it.name }.toList()

                    if (filteredItems.isNotEmpty() && name.isNotBlank()) {
                        ExposedDropdownMenu(
                            expanded = nameExpandedState.value,
                            onDismissRequest = { nameExpandedState.value = false }
                        ) {
                            filteredItems.forEach { item ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(item.name)
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                item.category?.let {
                                                    Text(
                                                        text = it,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                item.location?.let {
                                                    Text(
                                                        text = "• $it",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    },
                                    onClick = {
                                        name = item.name
                                        item.category?.let { category = it }
                                        item.location?.let { location = it }
                                        nameExpandedState.value = false
                                    }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = quantity,
                    onValueChange = { if (it.all { char -> char.isDigit() }) quantity = it },
                    label = { Text(stringResource(R.string.quantity)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = quantity.isNotEmpty() && (quantity.toIntOrNull() == null),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ExposedDropdownMenuBox(
                        expanded = expandedState.value,
                        onExpandedChange = { expandedState.value = !expandedState.value },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = category,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.category)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedState.value) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
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
                    
                    IconButton(
                        onClick = { showAddCategoryDialogState.value = true },
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_category))
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ExposedDropdownMenuBox(
                        expanded = locationExpandedState.value,
                        onExpandedChange = { locationExpandedState.value = !locationExpandedState.value },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = location,
                            onValueChange = { location = it },
                            label = { Text(stringResource(R.string.location)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = locationExpandedState.value) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable).fillMaxWidth(),
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
                    
                    IconButton(
                        onClick = { showAddLocationDialogState.value = true },
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_location))
                    }
                }

                OutlinedTextField(
                    value = expiryDateLabel.ifEmpty { defaultExpiryLabel },
                    onValueChange = { },
                    label = { Text(stringResource(R.string.expiry_date)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePickerState.value = true },
                    enabled = true,
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { showDatePickerState.value = true }) {
                            Icon(Icons.Default.CalendarToday, contentDescription = stringResource(R.string.select_date))
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )

                Button(
                    onClick = {
                        onAddItem(
                            name,
                            quantity.toIntOrNull() ?: 0,
                            imageUri?.toString(),
                            location.ifBlank { null },
                            category,
                            datePickerState.selectedDateMillis
                        )
                        navigateBack()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = name.isNotBlank() && quantity.isNotBlank()
                ) {
                    Text(stringResource(R.string.save_item))
                }
            }
        }
    }
}
