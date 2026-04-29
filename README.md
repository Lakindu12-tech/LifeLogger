# LifeLogger

LifeLogger is a simple Android personal life logger made for a college assignment. It lets a user register/login, write daily entries, attach images and audio notes, save entries locally for offline use, and sync them to Supabase when the internet is available.

## What the app does

- Register and login with **username + password**
- Show a dashboard with quick actions
- Create daily entries with:
  - title
  - content
  - category
  - image attachment
  - audio note
- View saved entries in a list
- Open an entry to see full details
- Delete entries
- Keep data stored locally in **Room**
- Sync entries to **Supabase** when online
- **Upload images and audio to Supabase Storage** (new!)
- Display media from cloud storage in entry details
- Show sync status in the entry detail screen

## Technologies used

- **Kotlin**
- **Android Jetpack**
  - Room
  - ViewModel
  - LiveData
  - Fragment
  - Navigation Component
  - View Binding
- **Material Design** components
- **RecyclerView**
- **Kotlin Coroutines**
- **Supabase**
  - Auth (username/password)
  - Postgrest (database access)
  - Storage (image and audio file storage)

## Main app screens

- `LoginFragment` - User login with username/password
- `RegisterFragment` - User registration
- `DashboardFragment` - Welcome screen with action buttons
- `CreateEntryFragment` - Create entry with media attachments
- `EntryListFragment` - List of user's entries  
- `EntryDetailFragment` - View entry details with media playback

## Main project layers

- **Model**: `LogEntry`
- **Database**: `LogEntryDao`, `LifeLoggerDatabase`
- **Repository**: `LogEntryRepository`
- **Cloud sync**: `SupabaseManager`, `SyncManager`
- **UI logic**: `AuthViewModel`, `LogEntryViewModel`
- **UI screens**: fragments and adapters under `ui/`

## How the data flows

`Fragment -> ViewModel -> Repository -> Room database`

If the user is online and authenticated:

`Room database -> SyncManager -> Supabase`

## Database notes

The Supabase table used by the app is `public.log_entries`. It stores:

- `id`
- `userId`
- `title`
- `content`
- `timestamp`
- `category`
- `imageUri` - Public URL to image in Supabase Storage
- `audioUri` - Public URL to audio in Supabase Storage
- `isSynced`
- `lastModified`

## Media Storage (Images and Audio)

Images and audio files are uploaded to **Supabase Storage** in the `entry_media` bucket:

**Storage path structure:**
```
entry_media/
└── users/{userId}/entries/{entryId}/
    ├── image.jpg
    └── audio.m4a
```

**How it works:**
1. User creates entry and selects/records media
2. Entry saved to local Room database
3. Media files uploaded asynchronously to Supabase Storage
4. `imageUri` and `audioUri` updated with public Storage URLs
5. When viewing entry, media loads from Supabase Storage URLs
6. Media persists in cloud even after app uninstall/reinstall

**Setup required:**
- See `MEDIA_SETUP_CHECKLIST.md` for Supabase Storage bucket creation
- See `SUPABASE_MEDIA_SETUP.md` for detailed setup instructions

## Permissions used

- `INTERNET`
- `ACCESS_NETWORK_STATE`
- `RECORD_AUDIO`

## Build and run

Open the project in Android Studio, then run it on an emulator or device.

Useful Gradle commands:

```bash
./gradlew clean
./gradlew assembleDebug
./gradlew test
```

On Windows PowerShell:

```powershell
.\gradlew.bat clean
.\gradlew.bat assembleDebug
.\gradlew.bat test
```

## Project structure

- `app/src/main/java/com/example/lifelogger/`
  - `data/`
  - `ui/`
- `app/src/main/res/`
  - `layout/`
  - `navigation/`
  - `values/`
  - `drawable/`
- `app/src/main/AndroidManifest.xml`

## Short summary

This project is a clean, simple personal life logger for college use. The core focus is offline saving, readable UI, and Supabase cloud sync when available.

