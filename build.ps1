param(
    [string]$AndroidSdk = "$env:LOCALAPPDATA\Android\Sdk",
    [switch]$AndroidTests
)

$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
$buildTools = Join-Path $AndroidSdk 'build-tools\35.0.0'
$androidJar = Join-Path $AndroidSdk 'platforms\android-35\android.jar'
if (!(Test-Path $androidJar) -or !(Test-Path "$buildTools\aapt2.exe")) {
    throw 'Install Android SDK Platform 35 and Build Tools 35.0.0, or pass -AndroidSdk with their SDK directory.'
}

function Invoke-Checked {
    param([string]$Command, [string[]]$Arguments)
    & $Command @Arguments
    if ($LASTEXITCODE -ne 0) { throw "$Command failed with exit code $LASTEXITCODE" }
}

function Add-Dex([string]$Apk, [string]$DexDirectory) {
    $archive = [System.IO.Compression.ZipFile]::Open($Apk, 'Update')
    try {
        Get-ChildItem $DexDirectory -Filter '*.dex' | ForEach-Object {
            [System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile($archive, $_.FullName, $_.Name) | Out-Null
        }
    } finally { $archive.Dispose() }
}

Add-Type -AssemblyName System.IO.Compression.FileSystem
New-Item -ItemType Directory -Force build, dist, .local | Out-Null
# These are disposable compiler outputs within this repository's build directory.
foreach ($name in @('classes', 'dex', 'generated', 'tests', 'instrumentation')) {
    $target = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot "build\$name"))
    $buildRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot 'build')) + [System.IO.Path]::DirectorySeparatorChar
    if (!$target.StartsWith($buildRoot, [System.StringComparison]::OrdinalIgnoreCase)) { throw 'Invalid build output path' }
    if (Test-Path -LiteralPath $target) { Remove-Item -LiteralPath $target -Recurse -Force }
    New-Item -ItemType Directory -Path $target | Out-Null
}

Invoke-Checked py @('-3', 'tools/check_translations.py')
$sourceRoot = 'app/src/main/java/com/personal/medtracker'
Invoke-Checked javac @('--release', '8', '-Xlint:-options', '-d', 'build/tests', "$sourceRoot/Schedule.java", "$sourceRoot/Treatment.java", "$sourceRoot/Adherence.java", 'tests/ScheduleTest.java', 'tests/TreatmentTest.java')
Invoke-Checked java @('-cp', 'build/tests', 'ScheduleTest')
Invoke-Checked java @('-cp', 'build/tests', 'TreatmentTest')

Invoke-Checked "$buildTools\aapt2.exe" @('compile', '--dir', 'app/src/main/res', '-o', 'build/resources.zip')
Invoke-Checked "$buildTools\aapt2.exe" @('link', '-o', 'build/base.apk', '-I', $androidJar, '--manifest', 'app/src/main/AndroidManifest.xml', '--java', 'build/generated', 'build/resources.zip')
$sources = Get-ChildItem app/src/main/java, build/generated -Recurse -Filter '*.java' | Sort-Object FullName | ForEach-Object {
    '"' + $_.FullName.Replace('\', '/') + '"'
}
[System.IO.File]::WriteAllLines((Join-Path $PSScriptRoot 'build/sources.txt'), [string[]]$sources, [System.Text.UTF8Encoding]::new($false))
$bootClasspath = "$androidJar;$buildTools\core-lambda-stubs.jar"
Invoke-Checked javac @('-source', '8', '-target', '8', '-Xlint:-options', '-encoding', 'UTF-8', '-bootclasspath', $bootClasspath, '-d', 'build/classes', '@build/sources.txt')
Invoke-Checked jar @('--create', '--file', 'build/classes.jar', '-C', 'build/classes', '.')
Invoke-Checked java @('-cp', "$buildTools\lib\d8.jar", 'com.android.tools.r8.D8', '--min-api', '26', '--lib', $androidJar, '--output', 'build/dex', 'build/classes.jar')
Copy-Item build/base.apk build/unaligned.apk -Force
Add-Dex (Join-Path $PSScriptRoot 'build/unaligned.apk') (Join-Path $PSScriptRoot 'build/dex')
Invoke-Checked "$buildTools\zipalign.exe" @('-f', '4', 'build/unaligned.apk', 'build/aligned.apk')
if (!(Test-Path .local/med-tracker.keystore)) {
    Invoke-Checked keytool @('-genkeypair', '-keystore', '.local/med-tracker.keystore', '-storepass', 'android', '-keypass', 'android', '-alias', 'medtracker', '-keyalg', 'RSA', '-keysize', '3072', '-validity', '10000', '-dname', 'CN=Med Tracker Personal Development')
}
$signArguments = @('sign', '--ks', '.local/med-tracker.keystore', '--ks-key-alias', 'medtracker', '--ks-pass', 'pass:android', '--key-pass', 'pass:android')
Invoke-Checked "$buildTools\apksigner.bat" ($signArguments + @('--out', 'dist/med-tracker.apk', 'build/aligned.apk'))
Invoke-Checked "$buildTools\apksigner.bat" @('verify', '--verbose', 'dist/med-tracker.apk')
$hash = (Get-FileHash dist/med-tracker.apk -Algorithm SHA256).Hash.ToLowerInvariant()
"$hash  dist/med-tracker.apk" | Set-Content dist/med-tracker.apk.sha256 -Encoding ascii

if ($AndroidTests) {
    New-Item -ItemType Directory -Force build/instrumentation/classes, build/instrumentation/dex | Out-Null
    Invoke-Checked "$buildTools\aapt2.exe" @('link', '-o', 'build/instrumentation/base.apk', '-I', $androidJar, '--manifest', 'tests/android/AndroidManifest.xml')
    Invoke-Checked javac @('-source', '8', '-target', '8', '-Xlint:-options', '-encoding', 'UTF-8', '-bootclasspath', $bootClasspath, '-classpath', 'build/classes', '-d', 'build/instrumentation/classes', 'tests/android/AppInstrumentation.java')
    Invoke-Checked jar @('--create', '--file', 'build/instrumentation/classes.jar', '-C', 'build/instrumentation/classes', '.')
    Invoke-Checked java @('-cp', "$buildTools\lib\d8.jar", 'com.android.tools.r8.D8', '--min-api', '26', '--lib', $androidJar, '--classpath', 'build/classes.jar', '--output', 'build/instrumentation/dex', 'build/instrumentation/classes.jar')
    Copy-Item build/instrumentation/base.apk build/instrumentation/unaligned.apk -Force
    Add-Dex (Join-Path $PSScriptRoot 'build/instrumentation/unaligned.apk') (Join-Path $PSScriptRoot 'build/instrumentation/dex')
    Invoke-Checked "$buildTools\zipalign.exe" @('-f', '4', 'build/instrumentation/unaligned.apk', 'build/instrumentation/aligned.apk')
    Invoke-Checked "$buildTools\apksigner.bat" ($signArguments + @('--out', 'build/instrumentation/tests.apk', 'build/instrumentation/aligned.apk'))
}
Write-Host 'APK ready: dist/med-tracker.apk'
