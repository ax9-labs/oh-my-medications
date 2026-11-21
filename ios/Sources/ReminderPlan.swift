import Foundation

struct ReminderPlan: Equatable {
    struct Entry: Equatable {
        var medication: Medication
        var time: DoseTime
        var next: Date
        var repeats: Bool
    }

    var entries: [Entry]
    var firstUnscheduled: Date?

    init(data: MedicationData, now: Date, limit: Int = 32, calendar: Calendar = ScheduleCalendar.local) {
        let today = calendar.startOfDay(for: now)
        var candidates: [Entry] = []
        let futureLogs = data.events.values.map(\.slot).filter { $0.scheduled > now }

        for medication in data.currentMedications where medication.reminders {
            guard let start = ScheduleCalendar.date(medication.start, in: calendar) else { continue }
            for time in medication.times {
                let repeats = medication.duration == 0 && start <= today &&
                    !futureLogs.contains { $0.medication.id == medication.id && $0.time == time }
                var day = max(start, today)
                var count = 0
                while medication.includes(day, calendar: calendar) && count <= limit {
                    let slotID = "\(medication.id.uuidString)/\(ScheduleCalendar.day(day, in: calendar))/\(time.label)"
                    if let scheduled = ScheduleCalendar.scheduled(time, on: day, in: calendar),
                       scheduled > now, data.events[slotID] == nil {
                        candidates.append(Entry(medication: medication, time: time, next: scheduled, repeats: repeats))
                        if repeats { break }
                        count += 1
                    }
                    day = ScheduleCalendar.adding(1, to: day, in: calendar)
                }
            }
        }
        candidates.sort {
            if $0.next != $1.next { return $0.next < $1.next }
            return $0.medication.id.uuidString < $1.medication.id.uuidString
        }
        entries = Array(candidates.prefix(limit))
        firstUnscheduled = candidates.dropFirst(limit).first?.next
    }
}
