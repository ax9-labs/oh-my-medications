# Oh my Medications!
UI system

Version 2.0 uses standard Android views and property animations; it adds no UI framework, network access or permissions.

`colors.xml` is the shared color source.
`Palette.java` exposes these resource colors to programmatic views, while the native theme and launcher vector refer to the same resources.
Light-blue primary surfaces use dark text for readable contrast.
User-selected medication icon colors are independent of the app theme.

`FloatingNavBar` stays mounted while pages change.
It contains four outline icons, an active label, localized accessibility names, tooltips, ripple feedback and a measured selection pill.
Its width is capped at 420 dp on wider screens.
The bar is at least 76 dp high, growing with the system text size, and is inset horizontally by 22 dp and vertically by 16 dp above Android's bottom system inset.
Content reserves the measured navigation height plus 36 dp so final controls can move above the overlay.
On compact screens and with large text, the selected item expands while the other three retain at least 48 dp touch targets.
Its label sizes to the available width and keeps its full localized accessible name.

The selection pill glides for 300 ms.
Icons lift 10 dp and scale gently over 260 ms; the active label fades in over 200 ms.
Outgoing content fades/slides out over 150 ms and new content settles over 260 ms.
Motion checks `ValueAnimator.areAnimatorsEnabled()` and uses native interpolators.
Disabling system animations shows the requested tab immediately.

`MainActivity` owns a persistent root, page host and navigation bar.
Changing tabs cancels stale transitions, releases outgoing pages, keeps only the latest request, and restores each tab's scroll offset.
Outgoing pages are removed from accessibility traversal during the transition.
Ordinary data refreshes replace page content without recreating the navigation bar.
Tab and scroll offsets are saved across activity recreation, including language changes.

Android instrumentation covers rapid alternating taps, selected accessibility state, motion enabled/disabled behavior, first-layout selection geometry, persistent navigation identity and final page visibility/cleanup.
The full suite also covers language, medication, history and alarm flows.
The focused check can run on a disposable emulator with:

```sh
adb shell settings put global animator_duration_scale 0
adb shell am instrument -w -e navOnly true com.personal.medtracker.tests/com.personal.medtracker.AppInstrumentation
```

Like the full integration runner, this clears test medication data and changes the emulator's animation scale.
It is not intended for a personal phone containing real medication records.


## Shared components

`AmbientBackground` draws restrained lavender, peach and blue radial gradients.
`UiKit` provides rounded translucent surfaces, gradient highlight cards and 48 dp icon buttons with localized accessible names, tooltips and ripple feedback.
Primary icon actions are dark with white glyphs.
`IconGlyph` supplies scalable native outline icons.
Native form controls keep visible text where a choice needs explanation.

`AnimatedLine` reveals a treatment’s calendar progress over 800 ms when its view opens.
`AdherenceChart` reveals stacked daily dose counts over 700 ms and exposes a localized summary to accessibility services.
Both cancel animations on detach and respect disabled system animations.

`ScheduleFeed` is a native ListView that mounts visible rows only.
It loads day groups in two-week increments, shows a next-dose hero and current-time divider, and supports date jumping.
A day rail offers quick navigation.
Future dates show expected doses without logging controls.
Scroll position and expanded date range survive tab changes and activity recreation.
The date picker is bounded to ten years either side of today; continuous forward scrolling is capped at ten years.

Dashboard uses a 7/30-day range selector, prominent timing percentage, daily stacked chart with numeric legend, average absolute time difference and explicit coverage notes.
Unknown legacy timing never receives an invented accuracy score.
`Store` preserves expected plans separately from handling events, so changing an active medication cannot erase old unlogged expectations.

## Native interface audit

The [Web Interface Guidelines](https://raw.githubusercontent.com/vercel-labs/web-interface-guidelines/main/command.md) inform the native Android equivalents of focus, form labeling, state, content handling, motion, and long-list rendering.
HTML-specific rules do not apply to these Android views.

Page titles share a 32 sp heading and subtitle pattern; Dashboard also uses the shared eyebrow heading.
Schedule rows and cards use matching 22 dp outer edges.
Compact dose rows place the scheduled time above the card, and compact dashboard metrics stack their explanation below the percentage.

Custom actions, picker tiles, and navigation expose native button or radio-button roles, visible focus outlines, hover outlines, and pressed ripples.
Decorative children stay outside the accessibility traversal, and navigation labels share their parent hit target.
Dashboard ranges expose checked selection and retain keyboard focus when changed.
Picker tiles retain keyboard focus after their preview is rebuilt.

Each medication input has a persistent associated label and a visible focused border.
Changed drafts require confirmation before Cancel, Back, or outside dismissal discards them.
Keep editing retains the draft, while unchanged forms close immediately.
Saving and archiving retain their existing behavior.
The confirmation is translated into English and Spanish.

History uses a native ListView, mounting visible cards instead of constructing the full history as views.
The underlying history and CSV export retain all entries.
The alarm screen uses a scrolling viewport so its controls remain reachable with large fonts and short displays.

Chart colors come from colors.xml and use stronger contrast against the card surface.
Chart labels follow the system text size and selected locale, and zero or one expected dose no longer produces duplicate axis ticks.
The legend and accessible summary retain explicit counts so color is not the only cue.
No-data accuracy has an accessible text description.

## Audit verification

The focused instrumentation audit checks real medication dialogs, Cancel and Android cancellation, retained drafts, selected dashboard ranges, native action roles, complete navigation labels, a 200-entry history, and reachable alarm controls.
Run it only on a disposable emulator because it replaces the app's test medication data:

```powershell
.\build.ps1 -AndroidTests
adb -s emulator-5556 install -r dist/med-tracker.apk
adb -s emulator-5556 install -r build/instrumentation/tests.apk
adb -s emulator-5556 shell am instrument -w -e uiAudit true com.personal.medtracker.tests/com.personal.medtracker.AppInstrumentation
```

The navigation test sets its required animation preference explicitly and restores the original value afterward.
Screenshot capture waits for tab transitions and chart or treatment reveals to settle.
