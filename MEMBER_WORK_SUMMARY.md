# LifeLogger - Member Work Summary (Quick Reference)

## Project Overview

**LifeLogger** is a personal life logger Android app built with:
- **Language:** Kotlin
- **Architecture:** MVVM (Model-View-ViewModel)
- **Local Storage:** SQLite Room Database
- **Cloud Backend:** Supabase (PostgreSQL + Auth)
- **UI Framework:** Android Jetpack Components

---

## MEMBER 1: Backend & Data Layer (Database Focus)

### Responsibilities (50%)

#### 1. Database & Local Storage
- Room entity definition (LogEntry)
- DAO (Data Access Object) implementations
- User-scoped query methods:
  - `getEntriesByUser(userId)` - All user's entries
  - `getEntryByIdForUser(id, userId)` - Single entry with ownership check
  - `getUnsyncedEntriesByUser(userId)` - Entries needing sync

#### 2. Authentication & Session
- **SessionManager.kt**
  - Offline credential storage with SHA-256 salted hashing
  - Methods: `setActiveUser()`, `getActiveUserId()`, `clearActiveUser()`
  - Methods: `saveOfflineLogin()`, `verifyOfflineLogin()`
  - Persists to SharedPreferences with Base64 encoding

- **AuthViewModel.kt** (backend logic)
  - Online login: Authenticates with Supabase, stores session
  - Offline login: Uses SessionManager to verify stored hash
  - After first successful login, offline access enabled

#### 3. Cloud Synchronization
- **SupabaseManager.kt** (data sync methods)
  - `uploadEntry()` - Upload to PostgreSQL via PostgREST
  - `downloadEntries()` - Fetch from cloud
  - Uses Row Level Security (RLS) policies for per-user isolation

- **SyncManager.kt**
  - Background sync triggered on app resume
  - Conflict resolution: last-write-wins using `lastModified` timestamp
  - Handles both insert and update scenarios

#### 4. Repository Pattern
- **LogEntryRepository.kt**
  - Abstracts local (Room) and remote (Supabase) data sources
  - User-scoped methods for safe data access
  - Implements offline-first strategy

#### 5. Database Schema & RLS
- PostgreSQL table with UUID userId and composite primary key
- RLS policies ensuring users can only see their own entries:
  - SELECT: `auth.uid() = userId`
  - INSERT: `auth.uid() = userId`
  - UPDATE: `auth.uid() = userId`
  - DELETE: `auth.uid() = userId`

### Key Files Modified by Member 1

```
Database Layer:
├── app/src/main/java/com/example/lifelogger/data/database/
│   ├── LogEntryDatabase.kt           (Room DB setup)
│   └── ...
├── app/src/main/java/com/example/lifelogger/data/dao/
│   └── LogEntryDao.kt                (All queries here)
├── app/src/main/java/com/example/lifelogger/data/model/
│   └── LogEntry.kt                   (Room entity)

Auth & Session:
├── app/src/main/java/com/example/lifelogger/data/auth/
│   ├── SessionManager.kt             (Local credential storage)
│   └── AuthViewModel.kt              (Auth logic)

Cloud Sync:
├── app/src/main/java/com/example/lifelogger/data/supabase/
│   ├── SupabaseManager.kt            (Supabase client)
│   └── (sync methods added)
├── app/src/main/java/com/example/lifelogger/data/sync/
│   └── SyncManager.kt                (Background sync)

Repository:
├── app/src/main/java/com/example/lifelogger/data/repository/
│   └── LogEntryRepository.kt         (Data abstraction)
```

### What Member 1 Implemented

✅ User registration & login with offline support  
✅ Secure credential storage (salted hash)  
✅ Local database with per-user data isolation  
✅ Cloud sync with conflict resolution  
✅ Row-level security for data protection  
✅ Background sync on app resume  
✅ Unit tests for database and auth logic  

---

## MEMBER 2: Frontend & UI Layer (User Interface Focus)

### Responsibilities (50%)

#### 1. User Authentication UI
- **LoginFragment.kt**
  - Username and password input fields
  - Login button with validation
  - Link to register
  - Dark, readable text colors
  - Permission handling for after-session logic

- **RegisterFragment.kt**
  - New account creation form
  - Username and password inputs
  - Register button
  - Error handling and toasts

#### 2. Main Dashboard
- **DashboardFragment.kt**
  - Welcome message with username
  - Gradient header design
  - "Create New Entry" button
  - "View Previous Entries" button
  - Logout menu option

#### 3. Entry List View
- **EntryListFragment.kt**
  - RecyclerView with entry cards
  - Shows: title, content preview, timestamp, category
  - Delete button for each entry
  - LiveData observer for entries
  - Logout functionality with session clearing
  - Refresh list when user switches

