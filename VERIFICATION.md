# Med Tracker 2.0 verification

Build date: 2026-11-07. Package `com.personal.medtracker`, version 2.0 (7), min API 26, target API 35, database schema 3.

The final APK passed 144 Android integration assertions and 36 pure Java schedule/treatment/adherence assertions. A further 53 focused navigation/migration assertions and 54 permission-flow assertions passed across four real permission configurations, including the both-missing scenario in Spanish. Static localization validation passed for 244 English/Spanish resources, placeholders, language registration and UI prose.

## Coverage and results

- Installed using `pm install -r` on Android 15/API 35 AOSP x86_64, retaining the package identity. APK v2/v3 signatures verify. Its SHA-256 signing certificate matches the published 1.5 APK: `ec670131b46834e962af9d3ddb90df322eb7f19d8f4d71eba58900fae2280863`.
- Additive upgrades from schemas 1 and 2 preserve IDs, active/archive state, names, dose text, icon snapshots, schedules and original recorded timestamps. Existing actual times remain unknown rather than being inferred. Existing courses default to indefinite duration.
- Treatment start, inclusive end, day progress, future start and final reminder boundaries are tested. Saved plan counts stop at the end date. New fields persist through the real medication form.
- Actual taken and scheduled instants are stored separately. The native clock editor is opened and saved. Corrections preserve the original logging timestamp; future actual times and future-day logging are rejected.
- ±30-minute boundaries, early/late classification, mean absolute deviation, future/grace exclusions, skipped counts and untracked slots are tested. Old logs count as taken while unknown timing is excluded from accuracy. Confirming an old actual time makes it eligible.
- Schedule changes keep past expectations and logs, remove superseded unhandled future slots and generate replacements. Editing after an app absence first fills old past expectations. Archiving retains history and removes future unhandled plans.
- The scrollable Schedule supports date jumping past the initial horizon, completed-course empty days and remembering the expanded range after tab changes. The current-time divider and next-dose card are visually reviewed with sample data.
- Four floating tabs retain navigation identity and final-page cleanup during rapid transitions, and respect enabled/disabled system animations. Treatment bars and charts also check the system animation setting.
- Real timed AlarmManager delivery starts foreground alarm playback. Notification full-screen/Stop actions, logging-to-stop, test alarms, the alarm-screen Stop button, Spanish notification text and audio playback state pass. Refreshing schedules after the final course dose does not silence that ringing dose; it rings until stopped.
- Real AppOps permission scenarios pass: both missing, precise missing, full-screen missing and both allowed. Welcome gating, settings badge, correct repair actions, Continue, recreation, later visits and Spanish accessibility text pass.
- Native screenshots were reviewed for Schedule, timeline, treatment progress, dashboard, history and Spanish dashboard. The compact final layout brings the graph higher and places common icon actions beside headings. Screenshots use disposable test data, which is never bundled in the production APK.
- No new permissions or network services are added. The production build includes only app sources, not instrumentation or sample fixtures.

The emulator uses software CPU/rendering and has no physical audio output. Tests inspect native playback state and rendered Android views; unrelated System UI dialogs can occur in this environment. Physical Pixel hardware and older Android versions were not independently tested. Install over the existing app without uninstalling to retain data. New tracking coverage starts on upgrade; old unknown schedules are not classified as missed.

## Commands

```sh
./test.sh
./build.sh
./build-android-tests.sh
adb install -r dist/med-tracker.apk
adb install -r build/instrumentation/tests.apk
adb shell pm grant com.personal.medtracker android.permission.POST_NOTIFICATIONS
adb shell appops set com.personal.medtracker SCHEDULE_EXACT_ALARM allow
adb shell appops set com.personal.medtracker USE_FULL_SCREEN_INTENT allow
adb shell am instrument -w com.personal.medtracker.tests/com.personal.medtracker.AppInstrumentation
```

The runner clears app data and is intended for a disposable emulator. Permission configurations are set externally before each permission-mode run. Gallery mode `-e gallery true` creates sample screenshot data and removes it at completion.

## Final broad Android results

