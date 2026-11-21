import SwiftUI
import UIKit

enum AppLanguage {
    static let defaultCode = Locale.preferredLanguages.first?.hasPrefix("es") == true ? "es" : "en"
}

@main
struct MedicationApp: App {
    @Environment(\.scenePhase) private var scenePhase
    @StateObject private var store = MedicationStore()
    @StateObject private var reminders = Reminders()
    @AppStorage("language") private var language = AppLanguage.defaultCode

    var body: some Scene {
        WindowGroup {
            TabView {
                ScheduleView()
                    .tabItem { Label("Schedule", systemImage: "calendar") }
                MedicationsView()
                    .tabItem { Label("Medications", systemImage: "pills") }
                DashboardView()
                    .tabItem { Label("Dashboard", systemImage: "chart.bar.xaxis") }
                HistoryView()
                    .tabItem { Label("History", systemImage: "clock.arrow.circlepath") }
            }
            .tint(.teal)
            .environment(\.locale, Locale(identifier: language))
            .environmentObject(store)
            .environmentObject(reminders)
            .task { refreshReminders() }
            .onChange(of: scenePhase) { _, phase in
                if phase == .active {
                    store.load()
                    refreshReminders()
                }
            }
            .onChange(of: language) { _, _ in refreshReminders() }
            .onReceive(store.$data) { data in
                if store.loaded { reminders.refresh(data, language: language) }
            }
            .onReceive(NotificationCenter.default.publisher(for: UIApplication.significantTimeChangeNotification)) { _ in
                refreshReminders()
            }
            .alert("Medication records", isPresented: Binding(
                get: { store.error != nil },
                set: { if !$0 { store.error = nil } }
            )) {
                Button("OK", role: .cancel) { store.error = nil }
            } message: {
                Text(translate(store.error ?? "", language))
            }
        }
    }

    private func refreshReminders() {
        if store.loaded { reminders.refresh(store.data, language: language) }
    }
}

struct SettingsButton: View {
    @State private var showing = false

    var body: some View {
        Button { showing = true } label: {
            Label("Settings", systemImage: "gearshape")
        }
        .sheet(isPresented: $showing) { SettingsView() }
    }
}

struct SettingsView: View {
    @Environment(\.dismiss) private var dismiss
    @Environment(\.openURL) private var openURL
    @EnvironmentObject private var store: MedicationStore
    @EnvironmentObject private var reminders: Reminders
    @AppStorage("language") private var language = AppLanguage.defaultCode

    var body: some View {
        NavigationStack {
            Form {
                Picker("Language", selection: $language) {
                    Text(verbatim: "English").tag("en")
                    Text(verbatim: "Español").tag("es")
                }
                Section {
                    if reminders.authorized {
                        Button("Start a test alarm") {
                            Task { await reminders.test(language: language) }
                        }
                        if let date = reminders.firstUnscheduled {
                            LabeledContent("First alarm outside the queue") {
                                Text(date, format: .dateTime.month().day().hour().minute())
                            }
                            Text("Open the app regularly to schedule more doses.")
                                .foregroundStyle(.secondary)
                        }
                    } else {
                        Button("Allow alarms") {
                            Task {
                                await reminders.authorize()
                                if store.loaded { reminders.refresh(store.data, language: language) }
                            }
                        }
                    }
                    Button("Open iOS Settings") {
                        if let url = URL(string: UIApplication.openSettingsURLString) { openURL(url) }
                    }
                    if reminders.schedulingFailed {
                        Text("Some alarms could not be scheduled. Check alarm access and try again.")
                            .foregroundStyle(.secondary)
                    }
                } header: {
                    Text("Reminders")
                } footer: {
                    Text("Stopping an alarm only silences it. Record the dose in Schedule.")
                }
            }
            .navigationTitle("Settings")
            .toolbar {
                ToolbarItem(placement: .confirmationAction) { Button("Done") { dismiss() } }
            }
        }
    }
}

extension MedicationColor {
    var tint: Color {
        switch self {
        case .standard: .teal
        case .red: .red
        case .orange: .orange
        case .yellow: .yellow
        case .green: .green
        case .teal: .cyan
        case .blue: .blue
        case .purple: .purple
        case .pink: .pink
        }
    }
}

struct MedicationLabel: View {
    var medication: Medication

    var body: some View {
        Label {
            VStack(alignment: .leading) {
                Text(medication.name).font(.headline)
                Text(medication.dose).font(.subheadline).foregroundStyle(.secondary)
            }
        } icon: {
            Image(systemName: medication.icon.rawValue)
                .font(.title3)
                .foregroundStyle(medication.color.tint)
                .frame(width: 36, height: 36)
                .background(medication.color.tint.opacity(0.12), in: .rect(cornerRadius: 10))
                .accessibilityHidden(true)
        }
    }
}
