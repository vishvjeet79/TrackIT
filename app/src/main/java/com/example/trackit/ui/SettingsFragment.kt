package com.example.trackit.ui

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.trackit.databinding.FragmentSettingsBinding
import com.example.trackit.ui.inventory.PeerAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: SettingsViewModel by viewModels { AppViewModelProvider.Factory }

    private val exportLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        uri?.let {
            requireContext().contentResolver.openOutputStream(it)?.let { outputStream ->
                viewModel.exportData(
                    outputStream = outputStream,
                    includeItemImages = true, // Defaulting for simplicity in XML migration
                    includeLocationImages = true,
                    context = requireContext(),
                    onSuccess = { Toast.makeText(requireContext(), "Exported", Toast.LENGTH_SHORT).show() },
                    onError = { e -> Toast.makeText(requireContext(), "Export failed", Toast.LENGTH_SHORT).show() }
                )
            }
        }
    }

    private val importLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            showImportChoiceDialog(it)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupDarkMode()
        setupDataManagement()
        setupNearbySync()
    }

    private fun setupDarkMode() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isDarkModeEnabled.collect { isEnabled ->
                    if (binding.darkModeSwitch.isChecked != isEnabled) {
                        binding.darkModeSwitch.isChecked = isEnabled
                    }
                }
            }
        }
        binding.darkModeSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            if (buttonView.isPressed) {
                viewModel.toggleDarkMode(isChecked)
            }
        }
    }

    private fun setupDataManagement() {
        binding.exportBtn.setOnClickListener {
            exportLauncher.launch("inventory_backup.zip")
        }
        binding.importBtn.setOnClickListener {
            importLauncher.launch("*/*")
        }
    }

    private fun setupNearbySync() {
        val adapter = PeerAdapter { peer ->
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Sync with ${peer.serviceName}")
                .setMessage("Download inventory from this device?")
                .setPositiveButton("Merge") { _, _ -> sync(peer, false) }
                .setPositiveButton("Replace") { _, _ -> sync(peer, true) }
                .setNegativeButton("Cancel", null)
                .show()
        }
        
        binding.peersRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.peersRecycler.adapter = adapter
        
        binding.syncHostBtn.setOnClickListener {
            if (viewModel.isHosting.value) {
                viewModel.stopHosting()
                binding.syncHostBtn.text = "Enable Nearby Visibility"
            } else {
                viewModel.startHosting(requireContext(), true) { 
                    Toast.makeText(requireContext(), "Hosting error", Toast.LENGTH_SHORT).show()
                }
                binding.syncHostBtn.text = "Stop Visibility"
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.discoveredPeers.collect { adapter.submitList(it) }
                }
                launch {
                    viewModel.isHosting.collect { isHosting ->
                        binding.syncHostBtn.text = if (isHosting) "Stop Visibility" else "Enable Nearby Visibility"
                    }
                }
            }
        }
        
        viewModel.startDiscovery()
    }

    private fun showImportChoiceDialog(uri: Uri) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Import Options")
            .setMessage("Merge or Replace existing inventory?")
            .setPositiveButton("Replace") { _, _ -> performImport(uri, true) }
            .setNeutralButton("Merge") { _, _ -> performImport(uri, false) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performImport(uri: Uri, clearExisting: Boolean) {
        requireContext().contentResolver.openInputStream(uri)?.let { inputStream ->
            viewModel.importData(
                inputStream = inputStream,
                clearExisting = clearExisting,
                context = requireContext(),
                onSuccess = { Toast.makeText(requireContext(), "Imported", Toast.LENGTH_SHORT).show() },
                onError = { Toast.makeText(requireContext(), "Import failed", Toast.LENGTH_SHORT).show() }
            )
        }
    }

    private fun sync(peer: android.net.nsd.NsdServiceInfo, clearExisting: Boolean) {
        viewModel.syncWithPeer(
            peer = peer,
            clearExisting = clearExisting,
            context = requireContext(),
            onSuccess = { Toast.makeText(requireContext(), "Sync Success", Toast.LENGTH_SHORT).show() },
            onError = { Toast.makeText(requireContext(), "Sync Failed", Toast.LENGTH_SHORT).show() }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
