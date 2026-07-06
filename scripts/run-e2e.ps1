param(
    [string]$AvdName = "AttentionPetApi35",
    [string]$AndroidHome = $(if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { "D:\Android\Sdk" }),
    [switch]$SkipEmulatorStart
)

$ErrorActionPreference = "Stop"
$env:ANDROID_HOME = $AndroidHome

$Emulator = Join-Path $AndroidHome "emulator\emulator.exe"
$Adb = Join-Path $AndroidHome "platform-tools\adb.exe"
$AvdManager = Join-Path $AndroidHome "cmdline-tools\latest\bin\avdmanager.bat"
$SystemImage = "system-images;android-35;google_apis;x86_64"

if (-not (Test-Path $Emulator)) { throw "Missing emulator.exe at $Emulator" }
if (-not (Test-Path $Adb)) { throw "Missing adb.exe at $Adb" }
if (-not (Test-Path $AvdManager)) { throw "Missing avdmanager.bat at $AvdManager" }

$existingAvds = & $Emulator -list-avds
if ($existingAvds -notcontains $AvdName) {
    "no" | & $AvdManager create avd -n $AvdName -k $SystemImage -d pixel_6 --force
}

$devices = & $Adb devices
$hasOnlineEmulator = $devices -match "emulator-\d+\s+device"
if (-not $SkipEmulatorStart -and -not $hasOnlineEmulator) {
    Start-Process `
        -FilePath $Emulator `
        -ArgumentList @("-avd", $AvdName, "-no-window", "-no-audio", "-no-boot-anim", "-gpu", "swiftshader_indirect", "-netdelay", "none", "-netspeed", "full") `
        -WindowStyle Hidden
    & $Adb wait-for-device
}

$deadline = (Get-Date).AddMinutes(5)
do {
    Start-Sleep -Seconds 3
    $bootCompleted = (& $Adb shell getprop sys.boot_completed 2>$null).Trim()
    if ($bootCompleted -eq "1") { break }
} while ((Get-Date) -lt $deadline)

if ($bootCompleted -ne "1") {
    throw "Emulator did not finish booting within 5 minutes."
}

& .\gradlew.bat "-Pandroid.testInstrumentationRunnerArguments.class=com.attentionpet.e2e.AttentionPetOverlayE2eTest" :app:connectedDebugAndroidTest
