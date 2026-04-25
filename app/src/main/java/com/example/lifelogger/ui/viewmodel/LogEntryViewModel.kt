package com.example.lifelogger.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.lifelogger.data.database.LifeLoggerDatabase
import com.example.lifelogger.data.model.LogEntry
import com.example.lifelogger.data.repository.LogEntryRepository
import com.example.lifelogger.data.supabase.SupabaseManager
import com.example.lifelogger.data.sync.SyncManager
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.launch

/**
 * MEMBER 2 RESPONSIBILITY: UI Layer - ViewModel
 *
 * ViewModel is a bridge between UI and Data:
 * - Holds business logic
 * - Provides data to UI via LiveData
 * - Survives configuration changes (rotation)
 * - Manages coroutines for background operations
 *
 * AndroidViewModel extends ViewModel and has access to Application context
 * (useful for database and resources)
 *
 * viewModelScope: coroutines launched here are automatically cancelled
 * when ViewModel is destroyed, preventing memory leaks
 */
class LogEntryViewModel(application: Application) : AndroidViewModel(application) {

    private val database = LifeLoggerDatabase.getDatabase(application)
    private val repository = LogEntryRepository(database.logEntryDao())
    private val supabaseManager = SupabaseManager()
    private val currentUserId = supabaseManager.client.auth.currentSessionOrNull()?.user?.id.orEmpty()
    private val syncManager = SyncManager(application, repository, supabaseManager, currentUserId)

    // LiveData that UI observes for list of entries
    val allEntries: LiveData<List<LogEntry>> = if (currentUserId.isNotBlank()) {
        repository.getEntriesByUser(currentUserId)
    } else {
        MutableLiveData(emptyList())
    }

    /**
     * Insert a new entry into database
     * Launched in viewModelScope so it runs on background thread
     * and automatically cancels when ViewModel destroyed
     */
    fun insertEntry(entry: LogEntry) {
        viewModelScope.launch {
            val entryForCurrentUser = if (entry.userId.isBlank() && currentUserId.isNotBlank()) {
                entry.copy(userId = currentUserId)
            } else {
                entry
            }
            repository.insertEntry(entryForCurrentUser)
            syncManager.syncData()
        }
    }

    /**
     * Update an existing entry
     */
    fun updateEntry(entry: LogEntry) {
        viewModelScope.launch {
            repository.updateEntry(entry)
            syncManager.syncData()
        }
    }

    /**
     * Delete an entry
     */
    fun deleteEntry(entry: LogEntry) {
        viewModelScope.launch {
            runCatching {
                if (supabaseManager.isAvailable()) {
                    supabaseManager.deleteEntry(entry.id)
                }
            }
            repository.deleteEntry(entry)
        }
    }

    /**
     * Get a single entry by ID
     * Useful for opening entry details
     * Launched in viewModelScope on background thread
     */
    fun getEntryById(id: Long, callback: (LogEntry?) -> Unit) {
        viewModelScope.launch {
            val entry = if (currentUserId.isBlank()) {
                null
            } else {
                repository.getEntryByIdForUser(id, currentUserId)
            }
            callback(entry)
        }
    }

    /**
     * Search entries by query text
     */
    fun searchEntries(query: String): LiveData<List<LogEntry>> {
        return if (currentUserId.isBlank()) {
            MutableLiveData(emptyList())
        } else {
            repository.searchEntriesByUser(currentUserId, query)
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            syncManager.syncData()
        }
    }
}

