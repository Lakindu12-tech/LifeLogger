package com.example.lifelogger.ui.fragment

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.lifelogger.data.model.LogEntry
import com.example.lifelogger.databinding.FragmentCreateEntryBinding
import com.example.lifelogger.ui.viewmodel.LogEntryViewModel

/**
 * MEMBER 2 RESPONSIBILITY: UI Layer - Create Entry Fragment
 *
 * Allows user to:
 * 1. Enter title
 * 2. Enter content/text
 * 3. Select category
 * 4. Record audio (permission required)
 * 5. Attach image (permission required)
 *
 * When user taps Save:
 * - Validate input
 * - Create LogEntry object
 * - Pass to ViewModel to save
 * - Navigate back to list
 */
class CreateEntryFragment : Fragment() {

    private lateinit var binding: FragmentCreateEntryBinding
    private val viewModel: LogEntryViewModel by viewModels()

    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCreateEntryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Request permissions on fragment creation
        requestPermissions()

        // Setup category spinner
        setupCategorySpinner()

        // Save button listener
        binding.saveButton.setOnClickListener {
            saveEntry()
        }

        // Cancel button listener
        binding.cancelButton.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    /**
     * Setup category dropdown spinner
     */
    private fun setupCategorySpinner() {
        val categories = arrayOf("general", "workout", "study", "reflection", "event")
        // Note: In production, use ArrayAdapter with a proper spinner
        // For simplicity in this assignment, keeping it as text field
    }

    /**
     * Save entry to database
     * Validates input and creates LogEntry
     */
    private fun saveEntry() {
        val title = binding.titleInput.text.toString().trim()
        val content = binding.contentInput.text.toString().trim()
        val category = binding.categoryInput.text.toString().trim().ifEmpty { "general" }

        // Validate
        if (content.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter some content", Toast.LENGTH_SHORT).show()
            return
        }

        // Create entry
        val entry = LogEntry(
            title = title,
            content = content,
            category = category,
            timestamp = System.currentTimeMillis(),
            lastModified = System.currentTimeMillis()
        )

        // Save using ViewModel
        viewModel.insertEntry(entry)

        Toast.makeText(requireContext(), "Entry saved!", Toast.LENGTH_SHORT).show()

        // Navigate back to list
        findNavController().navigateUp()
    }

    /**
     * Request required permissions (audio, camera, storage)
     */
    private fun requestPermissions() {
        val permissionsToRequest = REQUIRED_PERMISSIONS.filter { permission ->
            ContextCompat.checkSelfPermission(
                requireContext(),
                permission
            ) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissions(
                permissionsToRequest.toTypedArray(),
                PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQUEST_CODE) {
            val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            if (!allGranted) {
                Toast.makeText(
                    requireContext(),
                    "Some permissions were denied",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}

