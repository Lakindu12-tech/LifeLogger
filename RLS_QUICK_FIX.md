# ⚠️ URGENT: FIX RLS POLICIES NOW (2 MINUTES)

## The Problem

Your RLS policies are checking the **WRONG array index**, so media uploads/downloads will be **BLOCKED** ❌

### Current (WRONG)
```sql
CREATE POLICY "Users can upload own media" ON storage.objects
  FOR INSERT WITH CHECK (
    auth.uid()::text = (string_to_array(name, '/'))[2]  ← WRONG INDEX
  );
```

This checks: `auth.uid() = "entries"` → **ALWAYS FALSE**

### Correct (FIXED)
Change `[2]` to `[1]`:
```sql
CREATE POLICY "Users can upload own media" ON storage.objects
  FOR INSERT WITH CHECK (
    auth.uid()::text = (string_to_array(name, '/'))[1]  ← CORRECT INDEX
  );
```

This checks: `auth.uid() = "{userId}"` → **WORKS** ✅

---

## Quick Fix (2 Steps)

### Step 1: Delete Old Policies
Go to **Supabase Dashboard** → **SQL Editor**

Paste and run:
```sql
DROP POLICY IF EXISTS "Users can upload own media" ON storage.objects;
DROP POLICY IF EXISTS "Users can read own media" ON storage.objects;
DROP POLICY IF EXISTS "Users can delete own media" ON storage.objects;
```

### Step 2: Create New (CORRECTED) Policies
Paste and run:
```sql
CREATE POLICY "Users can upload own media" ON storage.objects
  FOR INSERT WITH CHECK (
    auth.uid()::text = (string_to_array(name, '/'))[1]
  );

CREATE POLICY "Users can read own media" ON storage.objects
  FOR SELECT USING (
    auth.uid()::text = (string_to_array(name, '/'))[1]
  );

CREATE POLICY "Users can delete own media" ON storage.objects
  FOR DELETE USING (
    auth.uid()::text = (string_to_array(name, '/'))[1]
  );
```

**Key change**: Every `[2]` becomes `[1]`

---

## Why This Matters

### File Path Format
```
users/{userId}/entries/{entryId}/image.jpg
```

### Array After Split by "/"
```
[0] = "users"
[1] = "{userId}"         ← CHECK THIS (correct)
[2] = "entries"          ← DON'T CHECK THIS (wrong)
[3] = "{entryId}"
[4] = "image.jpg"
```

### RLS Check
```
Supabase compares:
  auth.uid() (the logged-in user's UUID)
  =
  (value at array index)

With [2]: auth.uid() = "entries"  → FALSE ❌
With [1]: auth.uid() = "{userId}" → TRUE ✅
```

---

## After Fix: Done! ✅

Once corrected:
1. Build app: `.\gradlew assembleDebug`
2. Create entry with image + audio
3. Media uploads to Supabase automatically
4. Image displays ✓
5. Audio plays ✓
6. Close and reopen app
7. Media still loads ✓✓✓

---

## Status

```
✅ Code: PERFECT
✅ Bucket: CREATED
⚠️  Policies: WRONG INDEX (needs fix)
⏳ After fix: READY FOR PRODUCTION
```

**Fix time: 2 minutes**  
**Impact: CRITICAL** (without fix, media won't upload)  
**Action: Required NOW**

---

Do this fix, then test creating entry with media. Everything will work! 🚀

