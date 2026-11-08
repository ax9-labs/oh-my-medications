#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
TOOLS="${MED_TRACKER_TOOLS:-../android-tools}"
python3 tools/check_translations.py
mkdir -p build/tests
java -jar "$TOOLS/ecj.jar" -8 -warn:none -d build/tests app/src/main/java/com/personal/medtracker/Schedule.java app/src/main/java/com/personal/medtracker/Treatment.java app/src/main/java/com/personal/medtracker/Adherence.java tests/ScheduleTest.java tests/TreatmentTest.java
java -cp build/tests ScheduleTest
java -cp build/tests TreatmentTest
