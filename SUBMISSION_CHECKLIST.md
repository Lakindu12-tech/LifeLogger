# Report Submission Checklist & Instructions

## PDF Report Creation Instructions

### Option 1: Using Microsoft Word
1. Copy contents from `REPORT_TEMPLATE.md`
2. Paste into a Word document
3. **Format:**
   - Font: Arial or Calibri, 11pt
   - Line spacing: 1.5
   - Margins: 1 inch (2.54 cm) all sides
   - Page numbers: Bottom right
4. Add your cover page with personal details
5. Add screenshots in appropriate sections (8.1-8.8)
6. Export as PDF

### Option 2: Using Google Docs
1. Open Google Docs
2. Paste contents from `REPORT_TEMPLATE.md`
3. Format as above
4. File → Download → PDF Document

### Option 3: Using Markdown to PDF Converter
- Use Pandoc or online converters (markdowntopdf.com)
- Command: `pandoc REPORT_TEMPLATE.md -o LifeLogger_Report.pdf`

---

## Personal Information to Fill In

**Find and replace these placeholders in the report:**

```
[Your Name]                    → Your actual name
[Your Registration Number]     → Your student ID
[Partner Name]                 → Your group partner's name
[Partner Registration Number]  → Your partner's student ID
[Current Date]                 → Date of submission
[Year]                         → Academic year (e.g., 2024-2025)
```

---

## Screenshots Required (8 Total)

You need to capture these screenshots from your running app:

### Screenshot 1: Login Screen (`screenshot_01_login.png`)
- Show login form with username/password fields empty
- Show login button and register link
- Demonstrate readable dark text

### Screenshot 2: Register Screen (`screenshot_02_register.png`)
- Show register form fields
- Show register button and login link

### Screenshot 3: Dashboard Screen (`screenshot_03_dashboard.png`)
- Show welcome header with username
- Show action buttons (Create Entry, Previous Entries)
- Show gradient design

### Screenshot 4: Create Entry Screen (`screenshot_04_create_entry.png`)
- Show form with all input fields
- Show Attach Image and Record Audio buttons
- Show readable dark input text

### Screenshot 5: Entry List Screen (`screenshot_05_entry_list.png`)
- Show multiple entry cards in a list
- Show entry titles, previews, timestamps
- Show delete buttons

### Screenshot 6: Entry Detail Screen (`screenshot_06_entry_detail.png`)
- Show full entry details
- Show image (if attached)
- Show audio play button (if audio attached)

### Screenshot 7: Audio Playback Active (`screenshot_07_audio_playback.png`)
- Show MediaPlayer controls playing
- Show seek bar
- Show play/pause buttons

### Screenshot 8: Offline Usage (`screenshot_08_offline_create.png`)
- Optional: Show app creating/viewing entries offline
- Or show toast message indicating offline mode

---

## How to Take Screenshots on Android

### On Emulator
- In Android Studio, run app
- Click "Screenshots" button in Logcat tab
- Or use adb: `adb shell screencap -p /sdcard/screenshot.png`

### On Physical Device
- Power + Volume Down (simultaneously) held for 1-2 seconds
- Screenshot saved to Pictures or Screenshots folder

---

## File Preparation Checklist

### Before Creating Final Submission ZIP:

- [ ] All code compiles without errors
- [ ] All unit tests pass
- [ ] App builds successfully (assembleDebug)
- [ ] Clean project: In Android Studio → Build → Clean Project
- [ ] Wait for gradle rebuild to finish
- [ ] Delete `/build` folder to reduce ZIP size
- [ ] All screenshots captured and organized
- [ ] Report PDF created with:
  - [ ] Cover page with names and registration numbers
  - [ ] All sections filled with project details
  - [ ] Screenshots inserted in correct sections
  - [ ] Page numbers added
  - [ ] No placeholder text remaining

---

## Submission Structure

Your final submission should contain:

```
LMS Submission/
├── LifeLogger_project.zip          (Cleaned project folder)
└── LifeLogger_Report.pdf           (Completed report with screenshots)
```

### Creating the ZIP File

