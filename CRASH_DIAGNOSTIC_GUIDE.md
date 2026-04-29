# Crash Diagnostic Guide for Entry Access After User Switch

## Overview
This guide explains the defensive fixes added to prevent crashes when switching users and accessing previous entries. The app now includes comprehensive logging to help diagnose the root cause of the crash.

## What Was Changed

### 1. **LogEntryViewModel.kt** - Enhanced Diagnostic Logging
Added detailed logging in critical methods:

```
[currentUserId] - Logs which userId source (online vs offline) is being used
[getEntryById] - Logs entry lookup failures with userId information
[refreshActiveUser] - Logs activeUserId changes
[hasActiveUser] - Logs user verification status
[syncNow] - Logs sync operations
```

**Key additions:**
- Log entry fetch attempts and failures
- Verify entry ownership (userId match between stored and current user)
- Return null safely if userId mismatches occur
- Guard against empty userIds

### 2. **EntryDetailFragment.kt** - Defensive Null Checks
Added lifecycle-aware callback handling:

```kotlin
// Guard: Fragment may have been destroyed before callback returns
if (!isAdded) {
    Log.d(TAG, "[getEntryById callback] Fragment no longer added, ignoring")
    return@getEntryById
}
```

**Improvements:**
- Check fragment is still added before using context
- Null-check currentEntry before delete operations
- Guard against callback returning after fragment destruction
- Enhanced error messages with specific failure reasons

### 3. **EntryListFragment.kt** - Entry List Observer Safety
Enhanced LiveData observer with null checks:

```kotlin
if (entries == null) {
    Log.w(TAG, "[allEntries observer] entries list is NULL!")
    adapter.submitList(emptyList())
    return@observe
}
```

**Benefits:**
- Log when entry list is null (shouldn't happen but guards against it)
- All user actions logged (logout, sync, navigation)
- Entry click logging for debugging

### 4. **SessionManager.kt** - Session Transition Logging
Added detailed logging for session changes:

```
[setActiveUser] - Logs when active user is changed
[getActiveUserId] - Logs session state
[clearActiveUser] - Logs logout events
[verifyOfflineLogin] - Logs offline auth attempts with success/failure details
```

**Helps diagnose:**
- Whether session is being properly cleared on logout
- If offline credentials are working
- User switching flow

## How to Reproduce & Capture Logs

### Reproduction Steps:
1. **Launch app** → Login as **UserA**
2. **Create entry** → Confirm it appears in "Previous Entries"
3. **Logout** (menu) → Confirm logged out
4. **Login as UserB** → Confirm no entries (UserB hasn't created any)
5. **Logout** → Re-login as **UserA**
6. **Tap "Previous Entries"** or list entry → **CRASH HAPPENS**

### Capturing Logcat Output:

#### Option A: Using Android Studio
1. Open **Logcat** tab at bottom of Android Studio
2. Before reproducing steps above, type filter: `lifelogger`
3. Set **Log Level** to "Verbose"
4. Reproduce the crash steps
5. **Copy full logcat output** and paste in the issue report

#### Option B: Using adb Command (PowerShell)
```powershell
# Clear existing logs
adb logcat -c

# Read logs in real-time (after reproducing crash)
adb logcat | Select-String "lifelogger|AuthViewModel|SessionManager"
```

#### Option C: Save logs to file
```powershell
# Save crashlog to file
adb logcat > D:\crash_logs.txt

# Reproduce crash, then press Ctrl+C to stop
# Share crash_logs.txt
```

## What the Logs Will Tell Us

### Success Pattern (Normal Flow)
```
D/LogEntryViewModel: [currentUserId] Using online userId: a1b2c3d4...
D/LogEntryViewModel: [refreshActiveUser] Updated activeUserId from null... to a1b2c3d4...
D/EntryListFragment: [allEntries observer] Received 3 entries
D/LogEntryViewModel: [getEntryById] Fetching entry id=123, current userId=a1b2c3d4...
D/LogEntryViewModel: [getEntryById] Entry found: My Entry Title
D/EntryDetailFragment: [getEntryById callback] Successfully loaded entry: My Entry Title
```

### Error Patterns (What We're Looking For)

#### Pattern 1: Entry Not Found
```
W/LogEntryViewModel: [getEntryById] Entry NOT FOUND for id=123, userId=user1uuid...
E/EntryDetailFragment: [getEntryById callback] Entry returned as NULL for id=123
```
**Possible cause:** Entry exists in database but query fails (userId format mismatch?)

#### Pattern 2: userId Mismatch
```
E/LogEntryViewModel: [getEntryById] CRITICAL: Entry userId mismatch! entry.userId=xyz..., current=abc...
```
**Possible cause:** Old entry has different userId format or database corruption

#### Pattern 3: Session Not Clearing
```
D/SessionManager: [clearActiveUser] Clearing active user
D/LogEntryViewModel: [currentUserId] Using online userId: (for wrong user)
```
**Possible cause:** Supabase auth session not actually signed out

#### Pattern 4: Fragment Already Destroyed
```
D/EntryDetailFragment: [getEntryById callback] Fragment no longer added, ignoring
```
**Possible cause:** Navigation happened too fast, callback returned after fragment was removed (this is OK, not a crash)

## Common Fixes Based on Log Patterns

### If Pattern 1: Entry Not Found
**Check:**
1. Is the entry's `userId` column in database correct format for the current user?
2. Run local query: Check `Room Database Inspector` in Android Studio > Database
3. Verify Supabase entries have correct `userId` UUIDs

**Fix options:**
- Ensure `LogEntry.userId` matches the current user's UUID exactly
- Verify database schema `userId` column type matches app usage

### If Pattern 2: userId Mismatch
**Check:**
1. Are you storing userId as text somewhere but comparing to UUID?
2. Is there a UUID vs String type mismatch in Room entity?
3. Check `LogEntry` data class definition

**Fix:**
- Ensure `LogEntry.userId: String` field is consistent throughout
- Verify database migration didn't corrupt existing records

### If Pattern 3: Session Not Clearing
**Check:**
1. Is `supabaseManager.client.auth.signOut()` actually executing?
2. Is `sessionManager.clearActiveUser()` removing prefs correctly?

**Fix:**
- Check Supabase auth state directly
- Verify SharedPreferences is being cleared

## Next Steps

1. **Run the app** using these reproduction steps
2. **Capture the logcat output** when crash occurs
3. **Share the log output** and these details:
   - Device: (Emulator model or physical device)
   - Android API Level
   - Any error stacktrace Android Studio shows
4. **We'll analyze logs** and identify the exact failure point

## Additional Improvements Made

✅ All fragment callbacks are now **lifecycle-aware**  
✅ Entry lookups verify **userId ownership**  
✅ Null-checks prevent NPE crashes  
✅ Comprehensive diagnostic logging for every critical operation  
✅ Session transitions fully logged  
✅ Build and tests pass successfully  

## Build Status
- ✅ `assembleDebug` - **SUCCESSFUL**
- ✅ `testDebugUnitTest` - **SUCCESSFUL**
- ✅ All code compiles without errors

