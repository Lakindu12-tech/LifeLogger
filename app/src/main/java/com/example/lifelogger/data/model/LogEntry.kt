package com.example.lifelogger.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.database.IgnoreExtraProperties

/**
 * MEMBER 1 RESPONSIBILITY: Data Layer
 *
 * LogEntry represents a single journal/log entry by the user.
 * It contains:
 * - Text content (what the user wrote)
 * - Timestamp (when it was created)
 * - List of attached images and audio files
 * - Sync status (whether it's been uploaded to Firebase)
 *
 * @Entity annotation tells Room this is a database table
 * @PrimaryKey ensures each entry has a unique ID
 */
@Entity(tableName = "log_entries")
@IgnoreExtraProperties
data class LogEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // The main text content written by the user
    val title: String = "",
    val content: String = "",

    // When this entry was created (milliseconds since epoch)
    val timestamp: Long = System.currentTimeMillis(),

    // Category/type of entry (e.g., "workout", "study", "reflection", "event")
    val category: String = "general",

    // Paths to attached files stored locally on device
    val imageUri: String = "", // Local file path or URI
    val audioUri: String = "",  // Local file path or URI

    // Cloud sync status - to track if uploaded to Firebase
    val isSynced: Boolean = false,

    // For conflict resolution during sync
    val lastModified: Long = System.currentTimeMillis()
) {
    // Empty constructor for Firebase deserialization
    constructor() : this(
        id = 0,
        title = "",
        content = "",
        timestamp = System.currentTimeMillis(),
        category = "general",
        imageUri = "",
        audioUri = "",
        isSynced = false,
        lastModified = System.currentTimeMillis()
    )
}

