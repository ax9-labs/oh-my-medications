# Med Tracker 1.4 verification

Build date: 2026-11-06. Package: `com.personal.medtracker`, version 1.4 (5), minimum API 26, target API 35. Database schema remains 2.

The broad regression suite passed 123 checks: 20 schedule/CSV-format checks and 103 Android integration checks. A preview then exposed an initial-layout sizing issue in the selection pill. After correcting measurement, the final build passed 21 focused navigation and migration checks, including a new assertion for visible selection bounds on the first layout. The broad suite was not rerun after this targeted layout-only correction. Translation validation passed for all 172 translatable entries.

- APK v2/v3 signatures verified; signing uses the same key as version 1.3. Install as an update without uninstalling to retain saved data.
- Tested on Android 15/API 35 in an AOSP x86_64 emulator.
- New floating navigation uses a persistent elevated surface, three outline icons, a measured light-blue selection pill, icon/label transitions and fade/slide page transitions.
- Tests exercised fast alternating tab selections with native animations enabled and disabled. The last requested page remained selected, fully visible and aligned; outgoing pages were released. Accessibility names and selected states were verified.
- System animation preferences are respected. Final first-layout selection geometry and navigation preview were checked after the measurement correction.
- Shared theme colors apply to main screens, native controls, progress cards, launcher icon and alarm screen. Primary dark text on light-blue buttons has approximately 7.5:1 contrast. Body text was darkened for readability.
- Tab scroll positions and bottom space are maintained so the final controls can scroll above the floating bar.
- Existing medication editing, icon choices, history, CSV, language switching, background alarms and Stop controls passed in the broad suite. English and Spanish application images were reviewed; the final corrected floating navigation preview was reviewed separately.
- No new permissions, network service or database migration were added for this design update.

Application view handlers and rendered view images were exercised directly; the software emulator can show unrelated System UI ANR dialogs and has no audio hardware. Playback state was verified rather than physical audibility. Native elevation shadows and perceived frame smoothness should be assessed on the phone; no physical Pixel frame-rate benchmark was performed.

## Translation validation

```text
PASS: values-es: 172 translated resources and matching format arguments
PASS: app language choices match Android locale registration; English is the default
PASS: UI, icon descriptions and notification classes contain no hard-coded English prose
```

## Final focused navigation results

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
PASS: 21 focused navigation and migration checks
```

## Broad Android regression results before the final layout correction

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
PASS: Navigation is a floating elevated surface
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
PASS: 103 Android integration checks
```
