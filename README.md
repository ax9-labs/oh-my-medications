# Med Tracker

A personal, offline Android medication tracker with a calendar schedule, treatment progress, actual taken times, accuracy dashboard, ringing alarms and English/Spanish support. Version 2.0. Android 8.0 (API 26) or later.

## Install

Copy `dist/med-tracker.apk` to your Android phone, open it, and allow installation from that browser or file manager if Android asks. The APK is signed for personal testing; it is not a Google Play release.

If you already installed an earlier version, install version 2.0 over it. **Do not uninstall the existing app.** The package name and signing key are unchanged. Existing medications, alarm schedules, icons, archived entries, language choice and dose history are retained. The database upgrades additively to schema 3 from either previous schema. Existing log timestamps remain log-creation times; actual taken times for those entries start unknown. No past missing doses are invented.

1. Tap the **+** icon. Enter your own medication name, prescribed dose, daily times, and optional instructions.
2. Leave **Remind me at these times** enabled and allow Android notifications when asked.
3. Use **Settings → Precise alarm settings** to allow precise alarms on Android 12 or later.
4. On Android 14 or later (including a Pixel 9 Pro XL), use **Settings → Full-screen alarm settings** and allow full-screen notifications/alarms. Android decides whether to open the alarm screen automatically; when the phone is unlocked it can show an alarm notification instead. Tap that notification to open the alarm screen. The looping audio does not require the alarm screen to be visible.
5. Use **Settings → Start a test alarm**. It plays at the phone’s **alarm volume**, not notification volume, and rings until stopped. Check the phone’s alarm volume and Do Not Disturb settings if it is silent. The original **Send a test notification** control remains available for checking notification delivery only.
6. Use **Stop alarm** on the alarm screen or notification to silence the alarm. This does not log a dose. **Stop & open tracker** silences it and opens your schedule.
7. In **Schedule**, use the **check** icon after taking a dose; it saves the current time as the actual taken time. The **clock** icon records a different actual time, and **×** records a skipped dose after confirmation. Future-day logging is disabled; logging a dose early today asks for confirmation. Logging the ringing dose silences its alarm. The **undo** icon removes an accidental entry.

## Treatment length, actual times and dashboard

In a medication editor, choose a start date and optionally enter a number of days (1–3650). A blank length means an ongoing treatment. Start dates apply to reminders even when no end is defined. The final day is included: a 7-day course starting November 7 ends November 13. No alarm is scheduled before the start or after the last dose on the final day. Completed courses stay in Medications until archived. Progress shows the calendar day of treatment, including today; it does not claim that doses were taken.

**Schedule** starts with today and loads more days as you scroll. The calendar icon jumps to a date; the next-dose card jumps to the next unlogged dose. A current-time line separates earlier and upcoming times. The clock action is available on pending past/today doses and taken entries. In **History**, it can also add a missing actual time to an old log. Actual times must be in the past or now. Correcting one retains the original log-creation timestamp.

**Dashboard** shows the last 7 or 30 calendar days, including today. Accuracy is on-time doses divided by taken doses with known actual and scheduled instants. On time means at most 30 minutes early or late, including the boundaries. Average difference is the mean absolute distance from the scheduled instant. Daily stacked bars distinguish on-time, outside-window, skipped and not-logged doses. Their totals are also available as accessible text.

A tracked dose becomes **not logged** only after its scheduled time plus 30 minutes, if it has no taken/skipped entry. This does not prove that it was missed. Future and grace-period doses are excluded. Old logs with unknown actual times appear in History and taken totals but are excluded from timing accuracy and timing bars until corrected. No-data accuracy is shown as a dash.

Expected-dose tracking starts when this version is first opened. Earlier times on that first tracking day are marked untracked. Unchanged schedules are filled across later app absences. Schedule changes first retain old past expectations and replace only unlogged future plans. Archiving stops future expectations and alarms while keeping past plans and logs. Time zones are saved with planned and actual instants; future pending plans follow phone timezone changes. CSV export includes scheduled and actual UTC instants, schedule timezone and signed timing difference, keeping the original log timestamp.

