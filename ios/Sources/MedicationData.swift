import Foundation

struct DoseTime: Codable, Hashable, Comparable, Identifiable {
    var hour: Int
    var minute: Int
    var id: Int { hour * 60 + minute }
    var label: String { String(format: "%02d:%02d", hour, minute) }

    static func < (lhs: Self, rhs: Self) -> Bool { lhs.id < rhs.id }

    init(hour: Int, minute: Int) { self.hour = hour; self.minute = minute }

    init(_ date: Date) {
        let parts = ScheduleCalendar.local.dateComponents([.hour, .minute], from: date)
        self.init(hour: parts.hour ?? 0, minute: parts.minute ?? 0)
    }
}

enum ScheduleCalendar {
    static var local: Calendar {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = .autoupdatingCurrent
        return calendar
    }

    static func day(_ date: Date, in calendar: Calendar = local) -> String {
        let parts = calendar.dateComponents([.year, .month, .day], from: date)
        return String(format: "%04d-%02d-%02d", parts.year ?? 0, parts.month ?? 0, parts.day ?? 0)
    }

    static func date(_ day: String, in calendar: Calendar = local) -> Date? {
        let parts = day.split(separator: "-").compactMap { Int($0) }
        guard parts.count == 3 else { return nil }
        return calendar.date(from: DateComponents(year: parts[0], month: parts[1], day: parts[2]))
    }

    static func adding(_ days: Int, to date: Date, in calendar: Calendar = local) -> Date {
        calendar.date(byAdding: .day, value: days, to: date) ?? date
    }

    static func scheduled(_ time: DoseTime, on day: Date, in calendar: Calendar = local) -> Date? {
        calendar.nextDate(
            after: calendar.startOfDay(for: day).addingTimeInterval(-1),
            matching: DateComponents(hour: time.hour, minute: time.minute),
            matchingPolicy: .nextTimePreservingSmallerComponents,
            repeatedTimePolicy: .first
        )
    }
}

enum MedicationIcon: String, CaseIterable, Codable, Identifiable {
    case capsule = "pills.fill"
    case tablet = "pill.fill"
    case bottle = "cross.vial.fill"
    case drops = "drop.fill"
    case injection = "syringe.fill"
    case cream = "bandage.fill"
    var id: String { rawValue }
}

enum MedicationColor: String, CaseIterable, Codable, Identifiable {
    case standard, red, orange, yellow, green, teal, blue, purple, pink
    var id: String { rawValue }
}

struct Medication: Identifiable, Codable, Equatable {
    var id = UUID()
    var name = ""
    var dose = ""
    var notes = ""
    var times = [DoseTime(hour: 8, minute: 0)]
    var reminders = true
    var start = ScheduleCalendar.day(.now)
    var duration = 0
    var icon = MedicationIcon.capsule
    var color = MedicationColor.standard

    func includes(_ day: Date, calendar: Calendar = ScheduleCalendar.local) -> Bool {
        guard let first = ScheduleCalendar.date(start, in: calendar), day >= first else { return false }
        return duration == 0 || day < ScheduleCalendar.adding(duration, to: first, in: calendar)
    }

    func courseDay(on date: Date) -> Int {
        let calendar = ScheduleCalendar.local
        guard duration > 0, let first = ScheduleCalendar.date(start) else { return 0 }
        let elapsed = calendar.dateComponents([.day], from: first, to: calendar.startOfDay(for: date)).day ?? 0
        return max(0, min(duration, elapsed + 1))
    }
}

struct MedicationRevision: Codable, Equatable {
    var medication: Medication
    var effective: Date
    var archived = false
}

struct DoseSlot: Codable, Identifiable {
    var medication: Medication
    var day: String
    var time: DoseTime
    var scheduled: Date
    var zone: String
    var tracked: Bool

    var id: String { "\(medication.id.uuidString)/\(day)/\(time.label)" }
    func canTake(at now: Date) -> Bool { scheduled <= now.addingTimeInterval(12 * 3600) }
    func canSkip(at now: Date) -> Bool { day <= ScheduleCalendar.day(now) }
}

enum DoseStatus: String, Codable {
    case taken, skipped
}

struct DoseEvent: Codable, Identifiable {
    var slot: DoseSlot
    var status: DoseStatus
    var recorded: Date
    var taken: Date?
    var updated: Date?
    var id: String { slot.id }
}

struct MedicationData: Codable {
    var revisions: [MedicationRevision] = []
    var events: [String: DoseEvent] = [:]

    var currentMedications: [Medication] {
        latestRevisions.values.filter { !$0.archived }.map(\.medication)
            .sorted { $0.name.localizedStandardCompare($1.name) == .orderedAscending }
    }

