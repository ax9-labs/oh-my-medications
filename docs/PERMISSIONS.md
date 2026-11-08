# Startup alarm guidance

The two special alarm permissions are separate from notification permission:

- `AlarmManager.canScheduleExactAlarms()` on Android 12+ controls exact alarm scheduling. Older versions are treated as having access.
- `NotificationManager.canUseFullScreenIntent()` on Android 14+ controls the full-screen alarm presentation. Older versions are treated as having access.

`AlarmPermissions` reads the real Android state. Its two predicates intentionally differ:

| Precise access | Full-screen access | Startup welcome | Settings alert |
| --- | --- | --- | --- |
| Missing | Missing | Yes | Yes |
| Allowed | Missing | No | Yes |
| Missing | Allowed | No | Yes |
| Allowed | Allowed | No | No |

The welcome appears before any medication is added. It explains exact timing and lock-screen presentation, and links directly to the matching Android settings actions for this app. It does not claim that full-screen access is required for audio while the phone is unlocked. It provides **Continue for now**, allowing tracking without granting special access.

Dismissal is local to a visit, not a permanent completed-setup preference. Activity recreation in the same process preserves it; a later app visit can show guidance again if both permissions remain absent. Returns from settings and other app-initiated activities preserve visit context. Deferred medication-editor state is retained while welcome guidance is visible.

MainActivity reads permissions on resume and refreshes the Settings icon on every page render. Settings shows an amber warning card identifying each missing permission with an appropriate enable button. Returning from Android settings refreshes the card and removes the icon when access is restored. Once either permission is granted, the welcome is no longer eligible; any remaining missing access is highlighted in Settings.

Notification permission remains managed by the existing medication/reminder flow. Missing notification access alone does not trigger this welcome or the special-alarm warning icon.

## Emulator tests

The Android integration runner has a permission-state mode. It clears target-app test medication data and is intended only for a disposable Android 14+ emulator. Set the real AppOps state **before** starting instrumentation: revoking precise alarm access may terminate the target process and cancel pending alarms.

```sh
adb shell appops set com.personal.medtracker SCHEDULE_EXACT_ALARM deny
adb shell appops set com.personal.medtracker USE_FULL_SCREEN_INTENT deny
adb shell am instrument -w -e permissionState missing-both -e testLocale en com.personal.medtracker.tests/com.personal.medtracker.AppInstrumentation
```

The scenarios are `missing-both`, `missing-precise`, `missing-full`, and `ready`; configure the corresponding allow/deny modes first. `testLocale` accepts `en` or `es`. Tests cover real platform flags, startup eligibility, navigation visibility, the header warning, localized accessibility descriptions, detailed repair guidance, the absence of medication prerequisites, Continue, recreation and a later fresh start. Android settings launches are intercepted with ActivityMonitor to verify their intent actions without granting permissions inside the running test.

Restore both AppOps modes to `allow` before running the broad medication, language, navigation and ringing-alarm regression suite.