## Welcome and permission status

At the start of a visit, a welcome view appears **only when both precise-alarm access and full-screen alarm access are missing**. It works before any medication has been added and explains each setting with a button to open the matching Android permission screen. **Continue for now** opens the tracker without granting access. Dismissal is kept during activity recreation for the current visit; a later app start can show the welcome again if both permissions are still missing.

If just one permission is missing, the app opens normally. An amber warning icon appears on the Settings button whenever **either** alarm permission is missing. Settings explains the specific problem and provides repair buttons. Permission status is read again when returning from Android settings; it is not stored as a permanent completed-setup flag. When both permissions are allowed, the welcome and warning are absent.

The welcome is no longer shown once either permission has been enabled. Remaining setup is highlighted in Settings, keeping the startup rule limited to the both-missing case. Notification permission is separate and still requested when saving a medication with reminders enabled. On Android versions that do not require one of these special alarm permissions, that permission is treated as already available.

The welcome and warning text are available in English and Spanish. Permission guidance does not mark doses, edit schedules or require access before tracking medication.

## Floating navigation and theme

The bottom bar floats above the content with a rounded white surface and light-blue selection pill. Calendar, capsule, chart and history icons open Schedule, Medications, Dashboard and History. The active tab displays its label; all tabs keep localized screen-reader names and long-press tooltips.

The selected pill glides between icons, the selected icon lifts slightly, and pages use a short fade and slide. Fast repeated taps settle on the last selected tab. Android's disabled-animation setting is respected. Each tab remembers its scroll position; tapping the active tab scrolls it back to the top. Bottom spacing allows the final list controls to scroll clear of the floating bar.

Soft blue, lavender and peach ambient gradients sit behind translucent white cards. Primary icon actions use dark surfaces; light blue remains the app accent in navigation and native controls. Medication icon colors remain the user's own visual cues. Theme colors are defined in `app/src/main/res/values/colors.xml`; see [design notes](docs/DESIGN.md).

## Language

The app starts in English. To use Spanish, open **Settings → Language: English → Español**. To switch back, open **Ajustes → Idioma: Español → English**. The selection is saved. Android 13 or later also supports selection from the phone's per-app language settings.

Screens, forms, errors, confirmations, icon/color names, accessibility descriptions, alarm screens, notifications, channel descriptions, permission guidance and CSV headers/status labels are translated. Displayed dates use the selected locale. Medication names and notes entered by users remain as written. English is the fallback for unsupported languages.

Future languages use standard Android resource catalogs. See [localization instructions](docs/LOCALIZATION.md). `./test.sh` checks translation completeness and placeholders as well as schedule behavior.

## Medication icons

In **Add medication** or **Edit medication**, tap **Choose icon**. Pick a pill, tablet, syrup bottle, package, blister pack, or drops bottle. Each has a default color; optional colors are Rose, Coral, Amber, Mint, Blue, Purple, Slate, and Teal. The preview updates immediately. Tap **Use this icon**, then **Save** the medication to keep your choice. **Cancel** keeps the previous choice.

Icons appear beside medications in Schedule, Medications and History. Existing medications and older history entries start with the default pill icon. Newly logged doses retain their icon and color even if the medication is later edited. Choose **Default** in the color picker to restore a shape's default look.

## Included

