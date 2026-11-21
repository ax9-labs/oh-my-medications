// swift-tools-version: 6.0

import PackageDescription

let package = Package(
    name: "MedicationCore",
    products: [.library(name: "MedicationCore", targets: ["MedicationCore"])],
    targets: [
        .target(
            name: "MedicationCore",
            path: "Sources",
            exclude: ["MedicationApp.swift", "MedicationStore.swift", "MedicationsView.swift",
                      "Reminders.swift", "ReportsView.swift", "ScheduleView.swift"],
            sources: ["MedicationData.swift", "ReminderPlan.swift"]
        ),
        .testTarget(name: "MedicationCoreTests", dependencies: ["MedicationCore"], path: "Tests")
    ]
)
