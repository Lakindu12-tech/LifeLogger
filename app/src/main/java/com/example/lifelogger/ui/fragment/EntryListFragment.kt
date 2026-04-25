package com.example.lifelogger.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.lifelogger.R
import com.example.lifelogger.databinding.FragmentEntryListBinding
import com.example.lifelogger.ui.adapter.LogEntryAdapter
import com.example.lifelogger.ui.viewmodel.LogEntryViewModel
import io.github.jan.supabase.gotrue.auth
import com.example.lifelogger.data.supabase.SupabaseManager
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
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
    
    private lateinit var binding: FragmentEntryListBinding
    private val viewModel: LogEntryViewModel by viewModels()
    private val supabaseManager = SupabaseManager()
    
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
        viewModel.syncNow()
        
        setupMenu()
        
        // Setup RecyclerView with adapter
        val adapter = LogEntryAdapter { entry ->
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
        
        // Observe entries from ViewModel
        // When data changes, adapter automatically updates with animation
        viewModel.allEntries.observe(viewLifecycleOwner) { entries ->
            adapter.submitList(entries)
            
            // Show/hide empty state message
            if (entries.isEmpty()) {
                binding.emptyStateText.visibility = View.VISIBLE
                binding.entriesRecyclerView.visibility = View.GONE
            } else {
                binding.emptyStateText.visibility = View.GONE
                binding.entriesRecyclerView.visibility = View.VISIBLE
            }
        }
        
        // FAB (Floating Action Button) - navigate to create entry screen
        binding.createEntryButton.setOnClickListener {
            findNavController().navigate(R.id.createEntryFragment)
        }
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
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                supabaseManager.client.auth.signOut()
                findNavController().navigate(
                    R.id.loginFragment,
                    null,
                    androidx.navigation.NavOptions.Builder()
                        .setPopUpTo(R.id.entryListFragment, true)
                        .build()
                )
            } catch (e: Exception) {
                // Ignore error on sign out
                findNavController().navigate(
                    R.id.loginFragment,
                    null,
                    androidx.navigation.NavOptions.Builder()
                        .setPopUpTo(R.id.entryListFragment, true)
                        .build()
                )
            }
        }
    }
}

