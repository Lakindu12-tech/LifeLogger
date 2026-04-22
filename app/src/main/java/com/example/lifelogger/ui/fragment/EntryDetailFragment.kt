package com.example.lifelogger.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.lifelogger.data.model.LogEntry
import com.example.lifelogger.databinding.FragmentEntryDetailBinding
import com.example.lifelogger.ui.viewmodel.LogEntryViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * MEMBER 2 RESPONSIBILITY: UI Layer - Entry Detail Fragment
 * 
 * Displays full details of a single entry:
 * - Title
 * - Content
 * - Category
 * - Date/time created
 * - Can delete
 * 
 * Arguments passed via Bundle:
 * - entryId: the ID of entry to display
 */
class EntryDetailFragment : Fragment() {
    
    private lateinit var binding: FragmentEntryDetailBinding
    private val viewModel: LogEntryViewModel by viewModels()
    private var currentEntry: LogEntry? = null
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentEntryDetailBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Get entry ID from arguments bundle
        val entryId = arguments?.getLong("entryId") ?: 0L
        
        if (entryId <= 0) {
            Toast.makeText(requireContext(), "Entry not found", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
            return
        }
        
        // Load entry from database
        viewModel.getEntryById(entryId) { entry ->
            if (entry != null) {
                currentEntry = entry
                displayEntry(entry)
            } else {
                Toast.makeText(requireContext(), "Entry not found", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            }
        }
        
        // Delete button
        binding.deleteButton.setOnClickListener {
            currentEntry?.let { entry ->
                viewModel.deleteEntry(entry)
                Toast.makeText(requireContext(), "Entry deleted", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            }
        }
        
        // Back button
        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
        }
    }
    
    /**
     * Display entry information in UI
     */
    private fun displayEntry(entry: LogEntry) {
        binding.apply {
            titleText.text = entry.title.ifEmpty { "Untitled" }
            contentText.text = entry.content
            
            val formatter = SimpleDateFormat("MMMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
            dateText.text = formatter.format(Date(entry.timestamp))
            
            categoryText.text = entry.category.uppercase()
            
            // Show sync status
            syncStatusText.text = if (entry.isSynced) {
                "✓ Synced to Cloud"
            } else {
                "⊘ Pending Sync"
            }
        }
    }
}

