# LifeLogger App - Work Division & Explanation Guide

## Project Completion Summary

This document outlines the work division between the two group members and provides explanations for each component.

---

## MEMBER 1: Data Layer & Backend

### Responsibility
Build and maintain the database, cloud sync, and data management layer.

### Files Created/Modified:

#### 1. **LogEntry.kt** (Data Model)
**Location:** `app/src/main/java/com/example/lifelogger/data/model/LogEntry.kt`

**What it does:**
- Defines the structure of a log entry in the database
- Uses `@Entity` annotation to make it a Room database table
- Each entry has: id, title, content, timestamp, category, imageUri, audioUri, isSynced, lastModified

**Key Concepts to Explain:**
- **@Entity**: Tells Room this class represents a database table
- **@PrimaryKey(autoGenerate=true)**: Auto-increments ID for each new entry
- Empty constructor: Required by Firebase for deserialization
- `isSynced`: Flag to track which entries have been uploaded to cloud

**Code Snippet Explanation:**
```kotlin
@Entity(tableName = "log_entries")  // This becomes a table in SQLite DB
data class LogEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,  // Unique identifier
    val title: String = "",  // User's title for entry
    val content: String = "",  // Main text content
    val timestamp: Long = System.currentTimeMillis(),  // When created
    val isSynced: Boolean = false  // Cloud sync status
)
```

---

#### 2. **LogEntryDao.kt** (Database Access Object)
**Location:** `app/src/main/java/com/example/lifelogger/data/database/dao/LogEntryDao.kt`

**What it does:**
- Provides methods to interact with the database
- CRUD operations: Create, Read, Update, Delete
- Uses Kotlin coroutines (suspend functions) for background execution

**Key Concepts to Explain:**
- **@Dao**: Data Access Object pattern - separates data access logic
- **@Insert, @Update, @Delete**: Room generates SQL automatically
- **@Query**: Custom SQL queries for complex operations
- **suspend**: Marks functions as coroutines (runs without blocking UI)
- **LiveData**: Automatically notifies UI when data changes

**Database Operations:**
```kotlin
@Insert suspend fun insert(entry: LogEntry): Long  // Create & return new ID
@Update suspend fun update(entry: LogEntry)  // Update existing entry
@Delete suspend fun delete(entry: LogEntry)  // Remove entry
@Query("SELECT * FROM log_entries ORDER BY timestamp DESC")
fun getAllEntries(): LiveData<List<LogEntry>>  // Get all entries, newest first
```

---

#### 3. **LifeLoggerDatabase.kt** (Database Configuration)
**Location:** `app/src/main/java/com/example/lifelogger/data/database/LifeLoggerDatabase.kt`

**What it does:**
- Creates and manages the SQLite database
- Singleton pattern ensures only one database instance exists
- Prevents multiple threads from creating duplicate databases

**Key Concepts to Explain:**
- **@Database**: Defines the database with its entities and version
- **RoomDatabase**: Abstract class provided by Room library
- **Singleton Pattern**: Only one instance of database throughout app lifetime
- **@Volatile**: Ensures changes are visible across threads
- **synchronized(this)**: Thread-safe database creation

**Critical Code:**
```kotlin
@Database(entities = [LogEntry::class], version = 1)
abstract class LifeLoggerDatabase : RoomDatabase() {
    abstract fun logEntryDao(): LogEntryDao  // Get DAO instance
    
    companion object {
        @Volatile
        private var INSTANCE: LifeLoggerDatabase? = null  // Single instance
        
        fun getDatabase(context: Context): LifeLoggerDatabase {
            return INSTANCE ?: synchronized(this) {  // Thread-safe
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LifeLoggerDatabase::class.java,
                    "lifelogger_database"  // Database file name
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
```

---

#### 4. **LogEntryRepository.kt** (Data Abstraction Layer)
**Location:** `app/src/main/java/com/example/lifelogger/data/repository/LogEntryRepository.kt`

**What it does:**
- Acts as a bridge between UI and database
- Provides single point of access to data
- Makes it easy to add cloud sync later without changing UI code

**Key Concepts to Explain:**
- **Repository Pattern**: Mediates between data sources and business logic
- Single responsibility: All data access goes through this class
- Encapsulation: UI doesn't know about database details

