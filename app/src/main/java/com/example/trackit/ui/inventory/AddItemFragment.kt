package com.example.trackit.ui.inventory

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.trackit.databinding.FragmentAddItemBinding
import com.example.trackit.ui.AppViewModelProvider
import com.google.android.material.datepicker.MaterialDatePicker
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class AddItemFragment : Fragment() {

    private var _binding: FragmentAddItemBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: InventoryViewModel by viewModels { AppViewModelProvider.Factory }
    
    private var capturedImageUri: Uri? = null
    private var selectedExpiryDate: Long? = null

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            binding.imagePreview.visibility = View.VISIBLE
            binding.imagePreview.setImageURI(capturedImageUri)
        }
    }

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            takePhoto()
        } else {
            Toast.makeText(requireContext(), "Camera permission required", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddItemBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupDropdowns()
        setupDateInput()
        
        binding.cameraBtn.setOnClickListener {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                takePhoto()
            } else {
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
        
        binding.discardBtn.setOnClickListener {
            findNavController().popBackStack()
        }
        
        binding.saveBtn.setOnClickListener {
            saveItem()
        }
    }

    private fun setupDropdowns() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.categories.collect { categories ->
                        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categories.map { it.name })
                        binding.categoryAuto.setAdapter(adapter)
                    }
                }
                launch {
                    viewModel.locations.collect { locations ->
                        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, locations.map { it.name })
                        binding.locationAuto.setAdapter(adapter)
                    }
                }
            }
        }
    }

    private fun setupDateInput() {
        binding.expiryLayout.setEndIconOnClickListener {
            val calendar = Calendar.getInstance()
            val datePickerDialog = android.app.DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    val selectedCalendar = Calendar.getInstance()
                    selectedCalendar.set(year, month, dayOfMonth)
                    selectedExpiryDate = selectedCalendar.timeInMillis
                    val sdf = SimpleDateFormat("dd-MM-yy", Locale.getDefault())
                    binding.expiryEdit.setText(sdf.format(selectedCalendar.time))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            datePickerDialog.show()
        }

        binding.expiryEdit.addTextChangedListener(object : TextWatcher {
            private var isUpdating = false
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isUpdating) return
                isUpdating = true
                
                val input = s.toString().filter { it.isDigit() }.take(6)
                var formatted = ""
                for (i in input.indices) {
                    formatted += input[i]
                    if ((i == 1 || i == 3) && i != input.lastIndex) {
                        formatted += "-"
                    }
                }
                
                binding.expiryEdit.setText(formatted)
                binding.expiryEdit.setSelection(formatted.length)
                isUpdating = false
            }
        })
    }

    private fun takePhoto() {
        val photoFile = File(requireContext().cacheDir, "temp_photo_${System.currentTimeMillis()}.jpg")
        capturedImageUri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.provider", photoFile)
        cameraLauncher.launch(capturedImageUri!!)
    }

    private fun saveItem() {
        val name = binding.nameEdit.text.toString()
        val quantity = binding.quantityEdit.text.toString().toIntOrNull() ?: 1
        val category = binding.categoryAuto.text.toString()
        val location = binding.locationAuto.text.toString()
        val expiryText = binding.expiryEdit.text.toString()
        
        val parsedDate = try {
            if (expiryText.isNotBlank()) {
                val sdf = SimpleDateFormat("dd-MM-yy", Locale.getDefault())
                sdf.isLenient = false
                sdf.parse(expiryText)?.time
            } else null
        } catch (e: Exception) {
            null
        }

        if (name.isNotBlank()) {
            viewModel.addItem(
                name = name,
                quantity = quantity,
                imagePath = capturedImageUri?.toString(),
                location = location.ifBlank { null },
                category = category.ifBlank { null },
                expiryDate = parsedDate ?: selectedExpiryDate
            )
            findNavController().popBackStack()
        } else {
            Toast.makeText(requireContext(), "Name is required", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
