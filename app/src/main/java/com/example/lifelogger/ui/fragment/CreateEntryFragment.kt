package com.example.lifelogger.ui.fragment

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.lifelogger.data.auth.SessionManager
import com.example.lifelogger.data.model.LogEntry
import com.example.lifelogger.data.supabase.SupabaseManager
import com.example.lifelogger.databinding.FragmentCreateEntryBinding
import com.example.lifelogger.ui.viewmodel.LogEntryViewModel
import io.github.jan.supabase.gotrue.auth
import java.io.File

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

    companion object {
        private const val AUDIO_PERMISSION = Manifest.permission.RECORD_AUDIO
        private const val TAG = "CreateEntryFragment"
    }

    private lateinit var binding: FragmentCreateEntryBinding
    private val viewModel: LogEntryViewModel by activityViewModels()
    private val supabaseManager = SupabaseManager()
    private lateinit var sessionManager: SessionManager
    private var selectedImageUri: Uri? = null
    private var audioFilePath: String? = null
    private var recorder: MediaRecorder? = null
    private var isRecording = false

    private val audioPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            startAudioRecordingInternal()
        } else {
            Toast.makeText(requireContext(), "Microphone permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            runCatching {
                requireContext().contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            // Setting the preview image may throw if the URI cannot be accessed for some reason;
            // protect against crashes and simply hide the preview on failure.
            runCatching {
                binding.selectedImagePreview.setImageURI(uri)
                binding.selectedImagePreview.visibility = View.VISIBLE
            }.onFailure {
                binding.selectedImagePreview.visibility = View.GONE
            }
         }
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
        sessionManager = SessionManager(requireContext())

        if (!viewModel.hasActiveUser()) {
            findNavController().navigate(
                com.example.lifelogger.R.id.loginFragment,
                null,
                androidx.navigation.NavOptions.Builder()
                    .setPopUpTo(com.example.lifelogger.R.id.loginFragment, true)
                    .build()
            )
            return
        }

        binding.attachImageButton.setOnClickListener {
            imagePickerLauncher.launch(arrayOf("image/*"))
        }

        binding.startRecordingButton.setOnClickListener {
            startAudioRecording()
        }

        binding.stopRecordingButton.setOnClickListener {
            stopAudioRecording()
        }

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
     * Save entry to database
     * Validates input and creates LogEntry with local media URIs
     * Media is stored locally in app storage
     */
    private fun saveEntry() {
        if (isRecording) {
            stopAudioRecording()
        }

        val title = binding.titleInput.text.toString().trim()
        val content = binding.contentInput.text.toString().trim()
        val category = binding.categoryInput.text.toString().trim().ifEmpty { "general" }

        // Validate
        if (content.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter some content", Toast.LENGTH_SHORT).show()
            return
        }

        // Get current user ID from Supabase
        val userId = supabaseManager.client.auth.currentSessionOrNull()?.user?.id
            ?: sessionManager.getActiveUserId()

        if (userId.isBlank()) {
            Toast.makeText(requireContext(), "Please log in first", Toast.LENGTH_SHORT).show()
            findNavController().navigate(
                com.example.lifelogger.R.id.loginFragment,
                null,
                androidx.navigation.NavOptions.Builder()
                    .setPopUpTo(com.example.lifelogger.R.id.loginFragment, true)
                    .build()
            )
            return
        }

        val storedImagePath = selectedImageUri?.let { copyImageToInternalStorage(it) }.orEmpty()
        if (selectedImageUri != null && storedImagePath.isEmpty()) {
            Toast.makeText(requireContext(), "Image could not be saved", Toast.LENGTH_SHORT).show()
            return
        }

        // Create entry WITH local media paths
        // Media files are saved locally; we store the local file paths in database
        val entry = LogEntry(
            userId = userId,
            title = title,
            content = content,
            category = category,
            imageUri = storedImagePath,
            audioUri = audioFilePath.orEmpty(),
            timestamp = System.currentTimeMillis(),
            lastModified = System.currentTimeMillis()
        )

        Log.d(TAG, "[saveEntry] Saving entry with local media to Room")
        // Save using ViewModel (includes media URIs)
        viewModel.insertEntry(entry)

        Toast.makeText(requireContext(), "Entry saved!", Toast.LENGTH_SHORT).show()

        // Open list so user can immediately see the new saved entry
        findNavController().navigate(
            com.example.lifelogger.R.id.entryListFragment,
            null,
            androidx.navigation.NavOptions.Builder()
                .setPopUpTo(com.example.lifelogger.R.id.createEntryFragment, true)
                .build()
        )
    }

    private fun copyImageToInternalStorage(sourceUri: Uri): String? {
        return runCatching {
            val context = requireContext()
            val extension = resolveImageExtension(sourceUri)
            val fileName = "image_${System.currentTimeMillis()}_${sourceUri.hashCode().toString(16)}.$extension"
            val destinationFile = File(context.filesDir, fileName)

            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                destinationFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return null

            if (destinationFile.exists() && destinationFile.length() > 0L) {
                destinationFile.absolutePath
            } else {
                destinationFile.delete()
                null
            }
        }.getOrNull()
    }

    private fun resolveImageExtension(uri: Uri): String {
        val mimeType = requireContext().contentResolver.getType(uri)
        val extension = mimeType?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }
        return extension?.ifBlank { null } ?: "jpg"
    }

    private fun requestAudioPermissionIfNeeded() {
        if (ContextCompat.checkSelfPermission(requireContext(), AUDIO_PERMISSION) != PackageManager.PERMISSION_GRANTED) {
            audioPermissionLauncher.launch(AUDIO_PERMISSION)
            return
        }
        startAudioRecordingInternal()
    }

    private fun startAudioRecording() {
        if (isRecording) return
        requestAudioPermissionIfNeeded()
    }

    private fun startAudioRecordingInternal() {
        if (isRecording) return

        val outputFile = File(requireContext().filesDir, "audio_note_${System.currentTimeMillis()}.m4a")
        audioFilePath = outputFile.absolutePath

        recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(requireContext())
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

        runCatching {
            recorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(audioFilePath)
                prepare()
                start()
            }
        }.onSuccess {
            isRecording = true
            binding.audioStatusText.text = "Recording... tap Stop when done"
            binding.startRecordingButton.isEnabled = false
            binding.stopRecordingButton.isEnabled = true
        }.onFailure {
            audioFilePath = null
            releaseRecorder()
            Toast.makeText(requireContext(), "Unable to start recording", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopAudioRecording() {
        if (!isRecording) return

        val stopResult = runCatching {
            recorder?.stop()
        }

        releaseRecorder()
        isRecording = false
        val audioPath = audioFilePath
        val hasAudio = audioPath != null && File(audioPath).exists() && File(audioPath).length() > 0L
        if (stopResult.isSuccess && hasAudio) {
            binding.audioStatusText.text = "Audio note attached"
        } else {
            audioFilePath = null
            binding.audioStatusText.text = "No audio note attached"
            Toast.makeText(requireContext(), "Recording failed, please try again", Toast.LENGTH_SHORT).show()
        }
        binding.startRecordingButton.isEnabled = true
        binding.stopRecordingButton.isEnabled = false
    }

    private fun releaseRecorder() {
        recorder?.reset()
        recorder?.release()
        recorder = null
    }


    override fun onDestroyView() {
        super.onDestroyView()
        if (isRecording) {
            runCatching { recorder?.stop() }
        }
        releaseRecorder()
        isRecording = false
    }
}
