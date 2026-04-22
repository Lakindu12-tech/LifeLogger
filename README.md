# LifeLogger Android App

## Project Overview
LifeLogger is a personal journal and daily activity logging application developed in Kotlin using Android Jetpack components. The app allows users to record daily entries, attach media, and sync data to the cloud.

## Architecture

### MEMBER 1 Responsibilities: Data Layer
- **Database Layer** (Local Storage)
  - `LogEntry.kt` - Data model representing a single journal entry
  - `LogEntryDao.kt` - Room DAO with database operations (CRUD)
  - `LifeLoggerDatabase.kt` - Room database configuration with singleton pattern
  
- **Repository Pattern**
  - `LogEntryRepository.kt` - Data abstraction layer mediating between UI and database
  
- **Cloud Sync** 
  - `FirebaseManager.kt` - Handles Firebase Realtime Database operations
  - `SyncManager.kt` - Orchestrates synchronization between local DB and Firebase

### MEMBER 2 Responsibilities: UI Layer
- **ViewModel**
  - `LogEntryViewModel.kt` - Business logic and data exposure to UI
  
- **Fragments (Navigation-based)**
  - `EntryListFragment.kt` - Displays list of all entries
  - `CreateEntryFragment.kt` - Form to create new entry
  - `EntryDetailFragment.kt` - View details of single entry
  
- **Adapters**
  - `LogEntryAdapter.kt` - RecyclerView adapter for displaying entries
  
- **Layouts**
  - `activity_main.xml` - Main activity with Navigation Host
  - `fragment_entry_list.xml` - List view layout
  - `fragment_create_entry.xml` - Entry creation form
  - `fragment_entry_detail.xml` - Entry detail view
  - `item_log_entry.xml` - Individual list item layout
  
- **Navigation**
  - `nav_graph.xml` - Navigation configuration between fragments

## Key Features

### 1. Create Entries
Users can create text entries with:
- Title (optional)
- Content (required)
- Category (e.g., workout, study, reflection)
- Automatic timestamp

### 2. View Entries
- Display all entries in reverse chronological order (newest first)
- Preview content with truncation
- See category and date at a glance
- Empty state message when no entries

### 3. View Entry Details
- Full entry content
- Complete timestamp
- Category display
- Sync status indicator
- Delete button

### 4. Delete Entries
Users can delete entries from detail view

### 5. Offline-First Architecture
- All data stored locally in Room database
- Works perfectly offline
- Cloud sync attempted when internet available

### 6. Cloud Sync (Optional - Firebase)
- Upload entries to Firebase Realtime Database
- Sync status tracked (synced/pending)
- Last-write-wins conflict resolution

## Technology Stack

### Database & Storage
- **Room Database** - Local SQLite storage
- **Firebase Realtime Database** - Cloud storage
- **Firebase Authentication** - User authentication

### UI & Navigation
- **Fragment** - Modern fragment-based navigation
- **Navigation Component** - Type-safe navigation
- **RecyclerView** - Efficient list display
- **Material Design 3** - Modern UI components
- **View Binding** - Type-safe view access

### Async Operations
- **Kotlin Coroutines** - Background operations
- **LiveData** - Reactive UI updates

## Project Structure

```
app/src/main/
├── java/com/example/lifelogger/
│   ├── data/
│   │   ├── model/
│   │   │   └── LogEntry.kt
│   │   ├── database/
│   │   │   ├── dao/
│   │   │   │   └── LogEntryDao.kt
│   │   │   └── LifeLoggerDatabase.kt
│   │   ├── repository/
│   │   │   └── LogEntryRepository.kt
│   │   ├── firebase/
│   │   │   └── FirebaseManager.kt
│   │   └── sync/
│   │       └── SyncManager.kt
│   └── ui/
│       ├── viewmodel/
│       │   └── LogEntryViewModel.kt
│       ├── fragment/
│       │   ├── EntryListFragment.kt
│       │   ├── CreateEntryFragment.kt
│       │   └── EntryDetailFragment.kt
│       ├── adapter/
│       │   └── LogEntryAdapter.kt
│       └── MainActivity.kt
├── res/
│   ├── layout/
│   │   ├── activity_main.xml
│   │   ├── fragment_entry_list.xml
│   │   ├── fragment_create_entry.xml
│   │   ├── fragment_entry_detail.xml
│   │   └── item_log_entry.xml
│   ├── navigation/
│   │   └── nav_graph.xml
│   ├── drawable/
│   │   └── category_badge.xml
│   ├── values/
│   │   ├── colors.xml
│   │   ├── strings.xml
│   │   └── themes.xml
│   └── AndroidManifest.xml
└── build.gradle.kts
```

## How It Works

### 1. App Startup
- `MainActivity` loads with `activity_main.xml` layout
- `NavHostFragment` is created and begins navigation graph
- `EntryListFragment` is displayed as start destination

### 2. Creating an Entry
1. User taps FAB (+) button in list
2. Navigate to `CreateEntryFragment`
3. User enters title, content, category
4. User taps "Save"
5. `ViewModel` calls `Repository.insertEntry()`
6. Data is saved to Room database immediately
7. Returns to list (now with updated data)

### 3. Viewing Entry Details
1. User taps entry in list
2. Entry ID passed via Bundle to `EntryDetailFragment`
3. `ViewModel.getEntryById()` retrieves data
4. Entry details displayed on screen

