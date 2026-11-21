import Charts
import SwiftUI

struct DashboardView: View {
    @EnvironmentObject private var store: MedicationStore
    @State private var range = 7
    @State private var selectedDay: Date?

    private let categories: [(LocalizedStringKey, Color)] = [
        ("On time", .teal), ("Outside window", .orange), ("Skipped", .purple), ("Not logged", .gray)
    ]

    var body: some View {
        NavigationStack {
            TimelineView(.periodic(from: .now, by: 60)) { context in
                let report = AdherenceReport(data: store.data, range: range, now: context.date)
                List {
                    Picker("Reporting period", selection: $range) {
                        Text("7 days").tag(7)
                        Text("30 days").tag(30)
                    }
                    .pickerStyle(.segmented)
                    .onChange(of: range) { _, _ in selectedDay = nil }
                    Section {
                        LabeledContent("On-time accuracy") {
                            if let accuracy = report.accuracy {
                                Text(Double(accuracy) / 100, format: .percent.precision(.fractionLength(0)))
                            } else {
                                Text(verbatim: "—")
                            }
                        }
                        LabeledContent("Average timing difference") {
                            if let minutes = report.averageMinutes {
                                Text("\(minutes) min")
                            } else {
                                Text(verbatim: "—")
                            }
                        }
                        Text("Doses are on time within 30 minutes of the scheduled time.")
                            .font(.footnote).foregroundStyle(.secondary)
                    }
                    Section("Daily doses") {
                        Chart {
                            ForEach(report.days) { day in
                                ForEach(categories.indices, id: \.self) { index in
                                    BarMark(x: .value("Date", day.date, unit: .day),
                                            y: .value("Doses", day.counts[index]))
                                        .foregroundStyle(categories[index].1)
                                        .accessibilityLabel(Text(categories[index].0))
                                        .accessibilityValue(Text(day.counts[index], format: .number))
                                }
                            }
                        }
                        .chartXSelection(value: $selectedDay)
                        .frame(height: 190)
                        if let day = report.days.first(where: {
                            guard let selectedDay else { return false }
                            return ScheduleCalendar.local.isDate($0.date, inSameDayAs: selectedDay)
                        }) {
                            Text(day.date, format: .dateTime.weekday().month().day())
                                .font(.headline)
                            counts(day.counts)
                        } else {
                            counts(categories.indices.map { index in
                                report.days.reduce(0) { $0 + $1.counts[index] }
                            })
                        }
                    }
                }
            }
            .navigationTitle("Dashboard")
        }
    }

    private func counts(_ values: [Int]) -> some View {
        ForEach(categories.indices, id: \.self) { index in
            LabeledContent {
                Text(values[index], format: .number)
            } label: {
                Label {
                    Text(categories[index].0)
                } icon: {
                    Image(systemName: "circle.fill").foregroundStyle(categories[index].1)
                }
            }
        }
    }
}

struct HistoryView: View {
    @EnvironmentObject private var store: MedicationStore
    @AppStorage("language") private var language = AppLanguage.defaultCode
    @State private var editing: DoseEvent?
    @State private var exporting = false
    @State private var document: HistoryCSV?
    @State private var exportFailed = false

    var body: some View {
        NavigationStack {
            List(store.data.history) { event in
                VStack(alignment: .leading, spacing: 8) {
                    MedicationLabel(medication: event.slot.medication)
                    Text(LocalizedStringKey(event.status == .taken ? "Taken" : "Skipped"))
                        .font(.subheadline)
                    LabeledContent("Scheduled at") {
                        Text(event.slot.scheduled, format: .dateTime.year().month().day().hour().minute())
                    }
                    if let taken = event.taken {
                        LabeledContent("Taken at") {
                            Text(taken, format: .dateTime.year().month().day().hour().minute())
                        }
                    }
                    LabeledContent("Recorded at") {
                        Text(event.recorded, format: .dateTime.year().month().day().hour().minute())
                    }
                    HStack {
                        if event.status == .taken {
                            Button("Correct time") { editing = event }
                        }
                        Spacer()
                        Button("Undo") { store.undo(event) }
                    }
                    .buttonStyle(.borderless)
                }
                .padding(.vertical, 4)
            }
            .overlay {
                if store.data.history.isEmpty {
                    ContentUnavailableView("No dose history", systemImage: "clock")
                }
            }
            .navigationTitle("History")
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button {
                        document = HistoryCSV(events: store.data.history, language: language)
                        exporting = true
                    } label: { Label("Export CSV", systemImage: "square.and.arrow.up") }
                    .disabled(!store.loaded)
                }
            }
            .sheet(item: $editing) { event in
                DoseTimeEditor(initial: event.taken ?? event.recorded) {
                    store.correct(event, to: $0)
                }
            }
            .fileExporter(isPresented: $exporting, document: document, contentType: .commaSeparatedText,
                          defaultFilename: "medication-history.csv") { result in
                if case .failure = result { exportFailed = true }
            }
            .alert("Could not export history.", isPresented: $exportFailed) {
                Button("OK", role: .cancel) {}
            }
        }
    }
}