- Daily schedules with 1–12 distinct times per medication.
- Six scalable illustrated medication icons with default colors and eight optional color choices.
- Scrollable day-by-day schedule, a next-dose card, larger times, current-time divider and calendar date selection.
- Optional start date and number of treatment days, inclusive end dates and animated treatment progress.
- Saved expected-dose schedules and actual taken times, corrections, and a 7/30-day accuracy dashboard.
- Medication editing and archiving, preserving existing history.
- Taken and skipped history with separate actual taken and original logging timestamps.
- CSV export through Android’s file picker.
- Repeating alarm sound and vibration with a foreground alarm notification and Stop action.
- Alarm screen that can appear over the lock screen when Android permits full-screen alarms. Its text contains no medication names.
- Exact dose times registered with Android’s alarm-clock scheduling API. The system may show an upcoming-alarm indicator; these are managed by Med Tracker and are not entries in the Google Clock app’s alarm list.
- Multiple simultaneous due doses share one alarm sound. Stopping the sound never marks any dose taken or skipped.
- A bundled fallback alarm tone if the default system alarm tone is unavailable.
- Reminder restoration after restarting the phone, changing time or timezone, updating the app, and reopening the app.
- No internet permission, account, ads, analytics, or automatic Android backups.
- App-private SQLite storage. No medication examples are preloaded.

This version supports daily schedules. It does not support as-needed, weekly, interval-based or tapering schedules, refill management, cloud sync, or restoring from CSV. An unlogged dose remains unlogged; it is not automatically classified as skipped. Schedule edits affect the current schedule; past logs keep their original names and doses. Archived medications retain logs and can be re-added as a new medication.

Alarms follow the phone’s local time. A nonexistent time during the spring daylight-saving transition moves forward by the DST gap; a repeated time during fall is scheduled once at its first occurrence. Allow notifications and precise alarms for scheduled ringing. Without precise alarm access Android may delay delivery or prevent background ringing, in which case the app attempts a notification prompting you to check permissions. Phone alarm volume and Do Not Disturb settings control whether audio is audible; the app does not override them. Force-stopping the app disables pending alarms until it is reopened. Keep following your prescribed instructions; this app records your choices and does not calculate doses or advise whether to take a dose.

Data is kept on the phone and is cleared when the app is uninstalled or its storage is cleared. CSV exports are separate files at the destination you choose and may contain health information. They are a history report, not a complete app backup.

## Build and test on Linux

Requires a Java 21 runtime with `keytool`, Python 3, `curl`, and standard Linux libraries for Android build tools. No third-party runtime libraries are bundled in the app.

```sh
python3 setup-tools.py
chmod +x build.sh test.sh
./test.sh
./build.sh
```

The pinned Android SDK 35 and Eclipse Java compiler are downloaded into `../android-tools`. The build compiles Java 8 bytecode, converts it with D8, packages resources with AAPT2, aligns the APK, signs it, and verifies its signature. `MED_TRACKER_TOOLS` can point to another directory with the same layout. The Android SDK is distributed under its own terms: <https://developer.android.com/studio/terms>.

`dist/med-tracker.apk.sha256` records the final APK hash. The build creates `.local/med-tracker.keystore` on its first run, using a development password `android`. Keep that key to install future builds over this one without uninstalling; a different key cannot update an existing installation. The source archive excludes the signing key. Use a privately managed release key before public distribution.

`tests/ScheduleTest.java` exercises ordering/deduplication, malformed times, daily boundaries, timezone conversion, both DST transitions, dose keys, and CSV quoting. `tests/TreatmentTest.java` covers inclusive course boundaries, progress, next alarms and the ±30-minute timing window. Android emulator verification is recorded in `VERIFICATION.md`.

To run Android integration tests on a **disposable emulator** (the test runner clears the app’s database):

```sh
./build-android-tests.sh
adb install -r dist/med-tracker.apk
adb install -r build/instrumentation/tests.apk
adb shell pm grant com.personal.medtracker android.permission.POST_NOTIFICATIONS
adb shell appops set com.personal.medtracker SCHEDULE_EXACT_ALARM allow
adb shell appops set com.personal.medtracker USE_FULL_SCREEN_INTENT allow
adb shell am instrument -w com.personal.medtracker.tests/com.personal.medtracker.AppInstrumentation
```

The test APK is separate from the personal APK and is not needed on your phone.
