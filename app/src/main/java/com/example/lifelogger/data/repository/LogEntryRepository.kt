package com.example.lifelogger.data.repository

import androidx.lifecycle.LiveData
import com.example.lifelogger.data.database.dao.LogEntryDao
import com.example.lifelogger.data.model.LogEntry

/**
 * MEMBER 1 RESPONSIBILITY: Data Layer - Repository Pattern
 * 
 * Repository acts as a mediator between:
 * - Data sources (Local Database, Firebase)
 * - Business logic (ViewModels)
 * 
 * Benefits:
 * 1. Single point of access to data
 * 2. Easy to switch between local and remote data
 * 3. Can combine multiple data sources (local + cloud sync)
 * 4. Easier to test
 * 
 * This repository currently uses only local database.
 * Can be extended to include Firebase sync logic.
 */
class LogEntryRepository(private val logEntryDao: LogEntryDao) {
    
    /**
     * Get all entries from local database
     * Returns LiveData so UI is notified when data changes
     */
    fun getAllEntries(): LiveData<List<LogEntry>> = logEntryDao.getAllEntries()
    
    /**
     * Insert a new entry
     * suspend = runs on background thread using coroutines
     * So it doesn't freeze the UI
     */
    suspend fun insertEntry(entry: LogEntry): Long {
        return logEntryDao.insert(entry)
    }
    
    /**
     * Update an existing entry
     */
    suspend fun updateEntry(entry: LogEntry) {
        logEntryDao.update(entry)
    }
    
    /**
     * Delete an entry
     */
    suspend fun deleteEntry(entry: LogEntry) {
        logEntryDao.delete(entry)
    }
    
    /**
     * Get a single entry by ID
     * Used when viewing entry details
     */
    suspend fun getEntryById(id: Long): LogEntry? {
        return logEntryDao.getEntryById(id)
    }
    
    /**
     * Get entries that haven't been synced to Firebase
     * Used in sync manager to determine what to upload
     */
    suspend fun getUnsyncedEntries(): List<LogEntry> {
        return logEntryDao.getUnsyncedEntries()
    }
    
    /**
     * Mark an entry as successfully synced to Firebase
     */
    suspend fun markAsSynced(id: Long) {
        logEntryDao.markAsSynced(id)
    }
    
    /**
     * Search entries by text
     */
    fun searchEntries(query: String): LiveData<List<LogEntry>> {
        return logEntryDao.searchEntries("%$query%")
    }
}

