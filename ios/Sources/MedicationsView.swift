import SwiftUI

struct MedicationsView: View {
    @EnvironmentObject private var store: MedicationStore
    @State private var editing: Medication?
    @State private var archiving: Medication?

    var body: some View {
        NavigationStack {
            List {
                ForEach(store.data.currentMedications) { medication in
                    Button {
                        editing = medication
                    } label: {
                        VStack(alignment: .leading, spacing: 8) {
                            MedicationLabel(medication: medication)
                            Text(medication.times.map(\.label).joined(separator: " · "))
                                .font(.subheadline).foregroundStyle(.secondary)
                            if medication.duration > 0 {
                                let day = medication.courseDay(on: .now)
                                ProgressView(value: Double(day), total: Double(medication.duration)) {
                                    Text("Day \(day) of \(medication.duration)")
                                }
                            }
                        }
                    }
                    .buttonStyle(.plain)
                    .swipeActions {
                        Button(role: .destructive) {
                            archiving = medication
                        } label: { Label("Archive", systemImage: "archivebox") }
                    }
                    .contextMenu {
                        Button(role: .destructive) {
                            archiving = medication
                        } label: { Label("Archive", systemImage: "archivebox") }
                    }
                }
            }
            .overlay {
                if store.data.currentMedications.isEmpty {
                    ContentUnavailableView("No medications", systemImage: "pills")
                }
            }
            .navigationTitle("Medications")
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button {
                        editing = Medication()
                    } label: { Label("Add medication", systemImage: "plus") }
                    .disabled(!store.loaded)
                }
            }
            .sheet(item: $editing) { medication in MedicationEditor(medication: medication) }
            .confirmationDialog("Archive medication?", isPresented: Binding(
                get: { archiving != nil }, set: { if !$0 { archiving = nil } }
            ), titleVisibility: .visible) {
                Button("Archive", role: .destructive) {
                    if let medication = archiving { store.archive(medication) }
                    archiving = nil
                }
            } message: {
                Text("Past dose records will be kept.")
            }
        }
    }
}

private struct MedicationEditor: View {
    @Environment(\.dismiss) private var dismiss
    @EnvironmentObject private var store: MedicationStore
    @EnvironmentObject private var reminders: Reminders
    @AppStorage("language") private var language = AppLanguage.defaultCode
    @State private var medication: Medication
    @State private var limited: Bool
    @State private var durationText: String
    @State private var addingTime = false
    @State private var proposedTime = Date.now

    init(medication: Medication) {
        _medication = State(initialValue: medication)
        _limited = State(initialValue: medication.duration > 0)
        _durationText = State(initialValue: String(medication.duration > 0 ? medication.duration : 7))
    }

    private var duration: Int? {
        guard limited else { return 0 }
        guard let days = Int(durationText), (1...3650).contains(days) else { return nil }
        return days
    }

    private var valid: Bool {
        !medication.name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty &&
        !medication.dose.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty && !medication.times.isEmpty
    }

    var body: some View {
        NavigationStack {
            Form {
                Section("Medication") {
                    TextField("Name", text: $medication.name)
                    TextField("Prescribed dose", text: $medication.dose)
                    TextField("Instructions", text: $medication.notes, axis: .vertical)
                }
                Section("Daily times") {
                    ForEach(medication.times) { time in
                        Text(verbatim: time.label)
                    }
                    .onDelete { medication.times.remove(atOffsets: $0) }
                    Button("Add time") { addingTime = true }
                        .disabled(medication.times.count >= 12)
                    Toggle("Remind me at these times", isOn: $medication.reminders)
                }
                Section("Treatment course") {
                    DatePicker("Start date", selection: Binding(
                        get: { ScheduleCalendar.date(medication.start) ?? .now },
                        set: { medication.start = ScheduleCalendar.day($0) }
                    ), displayedComponents: .date)
                    Toggle("Set a treatment length", isOn: $limited)
                    if limited {
                        TextField("Number of days", text: $durationText)
                            .keyboardType(.numberPad)
                        if duration == nil {
                            Text("Choose between 1 and 3650 days.").foregroundStyle(.red)
                        }
                    }
                }
                Section("Appearance") {
                    Picker("Icon", selection: $medication.icon) {
                        ForEach(MedicationIcon.allCases) { icon in
                            Label(LocalizedStringKey(icon.rawValue), systemImage: icon.rawValue).tag(icon)
                        }
                    }
                    Picker("Color", selection: $medication.color) {
                        ForEach(MedicationColor.allCases) { color in
                            Label(LocalizedStringKey(color.rawValue), systemImage: "circle.fill")
                                .foregroundStyle(color.tint).tag(color)
                        }
                    }
                }
            }
            .navigationTitle("Medication")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Cancel") { dismiss() } }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") {
                        guard let duration else { return }
                        medication.duration = duration
                        medication.name = medication.name.trimmingCharacters(in: .whitespacesAndNewlines)
                        medication.dose = medication.dose.trimmingCharacters(in: .whitespacesAndNewlines)
                        if store.save(medication) {
                            dismiss()
                            if medication.reminders && !reminders.authorized {
                                Task {
                                    await reminders.authorize()
                                    reminders.refresh(store.data, language: language)
                                }
                            }
                        }
                    }
                    .disabled(!valid || duration == nil)
                }
            }
            .sheet(isPresented: $addingTime) {
                NavigationStack {
                    Form {
                        DatePicker("Scheduled time", selection: $proposedTime, displayedComponents: .hourAndMinute)
                        if medication.times.contains(DoseTime(proposedTime)) {
                            Text("This time is already scheduled.").foregroundStyle(.secondary)
                        }
                    }
                    .navigationTitle("Add time")
                    .navigationBarTitleDisplayMode(.inline)
                    .toolbar {
                        ToolbarItem(placement: .cancellationAction) { Button("Cancel") { addingTime = false } }
                        ToolbarItem(placement: .confirmationAction) {
                            Button("Add") {
                                medication.times.append(DoseTime(proposedTime))
                                medication.times.sort()
                                addingTime = false
                            }
                            .disabled(medication.times.contains(DoseTime(proposedTime)))
                        }
                    }
                    .presentationDetents([.medium])
                }
            }
        }
    }
}
