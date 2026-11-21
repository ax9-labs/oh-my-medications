import AlarmKit
import AppIntents
import SwiftUI
import UIKit

struct OpenAlarmIntent: LiveActivityIntent {
    static let title: LocalizedStringResource = "Open app"
    static var supportedModes: IntentModes { .foreground }

    @Parameter(title: "Alarm identifier") var alarmID: String

    init() {}
    init(alarmID: String) { self.alarmID = alarmID }

    func perform() async throws -> some IntentResult {
        if let id = UUID(uuidString: alarmID) {
            await Reminders.shared.openAlarm(id: id)
        }
        return .result()
    }
}

struct AlarmView: View {
    @EnvironmentObject private var reminders: Reminders

    var body: some View {
        ScrollView {
            VStack(spacing: 28) {
                Image(systemName: "alarm.fill")
                    .font(.largeTitle)
                    .foregroundStyle(.teal)
                    .accessibilityHidden(true)
                Text("Medication reminder")
                    .font(.title2)
                if let alarm = reminders.activeAlarm {
                    if alarm.isTest {
                        Text("Test reminder")
                            .font(.largeTitle.bold())
                        Text("This is a test alarm. Check Schedule for your medications.")
                            .multilineTextAlignment(.center)
                    } else if !alarm.medications.isEmpty {
                        Text("Scheduled medications")
                            .font(.headline)
                        ForEach(alarm.medications) { medication in
                            VStack(alignment: .leading, spacing: 12) {
                                MedicationLabel(medication: medication)
                                if !medication.notes.isEmpty {
                                    Text(verbatim: medication.notes)
                                        .foregroundStyle(.secondary)
                                }
                            }
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .padding()
                            .background(.quaternary, in: .rect(cornerRadius: 16))
                        }
                    } else {
                        Text(verbatim: alarm.title)
                            .font(.largeTitle.bold())
                            .multilineTextAlignment(.center)
                    }
                }
                Button {
                    reminders.stopActiveAlarm()
                } label: {
                    Label("Stop", systemImage: "stop.circle.fill")
                        .font(.title2.bold())
                        .frame(maxWidth: .infinity, minHeight: 64)
                }
                .buttonStyle(.borderedProminent)
                .tint(.teal)
                Text("Stopping an alarm only silences it. Record the dose in Schedule.")
                    .font(.body)
                    .foregroundStyle(.secondary)
                    .multilineTextAlignment(.center)
                if reminders.stoppingFailed {
                    Text("The alarm could not be stopped. Try again.")
                        .foregroundStyle(.red)
                }
            }
            .padding(32)
            .frame(maxWidth: .infinity)
        }
        .background(Color(uiColor: .systemBackground))
        .interactiveDismissDisabled()
    }
}

// Present above the current sheet too, including Settings during an alarm test.
struct AlarmPresenter: UIViewControllerRepresentable {
    @ObservedObject var reminders: Reminders
    var language: String
    var isActive: Bool

    func makeUIViewController(context: Context) -> PresenterController {
        PresenterController()
    }

    func updateUIViewController(_ controller: PresenterController, context: Context) {
        controller.onAppear = { [weak controller] in
            guard let controller else { return }
            updatePresentation(controller, coordinator: context.coordinator)
        }
        updatePresentation(controller, coordinator: context.coordinator)
    }

    private func updatePresentation(_ controller: PresenterController, coordinator: Coordinator) {
        coordinator.host?.rootView = AnyView(alarmContent)
        guard isActive else { return }
        if reminders.activeAlarm != nil {
            guard coordinator.host == nil, controller.viewIfLoaded?.window != nil else { return }
            var top = controller.view.window?.rootViewController
            while let presented = top?.presentedViewController { top = presented }
            guard let top else { return }
            if let transition = top.transitionCoordinator {
                transition.animate(alongsideTransition: nil) { [weak controller] _ in
                    controller?.onAppear?()
                }
                return
            }
            guard !top.isBeingDismissed else { return }
            let host = UIHostingController(rootView: AnyView(alarmContent))
            host.modalPresentationStyle = .fullScreen
            host.isModalInPresentation = true
            coordinator.host = host
            top.present(host, animated: true)
        } else if let host = coordinator.host {
            coordinator.host = nil
            host.dismiss(animated: true)
        }
    }

    private var alarmContent: some View {
        AlarmView()
            .environmentObject(reminders)
            .environment(\.locale, Locale(identifier: language))
    }

    func makeCoordinator() -> Coordinator { Coordinator() }

    final class Coordinator {
        var host: UIHostingController<AnyView>?
    }

    final class PresenterController: UIViewController {
        var onAppear: (() -> Void)?

        override func viewDidAppear(_ animated: Bool) {
            super.viewDidAppear(animated)
            onAppear?()
        }
    }
}
