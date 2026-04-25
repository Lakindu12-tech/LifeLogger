package com.example.lifelogger.data.supabase

import android.util.Log
import com.example.lifelogger.data.model.LogEntry
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * SupabaseManager handles all cloud synchronization and authentication using Supabase:
 * - User Authentication (Login/Register)
 * - Upload entries to Supabase PostgreSQL table
 * - Download entries from Supabase
 * - Delete entries from Supabase
 */
class SupabaseManager {

    companion object {
        private const val TAG = "SupabaseManager"
        private const val TABLE_ENTRIES = "log_entries"
        
        // TODO: Replace with your actual Supabase URL and Anon Key
        private const val SUPABASE_URL = "https://imqpnudwhsrcqwyyypsv.supabase.co"
        private const val SUPABASE_KEY = "sb_publishable_od0s5SfonOau3RNCQ4ey-Q_vD-UCXZj"
    }

    val client: SupabaseClient by lazy {
        createSupabaseClient(SUPABASE_URL, SUPABASE_KEY) {
            install(Postgrest)
            install(Auth)
            install(Storage)
        }
    }

    /**
     * Authentication helper
     */
    val auth = client.auth

    /**
     * Check if Supabase is configured with real values
     */
    fun isAvailable(): Boolean {
        return SUPABASE_URL.isNotBlank() &&
            SUPABASE_KEY.isNotBlank() &&
            !SUPABASE_URL.contains("your-project", ignoreCase = true) &&
            !SUPABASE_KEY.contains("your-anon-key", ignoreCase = true)
    }

    fun currentUserId(): String = client.auth.currentSessionOrNull()?.user?.id.orEmpty()

    /**
     * Upload or Update an entry in Supabase
     */
    suspend fun uploadEntry(entry: LogEntry) = withContext(Dispatchers.IO) {
        try {
            val userId = currentUserId()
            if (userId.isBlank()) {
                Log.d(TAG, "Skipping upload: no active authenticated user")
                return@withContext
            }

            val entryToUpload = if (entry.userId.isBlank()) entry.copy(userId = userId) else entry
            client.postgrest[TABLE_ENTRIES].upsert(entryToUpload)
            Log.d(TAG, "Entry ${entry.id} upserted to Supabase")
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading to Supabase: ${e.message}")
            throw e
        }
    }

    /**
     * Download all entries from Supabase
     */
    suspend fun downloadEntriesForCurrentUser(): List<LogEntry> = withContext(Dispatchers.IO) {
        try {
            val userId = currentUserId()
            if (userId.isBlank()) {
                Log.d(TAG, "Skipping download: no active authenticated user")
                return@withContext emptyList()
            }

            val entries = client.postgrest[TABLE_ENTRIES]
                .select {
                    filter {
                        eq("userId", userId)
                    }
                    order("timestamp", Order.DESCENDING)
                }
                .decodeList<LogEntry>()
            Log.d(TAG, "Downloaded ${entries.size} entries from Supabase")
            entries
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading from Supabase: ${e.message}")
            emptyList()
        }
    }

    /**
     * Delete an entry from Supabase
     */
    suspend fun deleteEntry(entryId: Long) = withContext(Dispatchers.IO) {
        try {
            val userId = currentUserId()
            if (userId.isBlank()) {
                Log.d(TAG, "Skipping delete: no active authenticated user")
                return@withContext
            }

            client.postgrest[TABLE_ENTRIES].delete {
                filter {
                    eq("id", entryId)
                    eq("userId", userId)
                }
            }
            Log.d(TAG, "Entry $entryId deleted from Supabase")
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting from Supabase: ${e.message}")
            throw e
        }
    }
}
