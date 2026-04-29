package com.example.lifelogger.ui.fragment

import android.os.Bundle
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
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
    
    companion object {
        private const val TAG = "EntryDetailFragment"
    }

    private lateinit var binding: FragmentEntryDetailBinding
    private val viewModel: LogEntryViewModel by activityViewModels()
    private var currentEntry: LogEntry? = null
    private var mediaPlayer: MediaPlayer? = null

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

        if (!viewModel.hasActiveUser()) {
            Log.w(TAG, "[onViewCreated] No active user, redirecting to login")
            findNavController().navigate(
                com.example.lifelogger.R.id.loginFragment,
                null,
                androidx.navigation.NavOptions.Builder()
                    .setPopUpTo(com.example.lifelogger.R.id.loginFragment, true)
                    .build()
            )
            return
        }

        // Get entry ID from arguments bundle
        val entryId = arguments?.getLong("entryId") ?: 0L
        Log.d(TAG, "[onViewCreated] Loading entry id=$entryId")

        if (entryId <= 0) {
            Log.w(TAG, "[onViewCreated] Invalid entryId=$entryId")
            Toast.makeText(requireContext(), "Entry not found", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
            return
        }
        
        // Load entry from database with defensive callback
        viewModel.getEntryById(entryId) { entry ->
            // Guard: Fragment may have been destroyed before callback returns
            if (!isAdded) {
                Log.d(TAG, "[getEntryById callback] Fragment no longer added, ignoring")
                return@getEntryById
            }

            if (entry == null) {
                Log.e(TAG, "[getEntryById callback] Entry returned as NULL for id=$entryId")
                Toast.makeText(requireContext(), "Entry could not be loaded", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
                return@getEntryById
            }

            Log.d(TAG, "[getEntryById callback] Successfully loaded entry: ${entry.title}")
            currentEntry = entry
            displayEntry(entry)
        }
        
        // Delete button - with null check
        binding.deleteButton.setOnClickListener {
            val entry = currentEntry
            if (entry == null) {
                Log.w(TAG, "[deleteButton] currentEntry is null, ignoring delete")
                Toast.makeText(requireContext(), "No entry to delete", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            Log.d(TAG, "[deleteButton] Deleting entry id=${entry.id}")
            viewModel.deleteEntry(entry)
            Toast.makeText(requireContext(), "Entry deleted", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
        }
        
        // Back button
        binding.backButton.setOnClickListener {
            Log.d(TAG, "[backButton] Navigating back")
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

            if (entry.imageUri.isNotBlank()) {
                entryImageView.visibility = View.VISIBLE
                // Loading a persisted content URI can throw (SecurityException / IllegalArgumentException)
                // if the permission is no longer granted. Guard against crashes by catching failures
                // and hiding the image view if loading fails.
                runCatching {
                    entryImageView.setImageURI(Uri.parse(entry.imageUri))
                }.onFailure {
                    entryImageView.visibility = View.GONE
                }
            } else {
                entryImageView.visibility = View.GONE
            }

            if (entry.audioUri.isNotBlank()) {
                playAudioButton.visibility = View.VISIBLE
                playAudioButton.setOnClickListener {
                    playAudio(entry.audioUri)
                }
            } else {
                playAudioButton.visibility = View.GONE
            }
        }
    }

    private fun playAudio(audioUri: String) {
        runCatching {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                if (audioUri.startsWith("content://")) {
                    setDataSource(requireContext(), Uri.parse(audioUri))
                } else {
                    setDataSource(audioUri)
                }
                prepare()
                start()
            }
        }.onFailure {
            Toast.makeText(requireContext(), "Unable to play audio", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}

