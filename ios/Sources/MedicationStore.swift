import Foundation
import SwiftUI
import UniformTypeIdentifiers

func translate(_ key: String, _ language: String) -> String {
    guard let path = Bundle.main.path(forResource: language, ofType: "lproj"),
          let bundle = Bundle(path: path) else { return key }
    return bundle.localizedString(forKey: key, value: key, table: nil)
}

@MainActor
final class MedicationStore: ObservableObject {
    @Published private(set) var data = MedicationData()
    @Published var error: String?
    @Published private(set) var loaded = false
    private var file: URL?

    init() { load() }

    func load() {
        guard !loaded else { return }
        do {
            var folder = try FileManager.default.url(for: .applicationSupportDirectory,
                                                     in: .userDomainMask, appropriateFor: nil, create: true)
                .appendingPathComponent("MedicationData", isDirectory: true)
            try FileManager.default.createDirectory(at: folder, withIntermediateDirectories: true)
            var values = URLResourceValues()
            values.isExcludedFromBackup = true
            try folder.setResourceValues(values)
            let file = folder.appendingPathComponent("records.json")
            do {
                data = try JSONDecoder().decode(MedicationData.self, from: Data(contentsOf: file))
            } catch let error as CocoaError where error.code == .fileReadNoSuchFile {
                data = MedicationData()
            }
            self.file = file
            loaded = true
            error = nil
        } catch {
            self.error = "Medication records could not be opened."
        }
    }

    private func commit(_ change: (inout MedicationData) -> Void) -> Bool {
        guard let file else { return false }
        var updated = data
        change(&updated)
        do {
            let bytes = try JSONEncoder().encode(updated)
            try bytes.write(to: file, options: [.atomic, .completeFileProtectionUntilFirstUserAuthentication])
            data = updated
            return true
        } catch {
            self.error = "Changes could not be saved."
            return false
        }
    }

    func save(_ medication: Medication) -> Bool {
        commit { $0.revisions.append(MedicationRevision(medication: medication, effective: .now)) }
    }

    func archive(_ medication: Medication) {
        _ = commit { $0.revisions.append(MedicationRevision(medication: medication, effective: .now, archived: true)) }
    }

    func log(_ slot: DoseSlot, status: DoseStatus, actual: Date? = nil) -> Bool {
        let now = Date.now
        guard data.events[slot.id] == nil else { return false }
        let taken = status == .taken ? (actual ?? now) : nil
        guard status == .taken ? (slot.canTake(at: now) && (taken ?? now) <= now) : slot.canSkip(at: now) else {
            error = "Choose a valid time for this dose."
            return false
        }
        return commit { $0.events[slot.id] = DoseEvent(slot: slot, status: status, recorded: now, taken: taken) }
    }

    func correct(_ event: DoseEvent, to date: Date) -> Bool {
        guard data.events[event.id]?.status == .taken, date <= .now else {
            error = "Choose a valid time for this dose."
            return false
        }
        return commit {
            $0.events[event.id]?.taken = date
            $0.events[event.id]?.updated = .now
        }
    }

    func undo(_ event: DoseEvent) {
        _ = commit { $0.events.removeValue(forKey: event.id) }
    }
}

struct HistoryCSV: FileDocument {
    static var readableContentTypes: [UTType] { [.commaSeparatedText] }
    var data: Data

    init(configuration: ReadConfiguration) throws { data = configuration.file.regularFileContents ?? Data() }

    init(events: [DoseEvent], language: String) {
        let labels = ["Date", "Scheduled time", "Medication", "Dose", "Status", "Recorded at",
                      "Scheduled at", "Taken at", "Time zone", "Difference (min)"]
        let formatter = ISO8601DateFormatter()
        func safe(_ value: String) -> String {
            if let first = value.trimmingCharacters(in: .whitespacesAndNewlines).first, "=+@-".contains(first) {
                return "'" + value
            }
            return value
        }
        func row(_ cells: [String]) -> String {
            cells.map { "\"" + $0.replacingOccurrences(of: "\"", with: "\"\"") + "\"" }.joined(separator: ",")
        }
        var rows = [row(labels.map { translate($0, language) })]
        for event in events {
            let slot = event.slot
            let delta = event.taken.map {
                String(format: "%.2f", locale: Locale(identifier: "en_US_POSIX"),
                       $0.timeIntervalSince(slot.scheduled) / 60)
            } ?? ""
            rows.append(row([slot.day, slot.time.label, safe(slot.medication.name), safe(slot.medication.dose),
                             translate(event.status == .taken ? "Taken" : "Skipped", language),
                             formatter.string(from: event.recorded), formatter.string(from: slot.scheduled),
                             event.taken.map { formatter.string(from: $0) } ?? "", slot.zone, delta]))
        }
        data = Data((rows.joined(separator: "\r\n") + "\r\n").utf8)
    }

    func fileWrapper(configuration: WriteConfiguration) throws -> FileWrapper {
        FileWrapper(regularFileWithContents: data)
    }
}
