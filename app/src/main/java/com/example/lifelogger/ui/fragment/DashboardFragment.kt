package com.example.lifelogger.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import com.example.lifelogger.R
import com.example.lifelogger.data.auth.SessionManager
import com.example.lifelogger.data.supabase.SupabaseManager
import io.github.jan.supabase.gotrue.auth
import com.example.lifelogger.ui.viewmodel.LogEntryViewModel

class DashboardFragment : Fragment() {

    private val supabaseManager = SupabaseManager()
    private val entryViewModel: LogEntryViewModel by activityViewModels()
    private lateinit var sessionManager: SessionManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sessionManager = SessionManager(requireContext())
        entryViewModel.refreshActiveUser()

        if (!entryViewModel.hasActiveUser()) {
            findNavController().navigate(
                R.id.loginFragment,
                null,
                androidx.navigation.NavOptions.Builder()
                    .setPopUpTo(R.id.loginFragment, true)
                    .build()
            )
            return
        }

        val welcomeText = view.findViewById<TextView>(R.id.welcomeText)
        val addEntryButton = view.findViewById<MaterialButton>(R.id.addEntryButton)
        val previousEntriesButton = view.findViewById<MaterialButton>(R.id.previousEntriesButton)
        val deleteEntryButton = view.findViewById<MaterialButton>(R.id.deleteEntryButton)

        val rawEmail = supabaseManager.client.auth.currentSessionOrNull()?.user?.email.orEmpty()
        val username = rawEmail.substringBefore("@").ifBlank {
            sessionManager.getActiveUsername().ifBlank { "User" }
        }
        welcomeText.text = getString(R.string.welcome_user, username)

        addEntryButton.setOnClickListener {
            findNavController().navigate(R.id.createEntryFragment)
        }

        previousEntriesButton.setOnClickListener {
            findNavController().navigate(R.id.entryListFragment)
        }

        deleteEntryButton.setOnClickListener {
            val bundle = Bundle().apply { putBoolean("showDeleteHint", true) }
            findNavController().navigate(R.id.entryListFragment, bundle)
        }
    }

    override fun onResume() {
        super.onResume()
        entryViewModel.syncNow()
    }
}

