package com.example.lifelogger.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.lifelogger.data.database.LifeLoggerDatabase
import com.example.lifelogger.data.model.LogEntry
import com.example.lifelogger.data.repository.LogEntryRepository
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

    // LiveData that UI observes for list of entries
    val allEntries: LiveData<List<LogEntry>> = repository.getAllEntries()

    /**
     * Insert a new entry into database
     * Launched in viewModelScope so it runs on background thread
     * and automatically cancels when ViewModel destroyed
     */
    fun insertEntry(entry: LogEntry) {
        viewModelScope.launch {
            repository.insertEntry(entry)
        }
    }

    /**
     * Update an existing entry
     */
    fun updateEntry(entry: LogEntry) {
        viewModelScope.launch {
            repository.updateEntry(entry)
        }
    }

    /**
     * Delete an entry
     */
    fun deleteEntry(entry: LogEntry) {
        viewModelScope.launch {
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
        return repository.searchEntries(query)
    }
}