```text
PASS: Welcome appears when both alarm permissions are missing
PASS: Welcome stays hidden when only full-screen access is missing
PASS: Welcome stays hidden when only precise access is missing
PASS: Welcome stays hidden when both alarm permissions are allowed
PASS: Settings warns when both alarm permissions are missing
PASS: Settings warns when full-screen access is missing
PASS: Settings warns when precise access is missing
PASS: Settings warning clears when both permissions are allowed
PASS: Version 1 database upgrades additively to version 3
PASS: Upgrade preserves existing medication details and IDs
PASS: Upgrade preserves alarm schedules and creation dates
PASS: Existing medications receive a default pill icon
PASS: Upgrade preserves archived medication state
PASS: Upgrade preserves dose history snapshots and timestamps
PASS: Existing history receives safe default icons
PASS: Migration keeps actual taken time unknown for old logs
PASS: Repeating the additive migration does not remove history
PASS: Upgraded medications can save new icon choices
PASS: Unknown icon identifiers fall back safely
PASS: Version 2 database upgrades to version 3
PASS: Version 2 medication appearance survives the upgrade
PASS: Version 2 history retains its icon snapshot and unknown actual time
PASS: Existing medications default to indefinite duration with fresh coverage
PASS: Treatment dates persist in SQLite
PASS: Expected plans stop at the inclusive treatment end
PASS: Earlier doses on enrollment day do not become fabricated misses
PASS: Upcoming enrollment-day doses are tracked
PASS: Actual taken time can be logged independently of schedule
PASS: Log stores both actual and scheduled instants
PASS: Correcting actual time preserves original log audit time
PASS: Future actual taken timestamps are rejected
PASS: Schedule edits preserve past slots and existing logs
PASS: Schedule edits remove superseded future slots
PASS: Schedule edits generate replacement future slots
PASS: Archiving removes future unlogged treatment plans
PASS: Archiving keeps actual taken history
PASS: Legacy log remains explicitly untimed
PASS: Dashboard counts old logs without inventing their accuracy
PASS: A confirmed legacy actual time becomes eligible for accuracy
PASS: Editing after an absence first preserves old expected doses
PASS: Future-day logging is rejected by storage
PASS: Completed treatments do not schedule a new alarm
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
PASS: Rescheduling after the final course dose preserves its ringing alarm until stopped
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
PASS: Navigation is a floating elevated surface
PASS: Selected tab pill has visible measured bounds on the first layout
PASS: Navigation respects disabled system animations
PASS: History opens immediately with animations disabled
PASS: Tab switching preserves the same navigation bar
PASS: Navigation enables transitions when system animations are enabled
PASS: Rapid animated tab changes settle on the last requested view
PASS: Finished transitions release old pages
PASS: Selected page finishes fully visible and aligned
PASS: Icon navigation exposes the selected tab to accessibility services
PASS: Motion can be disabled again without affecting navigation
PASS: Alarm screen test starts its alarm
PASS: Alarm is active before opening the alarm screen
PASS: Alarm screen Stop button silences the alarm
PASS: Empty medication form stays open for validation
PASS: Medication form exposes editable text fields
PASS: Medication form shows the default pill preview
PASS: Picker updates its shape and color preview immediately
PASS: Canceling the icon picker keeps the previous choice
PASS: Confirmed icon selection appears in the medication form
PASS: Medication editor persists optional treatment duration
PASS: Saving the visible form creates a medication
PASS: Saving the form persists the selected icon and color
PASS: Today shows the saved medication icon
PASS: Schedule action records actual taken time
PASS: Finished treatment has no future dose cards
PASS: Calendar range survives switching tabs
PASS: Actual-time editor explains its audit behavior
PASS: History tab opens
PASS: History shows the logged medication
PASS: History shows the icon captured when the dose was logged
PASS: Dashboard shows its accuracy graph
PASS: Dashboard range changes to 30 days
PASS: Medications tab shows saved medication
PASS: Medications list shows the selected icon
PASS: Treatment card shows inclusive calendar progress
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
PASS: 144 Android integration checks
```

## Permission flow: missing-both, en

```text
PASS: Android reports the requested real permission state
PASS: Startup greeting follows the both-missing rule
PASS: Welcome and main navigation are shown in the correct state
PASS: Settings alert icon matches the current missing permissions
PASS: Permission warning has a localized accessible description
PASS: Permission guidance works before any medication is added
PASS: Welcome precise-alarm button opens the matching Android settings action
PASS: Welcome full-screen button opens the matching Android settings action
PASS: Settings details show a repair panel only when an alarm permission is missing
PASS: Settings identifies missing precise-alarm access accurately
PASS: Settings identifies missing full-screen access accurately
PASS: Continue for now opens the tracker without granting permissions
PASS: Settings keeps its warning after welcome is skipped
PASS: Activity recreation preserves welcome dismissal for the current visit
PASS: A new app start shows the welcome again if both permissions remain missing
PASS: 15 alarm permission flow checks
```

## Permission flow: missing-precise, en

```text
PASS: Android reports the requested real permission state
PASS: Startup greeting follows the both-missing rule
PASS: Welcome and main navigation are shown in the correct state
PASS: Settings alert icon matches the current missing permissions
PASS: Permission warning has a localized accessible description
PASS: Permission guidance works before any medication is added
PASS: Settings details show a repair panel only when an alarm permission is missing
PASS: Settings identifies missing precise-alarm access accurately
PASS: Settings identifies missing full-screen access accurately
PASS: 9 alarm permission flow checks
```

## Permission flow: missing-full, en

```text
PASS: Android reports the requested real permission state
PASS: Startup greeting follows the both-missing rule
PASS: Welcome and main navigation are shown in the correct state
PASS: Settings alert icon matches the current missing permissions
PASS: Permission warning has a localized accessible description
PASS: Permission guidance works before any medication is added
PASS: Settings details show a repair panel only when an alarm permission is missing
PASS: Settings identifies missing precise-alarm access accurately
PASS: Settings identifies missing full-screen access accurately
PASS: 9 alarm permission flow checks
```

## Permission flow: ready, en

```text
PASS: Android reports the requested real permission state
PASS: Startup greeting follows the both-missing rule
PASS: Welcome and main navigation are shown in the correct state
PASS: Settings alert icon matches the current missing permissions
PASS: Permission guidance works before any medication is added
PASS: Settings details show a repair panel only when an alarm permission is missing
PASS: 6 alarm permission flow checks
```

## Permission flow: missing-both, es

```text
PASS: Android reports the requested real permission state
PASS: Startup greeting follows the both-missing rule
PASS: Welcome and main navigation are shown in the correct state
PASS: Settings alert icon matches the current missing permissions
PASS: Permission warning has a localized accessible description
PASS: Permission guidance works before any medication is added
PASS: Welcome precise-alarm button opens the matching Android settings action
PASS: Welcome full-screen button opens the matching Android settings action
PASS: Settings details show a repair panel only when an alarm permission is missing
PASS: Settings identifies missing precise-alarm access accurately
PASS: Settings identifies missing full-screen access accurately
PASS: Continue for now opens the tracker without granting permissions
PASS: Settings keeps its warning after welcome is skipped
PASS: Activity recreation preserves welcome dismissal for the current visit
PASS: A new app start shows the welcome again if both permissions remain missing
PASS: 15 alarm permission flow checks
```
