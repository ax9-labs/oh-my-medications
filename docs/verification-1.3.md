# Med Tracker 1.3 verification

Build date: 2026-11-06. Package: `com.personal.medtracker`, version 1.3 (4), minimum API 26, target API 35. Database schema remains 2.

All 113 suite checks passed: 20 schedule/CSV-format checks and 93 Android integration checks. Static translation validation also passed for all 172 translatable resource entries, placeholder parity, language registration and hard-coded UI prose.

- APK v2/v3 signatures verified. Signing certificate matches version 1.2; this is an in-place update, preserving medication data, icons, history and alarm schedules.
- Installed and exercised on an Android 15/API 35 AOSP x86_64 emulator.
- Actual in-app language selection changed English → Spanish → English and recreated the UI with the selected resources. Android per-app locales persisted the selection.
- Unsupported languages fell back to English; regional Spanish resolved to the shared Spanish catalog.
- Spanish navigation, Today/Medications/History, dates, plural forms, validation errors, icon/color labels, accessibility descriptions, notifications, notification channels, alarm screen and Stop action were checked.
- Real CSV export generation used Spanish headers and status labels while preserving user-entered names and stable stored dose codes. Switching back restored English report headers.
- Medication names, doses, schedules, icon identifiers and history survived the language changes unchanged.
- Existing database migration, icon selection, dose logging, real timed alarm delivery, continuous playback state and Stop controls passed again.
- Spanish application view images were reviewed for layout and text fit. Scrollable dialogs accommodate longer translations.
- No new permissions, network service, translation SDK or cloud account were added.

Android view controls and rendered view images were tested directly because the software emulator can show unrelated System UI ANR dialogs. The emulator has no audio hardware; playback state is checked rather than physical speaker audibility. Tests ran on Android 15; the Android 8–12 preferences fallback is implemented but was not exercised on a separate older device. Brand/native language names, internal identifiers, user-entered medication details and developer diagnostics are intentionally not translated. See docs/LOCALIZATION.md for future language instructions.

## Translation validation

```text
PASS: values-es: 172 translated resources and matching format arguments
PASS: app language choices match Android locale registration; English is the default
PASS: UI, icon descriptions and notification classes contain no hard-coded English prose
```

## Android integration results

```text
PASS: Version 1 database upgrades additively to version 2
PASS: Upgrade preserves existing medication details and IDs
PASS: Upgrade preserves alarm schedules and creation dates
PASS: Existing medications receive a default pill icon
PASS: Upgrade preserves archived medication state
PASS: Upgrade preserves dose history snapshots and timestamps
PASS: Existing history receives safe default icons
PASS: Repeating the additive migration does not remove history
PASS: Upgraded medications can save new icon choices
PASS: Unknown icon identifiers fall back safely
PASS: Unsupported languages fall back to English
PASS: Regional Spanish falls back to the shared Spanish translation
PASS: Medication receives a stable ID
PASS: Medication saved to SQLite
PASS: Multiple dose times persist
PASS: New medications keep the default icon without an explicit selection
PASS: Taken dose is saved
PASS: Duplicate logging is rejected
PASS: A logged dose cannot be overwritten accidentally
PASS: Duplicate attempts do not add history
PASS: Skipped dose is saved independently
PASS: History retains original name after editing
PASS: History retains original dose after editing
PASS: Undo returns the dose to unlogged
PASS: Notification permission is enabled in emulator
PASS: Precise alarm permission is enabled in emulator
PASS: Dose schedules register as Android alarm-clock alarms
PASS: Receiver posts a real Android notification
PASS: Due dose starts the foreground ringing service
PASS: Alarm player starts audible playback through the alarm stream
PASS: Alarm notification contains a full-screen intent and Stop action
PASS: Due reminder dose can be logged
PASS: Handled dose does not notify again
PASS: Logging the ringing dose stops its alarm
PASS: Exact ringing alarm is registered with the requested trigger time
PASS: Android delivers the scheduled alarm to the ringing service
PASS: Scheduled alarm plays continuously
PASS: Notification Stop action silences a scheduled alarm
PASS: Stopping the alarm does not log a dose
PASS: Test alarm can be started
PASS: Test alarm rings
PASS: Test alarm can be stopped
PASS: Test alarm does not change dose history
PASS: Archiving removes active schedule
PASS: Archived medication cannot trigger reminders
PASS: Archiving preserves dose history
PASS: Dose history survives database close and reopen
PASS: Today opens with the real empty state
PASS: Alarm screen test starts its alarm
PASS: Alarm is active before opening the alarm screen
PASS: Alarm screen Stop button silences the alarm
PASS: Empty medication form stays open for validation
PASS: Medication form exposes editable text fields
PASS: Medication form shows the default pill preview
PASS: Picker updates its shape and color preview immediately
PASS: Canceling the icon picker keeps the previous choice
PASS: Confirmed icon selection appears in the medication form
PASS: Saving the visible form creates a medication
PASS: Saving the form persists the selected icon and color
PASS: Today shows the saved medication icon
PASS: Today button logs a taken dose
PASS: History tab opens
PASS: History shows the logged medication
PASS: History shows the icon captured when the dose was logged
PASS: Medications tab shows saved medication
PASS: Medications list shows the selected icon
PASS: Saved medication can be opened for editing
PASS: Editing restores the saved icon selection
PASS: An existing medication can return to a type's default color
PASS: Changing an icon preserves historical icon snapshots
PASS: Canceling medication editing does not overwrite its saved appearance
PASS: English is the initial app language
PASS: Language picker persists Spanish using Android app locales
PASS: Navigation is translated after switching language
PASS: Spanish Today screen uses translated headings
PASS: Dates use Spanish month names
PASS: Spanish singular medication counts are grammatical
PASS: Spanish plural medication counts are grammatical
PASS: Medication icon descriptions are translated for accessibility
PASS: Validation messages are translated
PASS: Icon and color labels are translated
PASS: Dose history is translated
PASS: CSV headers and status labels use Spanish
PASS: Translated exports preserve user-entered medication names
PASS: Changing language preserves medication data and stored identifiers
PASS: Stored dose status codes are independent of the UI language
PASS: System notification channel names use the app language
PASS: Alarm audio works while Spanish is selected
PASS: Background alarm notification and Stop action use Spanish
PASS: Alarm screen uses the saved Spanish preference
PASS: Spanish Stop button silences the alarm
PASS: Language picker can restore English
PASS: CSV export returns to English after switching back
PASS: 93 Android integration checks
```
