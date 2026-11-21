import Foundation
import XCTest
@testable import MedicationCore

final class MedicationCoreTests: XCTestCase {
    private let morning = DoseTime(hour: 8, minute: 0)

    private func instant(_ value: String) throws -> Date {
        try XCTUnwrap(ISO8601DateFormatter().date(from: value))
    }

    func testUsesFirstFallBackTimeAndPreservesMinutesThroughSpringGap() throws {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = try XCTUnwrap(TimeZone(identifier: "America/New_York"))
        let time = DoseTime(hour: 2, minute: 30)
        let spring = try XCTUnwrap(ScheduleCalendar.date("2026-03-08", in: calendar))
        XCTAssertEqual(ScheduleCalendar.scheduled(time, on: spring, in: calendar),
                       try instant("2026-03-08T07:30:00Z"))
        let fall = try XCTUnwrap(ScheduleCalendar.date("2026-11-01", in: calendar))
        XCTAssertEqual(ScheduleCalendar.scheduled(DoseTime(hour: 1, minute: 30), on: fall, in: calendar),
                       try instant("2026-11-01T05:30:00Z"))
    }

    func testFinalDayAndHistoricalSnapshotAfterArchive() throws {
        let first = try XCTUnwrap(ScheduleCalendar.date("2026-11-10"))
        let second = ScheduleCalendar.adding(1, to: first)
        let third = ScheduleCalendar.adding(2, to: first)
        let med = Medication(name: "Original", dose: "1 tablet", times: [morning],
                             start: "2026-11-10", duration: 2)
        var data = MedicationData(revisions: [.init(medication: med, effective: first)])
        XCTAssertEqual(data.slots(on: second).count, 1)
        XCTAssertTrue(data.slots(on: third).isEmpty)
        XCTAssertEqual(med.courseDay(on: third), 2)

        let slot = try XCTUnwrap(data.slots(on: first).first)
        data.events[slot.id] = DoseEvent(slot: slot, status: .taken, recorded: slot.scheduled,
                                       taken: slot.scheduled.addingTimeInterval(1800))
        var edited = med
        edited.name = "Edited"
        data.revisions.append(.init(medication: edited, effective: second, archived: true))
        XCTAssertTrue(data.currentMedications.isEmpty)
        XCTAssertEqual(data.history.first?.slot.medication.name, "Original")
        XCTAssertTrue(data.slots(on: second).isEmpty)
        XCTAssertEqual(AdherenceReport(data: data, range: 7, now: third).accuracy, 100)
    }

    func testReminderWindowExcludesLoggedDosesAndStopsAtEndOfCourse() throws {
        let start = try XCTUnwrap(ScheduleCalendar.date("2026-11-10"))
        let med = Medication(name: "Example", dose: "1 tablet", times: [morning],
                             start: "2026-11-10", duration: 3)
        var data = MedicationData(revisions: [.init(medication: med, effective: start)])
        let slot = try XCTUnwrap(data.slots(on: start).first)
        data.events[slot.id] = DoseEvent(slot: slot, status: .skipped, recorded: start)
        let tomorrow = ScheduleCalendar.adding(1, to: start)
        let final = ScheduleCalendar.adding(2, to: start)
        let plan = ReminderPlan(data: data, now: start, limit: 1)
        XCTAssertEqual(plan.entries.count, 1)
        XCTAssertEqual(plan.entries.first?.next, ScheduleCalendar.scheduled(morning, on: tomorrow))
        XCTAssertEqual(plan.firstUnscheduled, ScheduleCalendar.scheduled(morning, on: final))
        XCTAssertEqual(ReminderPlan(data: data, now: start).entries.count, 2)
    }

    func testOngoingReminderUsesDailyRepeat() throws {
        let start = try XCTUnwrap(ScheduleCalendar.date("2026-11-10"))
        let med = Medication(name: "Example", dose: "1 tablet", times: [morning], start: "2026-11-10")
        let data = MedicationData(revisions: [.init(medication: med, effective: start)])
        let plan = ReminderPlan(data: data, now: start)
        XCTAssertEqual(plan.entries.first?.repeats, true)
        XCTAssertNil(plan.firstUnscheduled)
    }

    func testNoRetroactiveTrackingAndMissingIsDistinctFromSkipped() throws {
        let start = try XCTUnwrap(ScheduleCalendar.date("2026-11-10"))
        let med = Medication(name: "Example", dose: "1 tablet", times: [morning],
                             start: "2026-11-10")
        let enrolled = start.addingTimeInterval(9 * 3600)
        let data = MedicationData(revisions: [.init(medication: med, effective: enrolled)])
        XCTAssertEqual(data.slots(on: start).first?.tracked, false)
        let tomorrow = ScheduleCalendar.adding(1, to: start)
        let pending = AdherenceReport(data: data, range: 7, now: tomorrow)
        XCTAssertEqual(pending.days.last?.counts, [0, 0, 0, 0])
        let past = AdherenceReport(data: data, range: 7, now: tomorrow.addingTimeInterval(9 * 3600))
        XCTAssertEqual(past.days.last?.counts, [0, 0, 0, 1])
        XCTAssertNil(past.accuracy)
    }
}