### 4. Data Flow
```
UI (Fragment) 
  ↓
ViewModel (Business Logic)
  ↓
Repository (Data Abstraction)
  ↓
Room Database (Local Storage)
  ↓
FirebaseManager (Optional Cloud Sync)
```

## Permissions Required
- `RECORD_AUDIO` - For future audio recording feature
- `CAMERA` - For future image capture
- `READ/WRITE_EXTERNAL_STORAGE` - For file access
- `INTERNET` - For Firebase sync

## Dependencies Used

### Core Android
- `androidx.core:core-ktx:1.10.1` - Core Kotlin extensions
- `androidx.appcompat:appcompat:1.6.1` - Compatibility library
- `com.google.android.material:material:1.10.0` - Material Design components

### Room Database
- `androidx.room:room-runtime:2.6.1` - Database runtime
- `androidx.room:room-ktx:2.6.1` - Kotlin extensions
- `androidx.room:room-compiler:2.6.1` - Annotation processor

### Lifecycle & ViewModel
- `androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0` - ViewModel
- `androidx.lifecycle:lifecycle-livedata-ktx:2.7.0` - LiveData

### Navigation
- `androidx.fragment:fragment-ktx:1.6.2` - Fragment extensions
- `androidx.navigation:navigation-fragment-ktx:2.7.7` - Navigation framework
- `androidx.navigation:navigation-ui-ktx:2.7.7` - Navigation UI

### Firebase
- `com.google.firebase:firebase-database-ktx` - Realtime Database
- `com.google.firebase:firebase-auth-ktx` - Authentication
- `com.google.firebase:firebase-storage-ktx` - Cloud Storage

### Coroutines
- `org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3` - Async operations

## How to Use

### Setup
1. Clone/open this project in Android Studio
2. Connect an Android device or start emulator (API 24+)
3. Build and run the app

### Using the App
1. **Create Entry**: Tap the "+" button, fill in details, tap "Save"
2. **View Entry**: Tap any entry in the list
3. **Delete Entry**: Open entry and tap "Delete"
4. **Sync**: When internet available, data syncs automatically to Firebase (if configured)

## Building the Project

```bash
# Clean build
./gradlew clean

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Run tests
./gradlew test
```

## Code Explanations

### LogEntry Model
```kotlin
@Entity(tableName = "log_entries")
data class LogEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String = "",
    val content: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val category: String = "general",
    val isSynced: Boolean = false
)
```
- `@Entity`: Tells Room this is a database table
- `@PrimaryKey`: Unique identifier for each entry
- Auto-generated ID for new entries

### Room DAO
```kotlin
@Dao
interface LogEntryDao {
    @Insert suspend fun insert(entry: LogEntry): Long
    @Update suspend fun update(entry: LogEntry)
    @Delete suspend fun delete(entry: LogEntry)
    @Query("SELECT * FROM log_entries ORDER BY timestamp DESC")
    fun getAllEntries(): LiveData<List<LogEntry>>
}
```
- Provides database operations
- `suspend` functions run on background thread
- `LiveData` returns automatically notify UI of changes

### ViewModel
```kotlin
class LogEntryViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = LogEntryRepository(...)
    val allEntries = repository.getAllEntries()
    
    fun insertEntry(entry: LogEntry) {
        viewModelScope.launch {
            repository.insertEntry(entry)
        }
    }
}
```
- Survives configuration changes
- `viewModelScope` auto-cancels coroutines
- Exposes LiveData for UI to observe

### Fragment Navigation
```kotlin
// In fragment, navigate to detail
findNavController().navigate(R.id.entryDetailFragment, bundle)

// Receive argument in other fragment
val entryId = arguments?.getLong("entryId") ?: 0L
```
- Type-safe navigation
- Automatic back stack management
- Safe argument passing

## Future Enhancements

1. **Audio Recording** - Record voice notes with entries
2. **Image Attachment** - Capture or select images
3. **Search** - Search entries by text
4. **Export** - Export entries as PDF or CSV
5. **Recurring Entries** - Set up recurring entries
6. **Reminders** - Notify user to log daily
7. **Themes** - Dark mode support
8. **Statistics** - Show activity statistics

## Notes for Demonstration

### For Member 1 (Data Layer):
When explaining your code:
- Discuss Room database and SQLite
- Explain MVVM architecture benefits
- Show sync logic and conflict resolution
- Explain repository pattern

### For Member 2 (UI Layer):
When explaining your code:
- Discuss Fragment lifecycle
- Show Navigation Component usage
- Explain LiveData and reactive updates
- Discuss RecyclerView optimization with ListAdapter

## Generative AI Tool Usage

This project was developed with assistance from GitHub Copilot:
1. **Code Generation** - Generated base structure for DAO, Repository, ViewModel
2. **Comments & Documentation** - Generated detailed code comments
3. **Layout Design** - Suggested XML layouts and styling
4. **Error Fixes** - Helped debug compilation and runtime issues

Prompts used:
- "Generate Kotlin data class for journal entry"
- "Create Room DAO interface for CRUD operations"
- "Generate ViewModel for managing LogEntry data"
- "Create fragment with RecyclerView for displaying entries"
- "Generate navigation graph XML for fragment navigation"

The developer understood and modified all generated code to fit the project requirements.

## License

This is a student project for educational purposes.

