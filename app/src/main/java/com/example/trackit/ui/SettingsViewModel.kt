package com.example.trackit.ui

import android.content.Context
import android.net.Uri
import android.net.nsd.NsdServiceInfo
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trackit.data.InventoryExport
import com.example.trackit.data.InventoryItem
import com.example.trackit.data.InventoryRepository
import com.example.trackit.data.LocalSyncManager
import com.example.trackit.data.Location
import com.example.trackit.data.UserPreferencesRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class SettingsViewModel(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val inventoryRepository: InventoryRepository,
    private val localSyncManager: LocalSyncManager
) : ViewModel() {

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()
    private val exportAdapter = moshi.adapter(InventoryExport::class.java)

    private val _discoveredPeers = MutableStateFlow<List<NsdServiceInfo>>(emptyList())
    val discoveredPeers = _discoveredPeers.asStateFlow()

    private val _isHosting = MutableStateFlow(false)
    val isHosting = _isHosting.asStateFlow()

    fun startHosting(context: Context, includeImages: Boolean, onError: (Exception) -> Unit) {
        _isHosting.value = true
        localSyncManager.startServer { outputStream ->
            exportData(
                outputStream,
                includeItemImages = includeImages,
                includeLocationImages = includeImages,
                context = context,
                onSuccess = {},
                onError = { onError(it) }
            )
        }
    }

    fun stopHosting() {
        _isHosting.value = false
        localSyncManager.stopServer()
    }

    fun startDiscovery() {
        viewModelScope.launch {
            localSyncManager.discoverServices().collect {
                _discoveredPeers.value = it
            }
        }
    }

    fun syncWithPeer(
        peer: NsdServiceInfo,
        clearExisting: Boolean,
        context: Context,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        viewModelScope.launch {
            try {
                localSyncManager.receiveData(peer) { inputStream ->
                    importData(inputStream, clearExisting, context, onSuccess, onError)
                }
            } catch (e: Exception) {
                onError(e)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        localSyncManager.stopServer()
    }
    val isDarkModeEnabled: StateFlow<Boolean> = userPreferencesRepository.isDarkModeEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = true
        )

    val isFirstLaunch: StateFlow<Boolean> = userPreferencesRepository.isFirstLaunch
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = false
        )

    fun toggleDarkMode(isEnabled: Boolean) {
        if (isDarkModeEnabled.value != isEnabled) {
            viewModelScope.launch {
                userPreferencesRepository.saveDarkModePreference(isEnabled)
            }
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            userPreferencesRepository.setFirstLaunchCompleted()
        }
    }

    fun exportData(
        outputStream: OutputStream,
        includeItemImages: Boolean,
        includeLocationImages: Boolean,
        context: Context,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val items = inventoryRepository.getAllItems()
                val categories = inventoryRepository.getAllCategories()
                val locations = inventoryRepository.getAllLocations()
                val export = InventoryExport(items, categories, locations)
                val json = exportAdapter.toJson(export)

                withContext(Dispatchers.IO) {
                    ZipOutputStream(outputStream).use { zipOut ->
                        // Add JSON
                        zipOut.putNextEntry(ZipEntry("inventory.json"))
                        zipOut.write(json.toByteArray())
                        zipOut.closeEntry()

                        // Add Images
                        if (includeItemImages) {
                            items.forEach { item ->
                                item.imagePath?.let { path ->
                                    addFileToZip(zipOut, path, "images/items/", context)
                                }
                            }
                        }
                        if (includeLocationImages) {
                            locations.forEach { location ->
                                location.imagePath?.let { path ->
                                    addFileToZip(zipOut, path, "images/locations/", context)
                                }
                            }
                        }
                    }
                }
                onSuccess()
            } catch (e: Exception) {
                onError(e)
            }
        }
    }

    private fun addFileToZip(zipOut: ZipOutputStream, path: String, zipDir: String, context: Context) {
        try {
            val uri = Uri.parse(path)
            val fileName = uri.lastPathSegment ?: return
            context.contentResolver.openInputStream(uri)?.use { input ->
                zipOut.putNextEntry(ZipEntry(zipDir + fileName))
                input.copyTo(zipOut)
                zipOut.closeEntry()
            }
        } catch (e: Exception) {
            // Skip files that can't be found
        }
    }

    fun importData(
        inputStream: InputStream,
        clearExisting: Boolean,
        context: Context,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        viewModelScope.launch {
            try {
                if (clearExisting) {
                    inventoryRepository.deleteAllItems()
                }
                withContext(Dispatchers.IO) {
                    val imagesDir = File(context.filesDir, "imported_images").apply { mkdirs() }
                    var inventoryJson: String? = null
                    
                    ZipInputStream(inputStream).use { zipIn ->
                        var entry = zipIn.nextEntry
                        while (entry != null) {
                            if (entry.name == "inventory.json") {
                                inventoryJson = zipIn.bufferedReader().readText()
                            } else if (entry.name.startsWith("images/")) {
                                val destFile = File(imagesDir, entry.name.substringAfterLast("/"))
                                destFile.outputStream().use { zipIn.copyTo(it) }
                            }
                            zipIn.closeEntry()
                            entry = zipIn.nextEntry
                        }
                    }

                    if (inventoryJson != null) {
                        val export = exportAdapter.fromJson(inventoryJson!!)
                        if (export != null) {
                            inventoryRepository.insertCategories(export.categories)
                            inventoryRepository.insertLocations(export.locations.map { loc ->
                                if (loc.imagePath != null) {
                                    val fileName = Uri.parse(loc.imagePath).lastPathSegment
                                    val newFile = File(imagesDir, fileName!!)
                                    loc.copy(imagePath = Uri.fromFile(newFile).toString())
                                } else loc
                            })
                            
                            val itemsToInsert = export.items.map { item ->
                                val updatedItem = item.copy(id = 0)
                                if (item.imagePath != null) {
                                    val fileName = Uri.parse(item.imagePath).lastPathSegment
                                    val newFile = File(imagesDir, fileName!!)
                                    updatedItem.copy(imagePath = Uri.fromFile(newFile).toString())
                                } else updatedItem
                            }
                            inventoryRepository.insertItems(itemsToInsert)
                            withContext(Dispatchers.Main) { onSuccess() }
                        } else {
                            throw Exception("Failed to parse inventory JSON")
                        }
                    } else {
                        throw Exception("inventory.json not found in ZIP")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onError(e) }
            }
        }
    }
}
