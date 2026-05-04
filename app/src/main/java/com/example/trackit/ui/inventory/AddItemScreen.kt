package com.example.trackit.ui.inventory

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.PreviewView
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.trackit.ui.AppViewModelProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemScreen(
    navigateBack: () -> Unit,
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: InventoryViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val context = LocalContext.current
    
    var name by rememberSaveable { mutableStateOf("") }
    var quantity by rememberSaveable { mutableStateOf("") }
    var location by rememberSaveable { mutableStateOf("") }
    
    val categoryList by viewModel.categories.collectAsStateWithLifecycle()
    var category by rememberSaveable { mutableStateOf("") }
    
    val locationList by viewModel.locations.collectAsStateWithLifecycle()
    val allItems by viewModel.allItems.collectAsStateWithLifecycle()
    
    // Set default category once categories are loaded
    LaunchedEffect(categoryList) {
        if (category.isEmpty() && categoryList.isNotEmpty()) {
            category = categoryList[0].name
        }
    }

    var imageUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var showCamera by rememberSaveable { mutableStateOf(false) }
    
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()
    var expiryDateLabel by rememberSaveable { mutableStateOf("Select Expiry Date") }

    var nameExpanded by rememberSaveable { mutableStateOf(false) }
    var expanded by rememberSaveable { mutableStateOf(false) }
    var locationExpanded by rememberSaveable { mutableStateOf(false) }
    var showAddCategoryDialog by rememberSaveable { mutableStateOf(false) }
    var showAddLocationDialog by rememberSaveable { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showCamera = true
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    showDatePicker = false
                    datePickerState.selectedDateMillis?.let {
                        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        expiryDateLabel = sdf.format(Date(it))
                    }
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showAddCategoryDialog) {
        var newCategoryName by rememberSaveable { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            title = { Text("Add New Category") },
            text = {
                OutlinedTextField(
                    value = newCategoryName,
                    onValueChange = { newCategoryName = it },
                    label = { Text("Category Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newCategoryName.isNotBlank()) {
                            viewModel.addCategory(newCategoryName)
                            category = newCategoryName
                            showAddCategoryDialog = false
                        }
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategoryDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showAddLocationDialog) {
        var newLocationName by rememberSaveable { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddLocationDialog = false },
            title = { Text("Add New Location") },
            text = {
                OutlinedTextField(
                    value = newLocationName,
                    onValueChange = { newLocationName = it },
                    label = { Text("Location Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newLocationName.isNotBlank()) {
                            viewModel.addLocation(newLocationName)
                            location = newLocationName
                            showAddLocationDialog = false
                        }
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddLocationDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Item") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (showCamera) {
            CameraView(
                onImageCaptured = { uri ->
                    imageUri = uri
                    showCamera = false
                },
                onClose = { showCamera = false },
                onError = { /* Handle error */ },
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
                        Button(onClick = {
                            val permissionCheckResult = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                            if (permissionCheckResult == PackageManager.PERMISSION_GRANTED) {
                                showCamera = true
                            } else {
                                permissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        }) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Take Photo")
                        }
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = nameExpanded,
                    onExpandedChange = { nameExpanded = !nameExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            nameExpanded = it.isNotBlank()
                        },
                        label = { Text("Item Name") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryEditable),
                        singleLine = true
                    )

                    val filteredItems = allItems.filter {
                        it.name.contains(name, ignoreCase = true)
                    }.distinctBy { it.name }

                    if (filteredItems.isNotEmpty() && name.isNotBlank()) {
                        ExposedDropdownMenu(
                            expanded = nameExpanded,
                            onDismissRequest = { nameExpanded = false }
                        ) {
                            filteredItems.forEach { item ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(item.name)
                                            item.category?.let {
                                                Text(
                                                    text = it,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        name = item.name
                                        item.category?.let { category = it }
                                        nameExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = quantity,
                    onValueChange = { if (it.all { char -> char.isDigit() }) quantity = it },
                    label = { Text("Quantity") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = quantity.isNotEmpty() && quantity.toIntOrNull() == null,
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = category,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Category") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            categoryList.forEach { categoryItem ->
                                DropdownMenuItem(
                                    text = { Text(categoryItem.name) },
                                    onClick = {
                                        category = categoryItem.name
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                    
                    IconButton(
                        onClick = { showAddCategoryDialog = true },
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Category")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ExposedDropdownMenuBox(
                        expanded = locationExpanded,
                        onExpandedChange = { locationExpanded = !locationExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = location,
                            onValueChange = { location = it },
                            label = { Text("Location") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = locationExpanded) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable).fillMaxWidth(),
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                        )
                        
                        val filteredOptions = locationList.filter { 
                            it.name.contains(location, ignoreCase = true) 
                        }
                        
                        if (filteredOptions.isNotEmpty()) {
                            ExposedDropdownMenu(
                                expanded = locationExpanded,
                                onDismissRequest = { locationExpanded = false }
                            ) {
                                filteredOptions.forEach { locationItem ->
                                    DropdownMenuItem(
                                        text = { Text(locationItem.name) },
                                        onClick = {
                                            location = locationItem.name
                                            locationExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    
                    IconButton(
                        onClick = { showAddLocationDialog = true },
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Location")
                    }
                }

                OutlinedTextField(
                    value = expiryDateLabel,
                    onValueChange = { },
                    label = { Text("Expiry Date") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true },
                    enabled = true,
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.CalendarToday, contentDescription = "Select Date")
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
                        viewModel.addItem(
                            name = name,
                            quantity = quantity.toIntOrNull() ?: 0,
                            imagePath = imageUri?.toString(),
                            location = location.ifBlank { null },
                            category = category,
                            expiryDate = datePickerState.selectedDateMillis
                        )
                        navigateBack()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = name.isNotBlank() && quantity.isNotBlank()
                ) {
                    Text("Save Item")
                }
            }
        }
    }
}

@Composable
fun CameraView(
    onImageCaptured: (Uri) -> Unit,
    onClose: () -> Unit,
    onError: (ImageCaptureException) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { androidx.camera.lifecycle.ProcessCameraProvider.getInstance(context) }
    val imageCapture = remember { ImageCapture.Builder().build() }
    val preview = androidx.camera.core.Preview.Builder().build()
    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            ) {
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx).apply {
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                        }
                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    cameraSelector,
                                    preview,
                                    imageCapture
                                )
                                preview.surfaceProvider = previewView.surfaceProvider
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }, ContextCompat.getMainExecutor(ctx))
                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            IconButton(
                onClick = {
                    takePhoto(
                        imageCapture = imageCapture,
                        context = context,
                        executor = ContextCompat.getMainExecutor(context),
                        onImageCaptured = onImageCaptured,
                        onError = onError
                    )
                },
                modifier = Modifier.size(72.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoCamera,
                    contentDescription = "Take Photo",
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        IconButton(
            onClick = onClose,
            modifier = Modifier.align(Alignment.TopStart).padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

private fun takePhoto(
    imageCapture: ImageCapture,
    context: Context,
    executor: Executor,
    onImageCaptured: (Uri) -> Unit,
    onError: (ImageCaptureException) -> Unit
) {
    val outputDirectory = context.cacheDir
    val photoFile = File(
        outputDirectory,
        SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US).format(System.currentTimeMillis()) + ".jpg"
    )

    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

    imageCapture.takePicture(
        outputOptions,
        executor,
        object : ImageCapture.OnImageSavedCallback {
            override fun onError(exception: ImageCaptureException) {
                onError(exception)
            }

            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                val savedUri = Uri.fromFile(photoFile)
                onImageCaptured(savedUri)
            }
        }
    )
}
