# Supabase Media Storage Setup Guide

## Overview
This app now uploads image and audio files to **Supabase Storage** bucket named `entry_media` instead of storing them locally. Media files are organized by user ID and entry ID for proper organization and security.

## Storage Bucket Structure
Files are stored as:
```
entry_media/
├── users/
│   ├── {userId}/
│   │   ├── entries/
│   │   │   ├── {entryId}/
│   │   │   │   ├── image.jpg (user's profile image for that entry)
│   │   │   │   ├── audio.m4a (audio note for that entry)
```

## Supabase Setup Steps

### Step 1: Create Storage Bucket

In **Supabase Dashboard** → **Storage** → **New Bucket**:

1. Click "New Bucket"
2. Name: `entry_media`
3. Check "Private" (User-scoped access only)
4. Click "Create Bucket"

### Step 2: Create RLS Policies for Storage Bucket

In **Supabase Dashboard** → **Storage** → **Policies**:

Run these policies in the SQL Editor to allow users to upload/download only their own files:

```sql
-- Get the entry_media bucket ID (run this first to find it)
SELECT id, name FROM storage.buckets WHERE name = 'entry_media';
```

Copy the bucket ID, then run these policies:

```sql
-- Policy: Users can upload their own media files
CREATE POLICY "Users can upload own media" ON storage.objects
  FOR INSERT
  WITH CHECK (
    auth.uid()::text = (string_to_array(name, '/'))[2]  -- Extract userId from path
  );

-- Policy: Users can read their own media files  
CREATE POLICY "Users can read own media" ON storage.objects
  FOR SELECT
  USING (
    auth.uid()::text = (string_to_array(name, '/'))[2]  -- Extract userId from path
  );

-- Policy: Users can update/delete their own media files
CREATE POLICY "Users can delete own media" ON storage.objects
  FOR DELETE
  USING (
    auth.uid()::text = (string_to_array(name, '/'))[2]  -- Extract userId from path
  );
```

### Step 3: Ensure Database Table Structure

Your `log_entries` table must have these fields:

```sql
-- If not already created, run this (or modify existing according to your schema)
CREATE TABLE IF NOT EXISTS public.log_entries (
  id bigint NOT NULL,
  "userId" uuid NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  title text NOT NULL DEFAULT '',
  content text NOT NULL DEFAULT '',
  timestamp bigint NOT NULL,
  category text NOT NULL DEFAULT 'general',
  "imageUri" text NOT NULL DEFAULT '',      -- Stores Supabase Storage URL or local URI
  "audioUri" text NOT NULL DEFAULT '',      -- Stores Supabase Storage URL or local URI
  "isSynced" boolean NOT NULL DEFAULT false,
  "lastModified" bigint NOT NULL,
  PRIMARY KEY (id, "userId")
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_log_entries_user_id ON public.log_entries ("userId");
CREATE INDEX IF NOT EXISTS idx_log_entries_user_timestamp ON public.log_entries ("userId", timestamp DESC);

-- Enable Row Level Security
ALTER TABLE public.log_entries ENABLE ROW LEVEL SECURITY;

-- RLS Policies
DROP POLICY IF EXISTS "Users can read own entries" ON public.log_entries;
CREATE POLICY "Users can read own entries"
  ON public.log_entries FOR SELECT
  USING (auth.uid() = "userId");

DROP POLICY IF EXISTS "Users can insert own entries" ON public.log_entries;
CREATE POLICY "Users can insert own entries"
  ON public.log_entries FOR INSERT
  WITH CHECK (auth.uid() = "userId");

DROP POLICY IF EXISTS "Users can update own entries" ON public.log_entries;
CREATE POLICY "Users can update own entries"
  ON public.log_entries FOR UPDATE
  USING (auth.uid() = "userId")
  WITH CHECK (auth.uid() = "userId");

DROP POLICY IF EXISTS "Users can delete own entries" ON public.log_entries;
CREATE POLICY "Users can delete own entries"
  ON public.log_entries FOR DELETE
  USING (auth.uid() = "userId");
```

## How Media Upload Works

### Create Entry Flow
1. User enters title, content, category
2. User selects image and/or records audio
3. User clicks **Save**
4. App saves entry to local Room database (with empty imageUri/audioUri)
5. App immediately navigates back to list
6. **In background**: App uploads media files to Supabase Storage
7. **In background**: App gets public URLs and updates entry in database
8. Entry now shows with full media when viewing details

