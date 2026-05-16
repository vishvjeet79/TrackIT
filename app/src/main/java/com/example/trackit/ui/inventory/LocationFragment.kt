package com.example.trackit.ui.inventory

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.trackit.databinding.FragmentLocationBinding
import com.example.trackit.ui.AppViewModelProvider
import kotlinx.coroutines.launch
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.widget.EditText
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.LinearLayout
import com.example.trackit.data.Location

class LocationFragment : Fragment() {

    private var _binding: FragmentLocationBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: InventoryViewModel by viewModels { AppViewModelProvider.Factory }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLocationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val adapter = LocationAdapter(
            onLocationClick = { /* Handle photo enlarge if any */ },
            onAddSubClick = { showAddLocationDialog(it.name) },
            onDeleteClick = { viewModel.deleteLocation(it) }
        )
        
        binding.locationRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.locationRecycler.adapter = adapter
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.locations.collect { locations ->
                    // Flatten locations for display: Parents then their children
                    val displayList = mutableListOf<Location>()
                    val parents = locations.filter { it.parentName == null }
                    parents.forEach { parent ->
                        displayList.add(parent)
                        displayList.addAll(locations.filter { it.parentName == parent.name })
                    }
                    adapter.submitList(displayList)
                }
            }
        }
        
        binding.addLocationFab.setOnClickListener {
            showAddLocationDialog()
        }
    }
    
    private fun showAddLocationDialog(parentName: String? = null) {
        val input = EditText(requireContext())
        input.hint = "Location Name"
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (parentName == null) "Add Location" else "Add Sub-location to $parentName")
            .setView(input)
            .setPositiveButton("Add") { _, _ ->
                val name = input.text.toString()
                if (name.isNotBlank()) {
                    viewModel.addLocation(name, parentName)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
