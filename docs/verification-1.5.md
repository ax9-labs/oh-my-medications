# Med Tracker 1.5 verification

Build date: 2026-11-06. Package: `com.personal.medtracker`, version 1.5 (6), minimum API 26, target API 35. Database schema remains 2.

All 186 behavioral checks passed: 20 schedule/CSV-format checks, 112 broad Android integration checks, and 54 permission-flow checks across five real permission/language scenarios. Static validation passed for all 187 English/Spanish translatable resources. After visual review, one Spanish welcome explanation was polished; the final resource build, translation validation and APK signature were checked again. Behavior code was unchanged after the passing runs.

- Installed and tested on Android 15/API 35 in an AOSP x86_64 emulator.
- Real AppOps configurations tested: both permissions missing, only precise access missing, only full-screen access missing, and both allowed. The both-missing flow was also exercised in Spanish.
- The welcome appeared only with both alarm permissions absent and did not require any medication records. It hid the main navigation while guidance was shown.
- Settings displayed its warning icon when either alarm permission was absent, with a localized accessible description. Repair text identified the specific missing permission; both-allowed state cleared the warning and repair panel.
- Welcome buttons were intercepted with ActivityMonitor and verified to target the correct Android settings intent actions and package URI scheme. Permissions were not granted by the app or test inside that running target process.
- Continue opened the tracker without granting access and preserved the warning. Activity recreation retained dismissal for the current visit; starting a new MainActivity showed the welcome again if both permissions were still absent.
- Real permission changes were applied externally between test runs because revoking precise access may terminate the target process. Startup state after those changes was checked; MainActivity also re-reads state and refreshes Settings when resumed.
- Existing database migration, medication data, icons, English/Spanish switching, floating navigation, actual timed alarm delivery, continuous playback state and Stop controls passed again.
- English welcome/Settings views and Spanish guidance were visually reviewed. The welcome and Settings content are scrollable on shorter screens.
- No new permissions, network services or database migration were added. Final APK v2/v3 signatures verify and the signing certificate matches version 1.4 for an in-place update.

View controls and rendered view images were exercised directly; the software emulator can show unrelated System UI ANR dialogs and has no physical audio output. Native audio playback state was tested. Physical Pixel behavior and older Android versions were not separately exercised. Missing notifications remain part of the existing reminder flow; they do not independently trigger the both-special-permissions welcome or Settings icon.

## Translation validation

```text
PASS: values-es: 187 translated resources and matching format arguments
PASS: app language choices match Android locale registration; English is the default
PASS: UI, icon descriptions and notification classes contain no hard-coded English prose
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

## Broad Android regression results

```text
PASS: Welcome appears when both alarm permissions are missing
PASS: Welcome stays hidden when only full-screen access is missing
PASS: Welcome stays hidden when only precise access is missing
PASS: Welcome stays hidden when both alarm permissions are allowed
PASS: Settings warns when both alarm permissions are missing
PASS: Settings warns when full-screen access is missing
PASS: Settings warns when precise access is missing
PASS: Settings warning clears when both permissions are allowed
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
PASS: 112 Android integration checks
```
