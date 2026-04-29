package com.example.lifelogger.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.example.lifelogger.data.auth.SessionManager
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

    companion object {
        private const val TAG = "LogEntryViewModel"
    }

    private val database = LifeLoggerDatabase.getDatabase(application)
    private val repository = LogEntryRepository(database.logEntryDao())
    private val supabaseManager = SupabaseManager()
    private val sessionManager = SessionManager(application)
    private val appContext = application
    private val emptyEntries = MutableLiveData<List<LogEntry>>(emptyList())
    private val activeUserId = MutableLiveData(currentUserId())

    private fun currentUserId(): String {
        val onlineUserId = supabaseManager.client.auth.currentSessionOrNull()?.user?.id.orEmpty()
        if (onlineUserId.isNotBlank()) {
            sessionManager.setActiveUser(onlineUserId, sessionManager.getActiveUsername())
            Log.d(TAG, "[currentUserId] Using online userId: ${onlineUserId.take(8)}...")
            return onlineUserId
        }
        val offlineUserId = sessionManager.getActiveUserId()
        Log.d(TAG, "[currentUserId] Using offline userId: ${if (offlineUserId.isEmpty()) "NONE" else offlineUserId.take(8) + "..."}")
        return offlineUserId
    }

    private suspend fun syncIfPossible(userId: String = currentUserId()) {
        if (userId.isBlank()) return
        SyncManager(appContext, repository, supabaseManager, userId).syncData()
    }

    // LiveData that UI observes for list of entries belonging to active user.
    val allEntries: LiveData<List<LogEntry>> = activeUserId.switchMap { userId ->
        if (userId.isBlank()) emptyEntries else repository.getEntriesByUser(userId)
    }

    fun refreshActiveUser() {
        val newUserId = currentUserId()
        val oldValue = activeUserId.value
        activeUserId.value = newUserId
        Log.d(TAG, "[refreshActiveUser] Updated activeUserId from ${oldValue?.take(8) ?: "null"}... to ${newUserId.take(8)}...")
    }

    fun hasActiveUser(): Boolean {
        val userId = currentUserId()
        val hasUser = userId.isNotBlank()
        Log.d(TAG, "[hasActiveUser] userId=${userId.take(8)}..., hasUser=$hasUser")
        return hasUser
    }

    /**
     * Insert a new entry into database
     * Launched in viewModelScope so it runs on background thread
     * and automatically cancels when ViewModel destroyed
     */
    fun insertEntry(entry: LogEntry) {
        viewModelScope.launch {
            val userId = if (entry.userId.isNotBlank()) entry.userId else currentUserId()
            if (userId.isBlank()) return@launch
            val entryForCurrentUser = if (userId.isNotBlank()) {
                entry.copy(userId = userId)
            } else {
                entry
            }
            repository.insertEntry(entryForCurrentUser)
            activeUserId.postValue(userId)
            syncIfPossible(userId)
        }
    }

    /**
     * Update an existing entry
     */
    fun updateEntry(entry: LogEntry) {
        viewModelScope.launch {
            val userId = currentUserId()
            if (userId.isBlank() || entry.userId != userId) return@launch
            repository.updateEntry(entry)
            syncIfPossible(entry.userId)
        }
    }

    /**
     * Delete an entry
     */
    fun deleteEntry(entry: LogEntry) {
        viewModelScope.launch {
            val userId = currentUserId()
            if (userId.isBlank() || entry.userId != userId) return@launch
            runCatching {
                if (supabaseManager.isAvailable() && userId.isNotBlank()) {
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
     *
     * DEFENSIVE: Safely handles null entries and logs warnings
     */
    fun getEntryById(id: Long, callback: (LogEntry?) -> Unit) {
        Log.d(TAG, "[getEntryById] Fetching entry id=$id, current userId=${currentUserId().take(8)}...")
        viewModelScope.launch {
            try {
                val userId = currentUserId()
                if (userId.isBlank()) {
                    Log.w(TAG, "[getEntryById] userId is blank! Returning null")
                    callback(null)
                    return@launch
                }
                val entry = repository.getEntryByIdForUser(id, userId)
                if (entry == null) {
                    Log.w(TAG, "[getEntryById] Entry NOT FOUND for id=$id, userId=${userId.take(8)}...")
                } else {
                    Log.d(TAG, "[getEntryById] Entry found: ${entry.title} (userId=${entry.userId.take(8)}..., current=${userId.take(8)}...)")
                    // Verify ownership
                    if (entry.userId != userId) {
                        Log.e(TAG, "[getEntryById] CRITICAL: Entry userId mismatch! entry.userId=${entry.userId.take(8)}..., current=${userId.take(8)}...")
                        callback(null)
                        return@launch
                    }
                }
                callback(entry)
            } catch (e: Exception) {
                Log.e(TAG, "[getEntryById] Exception fetching entry", e)
                callback(null)
            }
        }
    }

    /**
     * Search entries by query text
     */
    fun searchEntries(query: String): LiveData<List<LogEntry>> {
        val userId = currentUserId()
        return if (userId.isBlank()) {
            emptyEntries
        } else {
            repository.searchEntriesByUser(userId, query)
        }
    }

    fun syncNow() {
        Log.d(TAG, "[syncNow] Sync triggered")
        viewModelScope.launch {
            refreshActiveUser()
            val userId = currentUserId()
            Log.d(TAG, "[syncNow] Syncing for userId=${userId.take(8)}...")
            syncIfPossible()
        }
    }
}