#### 4. Entry Detail View
- **EntryDetailFragment.kt**
  - Full entry text display
  - Image display (if attached, with error handling)
  - Audio playback button
  - Delete entry option
  - Defensive null-checks and lifecycle guards

#### 5. Create Entry Screen
- **CreateEntryFragment.kt**
  - Title input field
  - Content input (multi-line)
  - Category selector
  - **Image Selection:**
    - Launches file picker with ActivityResultContracts
    - Copies selected image to app internal storage
    - Stores local file path in database
    - Shows preview image
  - **Audio Recording:**
    - Record audio with microphone permission
    - Status display ("Recording...", "Audio attached")
    - Saves as `.m4a` file in internal storage
    - Validates file exists and has content
  - Input validation
  - Save and Cancel buttons

#### 6. Navigation Setup
- **nav_graph.xml**
  - Fragment navigation graph
  - Start destination: LoginFragment
  - Navigation actions between all screens
  - Proper backstack handling
  - Pop-up behavior on logout

#### 7. Layouts & Styling
- **fragment_login.xml** - Login form layout
- **fragment_register.xml** - Register form layout
- **fragment_dashboard.xml** - Dashboard with buttons
- **fragment_entry_list.xml** - List with RecyclerView
- **fragment_entry_detail.xml** - Entry details + media
- **fragment_create_entry.xml** - Entry creation form

- **Material Design:**
  - Dark, readable text colors
  - Gradient backgrounds
  - Proper spacing and padding
  - Material buttons and input fields
  - Card-based layouts

#### 8. ViewModel Integration
- **LogEntryViewModel.kt** (UI-side usage)
  - LiveData: `allEntries` (scoped to current user)
  - Methods: `insertEntry()`, `deleteEntry()`
  - Methods: `refreshActiveUser()` (on user switch)
  - Activity-scoped for proper lifecycle

#### 9. Media Playback (UI Side)
- Audio playback with MediaPlayer
- Play/pause controls
- Seek bar and duration display
- Error handling with graceful fallback

### Key Files Modified by Member 2

```
UI Fragments:
├── app/src/main/java/com/example/lifelogger/ui/fragment/
│   ├── LoginFragment.kt
│   ├── RegisterFragment.kt
│   ├── DashboardFragment.kt
│   ├── EntryListFragment.kt
│   ├── EntryDetailFragment.kt
│   └── CreateEntryFragment.kt

ViewModel (UI methods):
├── app/src/main/java/com/example/lifelogger/ui/viewmodel/
│   └── LogEntryViewModel.kt          (UI observer methods)

Layouts:
├── app/src/main/res/layout/
│   ├── fragment_login.xml
│   ├── fragment_register.xml
│   ├── fragment_dashboard.xml
│   ├── fragment_entry_list.xml
│   ├── fragment_entry_detail.xml
│   ├── fragment_create_entry.xml
│   └── (other layouts)

Resources:
├── app/src/main/res/drawable/
│   └── (buttons, drawables, icons)
├── app/src/main/res/values/
│   ├── colors.xml                    (Dark readable colors)
│   ├── strings.xml
│   └── themes.xml

Navigation:
├── app/src/main/res/navigation/
│   └── nav_graph.xml                 (Complete navigation graph)
```

### What Member 2 Implemented

✅ All 6 user-facing screens (login, register, dashboard, list, detail, create)  
✅ Image selection and persistent display  
✅ Audio recording with permission handling  
✅ Audio playback controls  
✅ Fragment navigation and backstack  
✅ Material Design UI with proper colors/spacing  
✅ Dark, readable text throughout  
✅ Error handling and user feedback (toasts)  
✅ Media preview before save  
✅ Per-user list filtering in UI  

---

## Work Summary Table

| Component | Member 1 (Backend) | Member 2 (Frontend) |
|-----------|----------|----------|
| **Database Design** | ✓ | |
| **Room DAO & Queries** | ✓ | |
| **SessionManager** | ✓ | |
| **Supabase Integration** | ✓ | |
| **Sync Manager** | ✓ | |
| **RLS Policies** | ✓ | |
| **ViewModel (Core Logic)** | ✓ | |
| **LoginFragment UI** | | ✓ |
| **RegisterFragment UI** | | ✓ |
| **DashboardFragment UI** | | ✓ |
| **EntryListFragment UI** | | ✓ |
| **EntryDetailFragment UI** | | ✓ |
| **CreateEntryFragment UI** | | ✓ |
| **Image Handling** | | ✓ |
| **Audio Recording** | | ✓ |
| **Audio Playback** | | ✓ |
| **Navigation Graph** | | ✓ |
| **Layout XML Files** | | ✓ |
| **Styling & Colors** | | ✓ |
| **Backend Tests** | ✓ | |
| **UI Tests** | | ✓ |

