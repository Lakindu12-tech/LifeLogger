package com.example.lifelogger.data.sync

import android.content.Context
import android.util.Log
import android.net.ConnectivityManager
import com.example.lifelogger.data.repository.LogEntryRepository
import com.example.lifelogger.data.supabase.SupabaseManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * MEMBER 1 RESPONSIBILITY: Data Layer - Sync Manager
 *
 * SyncManager orchestrates the synchronization process between:
 * - Local Room Database (always works)
 * - Supabase Cloud (when internet available)
 *
 * Strategy:
 * 1. Always write to local database first (ensures data is never lost)
 * 2. When online, sync to Supabase in background
 * 3. On download, merge cloud data with local using timestamps
 * 4. Last-write-wins: if conflict, use entry with latest timestamp
 */
class SyncManager(
    private val context: Context,
    private val repository: LogEntryRepository,
    private val supabaseManager: SupabaseManager,
    private val currentUserId: String
) {

    companion object {
        private const val TAG = "SyncManager"
    }

    /**
     * Check if device has internet connectivity
     */
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return networkCapabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /**
     * Main sync function
     * Call this periodically or when app resumes
     *
     * Process:
     * 1. Check internet
     * 2. Upload unsynced local entries
     * 3. Download cloud entries and merge
     */
    suspend fun syncData() = withContext(Dispatchers.IO) {
        try {
            if (!supabaseManager.isAvailable()) {
                Log.d(TAG, "Supabase not configured - local database only mode")
                return@withContext
            }

            if (currentUserId.isBlank()) {
                Log.d(TAG, "No authenticated user - skipping sync")
                return@withContext
            }

            if (!isNetworkAvailable()) {
                Log.d(TAG, "No internet - skipping sync")
                return@withContext
            }

            Log.d(TAG, "Starting sync...")

            // Step 1: Upload unsynced entries to Supabase
            uploadUnsyncedEntries()

            // Step 2: Download entries from Supabase and merge
            downloadAndMergeEntries()

            Log.d(TAG, "Sync completed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Sync error: ${e.message}")
        }
    }

    /**
     * Upload all entries that haven't been synced yet
     */
    private suspend fun uploadUnsyncedEntries() {
        try {
            val unsyncedEntries = repository.getUnsyncedEntriesByUser(currentUserId)
            Log.d(TAG, "Found ${unsyncedEntries.size} unsynced entries to upload")

            for (entry in unsyncedEntries) {
                supabaseManager.uploadEntry(entry)
                repository.markAsSynced(entry.id)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading entries: ${e.message}")
        }
    }

    /**
     * Download entries from Supabase and merge with local database
     *
     * Merge strategy:
     * - If cloud entry doesn't exist locally: add it
     * - If cloud entry newer than local: update local
     * - If local entry newer than cloud: keep local (will upload on next sync)
     */
    private suspend fun downloadAndMergeEntries() {
        try {
            val cloudEntries = supabaseManager.downloadEntriesForCurrentUser()
            Log.d(TAG, "Downloaded ${cloudEntries.size} entries from cloud")

            for (cloudEntry in cloudEntries) {
                val localEntry = repository.getEntryByIdForUser(cloudEntry.id, currentUserId)

                when {
                    // New entry from cloud
                    localEntry == null -> {
                        repository.insertEntry(cloudEntry.copy(isSynced = true))
                        Log.d(TAG, "Merged new entry ${cloudEntry.id}")
                    }
                    // Cloud entry is newer
                    cloudEntry.lastModified > localEntry.lastModified -> {
                        repository.updateEntry(cloudEntry.copy(isSynced = true))
                        Log.d(TAG, "Updated entry ${cloudEntry.id} from cloud")
                    }
                    // Local entry is newer - will be re-uploaded
                    else -> {
                        Log.d(TAG, "Keeping local version of entry ${cloudEntry.id}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading entries: ${e.message}")
        }
    }
}

