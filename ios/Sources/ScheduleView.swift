import SwiftUI

struct ScheduleView: View {
    @EnvironmentObject private var store: MedicationStore
    @State private var day = Date.now
    @State private var editing: DoseSlot?

    var body: some View {
        NavigationStack {
            TimelineView(.periodic(from: .now, by: 30)) { context in
                let slots = store.data.slots(on: day)
                List {
                    if let next = store.data.nextDose(after: context.date) {
                        Section("Next dose") {
                            Button {
                                day = next.scheduled
                            } label: {
                                VStack(alignment: .leading, spacing: 6) {
                                    MedicationLabel(medication: next.medication)
                                    Text(next.scheduled, format: .dateTime.weekday().month().day().hour().minute())
                                        .font(.subheadline)
                                }
                            }
                            .buttonStyle(.plain)
                        }
                    }
                    Section {
                        HStack {
                            Button {
                                day = ScheduleCalendar.adding(-1, to: day)
                            } label: {
                                Label("Previous day", systemImage: "chevron.left").labelStyle(.iconOnly)
                            }
                            Spacer()
                            DatePicker("Schedule date", selection: $day, displayedComponents: .date)
                                .labelsHidden()
                                .accessibilityLabel("Schedule date")
                            Spacer()
                            Button {
                                day = ScheduleCalendar.adding(1, to: day)
                            } label: {
                                Label("Next day", systemImage: "chevron.right").labelStyle(.iconOnly)
                            }
                        }
                        .buttonStyle(.borderless)
                        if slots.isEmpty {
                            Text("No doses scheduled.").foregroundStyle(.secondary)
                        }
                        if ScheduleCalendar.local.isDate(day, inSameDayAs: context.date) {
                            ForEach(slots.filter { $0.scheduled <= context.date }) { slot in
                                doseRow(slot, now: context.date)
                            }
                            HStack(spacing: 12) {
                                Image(systemName: "clock.fill")
                                    .accessibilityHidden(true)
                                VStack(alignment: .leading, spacing: 4) {
                                    Text("Now")
                                        .font(.headline)
                                    Text(timeOfDay(at: context.date))
                                        .font(.subheadline)
                                }
                                Spacer()
                                Text(context.date, format: .dateTime.hour().minute())
                                    .font(.headline)
                                    .monospacedDigit()
                            }
                            .foregroundStyle(.teal)
                            .padding(.vertical, 8)
                            .listRowBackground(Color.teal.opacity(0.12))
                            .accessibilityElement(children: .combine)
                            ForEach(slots.filter { $0.scheduled > context.date }) { slot in
                                doseRow(slot, now: context.date)
                            }
                        } else {
                            ForEach(slots) { slot in
                                doseRow(slot, now: context.date)
                            }
                        }
                    }
                }
            }
            .navigationTitle("Schedule")
            .toolbar {
                ToolbarItem(placement: .topBarLeading) { Button("Today") { day = .now } }
                ToolbarItem(placement: .topBarTrailing) { SettingsButton() }
            }
            .sheet(item: $editing) { slot in
                DoseTimeEditor(initial: min(slot.scheduled, .now)) {
                    store.log(slot, status: .taken, actual: $0)
                }
            }
        }
    }

    private func timeOfDay(at date: Date) -> LocalizedStringKey {
        switch ScheduleCalendar.local.component(.hour, from: date) {
        case 5..<12: "Morning"
        case 12..<17: "Afternoon"
        case 17..<21: "Evening"
        default: "Night"
        }
    }

    private func doseRow(_ slot: DoseSlot, now: Date) -> some View {
        let taken = store.data.events[slot.id]?.status == .taken
        return VStack(alignment: .leading, spacing: 8) {
            LabeledContent {
                Text(verbatim: slot.time.label).monospacedDigit()
            } label: {
                MedicationLabel(medication: slot.medication)
            }
            if !slot.medication.notes.isEmpty {
                Text(slot.medication.notes).foregroundStyle(.secondary).font(.subheadline)
            }
            if let event = store.data.events[slot.id] {
                HStack {
                    Label(LocalizedStringKey(event.status == .taken ? "Taken" : "Skipped"),
                          systemImage: event.status == .taken ? "checkmark.circle.fill" : "forward.end.circle")
                        .foregroundStyle(event.status == .taken ? Color.green : Color.secondary)
                    Spacer()
                    Button { store.undo(event) } label: {
                        Label("Undo", systemImage: "arrow.uturn.backward.circle")
                            .labelStyle(.iconOnly)
                            .font(.title2)
                            .frame(minWidth: 44, minHeight: 44)
                    }
                }
                .buttonStyle(.borderless)
            } else {
                HStack(spacing: 20) {
                    doseActions(slot, now: now)
                }
                .labelStyle(.iconOnly)
                .font(.title2)
                .buttonStyle(.borderless)
            }
        }
        .padding(.vertical, 4)
        .listRowBackground(taken ? Color.green.opacity(0.12) : Color(uiColor: .secondarySystemGroupedBackground))
    }

    @ViewBuilder
    private func doseActions(_ slot: DoseSlot, now: Date) -> some View {
        Button {
            _ = store.log(slot, status: .taken)
        } label: {
            Label("Log as taken", systemImage: "checkmark.circle.fill")
                .frame(minWidth: 44, minHeight: 44)
        }
        .disabled(!slot.canTake(at: now))
        Button {
            editing = slot
        } label: {
            Label("Taken at…", systemImage: "clock.badge.checkmark")
                .frame(minWidth: 44, minHeight: 44)
        }
        .disabled(!slot.canTake(at: now))
        Button {
            _ = store.log(slot, status: .skipped)
        } label: {
            Label("Skip", systemImage: "forward.end.circle")
                .frame(minWidth: 44, minHeight: 44)
        }
        .disabled(!slot.canSkip(at: now))
    }
}

struct DoseTimeEditor: View {
    @Environment(\.dismiss) private var dismiss
    @State private var date: Date
    var save: (Date) -> Bool

    init(initial: Date, save: @escaping (Date) -> Bool) {
        _date = State(initialValue: initial)
        self.save = save
    }

    var body: some View {
        NavigationStack {
            Form {
                DatePicker("Actual taken time", selection: $date, in: ...Date.now)
            }
            .navigationTitle("Actual taken time")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Cancel") { dismiss() } }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") { if save(date) { dismiss() } }
                }
            }
        }
    }
}
