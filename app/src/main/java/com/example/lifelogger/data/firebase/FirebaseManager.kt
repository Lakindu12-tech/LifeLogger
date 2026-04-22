package com.example.lifelogger.data.firebase

import android.util.Log
import com.example.lifelogger.data.model.LogEntry
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

/**
 * MEMBER 1 RESPONSIBILITY: Data Layer - Firebase Integration
 *
 * FirebaseManager handles all cloud synchronization:
 * - Upload entries to Firebase Realtime Database
 * - Download entries from Firebase
 * - Handle sync conflicts
 * - Authentication with Firebase
 *
 * This creates an optional cloud backup feature.
 * App works offline with local database.
 * When internet available, automatically syncs.
 */
class FirebaseManager {

    private val database: FirebaseDatabase? by lazy {
        runCatching { FirebaseDatabase.getInstance() }
            .onFailure { Log.w(TAG, "Firebase not configured. Running in local-only mode.", it) }
            .getOrNull()
    }

    companion object {
        private const val TAG = "FirebaseManager"
        private const val ENTRIES_PATH = "entries"
    }

    private fun isConfigured(): Boolean = database != null

    /**
     * Upload a single entry to Firebase
     * Called when user creates/updates an entry and device is online
     */
    suspend fun uploadEntry(entry: LogEntry) {
        val entriesRef = database?.getReference(ENTRIES_PATH)
        if (entriesRef == null) {
            Log.d(TAG, "Skipping upload - Firebase unavailable")
            return
        }
        return try {
            // Push creates a new node with auto-generated ID if entry.id is 0
            // Otherwise updates existing entry
            entriesRef.child(entry.id.toString()).setValue(entry).await()
            Log.d(TAG, "Entry ${entry.id} uploaded successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading entry: ${e.message}")
            throw e
        }
    }

    /**
     * Download all entries from Firebase
     * Called during sync to get cloud data
     */
    suspend fun downloadAllEntries(): List<LogEntry> {
        val entriesRef = database?.getReference(ENTRIES_PATH)
        if (entriesRef == null) {
            Log.d(TAG, "Skipping download - Firebase unavailable")
            return emptyList()
        }
        return try {
            val snapshot = entriesRef.get().await()
            val entries = mutableListOf<LogEntry>()

            snapshot.children.forEach { child ->
                val entry = child.getValue(LogEntry::class.java)
                if (entry != null) {
                    entries.add(entry)
                }
            }

            Log.d(TAG, "Downloaded ${entries.size} entries from Firebase")
            entries
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading entries: ${e.message}")
            emptyList()
        }
    }

    /**
     * Delete an entry from Firebase
     * Called when user deletes an entry locally
     */
    suspend fun deleteEntry(entryId: Long) {
        val entriesRef = database?.getReference(ENTRIES_PATH)
        if (entriesRef == null) {
            Log.d(TAG, "Skipping cloud delete - Firebase unavailable")
            return
        }
        return try {
            entriesRef.child(entryId.toString()).removeValue().await()
            Log.d(TAG, "Entry $entryId deleted from Firebase")
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting entry: ${e.message}")
            throw e
        }
    }

    /**
     * Check if Firebase is available (connectivity test)
     * Used to determine if sync should attempt
     */
    fun isAvailable(): Boolean {
        return isConfigured()
    }
}

