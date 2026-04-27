package com.example.lifelogger.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
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
    private val appContext = application

    private fun currentUserId(): String {
        return supabaseManager.client.auth.currentSessionOrNull()?.user?.id.orEmpty()
    }

    private suspend fun syncIfPossible(userId: String = currentUserId()) {
        if (userId.isBlank()) return
        SyncManager(appContext, repository, supabaseManager, userId).syncData()
    }

    // LiveData that UI observes for list of entries
    val allEntries: LiveData<List<LogEntry>> = repository.getAllEntries()

    /**
     * Insert a new entry into database
     * Launched in viewModelScope so it runs on background thread
     * and automatically cancels when ViewModel destroyed
     */
    fun insertEntry(entry: LogEntry) {
        viewModelScope.launch {
            val userId = if (entry.userId.isNotBlank()) entry.userId else currentUserId()
            val entryForCurrentUser = if (userId.isNotBlank()) {
                entry.copy(userId = userId)
            } else {
                entry
            }
            repository.insertEntry(entryForCurrentUser)
            syncIfPossible(userId)
        }
    }

    /**
     * Update an existing entry
     */
    fun updateEntry(entry: LogEntry) {
        viewModelScope.launch {
            repository.updateEntry(entry)
            syncIfPossible(entry.userId)
        }
    }

    /**
     * Delete an entry
     */
    fun deleteEntry(entry: LogEntry) {
        viewModelScope.launch {
            runCatching {
                if (supabaseManager.isAvailable() && currentUserId().isNotBlank()) {
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
            val entry = repository.getEntryById(id)
            callback(entry)
        }
    }

    /**
     * Search entries by query text
     */
    fun searchEntries(query: String): LiveData<List<LogEntry>> {
        val userId = currentUserId()
        return if (userId.isBlank()) {
            repository.searchEntries(query)
        } else {
            repository.searchEntriesByUser(userId, query)
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            syncIfPossible()
        }
    }
}

