# Attention Pet E2E Tests

## Purpose

The E2E suite is the regression gate for user-visible monitoring behavior. Every new feature should keep this suite passing before handoff.

The current baseline uses Android instrumented tests with UI Automator because Attention Pet must verify behavior across app, system permission state, foreground service, and `TYPE_APPLICATION_OVERLAY` windows.

Official references:

- UI Automator: https://developer.android.com/training/testing/other-components/ui-automator
- Compose UI testing: https://developer.android.com/develop/ui/compose/testing
- AndroidX Test setup: https://developer.android.com/training/testing/instrumented-tests/androidx-test-libraries/test-setup

## Environment

Required local tools:

- Android SDK at `D:\Android\Sdk` or `$env:ANDROID_HOME`
- Android emulator package
- Android 35 Google APIs x86_64 system image
- WHPX or equivalent emulator acceleration

The helper script creates this AVD if it does not exist:

```text
AttentionPetApi35
system-images;android-35;google_apis;x86_64
pixel_6 profile
```

## Run

From `D:\app-bird`:

```powershell
.\scripts\run-e2e.ps1
```

To use an already-running emulator:

```powershell
.\scripts\run-e2e.ps1 -SkipEmulatorStart
```

Raw Gradle command:

```powershell
$env:ANDROID_HOME='D:\Android\Sdk'
.\gradlew.bat "-Pandroid.testInstrumentationRunnerArguments.class=com.attentionpet.e2e.AttentionPetOverlayE2eTest" :app:connectedDebugAndroidTest
```

## Current Baseline Case

`AttentionPetOverlayE2eTest.startsMonitoringAndShowsCapsulePanelOverlay`

Flow:

1. Grants Usage Access, overlay, and notification permission using shell commands.
2. Seeds the Room repository so the configured target package is `com.attentionpet`.
3. Launches `MainActivity`.
4. Taps `开始陪伴`.
5. Waits for the small overlay capsule.
6. Taps the capsule.
7. Waits for the expanded overlay panel.

This verifies the real chain:

```text
Home UI -> repository config -> foreground service -> UsageStats detector ->
SessionTracker -> RuleEvaluator -> WindowManager overlay -> UI Automator assertion
```

## Stable Selectors

The E2E test uses accessibility content descriptions from `AttentionPetTestIds`:

- `attention_pet_start_monitoring`
- `attention_pet_overlay_capsule`
- `attention_pet_overlay_panel`
- `attention_pet_timeout_sheet`

New E2E-visible controls should get stable IDs in the same file rather than relying on localized text.

## Current Limits

- The baseline targets the app itself (`com.attentionpet`) to avoid depending on TikTok/Douyin/Instagram being installed.
- Timeout sheet E2E is not automated yet because the MVP enforces minimum 5-minute limits and the service does not expose a test clock.
- Cross-app target coverage should use a small fixture app module later, for example `com.attentionpet.fixture.shortvideo`.
- Session persistence and extension grant consumption are still next-iteration scope.

## Add A New E2E Case

For every new user-facing monitoring feature:

1. Add or reuse stable IDs in `AttentionPetTestIds`.
2. Write an `androidTest` under `app/src/androidTest/java/com/attentionpet/e2e`.
3. Prefer UI Automator for overlay/system/cross-app behavior.
4. Prefer Compose UI Test for pure in-app Compose behavior.
5. Run `.\scripts\run-e2e.ps1`.
6. Keep unit tests and `:app:assembleDebug` passing too.
