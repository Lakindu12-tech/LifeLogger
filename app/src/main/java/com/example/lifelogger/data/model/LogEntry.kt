package com.example.lifelogger.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * MEMBER 1 RESPONSIBILITY: Data Layer
 *
 * LogEntry represents a single journal/log entry by the user.
 * It contains:
 * - Text content (what the user wrote)
 * - Timestamp (when it was created)
 * - List of attached images and audio files
 * - Sync status (whether it's been uploaded to cloud)
 *
 * @Entity annotation tells Room this is a database table
 * @PrimaryKey ensures each entry has a unique ID
 * @Serializable allows Supabase/Kotlinx-serialization to handle the data
 */
@Entity(tableName = "log_entries")
@Serializable
data class LogEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // The user who owns this entry (from Supabase Auth)
    val userId: String = "",

    // The main text content written by the user
    val title: String = "",
    val content: String = "",
    
    // ... rest of the fields
    val timestamp: Long = System.currentTimeMillis(),
    val category: String = "general",
    val imageUri: String = "",
    val audioUri: String = "",
    val isSynced: Boolean = false,
    val lastModified: Long = System.currentTimeMillis()
)


