package com.example.lifelogger.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.lifelogger.data.database.dao.LogEntryDao
import com.example.lifelogger.data.model.LogEntry

/**
 * MEMBER 1 RESPONSIBILITY: Data Layer - Room Database Setup
 *
 * This is the main database class that:
 * 1. Defines the database schema (entities = tables)
 * 2. Provides access to DAOs (data access objects)
 * 3. Is created as a singleton (only one instance throughout app lifetime)
 *
 * @Database annotation tells Room:
 * - entities: which classes are database tables
 * - version: the schema version (increment when making changes)
 * - exportSchema: whether to keep schema history (for migrations)
 */
@Database(entities = [LogEntry::class], version = 1, exportSchema = false)
abstract class LifeLoggerDatabase : RoomDatabase() {

    /**
     * Get the DAO for database operations
     * Abstract function - Room implements it automatically
     */
    abstract fun logEntryDao(): LogEntryDao

    companion object {
        // Volatile: changes to this are visible to all threads
        // Used to ensure only one database instance exists
        @Volatile
        private var INSTANCE: LifeLoggerDatabase? = null

        /**
         * Get the database instance (singleton pattern)
         * If it doesn't exist, create it. If it exists, return the existing one.
         *
         * synchronized ensures only one thread can create the database at a time
         * (prevents race conditions where multiple threads try to create DB)
         */
        fun getDatabase(context: Context): LifeLoggerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LifeLoggerDatabase::class.java,
                    "lifelogger_database" // Name of the database file
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

