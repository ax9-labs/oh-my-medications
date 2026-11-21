import AlarmKit
import CryptoKit
import SwiftUI

private struct DoseAlarmMetadata: AlarmMetadata {}

@MainActor
final class Reminders: ObservableObject {
    @Published private(set) var authorized = false
    @Published private(set) var schedulingFailed = false
    @Published private(set) var firstUnscheduled: Date?
    private let manager = AlarmManager.shared
    private let testID = UUID(uuidString: "5D7D9C99-4101-4580-8E82-BDDE4EA03B93")!
    private var queued: (MedicationData, String)?
    private var updating = false

    func authorize() async {
        do {
            authorized = try await manager.requestAuthorization() == .authorized
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

    func test(language: String) async {
        guard authorized else { return }
        do {
            if try manager.alarms.contains(where: { $0.id == testID }) {
                try manager.cancel(id: testID)
            }
            try await schedule(id: testID, title: translate("Test reminder", language),
                               schedule: .fixed(.now.addingTimeInterval(10)), language: language)
        } catch {
            schedulingFailed = true
        }
    }

    private func synchronize(_ data: MedicationData, language: String) async {
        authorized = manager.authorizationState == .authorized
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
        let key = [med.id.uuidString, entry.time.label, med.name, med.dose, language,
                   entry.repeats ? "daily" : String(entry.next.timeIntervalSince1970)]
        let input = key.map { "\($0.utf8.count):\($0)" }.joined()
        let bytes = Array(SHA256.hash(data: Data(input.utf8)))
        return UUID(uuid: (bytes[0], bytes[1], bytes[2], bytes[3], bytes[4], bytes[5], bytes[6], bytes[7],
                           bytes[8], bytes[9], bytes[10], bytes[11], bytes[12], bytes[13], bytes[14], bytes[15]))
    }

    private func schedule(id: UUID, title: String, schedule: Alarm.Schedule, language: String) async throws {
        let stop = AlarmButton(text: LocalizedStringResource(stringLiteral: translate("Stop", language)),
                               textColor: .white, systemImageName: "stop.circle")
        let alert = AlarmPresentation.Alert(title: LocalizedStringResource(stringLiteral: title), stopButton: stop)
        let attributes = AlarmAttributes(presentation: AlarmPresentation(alert: alert),
                                        metadata: DoseAlarmMetadata(), tintColor: .teal)
        let configuration = AlarmManager.AlarmConfiguration.alarm(schedule: schedule, attributes: attributes)
        _ = try await manager.schedule(id: id, configuration: configuration)
    }
}
