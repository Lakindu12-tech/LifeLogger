package com.example.lifelogger.ui.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.lifelogger.R
import com.example.lifelogger.data.auth.SessionManager
import com.example.lifelogger.databinding.FragmentEntryListBinding
import com.example.lifelogger.ui.adapter.LogEntryAdapter
import com.example.lifelogger.ui.viewmodel.LogEntryViewModel
import io.github.jan.supabase.gotrue.auth
import com.example.lifelogger.data.supabase.SupabaseManager
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Toast
import kotlinx.coroutines.launch

/**
 * MEMBER 2 RESPONSIBILITY: UI Layer - Entry List Fragment
 * 
 * Fragment displays a list of all log entries using RecyclerView
 * 
 * When user taps an entry, navigate to detail screen
 * When user taps FAB (+), navigate to create entry screen
 */
class EntryListFragment : Fragment() {
    
    companion object {
        private const val TAG = "EntryListFragment"
    }

    private lateinit var binding: FragmentEntryListBinding
    private val viewModel: LogEntryViewModel by activityViewModels()
    private val supabaseManager = SupabaseManager()
    private lateinit var sessionManager: SessionManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentEntryListBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "[onViewCreated] Fragment created")
        sessionManager = SessionManager(requireContext())
        viewModel.refreshActiveUser()

        if (!viewModel.hasActiveUser()) {
            Log.w(TAG, "[onViewCreated] No active user, redirecting to login")
            findNavController().navigate(
                R.id.loginFragment,
                null,
                androidx.navigation.NavOptions.Builder()
                    .setPopUpTo(R.id.loginFragment, true)
                    .build()
            )
            return
        }

        Log.d(TAG, "[onViewCreated] Active user confirmed, syncing now")
        viewModel.syncNow()

        if (arguments?.getBoolean("showDeleteHint") == true) {
            Toast.makeText(requireContext(), getString(R.string.delete_entry_hint), Toast.LENGTH_LONG).show()
        }
        
        setupMenu()
        
        // Setup RecyclerView with adapter
        val adapter = LogEntryAdapter { entry ->
            Log.d(TAG, "[adapter.onItemClick] User clicked entry id=${entry.id}, title=${entry.title}")
            // Navigate to detail fragment and pass entry ID via bundle
            val bundle = Bundle().apply {
                putLong("entryId", entry.id)
            }
            findNavController().navigate(R.id.entryDetailFragment, bundle)
        }
        
        binding.entriesRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            this.adapter = adapter
        }
        
        // Observe entries from ViewModel  with defensive null checks
        viewModel.allEntries.observe(viewLifecycleOwner) { entries ->
            Log.d(TAG, "[allEntries observer] Received ${entries?.size ?: 0} entries")
            if (entries == null) {
                Log.w(TAG, "[allEntries observer] entries list is NULL!")
                adapter.submitList(emptyList())
                binding.emptyStateText.visibility = View.VISIBLE
                binding.entriesRecyclerView.visibility = View.GONE
                return@observe
            }

            adapter.submitList(entries)
            
            // Show/hide empty state message
            if (entries.isEmpty()) {
                Log.d(TAG, "[allEntries observer] No entries, showing empty state")
                binding.emptyStateText.visibility = View.VISIBLE
                binding.entriesRecyclerView.visibility = View.GONE
            } else {
                Log.d(TAG, "[allEntries observer] Showing ${entries.size} entries")
                binding.emptyStateText.visibility = View.GONE
                binding.entriesRecyclerView.visibility = View.VISIBLE
            }
        }
        
        // FAB (Floating Action Button) - navigate to create entry screen
        binding.createEntryButton.setOnClickListener {
            Log.d(TAG, "[createEntryButton] Navigate to create entry")
            findNavController().navigate(R.id.createEntryFragment)
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.syncNow()
    }

    private fun setupMenu() {
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.main_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_logout -> {
                        logoutUser()
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun logoutUser() {
        Log.d(TAG, "[logoutUser] User logging out")
        viewLifecycleOwner.lifecycleScope.launch {
            // Clear session prefs and notify ViewModel so LiveData resets
            sessionManager.clearActiveUser()
            Log.d(TAG, "[logoutUser] Session cleared from prefs")
            viewModel.refreshActiveUser()
            Log.d(TAG, "[logoutUser] ViewModel activeUser refreshed")
            try {
                supabaseManager.client.auth.signOut()
                Log.d(TAG, "[logoutUser] Supabase signOut succeeded")
                findNavController().navigate(
                    R.id.loginFragment,
                    null,
                    androidx.navigation.NavOptions.Builder()
                        .setPopUpTo(R.id.dashboardFragment, true)
                        .build()
                )
            } catch (e: Exception) {
                Log.w(TAG, "[logoutUser] Supabase signOut failed", e)
                // Ignore error on sign out
                findNavController().navigate(
                    R.id.loginFragment,
                    null,
                    androidx.navigation.NavOptions.Builder()
                        .setPopUpTo(R.id.dashboardFragment, true)
                        .build()
                )
            }
        }
    }
}

