# Med Tracker 2.0 UI system

Version 2.0 uses standard Android views and property animations; it adds no UI framework, network access or permissions.

`colors.xml` is the shared color source. `Palette.java` exposes these resource colors to programmatic views, while the native theme and launcher vector refer to the same resources. Light-blue primary surfaces use dark text for readable contrast. User-selected medication icon colors are independent of the app theme.

`FloatingNavBar` stays mounted while pages change. It contains four outline icons, an active label, localized accessibility names, tooltips, ripple feedback and a measured selection pill. Its width is capped at 420 dp on wider screens. The bar is 76 dp high, inset horizontally by 22 dp and vertically by 16 dp above Android's bottom system inset. Scroll views reserve 112 dp of bottom padding so final controls can move above the overlay.

The selection pill glides for 300 ms. Icons lift 10 dp and scale gently over 260 ms; the active label fades in over 200 ms. Outgoing content fades/slides out over 150 ms and new content settles over 260 ms. Motion checks `ValueAnimator.areAnimatorsEnabled()` and uses native interpolators. Disabling system animations shows the requested tab immediately.

`MainActivity` owns a persistent root, page host and navigation bar. Changing tabs cancels stale transitions, releases outgoing pages, keeps only the latest request, and restores each tab's scroll offset. Outgoing pages are removed from accessibility traversal during the transition. Ordinary data refreshes replace page content without recreating the navigation bar. Tab and scroll offsets are saved across activity recreation, including language changes.

Android instrumentation covers rapid alternating taps, selected accessibility state, motion enabled/disabled behavior, first-layout selection geometry, persistent navigation identity and final page visibility/cleanup. The full suite also covers language, medication, history and alarm flows. The focused check can run on a disposable emulator with:

```sh
adb shell settings put global animator_duration_scale 0
adb shell am instrument -w -e navOnly true com.personal.medtracker.tests/com.personal.medtracker.AppInstrumentation
```

Like the full integration runner, this clears test medication data and changes the emulator's animation scale. It is not intended for a personal phone containing real medication records.


## Shared components

`AmbientBackground` draws restrained lavender, peach and blue radial gradients. `UiKit` provides rounded translucent surfaces, gradient highlight cards and 48 dp icon buttons with localized accessible names, tooltips and ripple feedback. Primary icon actions are dark with white glyphs. `IconGlyph` supplies scalable native outline icons. Native form controls keep visible text where a choice needs explanation.

`AnimatedLine` reveals a treatment’s calendar progress over 800 ms when its view opens. `AdherenceChart` reveals stacked daily dose counts over 700 ms and exposes a localized summary to accessibility services. Both cancel animations on detach and respect disabled system animations.

`ScheduleFeed` is a native ListView that mounts visible rows only. It loads day groups in two-week increments, shows a next-dose hero and current-time divider, and supports date jumping. A day rail offers quick navigation. Future dates show expected doses without logging controls. Scroll position and expanded date range survive tab changes and activity recreation. The date picker is bounded to ten years either side of today; continuous forward scrolling is capped at ten years.

Dashboard uses a 7/30-day range selector, prominent timing percentage, daily stacked chart with numeric legend, average absolute time difference and explicit coverage notes. Unknown legacy timing never receives an invented accuracy score. `Store` preserves expected plans separately from handling events, so changing an active medication cannot erase old unlogged expectations.
