#!/usr/bin/env python3
"""Download pinned Linux Android build tools into ../android-tools (Java runtime required)."""
import hashlib
import pathlib
import subprocess
import zipfile

root = pathlib.Path(__file__).resolve().parent.parent / 'android-tools'
root.mkdir(exist_ok=True)
artifacts = [
    ('https://dl.google.com/android/repository/platform-35_r02.zip', 'platform.zip', 'platform', 'sha1', '0bb560a90a7a2cbd0dd8348224d518b638fe7949'),
    ('https://dl.google.com/android/repository/build-tools_r35_linux.zip', 'build-tools.zip', 'build-tools', 'sha1', '2cfaa0bbb2336e9ec18ed3ecea84fa2e2af607bc'),
    ('https://repo.maven.apache.org/maven2/org/eclipse/jdt/ecj/3.39.0/ecj-3.39.0.jar', 'ecj.jar', None, 'sha256', '01f5a92ac19bb2b3bf85e295a68f2c73c264369109158b566ce9b490af982948'),
]
for url, name, target, algorithm, checksum in artifacts:
    file = root / name
    if not file.exists(): subprocess.run(['curl', '-fL', '--retry', '2', url, '-o', str(file)], check=True)
    actual = hashlib.new(algorithm, file.read_bytes()).hexdigest()
    if actual != checksum: raise SystemExit('Checksum mismatch: ' + name)
    if target:
        with zipfile.ZipFile(file) as archive:
            for item in archive.infolist():
                path = pathlib.Path(archive.extract(item, root / target))
                permissions = item.external_attr >> 16
                if permissions: path.chmod(permissions)
print('Build tools ready. Run ./test.sh && ./build.sh')
