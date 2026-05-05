package com.example.trackit.ui.inventory

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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.trackit.R
import com.example.trackit.data.Location
import com.example.trackit.ui.AppViewModelProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationScreen(
    modifier: Modifier = Modifier,
    viewModel: InventoryViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val locationList by viewModel.locations.collectAsStateWithLifecycle()
    var showAddLocationDialog by rememberSaveable { mutableStateOf(value = false) }
    var preselectedParentForAdd by rememberSaveable { mutableStateOf<String?>(null) }
    var locationToDelete by remember { mutableStateOf<Location?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.locations_title)) },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    preselectedParentForAdd = null
                    showAddLocationDialog = true
                },
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_location))
            }
        }
    ) { innerPadding ->
        LocationList(
            locations = locationList,
            onAddSublocation = { parent ->
                preselectedParentForAdd = parent.name
                showAddLocationDialog = true
            },
            onDeleteLocation = { location ->
                locationToDelete = location
            },
            modifier = modifier.padding(innerPadding)
        )

        if (locationToDelete != null) {
            AlertDialog(
                onDismissRequest = { locationToDelete = null },
                title = { Text(stringResource(R.string.delete)) },
                text = { Text(stringResource(R.string.delete_location_confirm, locationToDelete?.name ?: "")) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            locationToDelete?.let { viewModel.deleteLocation(it) }
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

        if (showAddLocationDialog) {
            var newLocationName by rememberSaveable { mutableStateOf("") }
            var selectedParent by rememberSaveable { mutableStateOf(preselectedParentForAdd) }
            var parentExpanded by rememberSaveable { mutableStateOf(value = false) }

            AlertDialog(
                onDismissRequest = { 
                    showAddLocationDialog = false
                },
                title = { Text(stringResource(R.string.add_location)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = newLocationName,
                            onValueChange = { newLocationName = it },
                            label = { Text(stringResource(R.string.location)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        ExposedDropdownMenuBox(
                            expanded = parentExpanded,
                            onExpandedChange = { parentExpanded = !parentExpanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = selectedParent ?: stringResource(R.string.none_main_location),
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(stringResource(R.string.parent_location)) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = parentExpanded) },
                                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                            )
                            ExposedDropdownMenu(
                                expanded = parentExpanded,
                                onDismissRequest = { parentExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.none_main_location)) },
                                    onClick = {
                                        selectedParent = null
                                        parentExpanded = false
                                    }
                                )
                                locationList.filter { it.parentName == null }.forEach { locationItem ->
                                    DropdownMenuItem(
                                        text = { Text(locationItem.name) },
                                        onClick = {
                                            selectedParent = locationItem.name
                                            parentExpanded = false
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
                                viewModel.addLocation(newLocationName, selectedParent)
                                showAddLocationDialog = false
                            }
                        }
                    ) {
                        Text(stringResource(R.string.add))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddLocationDialog = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }
    }
}

@Composable
fun LocationList(
    locations: List<Location>,
    onAddSublocation: (Location) -> Unit,
    onDeleteLocation: (Location) -> Unit,
    modifier: Modifier = Modifier,
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
                            expandedLocations = if (isExpanded) {
                                expandedLocations - root.name
                            } else {
                                expandedLocations + root.name
                            }
                        },
                        onAddClick = { onAddSublocation(root) }
                    )
                }
                
                if (isExpanded) {
                    items(subs, key = { it.name }) { sub ->
                        LocationItem(
                            location = sub,
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
                .padding(start = 16.dp, top = 4.dp, bottom = 4.dp, end = 8.dp)
                .fillMaxWidth()
                .heightIn(min = 56.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (location.parentName == null) Icons.Default.LocationOn else Icons.Default.SubdirectoryArrowRight,
                contentDescription = null,
                tint = if (location.parentName == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.width(16.dp))
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