---

## How They Work Together

### Complete User Journey

```
1. APP START
   └─> LoginFragment (Member 2 UI)
       └─> AuthViewModel.login() (Member 2)
           └─> SessionManager.verifyOfflineLogin() (Member 1)
               └─> onSuccess → DashboardFragment (Member 2 UI)

2. CREATE ENTRY
   └─> CreateEntryFragment (Member 2 UI)
       ├─ Image picked → copied to storage
       ├─ Audio recorded → saved to storage
       └─> Save button → viewModel.insertEntry()
           └─> LogEntryViewModel.insertEntry() (Member 1/2)
               └─> repository.insertEntry()
                   └─> LogEntryDao.insert() (Member 1)
                       └─> Room Database stores locally
                           └─> SyncManager.uploadEntry() (Member 1)
                               └─> Supabase cloud upload

3. VIEW ENTRIES
   └─> EntryListFragment (Member 2 UI)
       └─> Observes LiveData from LogEntryViewModel
           └─> viewModel.allEntries (Member 1 ViewModel)
               └─> SwitchMap on activeUserId
                   └─> repository.getEntriesByUser(userId)
                       └─> LogEntryDao.getEntriesByUser() (Member 1)
                           └─> Room query filtered by userId

4. MEDIA DISPLAY
   └─> EntryDetailFragment (Member 2 UI)
       ├─> imageView.setImageURI(storedFilePath)
       │   └─ Loads from app internal storage
       └─> mediaPlayer.setDataSource(audioFilePath)
           └─ Loads from app internal storage

5. OFFLINE SUPPORT
   └─> All entries in local Room DB accessible offline
   └─> When online, SyncManager syncs in background
       └─> Per-user RLS ensures data privacy (Member 1)
```

---

## Technology Breakdown

### Member 1 Uses:
- **Kotlin** - Language
- **Room Database** - Local SQLite ORM
- **Supabase PostgREST** - PostgreSQL API
- **Supabase GoTrue** - Authentication
- **Kotlin Coroutines** - Async operations
- **SharedPreferences** - Local session storage
- **SHA-256 & Base64** - Credential security

### Member 2 Uses:
- **Kotlin** - Language
- **Android Fragments** - UI containers
- **ViewBinding** - Type-safe view access
- **LiveData** - Reactive data holders
- **Navigation Component** - Fragment routing
- **RecyclerView** - List display
- **ConstraintLayout** - Responsive layout
- **MediaRecorder** - Audio recording
- **MediaPlayer** - Audio playback
- **ActivityResultContracts** - Permissions & file picker
- **Material Design** - UI components
- **Drawables & Colors** - Styling

---

## Interview Talking Points

### Member 1 Should Prepare
1. Explain why Room database is better than SQLite directly
2. How SessionManager stores credentials securely
3. Why RLS policies are important for data isolation
4. How offline-first sync works
5. What last-write-wins conflict resolution means
6. Why DAO queries are user-scoped

### Member 2 Should Prepare
1. How Fragment lifecycle works
2. Why ViewBinding is used instead of findViewById()
3. How LiveData observers update UI automatically
4. Why ActivityResultContracts for permissions
5. How image and audio files are persisted locally
6. Why navigation graph is better than manual intents

### Both Should Know
1. Complete data flow from login to viewing entries
2. How local and cloud storage interact
3. Why MVVM architecture is better
4. How the app works offline
5. User journey and different entry points
6. Where Generative AI helped and where manual work was needed

---

## Build & Test Status

✅ **Build:** `assembleDebug` - SUCCESSFUL  
✅ **Tests:** `testDebugUnitTest` - ALL PASS  
✅ **GitHub:** Code pushed to main branch  
✅ **App:** Ready for deployment

---

## Files to Reference in Report

```
Core Files Summary:

Backend (Member 1):
- Database: app/src/main/java/com/example/lifelogger/data/database/
- Auth: app/src/main/java/com/example/lifelogger/data/auth/
- Sync: app/src/main/java/com/example/lifelogger/data/sync/
- Repository: app/src/main/java/com/example/lifelogger/data/repository/

Frontend (Member 2):
- Fragments: app/src/main/java/com/example/lifelogger/ui/fragment/
- Layouts: app/src/main/res/layout/
- Navigation: app/src/main/res/navigation/nav_graph.xml
- Styles: app/src/main/res/values/colors.xml
```

---

## Final Note

The project successfully divides work logically:
- **Backend (Member 1):** Everything data-related (database, sync, auth logic)
- **Frontend (Member 2):** Everything UI-related (fragments, layouts, navigation, media display)

Both members used GitHub Copilot for code generation and worked together on architecture decisions. Each member understands their own domain deeply and can explain the other's implementation at a high level.

Assignment completion: **100%** ✅


