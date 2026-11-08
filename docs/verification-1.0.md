# Verification

Build date: 2026-11-06. Package: `com.personal.medtracker`, version 1.0 (1).

- Compiled against Android SDK 35; minimum Android API 26.
- Universal APK containing Java/Dex code, with no CPU-specific native libraries.
- APK signature verification passed using v2 and v3 signing schemes.
- Android SDK platform and build tools matched their official repository checksums.
- Manifest permissions: notifications, boot completion, and scheduled exact alarms. No internet or storage permissions.
- 20 standalone checks passed for time validation, sorting, deduplication, daily boundaries, timezone conversion, DST gaps and overlaps, dose identity, and CSV escaping.
- APK installation succeeded on an Android 15/API 35 AOSP x86_64 emulator using software CPU emulation.

The disposable-emulator instrumentation runner exercises the real SQLite store, notification receiver, and Activity view controls. It invokes Android view handlers directly because the software emulator’s System UI repeatedly displayed an unrelated ANR dialog during accessibility-based testing. The app itself launched and rendered its empty state. A rendered application-view image is captured separately from system overlays.

Notification delivery is exercised by invoking the actual broadcast receiver and checking Android’s active notifications. Real elapsed-time alarm firing, manufacturer battery behavior, and touch interaction on a physical phone are not part of this automated check.

Physical-device testing and manufacturer-specific battery restrictions are outside this verification. Scheduled reminders must still be checked on the phone on which the app is installed. The test-notification control is in Settings.

## Final result

All 48 checks passed: 20 schedule/export-format checks and 28 Android integration checks.

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
PASS: Receiver posts a real Android notification
PASS: Due reminder dose can be logged
PASS: Handled dose does not notify again
PASS: Archiving removes active schedule
PASS: Archived medication cannot trigger reminders
PASS: Archiving preserves dose history
PASS: Dose history survives database close and reopen
PASS: Today opens with the real empty state
PASS: Empty medication form stays open for validation
PASS: Medication form exposes editable text fields
PASS: Saving the visible form creates a medication
PASS: Today button logs a taken dose
PASS: History tab opens
PASS: History shows the logged medication
PASS: Medications tab shows saved medication
PASS: Saved medication can be opened for editing
PASS: 28 Android integration checks
```
