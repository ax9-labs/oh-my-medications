# Med Tracker 1.1 verification

Build date: 2026-11-06. Package: `com.personal.medtracker`, version 1.1 (2), minimum API 26, target API 35.

All 66 automated checks passed: 20 schedule/CSV checks and 46 Android integration checks.

- APK v2/v3 signatures verified. Signing certificate matches version 1.0; the database schema remains version 1, allowing installation as an update without uninstalling.
- Installed and exercised on an Android 15/API 35 AOSP x86_64 emulator.
- Actual timed delivery through Android AlarmManager's alarm-clock API started the foreground ringing service.
- MediaPlayer playback state, foreground notification, full-screen pending intent, notification Stop action, and alarm-screen Stop button were exercised.
- Stopping an alarm and running a test alarm did not modify dose history. Logging an actively ringing dose stopped its alarm.
- Existing medication form, daily schedule, taken/skipped logging, history snapshots, editing, archiving, persistence, duplicate prevention and export-format checks passed.
- Alarm sound uses USAGE_ALARM, an indefinite loop, Android audio focus, and a playback wake lock. A bundled fallback tone is included. Phone alarm volume and Do Not Disturb settings are respected.
- New permissions: full-screen intents, media-playback foreground service, wake lock, and vibration. No internet, microphone, or storage permission.

The emulator runs without audio hardware. Playback state was verified; physical speaker audibility, Pixel-specific settings and lock-screen presentation require the phone's Start a test alarm control. Full-screen permission controls lock-screen presentation. Android may show an alarm notification while unlocked. Activity view handlers are tested directly because the software emulator's System UI can show unrelated ANR dialogs.

## Android integration results

```text
PASS: Medication receives a stable ID
PASS: Medication saved to SQLite
PASS: Multiple dose times persist
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
PASS: Saving the visible form creates a medication
PASS: Today button logs a taken dose
PASS: History tab opens
PASS: History shows the logged medication
PASS: Medications tab shows saved medication
PASS: Saved medication can be opened for editing
PASS: 46 Android integration checks
```
