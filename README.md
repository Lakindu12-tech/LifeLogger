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
  - Auth
  - Postgrest database access
  - Storage dependency included in the project setup

## Main app screens

- `LoginFragment`
- `RegisterFragment`
- `DashboardFragment`
- `CreateEntryFragment`
- `EntryListFragment`
- `EntryDetailFragment`

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
- `imageUri`
- `audioUri`
- `isSynced`
- `lastModified`

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