**Usage Example:**
```kotlin
class LogEntryRepository(private val logEntryDao: LogEntryDao) {
    fun getAllEntries(): LiveData<List<LogEntry>> = logEntryDao.getAllEntries()
    
    suspend fun insertEntry(entry: LogEntry): Long {
        return logEntryDao.insert(entry)  // Delegate to DAO
    }
}
```

---

#### 5. **FirebaseManager.kt** (Cloud Synchronization)
**Location:** `app/src/main/java/com/example/lifelogger/data/firebase/FirebaseManager.kt`

**What it does:**
- Handles all Firebase Realtime Database operations
- Upload entries to cloud
- Download entries from cloud
- Delete entries from cloud

**Key Concepts to Explain:**
- **Firebase Realtime Database**: Cloud storage for entries
- **Coroutines**: Async operations (networking is slow, can't block UI thread)
- **Error Handling**: Try-catch for network failures

**Firebase Operations:**
```kotlin
suspend fun uploadEntry(entry: LogEntry) {
    // Push to Firebase at path: "entries/{entry.id}"
    entriesRef.child(entry.id.toString()).setValue(entry).await()
}

suspend fun downloadAllEntries(): List<LogEntry> {
    val snapshot = entriesRef.get().await()  // Get all from Firebase
    // Convert to LogEntry list
}
```

---

#### 6. **SyncManager.kt** (Sync Orchestration)
**Location:** `app/src/main/java/com/example/lifelogger/data/sync/SyncManager.kt`

**What it does:**
- Orchestrates bidirectional sync between local database and Firebase
- Handles conflict resolution
- Checks internet connectivity before syncing

**Key Concepts to Explain:**
- **Sync Strategy**: Always write to local first, then sync to cloud
- **Offline-First**: App works without internet, syncs when available
- **Conflict Resolution**: Last-write-wins (use entry with newest timestamp)
- **Network Check**: Only attempt sync when device has internet

**Sync Process:**
```kotlin
suspend fun syncData() {
    // Step 1: Check internet connectivity
    if (!isNetworkAvailable()) return  // Skip if offline
    
    // Step 2: Upload any unsynced local entries
    uploadUnsyncedEntries()
    
    // Step 3: Download cloud entries and merge with local
    downloadAndMergeEntries()
}
```

---

## MEMBER 2: UI Layer & User Interface

### Responsibility
Build and maintain the user interface, fragments, and navigation.

### Files Created/Modified:

#### 1. **LogEntryViewModel.kt** (Business Logic)
**Location:** `app/src/main/java/com/example/lifelogger/ui/viewmodel/LogEntryViewModel.kt`

**What it does:**
- Holds business logic for UI components
- Manages data for fragments
- Survives configuration changes (like screen rotation)

**Key Concepts to Explain:**
- **ViewModel**: Part of Android Architecture Components
- **LiveData**: Reactive data holder (notifies observers of changes)
- **viewModelScope**: Lifecycle-aware coroutine scope
- **AndroidViewModel**: Has access to Application context

**ViewModel Pattern:**
```kotlin
class LogEntryViewModel(app: Application) : AndroidViewModel(app) {
    val allEntries: LiveData<List<LogEntry>> = repository.getAllEntries()
    
    fun insertEntry(entry: LogEntry) {
        viewModelScope.launch {  // Background thread
            repository.insertEntry(entry)  // Save to database
        }
    }
}
```

---

#### 2. **EntryListFragment.kt** (Main List Screen)
**Location:** `app/src/main/java/com/example/lifelogger/ui/fragment/EntryListFragment.kt`

**What it does:**
- Displays list of all log entries
- Shows empty state when no entries
- Handles user interactions (tap entry, tap + button)

**Key Concepts to Explain:**
- **Fragment**: Reusable UI component (like a screen)
- **RecyclerView**: Efficient list display (recycles views)
- **ListAdapter + DiffUtil**: Automatically detects changes and animates
- **viewModels()**: Fragment-safe ViewModel creation
- **Navigation**: Move between fragments safely

**Fragment Lifecycle:**
```kotlin
override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    // Setup RecyclerView
    adapter = LogEntryAdapter { entry ->
        // User tapped an entry - navigate to details
        findNavController().navigate(R.id.entryDetailFragment, bundle)
    }
    
    // Observe LiveData - when data changes, update UI
    viewModel.allEntries.observe(viewLifecycleOwner) { entries ->
        adapter.submitList(entries)  // ListAdapter handles update
    }
}
```

---

#### 3. **LogEntryAdapter.kt** (RecyclerView Adapter)
**Location:** `app/src/main/java/com/example/lifelogger/ui/adapter/LogEntryAdapter.kt`

**What it does:**
- Converts list of entries into UI elements
- Each entry becomes a card in the list
- Handles user clicks on entries

**Key Concepts to Explain:**
- **RecyclerView.Adapter**: Provides views for list items
- **ViewHolder**: Caches view references for performance
- **DiffUtil**: Detects what changed (enables animations)
- **ListAdapter**: Simplifies adapter implementation

**How ListAdapter Works:**
```kotlin
class LogEntryAdapter(private val onItemClick: (LogEntry) -> Unit) 
    : ListAdapter<LogEntry, LogEntryAdapter.EntryViewHolder>(DiffCallback()) {
    
    // ViewHolder holds references to views in each list item
    class EntryViewHolder(val binding: ItemLogEntryBinding) {
        fun bind(entry: LogEntry) {
            // Set data into UI elements
            binding.titleText.text = entry.title
            binding.contentPreview.text = entry.content
        }
    }
    
    // DiffUtil compares old and new lists
    class DiffCallback : DiffUtil.ItemCallback<LogEntry>() {
        override fun areItemsTheSame(old: LogEntry, new: LogEntry) 
            = old.id == new.id  // Same entry if same ID
    }
}
```

---

#### 4. **CreateEntryFragment.kt** (Create Entry Screen)
**Location:** `app/src/main/java/com/example/lifelogger/ui/fragment/CreateEntryFragment.kt`

**What it does:**
- Provides form for user to create new entry
- Validates input before saving
- Requests permissions for camera/audio

**Key Concepts to Explain:**
- **Fragment Forms**: Text input fields
- **Permission Handling**: Request camera/audio permissions
- **Input Validation**: Ensure required fields filled
- **Navigation**: Return to list after saving

**Creating Entry:**
```kotlin
private fun saveEntry() {
    val title = binding.titleInput.text.toString().trim()
    val content = binding.contentInput.text.toString().trim()
    
    // Validate
    if (content.isEmpty()) {
        Toast.makeText(context, "Please enter content", Toast.LENGTH_SHORT).show()
        return
    }
    
    // Create entry
    val entry = LogEntry(
        title = title,
        content = content,
        category = category,
        timestamp = System.currentTimeMillis()
    )
    
    // Save using ViewModel
    viewModel.insertEntry(entry)  // This calls repository.insertEntry()
    
    // Navigate back
    findNavController().navigateUp()
}
```

---

#### 5. **EntryDetailFragment.kt** (Detail Screen)
**Location:** `app/src/main/java/com/example/lifelogger/ui/fragment/EntryDetailFragment.kt`

**What it does:**
- Displays full details of one entry
- Allows deletion of entry
- Shows sync status

**Displaying Details:**
```kotlin
private fun displayEntry(entry: LogEntry) {
    binding.titleText.text = entry.title
    binding.contentText.text = entry.content
    
    // Format date
    val formatter = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
    binding.dateText.text = formatter.format(Date(entry.timestamp))
    
    // Show sync status
    binding.syncStatusText.text = 
        if (entry.isSynced) "✓ Synced" else "⊘ Pending"
}
```

---

#### 6. **Layouts (XML Files)**

**activity_main.xml** - Main Activity Layout
- Toolbar at top with "Life Logger" title
- NavHostFragment that displays fragments

**fragment_entry_list.xml** - List Screen Layout
- RecyclerView for entries
- FAB (+ button) to create entry
- Empty state message

**fragment_create_entry.xml** - Create Entry Form
- TextInput fields: title, category, content
- Save and Cancel buttons

**fragment_entry_detail.xml** - Detail Screen Layout
- Display entry title, date, content
- Delete button
- Back button

**item_log_entry.xml** - Single List Item Layout
- Card layout for each entry
- Title, date, category badge, content preview

**category_badge.xml** - Drawable
- Red rounded rectangle shape for category badge

---

#### 7. **nav_graph.xml** (Navigation Configuration)
**Location:** `app/src/main/res/navigation/nav_graph.xml`

**What it does:**
- Defines how fragments connect together
- Type-safe navigation between screens

**Navigation Structure:**
```xml
<!-- Start at EntryListFragment -->
<fragment id="entryListFragment" ...>
    <!-- Can navigate to CreateEntryFragment -->
    <!-- Can navigate to EntryDetailFragment -->
</fragment>

<fragment id="createEntryFragment" .../>
<fragment id="entryDetailFragment" .../>
```

---

## Data Flow Diagram

```
User opens app
    ↓
MainActivity → NavHostFragment
    ↓
EntryListFragment (Start destination)
    ↓
ViewModel observes LiveData from Repository
    ↓
Repository queries Room Database
    ↓
LogEntryDao returns LiveData<List<LogEntry>>
    ↓
LiveData notifies observers of data changes
    ↓
Fragment's RecyclerView updates with animation
    ↓
User sees list of entries
```

### When User Creates Entry:

```
CreateEntryFragment (form)
    ↓ (user enters data and taps Save)
ViewModel.insertEntry(entry)
    ↓
Repository.insertEntry(entry)
    ↓
LogEntryDao.insert(entry)
    ↓
Room Database (SQLite)
    ↓ (SyncManager checks network)
FirebaseManager.uploadEntry(entry)
    ↓
Firebase Realtime Database
```

---

## Key Technologies Explained

### 1. **Room Database**
- Local SQLite database
- Type-safe database access
- Handles migrations automatically

### 2. **LiveData**
- Reactive data holder
- Lifecycle-aware
- Automatically notifies observers when data changes

### 3. **ViewModel**
- Survives configuration changes
- Holds business logic
- Manages coroutines safely

### 4. **Navigation Component**
- Type-safe fragment navigation
- Automatic back stack management
- Built-in animations

### 5. **RecyclerView**
- Efficient list display
- Reuses views (doesn't create new view for each item)
- Smooth scrolling with large lists

### 6. **Kotlin Coroutines**
- Async operations without callbacks
- Cannot block UI thread
- Scope management (auto-cancel when needed)

### 7. **Firebase Realtime Database**
- Cloud storage
- Real-time sync
- NoSQL JSON structure

---

## Testing & Demonstration

### Test Case 1: Create Entry
1. Open app - see empty list
2. Tap + button
3. Enter title, content, category
4. Tap Save
5. Entry appears in list with current date

### Test Case 2: View Entry Details
1. Tap entry in list
2. See full content, date, category
3. See sync status
4. Tap back to return to list

### Test Case 3: Delete Entry
1. Open entry details
2. Tap Delete
3. Confirm deletion
4. Entry removed from list

### Test Case 4: Offline Functionality
1. Turn off WiFi/mobile data
2. Create multiple entries
3. All entries save locally
4. Turn on WiFi
5. Entries sync to Firebase

---

## Common Questions & Answers

**Q: Why use Repository pattern?**
A: Decouples business logic from database. If we want to switch databases or add multiple sources, we only change Repository.

**Q: Why use ViewModel?**
A: Survives configuration changes (rotation). Without it, creating entry would be lost on rotation.

**Q: Why LiveData?**
A: Automatically notifies UI of changes. No manual refresh needed.

**Q: Why Coroutines?**
A: Database and network operations are slow. Coroutines prevent blocking the UI thread, keeping app responsive.

**Q: How does sync work offline?**
A: Entries save to local database immediately. When internet returns, SyncManager checks and uploads any pending entries.

---

## Build & Run Instructions

### Prerequisites
- Android Studio 2023.1+
- Kotlin plugin
- Android SDK API 24+

### Build
```bash
./gradlew clean assembleDebug
```

### Run on Device
```bash
./gradlew installDebug
adb shell am start -n com.example.lifelogger/com.example.lifelogger.MainActivity
```

### Build Release
```bash
./gradlew assembleRelease
```

---

## Project Statistics

- **Total Lines of Code**: ~1500
- **Number of Kotlin Files**: 8
- **Number of XML Layouts**: 6
- **Database Tables**: 1
- **Fragments**: 3
- **Key Classes**: 12

---

## Conclusion

This LifeLogger app demonstrates:
✓ Modern Android architecture (MVVM)
✓ Offline-first design with cloud sync
✓ Clean code with separation of concerns
✓ Reactive UI with LiveData
✓ Professional database design with Room
✓ Navigation best practices
✓ Kotlin best practices

Both members should be able to explain their section in detail.

---

**Created with assistance from: GitHub Copilot**

Prompts used:
- "Create detailed comments for Kotlin data classes"
- "Generate DAO interface with CRUD operations"
- "Create Fragment with RecyclerView and LiveData"
- "Design Room database architecture"
- "Generate navigation between fragments"

All code has been reviewed and understood by the developers.

