#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
TOOLS="${MED_TRACKER_TOOLS:-../android-tools}"
TOOLS="$(cd "$TOOLS" && pwd)"
BT="$TOOLS/build-tools/android-15"
ANDROID_JAR="$TOOLS/platform/android-35/android.jar"
mkdir -p build/instrumentation/classes build/instrumentation/dex
"$BT/aapt2" link -o build/instrumentation/base.apk -I "$ANDROID_JAR" --manifest tests/android/AndroidManifest.xml
java -jar "$TOOLS/ecj.jar" -8 -warn:none -bootclasspath "$ANDROID_JAR:$BT/core-lambda-stubs.jar" -classpath build/classes -d build/instrumentation/classes tests/android/AppInstrumentation.java
python3 - <<'PY'
import pathlib,zipfile
with zipfile.ZipFile('build/instrumentation/classes.jar','w',zipfile.ZIP_DEFLATED) as z:
    for p in pathlib.Path('build/instrumentation/classes').rglob('*.class'): z.write(p,p.relative_to('build/instrumentation/classes'))
PY
java -cp "$BT/lib/d8.jar" com.android.tools.r8.D8 --min-api 26 --lib "$ANDROID_JAR" --classpath build/classes.jar --output build/instrumentation/dex build/instrumentation/classes.jar
cp build/instrumentation/base.apk build/instrumentation/unaligned.apk
python3 - <<'PY'
import zipfile
with zipfile.ZipFile('build/instrumentation/unaligned.apk','a',zipfile.ZIP_DEFLATED) as z:z.write('build/instrumentation/dex/classes.dex','classes.dex')
PY
"$BT/zipalign" -f 4 build/instrumentation/unaligned.apk build/instrumentation/aligned.apk
"$BT/apksigner" sign --ks .local/med-tracker.keystore --ks-key-alias medtracker --ks-pass pass:android --out build/instrumentation/tests.apk build/instrumentation/aligned.apk
