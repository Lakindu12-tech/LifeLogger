package com.example.lifelogger.data.supabase

import android.content.Context
import android.net.Uri
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
import java.io.File

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

        // Android app uses the Supabase REST client with the public project URL and publishable key.
        // The PostgreSQL connection string is not used inside the mobile app.
        private const val SUPABASE_URL = "https://iaouuzdervnurhhqlzvc.supabase.co"
        private const val SUPABASE_KEY = "sb_publishable_VNdf44jszD9OYc5IzN0SOw_Lwb6F6aq"
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
        return SUPABASE_URL == "https://iaouuzdervnurhhqlzvc.supabase.co" &&
            SUPABASE_KEY == "sb_publishable_VNdf44jszD9OYc5IzN0SOw_Lwb6F6aq"
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

     /**
      * Upload image file to Supabase Storage
      * Returns the storage path (bucket_path) that should be stored in database
      */
     suspend fun uploadImage(context: Context, imageUri: Uri, userId: String, entryId: Long): String? = withContext(Dispatchers.IO) {
         try {
             if (userId.isBlank() || entryId <= 0) {
                 Log.w(TAG, "Cannot upload image: invalid userId or entryId")
                 return@withContext null
             }

             // Read file from URI and get bytes
             val inputStream = context.contentResolver.openInputStream(imageUri)
             val bytes = inputStream?.readBytes() ?: run {
                 Log.w(TAG, "Could not read image file")
                 return@withContext null
             }

             // Generate storage path: users/{userId}/entries/{entryId}/image.jpg
             val storagePath = "users/$userId/entries/$entryId/image.jpg"

             // Upload to Supabase Storage
             val bucket = client.storage.from("entry_media")
             bucket.upload(storagePath, bytes, upsert = true)
             Log.d(TAG, "Image uploaded to storage: $storagePath")
             storagePath
         } catch (e: Exception) {
             Log.e(TAG, "Error uploading image: ${e.message}")
             null
         }
     }

     /**
      * Upload audio file to Supabase Storage
      * Returns the storage path (bucket_path) that should be stored in database
      */
     suspend fun uploadAudio(audioFilePath: String, userId: String, entryId: Long): String? = withContext(Dispatchers.IO) {
         try {
             if (userId.isBlank() || entryId <= 0 || audioFilePath.isEmpty()) {
                 Log.w(TAG, "Cannot upload audio: invalid userId, entryId, or path")
                 return@withContext null
             }

             val audioFile = File(audioFilePath)
             if (!audioFile.exists()) {
                 Log.w(TAG, "Audio file does not exist: $audioFilePath")
                 return@withContext null
             }

             val bytes = audioFile.readBytes()
             if (bytes.isEmpty()) {
                 Log.w(TAG, "Audio file is empty")
                 return@withContext null
             }

             // Generate storage path: users/{userId}/entries/{entryId}/audio.m4a
             val storagePath = "users/$userId/entries/$entryId/audio.m4a"

             // Upload to Supabase Storage
             val bucket = client.storage.from("entry_media")
             bucket.upload(storagePath, bytes, upsert = true)
             Log.d(TAG, "Audio uploaded to storage: $storagePath")
             storagePath
         } catch (e: Exception) {
             Log.e(TAG, "Error uploading audio: ${e.message}")
             null
         }
     }

     /**
      * Get public URL for a stored media file
      * Works for any file in Supabase Storage
      */
     fun getMediaUrl(storagePath: String): String {
         return "$SUPABASE_URL/storage/v1/object/public/entry_media/$storagePath"
     }

     /**
      * Download media file from storage path (or generate public URL)
      * For simple retrieval, just use getMediaUrl() to get the public URL
      * This method is for cases where you need to download the actual bytes
      */
     suspend fun downloadMediaBytes(storagePath: String): ByteArray? = withContext(Dispatchers.IO) {
         try {
             if (storagePath.isBlank()) {
                 Log.w(TAG, "Cannot download media: empty storagePath")
                 return@withContext null
             }

             val bucket = client.storage.from("entry_media")
             val bytes = bucket.downloadAuthenticated(storagePath)
             Log.d(TAG, "Downloaded media from storage: $storagePath (${bytes.size} bytes)")
             bytes
         } catch (e: Exception) {
             Log.e(TAG, "Error downloading media: ${e.message}")
             null
         }
     }

     /**
      * Delete media file from storage
      */
     suspend fun deleteMedia(storagePath: String) = withContext(Dispatchers.IO) {
         try {
             if (storagePath.isBlank()) return@withContext

             val bucket = client.storage.from("entry_media")
             bucket.delete(storagePath)
             Log.d(TAG, "Media deleted from storage: $storagePath")
         } catch (e: Exception) {
             Log.e(TAG, "Error deleting media: ${e.message}")
         }
     }
}
