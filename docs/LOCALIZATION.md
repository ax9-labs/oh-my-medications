# App localization

English is the default and fallback. Spanish is available from **Settings → Language: English → Español**. On Android 13 or later, the app also registers English and Spanish in Android's per-app language settings. Clearing that selection falls back to English, independent of the phone's language. Android 8–12 store the choice in app preferences.

## Resource structure

- `app/src/main/res/values/strings.xml`: complete English catalog, shared messages and plurals.
- `app/src/main/res/values-es/strings.xml`: complete Spanish catalog.
- `app/src/main/res/values/languages.xml`: supported BCP 47 tags and their native display names, in matching order.
- `app/src/main/res/xml/locales_config.xml`: registration for Android's per-app language settings.
- `AppLocale.java`: locale selection, resource contexts, background text and date/time formatting.
- `LocalizedActivity.java`: applies the selected locale before each activity is created.
- `HistoryExport.java`: CSV report labels/status text use the chosen app language.

UI code references named Android resources rather than storing visible English strings in Java. `MedAppearance` retains stable icon/color identifiers and resolves their labels through resources. Background notification and channel labels use the same locale helper as the UI. A language change refreshes existing alarm notification text without restarting audio.

## Add another language

1. Copy the English catalog into the appropriate Android resource directory, such as `values-fr/strings.xml` for French. Use standard Android qualifiers for regional translations, such as `values-b+pt+BR`.
2. Translate every translatable string and plural item. Keep resource keys and numbered formatting arguments (`%1$s`, `%2$d`, etc.) unchanged; reorder the arguments in the sentence when needed. Add the plural quantities required by the target language. XML escapes and Android string escapes must remain valid. Preserve explicit quotes used to retain repeated spaces.
3. Add its BCP 47 tag and native display name to the paired arrays in `languages.xml`. Keep English first. Add the same tag to `locales_config.xml`.
4. Run `./test.sh`, `./build.sh` and the Android integration tests. The translation checker verifies complete catalogs, matching format arguments, plural basics and language registration. Android's base resource catalog remains the fallback for untranslated resources.
5. Review the translated screens, accessibility descriptions, dates, notification/channel text, alarm screen and CSV export on a device. Long text can wrap; layout and grammatical review are still necessary. Check RTL layouts when adding an RTL language.

The app name and version are marked `translatable="false"`. Native language names remain native names in every locale. Internal exceptions and developer logs are not user-facing UI.

## Data and behavior

Medication names, prescribed dose text and notes remain exactly as the user entered them. Database status codes (`taken`, `skipped`), icon/color identifiers, daily alarm times (`HH:mm`), ISO dates, exported UTC timestamps and pending-alarm identities are not translated. Version 2.0 uses database schema 3; treatment dates, schedule instants and actual taken times remain language-independent. Localization does not change medication instructions, timezone or dose schedules.

Displayed dates and times use the selected app locale and Android's 12/24-hour preference. CSV headers and dose status labels are localized; CSV dates/times/timestamps retain their stable machine formats. CSV export is a history report, not an app backup.
