# Version 2 update plan

## Agreed behavior

- Treatment length is optional. A start date and positive number of calendar days define an inclusive end date. Empty duration means no end date. Treatment progress describes calendar days, separately from doses taken.
- A dose is on time within 30 minutes either side of its saved scheduled instant. New taken logs retain both actual taken time and the time the log was created. The user can correct the taken time; future taken timestamps are rejected.
- Existing logs keep their original timestamps and details. Older logged times are not silently promoted to confirmed actual taken times.
- Schedule replaces Today and scrolls through consecutive days. A prominent next-dose card, larger dose times and a current-time divider locate the user in the day. Future dates are viewable without allowing future-day logging.
- Dashboard shows timing accuracy, average absolute time difference and daily counts of on-time, outside-window, skipped and not-logged doses. Future/unexpired doses are excluded from missing counts. A saved plan ledger supports honest denominators across schedule edits and archiving.
- New tracking coverage is recorded on upgrade. Past days without saved schedules are not fabricated as missed. Earlier times on the first tracking day can be shown as untracked.
- Four floating tabs: Schedule, Medications, Dashboard, History. English remains default; all new text also ships in Spanish. Permission guidance, alerts and ringing alarms remain supported.

## UI system

Use the reference as a direction for a soft lavender/peach/blue ambient background, translucent rounded surfaces and dark, compact primary actions. Create reusable icon buttons with localized accessibility labels/tooltips, reusable progress lines and native chart components. Keep labels where precision matters, including permission instructions and form fields. Respect Android's disabled-animation setting.

## Implementation and validation

1. Add treatment/date and timing domain logic with deterministic tests.
2. Migrate the database additively: treatment fields, expected-dose ledger, actual/scheduled timestamps and coverage metadata. Preserve original records and IDs.
3. Bound reminder scheduling by treatment dates and validate receiver delivery against the course.
4. Add optional treatment controls, animated course progress and actual-time editing.
5. Build a virtualized day-by-day calendar feed and current/next-dose indicators.
6. Add a dashboard with real ledger-based metrics and daily graphs.
7. Apply reusable visual components across Schedule, Medications, History, Dashboard and setup surfaces; complete English/Spanish catalogs.
8. Test migration, treatment boundaries, missing/pending classification, timing corrections, schedule edits/archiving, chart inputs, navigation, permissions and native alarm delivery. Review Android-rendered screenshots and package a signed in-place APK update.

## Completion

Implemented and verified in version 2.0. See ../VERIFICATION.md for migration, treatment, timing, calendar, dashboard, permission and alarm results. Native screenshots are saved in this folder.