    private var latestRevisions: [UUID: MedicationRevision] {
        Dictionary(revisions.map { ($0.medication.id, $0) }, uniquingKeysWith: { _, latest in latest })
    }

    func slots(on date: Date, calendar: Calendar = ScheduleCalendar.local) -> [DoseSlot] {
        let day = calendar.startOfDay(for: date)
        let dayKey = ScheduleCalendar.day(day, in: calendar)
        var slots: [String: DoseSlot] = [:]
        let timelines = Dictionary(grouping: revisions, by: { $0.medication.id })
        for timeline in timelines.values {
            for (index, revision) in timeline.enumerated() {
                let medication = revision.medication
                guard !revision.archived, medication.includes(day, calendar: calendar) else { continue }
                let next = index + 1 < timeline.count ? timeline[index + 1].effective : .distantFuture
                for time in medication.times {
                    guard let scheduled = ScheduleCalendar.scheduled(time, on: day, in: calendar),
                          scheduled < next else { continue }
                    let tracked = scheduled >= revision.effective
                    let firstDay = index == 0 && calendar.isDate(revision.effective, inSameDayAs: day)
                    guard tracked || firstDay else { continue }
                    let slot = DoseSlot(medication: medication, day: dayKey, time: time,
                                        scheduled: scheduled, zone: calendar.timeZone.identifier, tracked: tracked)
                    slots[slot.id] = slot
                }
            }
        }
        for event in events.values where event.slot.day == dayKey {
            slots[event.id] = event.slot
        }
        return slots.values.sorted {
            if $0.scheduled != $1.scheduled { return $0.scheduled < $1.scheduled }
            return $0.id < $1.id
        }
    }

    func nextDose(after now: Date) -> DoseSlot? {
        let calendar = ScheduleCalendar.local
        let today = calendar.startOfDay(for: now)
        let medIDs = Set(currentMedications.map(\.id))
        var candidates = slots(on: today).filter {
            $0.scheduled > now && medIDs.contains($0.medication.id) && events[$0.id] == nil
        }
        let tomorrow = ScheduleCalendar.adding(1, to: today)
        for medication in currentMedications {
            guard let start = ScheduleCalendar.date(medication.start) else { continue }
            var day = max(tomorrow, start)
            while medication.includes(day) {
                let dayKey = ScheduleCalendar.day(day)
                let slots = medication.times.compactMap { time -> DoseSlot? in
                    guard let scheduled = ScheduleCalendar.scheduled(time, on: day) else { return nil }
                    let slot = DoseSlot(medication: medication, day: dayKey, time: time, scheduled: scheduled,
                                        zone: calendar.timeZone.identifier, tracked: true)
                    return events[slot.id] == nil ? slot : nil
                }
                if !slots.isEmpty {
                    candidates.append(contentsOf: slots)
                    break
                }
                day = ScheduleCalendar.adding(1, to: day)
            }
        }
        return candidates.min { $0.scheduled < $1.scheduled }
    }

    var history: [DoseEvent] {
        events.values.sorted {
            if $0.slot.day != $1.slot.day { return $0.slot.day > $1.slot.day }
            if $0.slot.time != $1.slot.time { return $0.slot.time > $1.slot.time }
            return $0.recorded > $1.recorded
        }
    }
}

struct AdherenceDay: Identifiable {
    var date: Date
    var counts = [Int](repeating: 0, count: 4)
    var id: Date { date }
}

struct AdherenceReport {
    var days: [AdherenceDay] = []
    var difference: TimeInterval = 0
    var known = 0

    var accuracy: Int? {
        guard known > 0 else { return nil }
        return Int((Double(days.reduce(0) { $0 + $1.counts[0] }) * 100 / Double(known)).rounded())
    }

    var averageMinutes: Int? {
        known > 0 ? Int((difference / Double(known) / 60).rounded()) : nil
    }

    init(data: MedicationData, range: Int, now: Date) {
        let window: TimeInterval = 30 * 60
        for offset in (1 - range)...0 {
            let date = ScheduleCalendar.adding(offset, to: now)
            var day = AdherenceDay(date: date)
            for slot in data.slots(on: date) {
                switch data.events[slot.id] {
                case .some(let event) where event.status == .taken:
                    if let taken = event.taken {
                        let delta = abs(taken.timeIntervalSince(event.slot.scheduled))
                        difference += delta
                        known += 1
                        day.counts[delta <= window ? 0 : 1] += 1
                    }
                case .some:
                    day.counts[2] += 1
                case .none where slot.tracked && slot.scheduled.addingTimeInterval(window) < now:
                    day.counts[3] += 1
                default:
                    break
                }
            }
            days.append(day)
        }
    }
}
