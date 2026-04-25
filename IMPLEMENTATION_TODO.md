# LifeLogger Completion Checklist

This checklist tracks what is complete for the assignment and what still needs final verification.

## Core App Features

- [x] User login/register with Supabase auth
- [x] Create daily log entry (title, content, category)
- [x] List entries (newest first)
- [x] View entry details
- [x] Delete entry
- [x] Offline-first local storage with Room
- [x] Attach image to entry (gallery picker)
- [x] Attach short audio note (in-app recording)
- [x] Display attached image in detail screen
- [x] Play attached audio in detail screen

## Cloud / Backend

- [x] Supabase client integration
- [x] Supabase sync manager class
- [x] Trigger sync from app flow (list open + create/update)
- [x] Basic cloud delete attempt during local delete
- [ ] Verify Supabase table schema matches `LogEntry` fields
- [ ] Verify RLS/auth policies for `log_entries`

## Codebase Cleanup

- [x] Removed unused camera/storage permissions
- [x] Removed unused Firebase version-catalog entries
- [x] Removed unused/placeholder create-entry category helper code
- [ ] Optional: trim outdated documentation that still says Firebase

## Submission Readiness

- [ ] Capture screenshots for report (login, create, list, detail, media)
- [ ] Final report PDF with AI prompts + work division
- [ ] Run clean build before zipping project
- [ ] Zip project folder and submit

