package com.example.lifelogger.ui.fragment

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
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

    private lateinit var binding: FragmentCreateEntryBinding
    private val viewModel: LogEntryViewModel by viewModels()
    private val supabaseManager = SupabaseManager()
    private var selectedImageUri: Uri? = null
    private var audioFilePath: String? = null
    private var recorder: MediaRecorder? = null
    private var isRecording = false

    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            runCatching {
                requireContext().contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            binding.selectedImagePreview.setImageURI(uri)
            binding.selectedImagePreview.visibility = View.VISIBLE
        }
    }

    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
        private const val AUDIO_PERMISSION = Manifest.permission.RECORD_AUDIO
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
        requestAudioPermissionIfNeeded()

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

        // Get current user ID from Supabase
        val userId = supabaseManager.client.auth.currentSessionOrNull()?.user?.id ?: ""

        // Create entry
        val entry = LogEntry(
            userId = userId,
            title = title,
            content = content,
            category = category,
            imageUri = selectedImageUri?.toString().orEmpty(),
            audioUri = audioFilePath.orEmpty(),
            timestamp = System.currentTimeMillis(),
            lastModified = System.currentTimeMillis()
        )

        // Save using ViewModel
        viewModel.insertEntry(entry)

        Toast.makeText(requireContext(), "Entry saved!", Toast.LENGTH_SHORT).show()

        // Navigate back to list
        findNavController().navigateUp()
    }

    private fun requestAudioPermissionIfNeeded() {
        if (ContextCompat.checkSelfPermission(requireContext(), AUDIO_PERMISSION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(AUDIO_PERMISSION), PERMISSION_REQUEST_CODE)
        }
    }

    private fun startAudioRecording() {
        if (isRecording) return

        if (ContextCompat.checkSelfPermission(requireContext(), AUDIO_PERMISSION) != PackageManager.PERMISSION_GRANTED) {
            requestAudioPermissionIfNeeded()
            Toast.makeText(requireContext(), "Microphone permission is required", Toast.LENGTH_SHORT).show()
            return
        }

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
            releaseRecorder()
            Toast.makeText(requireContext(), "Unable to start recording", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopAudioRecording() {
        if (!isRecording) return

        runCatching {
            recorder?.stop()
        }

        releaseRecorder()
        isRecording = false
        binding.audioStatusText.text = "Audio note attached"
        binding.startRecordingButton.isEnabled = true
        binding.stopRecordingButton.isEnabled = false
    }

    private fun releaseRecorder() {
        recorder?.reset()
        recorder?.release()
        recorder = null
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQUEST_CODE) {
            val granted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                Toast.makeText(
                    requireContext(),
                    "Microphone permission denied",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (isRecording) {
            runCatching { recorder?.stop() }
        }
        releaseRecorder()
    }
}