**In Windows (PowerShell):**
```powershell
# Navigate to parent directory
cd D:\

# Create ZIP of LifeLogger-main (excluding build artifacts)
Compress-Archive -Path LifeLogger-main -DestinationPath LifeLogger_project.zip -Force
```

**Verify ZIP size is < 100MB** (after clean build)

---

## Work Division Reference

### Member 1 (You): Backend/Database
- Database architecture and Room setup
- Session management and offline login
- Supabase sync manager
- Cloud synchronization logic
- Per-user data isolation with RLS

### Member 2 (Partner): Frontend/UI
- Login/register UI implemention
- Dashboard and entry list screens
- Create entry form with validation
- Image picking and audio recording
- Entry detail view with media playback
- Navigation and fragments

---

## Report Quality Checklist

- [ ] **Content Complete:** All 10 sections included
- [ ] **Professional Format:** Proper headings, tables, code blocks
- [ ] **Accurate Library References:** All versions match your build.gradle.kts
- [ ] **Work Division Clear:** 50/50 split well-defined
- [ ] **AI Usage Documented:** Prompts, helpfulness, limitations
- [ ] **Screenshots Included:** All 8 screenshots in correct sections
- [ ] **No Placeholder Text:** All [brackets] replaced with real info
- [ ] **References Valid:** All URLs and documentation references accurate
- [ ] **Properly Formatted:** Consistent font, spacing, margins
- [ ] **PDF Export Quality:** Text readable, images clear, no corruption

---

## Common Issues & Solutions

### Issue: Report looks unprofessional
**Solution:** 
- Use consistent formatting throughout
- Match margins and fonts
- Ensure all text is readable
- Add page numbers
- Number all sections and subsections

### Issue: Screenshots too small or blurry
**Solution:**
- Capture screenshots from emulator at reasonable resolution (720p or higher)
- Scale images to fit page while maintaining clarity
- Add brief captions below each screenshot

### Issue: ZIP file too large
**Solution:**
- Make sure you ran "Build → Clean Project" in Android Studio
- Delete .gradle, .idea folders
- Delete app/build folder
- Exclude vendor files

### Issue: Some code missing from report
**Solution:**
- Verify all file paths in "Files Modified" sections are accurate
- Mention that full code is in the GitHub repository
- Reference specific commit or branch

---

## Final Submission Reminder

**Must Submit:**
1. ✅ Cleaned project ZIP file
2. ✅ PDF report with:
   - Cover page (names, registration numbers)
   - All required sections
   - Screenshots (8 total)
   - Proper formatting

**Due:** [Your submission date]

**Size Limits:**
- ZIP file: < 100MB (typically 15-20MB after clean)
- PDF: < 20MB

---

## GitHub Repository Reference

Your code is already pushed to:
- **Repository:** https://github.com/Lakindu12-tech/LifeLogger
- **Branch:** main

You can reference this in your report for reviewers to access the source code.

---

## Questions to Prepare For Interview

Based on the work division, be ready to explain:

### Member 1 Should Know:
- How Room database DAO queries work
- What Row Level Security (RLS) does and why it's important
- How offline login credential storage works (salted hashing)
- Sync conflict resolution using lastModified timestamp
- Why offline-first architecture is better

### Member 2 Should Know:
- How Fragment lifecycle and ViewBinding work
- Why ActivityResult API is used for permissions
- How to handle media file persistence
- Navigation Component flow
- Why per-user entry filtering in LiveData matters

### Both Should Know:
- Overall architecture (MVVM)
- How local and cloud storage interact
- The complete user flow from registration to viewing entries
- Why Generative AI was helpful and what still required manual work

---

## Quick Start for Report

1. **Copy template:** `cat REPORT_TEMPLATE.md`
2. **Replace placeholders** with your actual information
3. **Capture screenshots** following the 8 descriptions above
4. **Format in Word/Google Docs** with proper spacing and fonts
5. **Insert screenshots** in sections 8.1-8.8
6. **Export as PDF**
7. **Create ZIP** of cleaned project
8. **Submit both files** to LMS

---

**Good luck with your submission!** 🎓


