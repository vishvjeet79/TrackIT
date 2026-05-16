package com.example.trackit.ui.inventory

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.trackit.R
import com.example.trackit.data.InventoryItem
import com.example.trackit.databinding.FragmentInventoryBinding
import com.example.trackit.ui.AppViewModelProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.widget.EditText
import kotlinx.coroutines.launch

class InventoryFragment : Fragment() {

    private var _binding: FragmentInventoryBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: InventoryViewModel by viewModels { AppViewModelProvider.Factory }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInventoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val adapter = InventoryAdapter(
            onItemClick = { item ->
                // Show consume dialog or something
                showConsumeDialog(item)
            },
            onEditClick = { item ->
                // Maybe another fragment or dialog
                Toast.makeText(requireContext(), "Edit coming soon", Toast.LENGTH_SHORT).show()
            },
            onDeleteClick = { viewModel.deleteItem(it) }
        )
        
        binding.inventoryRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.inventoryRecycler.adapter = adapter
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.allItems.collect { items ->
                    adapter.submitList(items)
                }
            }
        }
        
        binding.addFab.setOnClickListener {
            findNavController().navigate(R.id.addItem_fragment)
        }
    }

    private fun showConsumeDialog(item: InventoryItem) {
        val input = EditText(requireContext())
        input.hint = "Amount to consume"
        input.inputType = android.text.InputType.TYPE_CLASS_NUMBER

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Consume ${item.name}")
            .setMessage("Available: ${item.quantity}")
            .setView(input)
            .setPositiveButton("Confirm") { _, _ ->
                val amount = input.text.toString().toIntOrNull() ?: 0
                if (amount > 0 && amount <= item.quantity) {
                    viewModel.consumeItem(item, amount)
                } else {
                    Toast.makeText(requireContext(), "Invalid amount", Toast.LENGTH_SHORT).show()
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
