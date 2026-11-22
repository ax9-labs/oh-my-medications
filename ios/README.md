# Oh my Medications! for iOS

The native SwiftUI app requires iOS 26 or later. It runs offline and keeps medication records in app-private, backup-excluded storage. Each platform has its own local data. CSV export is a history report.

## Build and run

Use Xcode 26 or later on macOS. Open `OhMyMedications.xcodeproj`, select the **OhMyMedications** scheme, and run it on an iOS 26 Simulator. To install on a device, select your development team under **Signing & Capabilities**. The bundle identifier is `com.ax9labs.ohmymedications`.

From this directory:

```sh
swift test
xcodebuild -project OhMyMedications.xcodeproj -scheme OhMyMedications \
  -destination 'generic/platform=iOS Simulator' CODE_SIGNING_ALLOWED=NO build
```

The Swift tests run without Apple UI frameworks. The GitHub Actions workflow also builds the app with Xcode.

## Features and platform differences

- **Schedule and medications** support 1–12 distinct daily times, optional instructions, a start date, optional courses of 1–3650 days, six icons, eight optional colors, and archiving.
- **Dose history** stores the scheduled, actual, and original recording times alongside the medication details at logging. You can log or skip doses, correct a taken time, undo an entry, and export CSV through Files.
- **Dashboard** reports 7- or 30-day timing accuracy, mean absolute timing difference, and daily counts. Taken doses within 30 minutes of the scheduled time are on time. Skipped and overdue unlogged doses are separate.
- **Reminders** use Apple AlarmKit to ring and show the system alarm controls, including while the app is open. **Open app** brings you to the regular app. **Stop** silences the alarm. Record doses separately in Schedule.
- **Alarm coverage** uses recurring alarms for eligible ongoing schedules and one-time alarms for bounded courses, future starts, and times with an upcoming dose already logged. The app queues up to 32 schedule entries, prioritizing the next due time, and refreshes when opened. Settings shows the first alarm outside the queue when applicable. Open the app before then to extend coverage. Fixed alarms are recalculated for the current time zone when the app next opens.
- **Language** can be switched between English and Spanish in the app’s Settings. iOS controls system UI, including permission prompts and the file picker.

To verify reminders on a device, start a test alarm in Settings and confirm that the iOS alarm appears over the active app and stops without a dose log. Let a scheduled alarm ring while the phone is locked and tap **Open app**. Confirm that you can navigate the app immediately. Then schedule a near-term dose, log it early, and confirm that its pending alarm is replaced. Check a short course through its final day and confirm that later doses do not ring.
