package com.example.trackit.ui.inventory

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SubdirectoryArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import com.example.trackit.ui.TourStep
import com.example.trackit.ui.components.GuidedTourBubble
import coil.compose.AsyncImage
import com.example.trackit.R
import com.example.trackit.data.Location
import com.example.trackit.ui.AppViewModelProvider
import com.example.trackit.ui.components.CameraView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationScreen(
    modifier: Modifier = Modifier,
    viewModel: InventoryViewModel? = null,
    tourStep: TourStep? = null,
    onNextStep: () -> Unit = {},
    onSkipTour: () -> Unit = {}
) {
    if (LocalInspectionMode.current && viewModel == null) {
        LocationList(
            locations = emptyList(),
            onAddSublocation = {},
            onDeleteLocation = {},
            onLocationClick = {},
            modifier = modifier
        )
    } else {
        val actualViewModel: InventoryViewModel = viewModel ?: viewModel(factory = AppViewModelProvider.Factory)
        val context = LocalContext.current
        val locationList by actualViewModel.locations.collectAsStateWithLifecycle()
        var showAddLocationDialog by rememberSaveable { mutableStateOf(value = false) }
        var locationToDelete by remember { mutableStateOf<Location?>(null) }
        var enlargedPhotoUri by remember { mutableStateOf<Uri?>(null) }

        val showCameraState = rememberSaveable { mutableStateOf(value = false) }
        var capturedImageUri by rememberSaveable { mutableStateOf<Uri?>(null) }

        var newLocationName by rememberSaveable { mutableStateOf("") }
        var selectedParent by rememberSaveable { mutableStateOf<String?>(null) }
        val parentExpandedState = rememberSaveable { mutableStateOf(value = false) }

        var listCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
        var fabCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }

        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) { isGranted ->
            if (isGranted) {
                showCameraState.value = true
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.locations_title)) },
                    modifier = Modifier.onGloballyPositioned { if (tourStep == TourStep.LOCATIONS_TAB) listCoordinates = it }
                )
            },
            floatingActionButton = {
                if (!showCameraState.value) {
                    FloatingActionButton(
                        onClick = {
                            capturedImageUri = null
                            newLocationName = ""
                            selectedParent = null
                            showAddLocationDialog = true
                        },
                        modifier = Modifier.onGloballyPositioned { fabCoordinates = it }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_location))
                    }
                }
            }
        ) { innerPadding ->
            if (showCameraState.value) {
                CameraView(
                    onImageCaptured = { uri ->
                        capturedImageUri = uri
                        showCameraState.value = false
                    },
                    onClose = { showCameraState.value = false },
                    onError = { showCameraState.value = false },
                    modifier = Modifier.padding(innerPadding)
                )
            } else {
                LocationList(
                    locations = locationList,
                    onAddSublocation = { parent ->
                        capturedImageUri = null
                        newLocationName = ""
                        selectedParent = parent.name
                        showAddLocationDialog = true
                    },
                    onDeleteLocation = { location ->
                        locationToDelete = location
                    },
                    onLocationClick = { location ->
                        if (!location.imagePath.isNullOrEmpty()) {
                            enlargedPhotoUri = Uri.parse(location.imagePath)
                        }
                    },
                    modifier = modifier
                        .padding(innerPadding)
                        .onGloballyPositioned { if (tourStep == TourStep.SUB_LOCATIONS) listCoordinates = it }
                )
            }

            if (tourStep == TourStep.LOCATIONS_TAB) {
                GuidedTourBubble(
                    text = "Here you can manage all your storage locations.",
                    onNext = onNextStep,
                    onSkip = onSkipTour,
                    targetCoordinates = listCoordinates
                )
            } else if (tourStep == TourStep.SUB_LOCATIONS) {
                GuidedTourBubble(
                    text = "You can add sub-locations (like 'Shelf A' inside 'Garage') to organize things even better!",
                    onNext = onNextStep,
                    onSkip = onSkipTour,
                    targetCoordinates = fabCoordinates,
                    isLastStep = true
                )
            }

            if (enlargedPhotoUri != null) {
                AlertDialog(
                    onDismissRequest = { enlargedPhotoUri = null },
                    title = null,
                    text = {
                        AsyncImage(
                            model = enlargedPhotoUri,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(MaterialTheme.shapes.medium),
                            contentScale = ContentScale.Fit
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = { enlargedPhotoUri = null }) {
                            Text(stringResource(android.R.string.ok))
                        }
                    }
                )
            }

            if (locationToDelete != null) {
                AlertDialog(
                    onDismissRequest = { locationToDelete = null },
                    title = { Text(stringResource(R.string.delete)) },
                    text = { Text(stringResource(R.string.delete_location_confirm, locationToDelete?.name ?: "")) },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                locationToDelete?.let { actualViewModel.deleteLocation(it) }
                                locationToDelete = null
                            }
                        ) {
                            Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { locationToDelete = null }) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                )
            }

            if (showAddLocationDialog && !showCameraState.value) {
                AlertDialog(
                    onDismissRequest = { 
                        showAddLocationDialog = false
                    },
                    title = { Text(stringResource(R.string.add_location)) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (capturedImageUri != null) {
                                Box(
                                    modifier = Modifier
                                        .size(100.dp)
                                        .align(Alignment.CenterHorizontally)
                                        .padding(bottom = 8.dp)
                                ) {
                                    AsyncImage(
                                        model = capturedImageUri,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize().clip(MaterialTheme.shapes.small),
                                        contentScale = ContentScale.Crop
                                    )
                                    IconButton(
                                        onClick = { capturedImageUri = null },
                                        modifier = Modifier.align(Alignment.TopEnd).size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Remove,
                                            contentDescription = "Clear Photo",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            } else {
                                Button(
                                    onClick = {
                                        val permissionCheckResult = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                                        if (permissionCheckResult == PackageManager.PERMISSION_GRANTED) {
                                            showCameraState.value = true
                                        } else {
                                            permissionLauncher.launch(Manifest.permission.CAMERA)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.PhotoCamera, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text(stringResource(R.string.take_photo))
                                }
                            }

                            OutlinedTextField(
                                value = newLocationName,
                                onValueChange = { newLocationName = it },
                                label = { Text(stringResource(R.string.location)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            ExposedDropdownMenuBox(
                                expanded = parentExpandedState.value,
                                onExpandedChange = { parentExpandedState.value = !parentExpandedState.value },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = selectedParent ?: stringResource(R.string.none_main_location),
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text(stringResource(R.string.parent_location)) },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = parentExpandedState.value) },
                                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                                )
                                ExposedDropdownMenu(
                                    expanded = parentExpandedState.value,
                                    onDismissRequest = { parentExpandedState.value = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.none_main_location)) },
                                        onClick = {
                                            selectedParent = null
                                            parentExpandedState.value = false
                                        }
                                    )
                                    locationList.filter { it.parentName == null }.forEach { locationItem ->
                                        DropdownMenuItem(
                                            text = { Text(locationItem.name) },
                                            onClick = {
                                                selectedParent = locationItem.name
                                                parentExpandedState.value = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                if (newLocationName.isNotBlank()) {
                                    actualViewModel.addLocation(newLocationName, selectedParent, capturedImageUri?.toString())
                                    showAddLocationDialog = false
                                    capturedImageUri = null
                                    newLocationName = ""
                                    selectedParent = null
                                }
                            }
                        ) {
                            Text(stringResource(R.string.add))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { 
                            showAddLocationDialog = false 
                            capturedImageUri = null
                            newLocationName = ""
                            selectedParent = null
                        }) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun LocationList(
    modifier: Modifier = Modifier,
    locations: List<Location>,
    onAddSublocation: (Location) -> Unit,
    onDeleteLocation: (Location) -> Unit,
    onLocationClick: (Location) -> Unit,
) {
    if (locations.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.no_locations), style = MaterialTheme.typography.bodyLarge)
        }
    } else {
        val rootLocations = remember(locations) { locations.filter { it.parentName == null } }
        val subLocationsMap = remember(locations) {
            locations.asSequence().filter { it.parentName != null }.groupBy { it.parentName }
        }
        var expandedLocations by rememberSaveable { mutableStateOf(setOf<String>()) }

        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            rootLocations.forEach { root ->
                val isExpanded = expandedLocations.contains(root.name)
                val subs = subLocationsMap[root.name] ?: emptyList()
                val hasSublocations = subs.isNotEmpty()

                item(key = root.name) {
                    LocationItem(
                        location = root,
                        isExpanded = isExpanded,
                        hasSublocations = hasSublocations,
                        onClick = {
                            if (hasSublocations) {
                                expandedLocations = if (isExpanded) {
                                    expandedLocations - root.name
                                } else {
                                    expandedLocations + root.name
                                }
                            } else {
                                onLocationClick(root)
                            }
                        },
                        onPhotoClick = { onLocationClick(root) },
                        onAddClick = { onAddSublocation(root) }
                    )
                }
                
                if (isExpanded) {
                    items(subs, key = { it.name }) { sub ->
                        LocationItem(
                            location = sub,
                            onClick = { onLocationClick(sub) },
                            onPhotoClick = { onLocationClick(sub) },
                            onDeleteClick = { onDeleteLocation(sub) },
                            modifier = Modifier.padding(start = 32.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LocationItem(
    location: Location,
    modifier: Modifier = Modifier,
    isExpanded: Boolean = false,
    hasSublocations: Boolean = false,
    onClick: () -> Unit = {},
    onPhotoClick: () -> Unit = {},
    onAddClick: (() -> Unit)? = null,
    onDeleteClick: (() -> Unit)? = null,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 8.dp)
                .fillMaxWidth()
                .heightIn(min = 56.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (location.parentName != null) {
                Icon(
                    imageVector = Icons.Default.SubdirectoryArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }

            if (!location.imagePath.isNullOrEmpty()) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .padding(end = 8.dp)
                        .clickable(onClick = onPhotoClick)
                ) {
                    AsyncImage(
                        model = Uri.parse(location.imagePath),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().clip(MaterialTheme.shapes.extraSmall),
                        contentScale = ContentScale.Crop
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = if (location.parentName == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    modifier = Modifier
                        .size(if (location.parentName == null) 24.dp else 16.dp)
                        .padding(end = if (location.parentName == null) 16.dp else 8.dp)
                )
            }

            Text(
                text = location.name,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )

            if (location.parentName == null) {
                IconButton(onClick = { onAddClick?.invoke() }) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_location))
                }
                if (hasSublocations) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand"
                    )
                } else {
                    Spacer(modifier = Modifier.width(24.dp))
                }
            } else {
                IconButton(onClick = { onDeleteClick?.invoke() }) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = stringResource(R.string.delete),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