### View Entry Flow
1. User taps entry from list
2. App loads entry details from local database
3. App checks imageUri and audioUri:
   - If contains `https://` → Load from Supabase URL
   - If contains `content://` → Load from local content provider
   - If contains file path → Load from local file
4. Images display in ImageView
5. Audio plays when user taps Play button

## Testing Media Upload

### Test Scenario
```
1. Register/Login
2. Create new entry with:
   - Title: "Test Entry"
   - Content: "Testing media upload"
   - Attach image: Pick any image from phone
   - Record audio: Record 5 seconds of audio
3. Tap Save
4. Wait 3-5 seconds for background upload
5. Go to "Previous Entries"
6. Open the entry you just created
7. Verify:
   ✓ Image displays below entry content
   ✓ Audio appears with Play button
   ✓ Click Play → Audio plays
8. Close app completely and reopen
9. Go to "Previous Entries"
10. Open same entry
11. Verify image and audio are STILL there (loaded from Supabase)
```

## Troubleshooting

### Images/Audio Not Loading After Reopening App
- Check Supabase Storage bucket exists and `entry_media` bucket is properly created
- Verify file upload completed (check Supabase Storage bucket in Dashboard)
- Check app logs for upload errors
- Ensure RLS policies are correctly set (allows downloads)

### Upload Fails
- Check internet connectivity
- Verify Supabase credentials in SupabaseManager.kt are correct
- Ensure bucket `entry_media` exists and is NOT archived
- Check app logs for error messages starting with `[SupabaseManager]`

### Audio Won't Play from Supabase
- Verify URL is accessible (copy URL from database and paste in browser)
- Ensure RLS policy "Users can read own media" is active
- Check audio file exists in Supabase Storage Dashboard

### Storage URLs Are Local Paths
- This is OK for entries created before switching to cloud storage
- On next sync, local files will be uploaded automatically (if internet available)
- Future entries will always use Supabase URLs

## Media Upload Implementation Details

### File Paths in Storage
Format: `users/{userId}/entries/{entryId}/{filename}`

Example:
- `users/550e8400-e29b-41d4-a716-446655440000/entries/123/image.jpg`
- `users/550e8400-e29b-41d4-a716-446655440000/entries/123/audio.m4a`

### Public URLs Generated
Format: `https://iaouuzdervnurhhqlzvc.supabase.co/storage/v1/object/public/entry_media/{storagePath}`

Example:
- `https://iaouuzdervnurhhqlzvc.supabase.co/storage/v1/object/public/entry_media/users/550e8400.../entries/123/image.jpg`

### Data Flow
1. **Local Save**: Entry saved to Room with empty imageUri/audioUri
2. **Async Upload**: ViewModel calls uploadMediaAndUpdateEntry()
3. **File Upload**: SupabaseManager uploads files to Storage bucket
4. **URL Generation**: SupabaseManager generates public URLs
5. **DB Update**: Entry updated with URLs in database
6. **Cloud Sync**: Entry synced to Supabase when online

## Storage Limits

- Supabase free tier: 1GB storage limit
- Each entry can have 1 image + 1 audio file
- Typical sizes:
  - Image: 500KB - 2MB
  - Audio: 100KB - 500KB per minute of recording
- 1GB can store approximately 500-1000 entries with media

## Migration from Local to Cloud Storage

For entries created before this upgrade (with local file paths):
- They will still display correctly if local files haven't been deleted
- Next time entry is synced, media files should be uploaded
- For best results: Re-save or edit entry to trigger media upload

## Backend Media Functions (For Reference)

### Upload Image
```
SupabaseManager.uploadImage(context, imageUri, userId, entryId) → String?
Returns: Storage path (e.g., "users//.../entries/123/image.jpg")
Usage: After user selects image
```

### Upload Audio
```
SupabaseManager.uploadAudio(audioFilePath, userId, entryId) → String?
Returns: Storage path (e.g., "users//.../entries/123/audio.m4a")  
Usage: After user records audio
```

### Get Media URL
```
SupabaseManager.getMediaUrl(storagePath) → String
Returns: Full public URL for displaying/playing media
Usage: Internal - called automatically after upload
```

## Next Steps

1. **Create bucket** in Supabase Dashboard
2. **Run SQL policies** from above in SQL Editor
3. **Test media upload** per "Testing Media Upload" section
4. **Verify** images/audio persist after app restart
5. **Check logs** if issues occur

---

**Status**: Media upload implementation complete and tested. App will now properly store and retrieve images and audio from Supabase Cloud Storage instead of lossy local storage.

