import AlarmKit
import AppIntents
import CryptoKit
import SwiftUI

private struct DoseAlarmMetadata: AlarmMetadata {}

@MainActor
final class Reminders: ObservableObject {
    @Published private(set) var authorized = AlarmManager.shared.authorizationState == .authorized
    @Published private(set) var permissionDenied = AlarmManager.shared.authorizationState == .denied

    var needsAttention: Bool { !authorized || schedulingFailed }

    func checkAuthorization() {
        authorized = manager.authorizationState == .authorized
        permissionDenied = manager.authorizationState == .denied
    }

    func observeAuthorization() async {
        checkAuthorization()
        for await state in manager.authorizationUpdates {
            guard !Task.isCancelled else { return }
            authorized = state == .authorized
            permissionDenied = state == .denied
        }
    }
    @Published private(set) var schedulingFailed = false
    @Published private(set) var firstUnscheduled: Date?
    static let shared = Reminders()
    @Published private(set) var activeAlarm: ActiveAlarm?
    @Published private(set) var stoppingFailed = false
    private var openedAlarmID: UUID?
    private let manager = AlarmManager.shared

    struct ActiveAlarm: Identifiable {
        let id: UUID
        let title: String
        let medications: [Medication]
        let isTest: Bool
    }

    func observeAlarms() async {
        updateActiveAlarms()
        for await alarms in manager.alarmUpdates {
            guard !Task.isCancelled else { return }
            updateActiveAlarms(alarms)
        }
    }

    func updateActiveAlarms(_ incoming: [Alarm]? = nil) {
        guard let alarms = incoming ?? (try? manager.alarms) else { return }
        let ringing = alarms.filter { $0.state == .alerting }
        let id = openedAlarmID
            ?? ringing.first(where: { $0.id == activeAlarm?.id })?.id
            ?? ringing.first?.id
        activeAlarm = id.map { id in
            let ids = Set(ringing.map(\.id) + [id])
            let medications = ids.compactMap { alarmID -> Medication? in
                guard let data = UserDefaults.standard.data(forKey: "alarmMedication.\(alarmID.uuidString)") else {
                    return nil
                }
                return try? JSONDecoder().decode(Medication.self, from: data)
            }
            let unique = Dictionary(medications.map { ($0.id, $0) }, uniquingKeysWith: { first, _ in first })
            return ActiveAlarm(
                id: id,
                title: UserDefaults.standard.string(forKey: "alarmTitle.\(id.uuidString)") ?? "",
                medications: unique.values.sorted { $0.name.localizedStandardCompare($1.name) == .orderedAscending },
                isTest: id == testID)
        }
    }

    func openAlarm(id: UUID) {
        openedAlarmID = id
        stoppingFailed = false
        updateActiveAlarms()
    }

    func stopActiveAlarm() {
        guard let alarm = activeAlarm else { return }
        do {
            // A custom Open action may already have stopped the system alert.
            if try manager.alarms.contains(where: { $0.id == alarm.id && $0.state == .alerting }) {
                try manager.stop(id: alarm.id)
            }
            openedAlarmID = nil
            stoppingFailed = false
            updateActiveAlarms()
        } catch {
            stoppingFailed = true
        }
    }
    private let testID = UUID(uuidString: "5D7D9C99-4101-4580-8E82-BDDE4EA03B93")!
    private var queued: (MedicationData, String)?
    private var updating = false

    func authorize() async {
        do {
            _ = try await manager.requestAuthorization()
            checkAuthorization()
        } catch {
            authorized = false
            schedulingFailed = true
        }
    }

    func refresh(_ data: MedicationData, language: String) {
        queued = (data, language)
        guard !updating else { return }
        updating = true
        Task {
            defer { updating = false }
            while let (data, language) = queued {
                queued = nil
                await synchronize(data, language: language)
            }
        }
    }

    @Published private(set) var testing = false
    @Published private(set) var testStatus: String?

