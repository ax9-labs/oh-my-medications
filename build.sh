#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
TOOLS="${MED_TRACKER_TOOLS:-../android-tools}"
TOOLS="$(cd "$TOOLS" && pwd)"
BT="${ANDROID_BUILD_TOOLS:-$TOOLS/build-tools/android-15}"
ANDROID_JAR="${ANDROID_JAR:-$TOOLS/platform/android-35/android.jar}"
mkdir -p build/classes build/dex build/generated dist .local
rm -rf build/classes/* build/dex/* build/generated/*
"$BT/aapt2" compile --dir app/src/main/res -o build/resources.zip
"$BT/aapt2" link -o build/base.apk -I "$ANDROID_JAR" --manifest app/src/main/AndroidManifest.xml --java build/generated build/resources.zip
find app/src/main/java build/generated -name '*.java' | sort > build/sources.txt
java -jar "$TOOLS/ecj.jar" -8 -warn:none -bootclasspath "$ANDROID_JAR:$BT/core-lambda-stubs.jar" -d build/classes @build/sources.txt
python3 - <<'PY'
import pathlib, zipfile
with zipfile.ZipFile('build/classes.jar','w',zipfile.ZIP_DEFLATED) as z:
    for p in pathlib.Path('build/classes').rglob('*.class'):
        z.write(p,p.relative_to('build/classes'))
PY
java -cp "$BT/lib/d8.jar" com.android.tools.r8.D8 --min-api 26 --lib "$ANDROID_JAR" --output build/dex build/classes.jar
cp build/base.apk build/unaligned.apk
python3 - <<'PY'
import pathlib, zipfile
with zipfile.ZipFile('build/unaligned.apk','a',zipfile.ZIP_DEFLATED) as z:
    for p in pathlib.Path('build/dex').glob('*.dex'): z.write(p,p.name)
PY
"$BT/zipalign" -f 4 build/unaligned.apk build/aligned.apk
if [ ! -f .local/med-tracker.keystore ]; then
    keytool -genkeypair -keystore .local/med-tracker.keystore -storepass android -keypass android -alias medtracker -keyalg RSA -keysize 3072 -validity 10000 -dname 'CN=Med Tracker Personal Development' >/dev/null 2>&1
fi
"$BT/apksigner" sign --ks .local/med-tracker.keystore --ks-key-alias medtracker --ks-pass pass:android --key-pass pass:android --out dist/med-tracker.apk build/aligned.apk
"$BT/apksigner" verify --verbose dist/med-tracker.apk
"$BT/aapt2" dump badging dist/med-tracker.apk | head -8
sha256sum dist/med-tracker.apk > dist/med-tracker.apk.sha256
echo 'APK ready: dist/med-tracker.apk'
