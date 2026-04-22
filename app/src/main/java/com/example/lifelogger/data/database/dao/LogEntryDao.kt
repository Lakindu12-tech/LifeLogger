package com.example.lifelogger.data.database.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.lifelogger.data.model.LogEntry

/**
 * MEMBER 1 RESPONSIBILITY: Data Layer - Database Access Object
 *
 * DAO (Data Access Object) is an interface that provides methods to access the database.
 * Each method here represents a database operation (Create, Read, Update, Delete).
 *
 * Room automatically generates the SQL code for these methods at compile time.
 * This is a clean abstraction that separates database logic from business logic.
 */
@Dao
interface LogEntryDao {

    /**
     * Insert a new log entry into the database
     * Returns the ID of the inserted entry (used when auto-generating IDs)
     */
    @Insert
    suspend fun insert(entry: LogEntry): Long

    /**
     * Update an existing log entry
     * Finds the entry by its ID and updates all fields
     */
    @Update
    suspend fun update(entry: LogEntry)

    /**
     * Delete a log entry from the database
     */
    @Delete
    suspend fun delete(entry: LogEntry)

    /**
     * Get all log entries sorted by timestamp (newest first)
     * Returns a LiveData<List<LogEntry>> so UI automatically updates when data changes
     * suspend = coroutine function (runs without blocking main thread)
     */
    @Query("SELECT * FROM log_entries ORDER BY timestamp DESC")
    fun getAllEntries(): LiveData<List<LogEntry>>

    /**
     * Get a single entry by its ID
     */
    @Query("SELECT * FROM log_entries WHERE id = :id")
    suspend fun getEntryById(id: Long): LogEntry?

    /**
     * Get entries that haven't been synced to Firebase yet
     * Used when syncing data to cloud
     */
    @Query("SELECT * FROM log_entries WHERE isSynced = 0")
    suspend fun getUnsyncedEntries(): List<LogEntry>

    /**
     * Mark an entry as synced to Firebase
     */
    @Query("UPDATE log_entries SET isSynced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: Long)

    /**
     * Search entries by text content (for search feature)
     */
    @Query("SELECT * FROM log_entries WHERE content LIKE :searchQuery OR title LIKE :searchQuery ORDER BY timestamp DESC")
    fun searchEntries(searchQuery: String): LiveData<List<LogEntry>>
}