    func test(language: String) async {
        guard !testing else { return }
        checkAuthorization()
        guard authorized else {
            testStatus = "Allow alarm access before testing."
            return
        }
        testing = true
        testStatus = nil
        defer { testing = false }
        do {
            if try manager.alarms.contains(where: { $0.id == testID }) {
                try manager.cancel(id: testID)
            }
            try await schedule(id: testID, title: translate("Test reminder", language),
                               schedule: .fixed(.now.addingTimeInterval(1)), language: language)
            testStatus = "Test alarm scheduled. It should ring in about a second."
        } catch {
            testStatus = "The test alarm could not be scheduled. Check alarm access and try again."
            schedulingFailed = true
        }
    }

    private func synchronize(_ data: MedicationData, language: String) async {
        checkAuthorization()
        firstUnscheduled = nil
        schedulingFailed = false
        guard authorized else { return }
        let plan = ReminderPlan(data: data, now: .now)
        firstUnscheduled = plan.firstUnscheduled
        let desired = Dictionary(uniqueKeysWithValues: plan.entries.map { (identifier($0, language: language), $0) })
        do {
            let existing = try manager.alarms
            for alarm in existing where alarm.id != testID && desired[alarm.id] == nil && alarm.state != .alerting {
                do { try manager.cancel(id: alarm.id) }
                catch { schedulingFailed = true }
            }
            let existingIDs = Set(existing.map(\.id))
            for entry in plan.entries {
                let id = identifier(entry, language: language)
                // Keep the prescribed details available after a cold launch from an alarm.
                let details = try JSONEncoder().encode(entry.medication)
                UserDefaults.standard.set(details, forKey: "alarmMedication.\(id.uuidString)")
                guard !existingIDs.contains(id), entry.next > .now else { continue }
                let schedule: Alarm.Schedule = entry.repeats
                    ? .relative(.init(time: .init(hour: entry.time.hour, minute: entry.time.minute),
                                      repeats: .weekly([.monday, .tuesday, .wednesday, .thursday, .friday, .saturday, .sunday])))
                    : .fixed(entry.next)
                let title = [entry.medication.name, entry.medication.dose].filter { !$0.isEmpty }.joined(separator: " · ")
                do { try await self.schedule(id: id, title: title, schedule: schedule, language: language) }
                catch { schedulingFailed = true }
            }
        } catch {
            schedulingFailed = true
        }
    }

    private func identifier(_ entry: ReminderPlan.Entry, language: String) -> UUID {
        let med = entry.medication
        let key = ["alarm-screen-v1", med.id.uuidString, entry.time.label, med.name, med.dose, language,
                   entry.repeats ? "daily" : String(entry.next.timeIntervalSince1970)]
        let input = key.map { "\($0.utf8.count):\($0)" }.joined()
        let bytes = Array(SHA256.hash(data: Data(input.utf8)))
        return UUID(uuid: (bytes[0], bytes[1], bytes[2], bytes[3], bytes[4], bytes[5], bytes[6], bytes[7],
                           bytes[8], bytes[9], bytes[10], bytes[11], bytes[12], bytes[13], bytes[14], bytes[15]))
    }

    private func schedule(id: UUID, title: String, schedule: Alarm.Schedule, language: String) async throws {
        let stop = AlarmButton(text: LocalizedStringResource(stringLiteral: translate("Stop", language)),
                               textColor: .white, systemImageName: "stop.circle")
        let open = AlarmButton(text: LocalizedStringResource(stringLiteral: translate("Open app", language)),
                               textColor: .white, systemImageName: "arrow.up.forward.app")
        let alert = AlarmPresentation.Alert(title: LocalizedStringResource(stringLiteral: title),
                                            stopButton: stop, secondaryButton: open,
                                            secondaryButtonBehavior: .custom)
        let attributes = AlarmAttributes(presentation: AlarmPresentation(alert: alert),
                                        metadata: DoseAlarmMetadata(), tintColor: .teal)
        let configuration = AlarmManager.AlarmConfiguration.alarm(
            schedule: schedule, attributes: attributes, secondaryIntent: OpenAlarmIntent(alarmID: id.uuidString))
        UserDefaults.standard.set(title, forKey: "alarmTitle.\(id.uuidString)")
        _ = try await manager.schedule(id: id, configuration: configuration)
    }
}
