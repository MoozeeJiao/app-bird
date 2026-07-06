# Attention Pet E2E Tests

## Purpose

The E2E suite is the regression gate for user-visible monitoring behavior. Every new feature should keep this suite passing before handoff.

The current baseline uses Android instrumented tests with UI Automator because Attention Pet must verify behavior across app, system permission state, foreground service, a separate target app, and `TYPE_APPLICATION_OVERLAY` windows.

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
.\gradlew.bat :fixture-shortvideo:installDebug
.\gradlew.bat "-Pandroid.testInstrumentationRunnerArguments.class=com.attentionpet.e2e.AttentionPetOverlayE2eTest" :app:connectedDebugAndroidTest
```

## Fixture Target App

The E2E suite uses an in-repo fixture app instead of depending on TikTok, Douyin, Instagram, or YouTube being installed:

```text
Module: :fixture-shortvideo
Package: com.attentionpet.fixture.shortvideo
Launcher label: Mock Shorts
```

The helper script installs this fixture before running `:app:connectedDebugAndroidTest`.

## Current Baseline Cases

`AttentionPetOverlayE2eTest.startsMonitoringFixtureTargetAndShowsCapsulePanelOverlay`

Flow:

1. Grants Usage Access, overlay, and notification permission using shell commands.
2. Seeds the Room repository so the configured target package is `com.attentionpet.fixture.shortvideo`.
3. Launches `MainActivity`.
4. Taps `开始陪伴`.
5. Launches the fixture target app.
6. Waits for the small overlay capsule.
7. Taps the capsule.
8. Waits for the expanded overlay panel.

`AttentionPetOverlayE2eTest.foregroundTimeIncreasesWhileFixtureTargetStaysOpen`

Flow:

1. Starts monitoring the fixture target.
2. Opens the expanded overlay panel and reads the E2E-only session millis semantic value.
3. Keeps the fixture target in foreground.
4. Re-opens the panel until the session millis value has increased by at least 2 seconds.

`AttentionPetOverlayE2eTest.fixtureTargetCanDriveEveryOverlayPetState`

Flow:

1. Re-seeds historical fixture usage for each state.
2. Starts monitoring and launches the fixture target.
3. Verifies the overlay exposes each expected pet state:
   - `relaxed`
   - `reminder`
   - `tense`
   - `timeout`

This verifies the real chain:

```text
Home UI -> repository config -> fixture target app -> foreground service ->
UsageStats detector -> SessionTracker -> historical usage repository query ->
RuleEvaluator -> WindowManager overlay -> UI Automator assertion
```

## Stable Selectors

The E2E test uses accessibility content descriptions from `AttentionPetTestIds`:

- `attention_pet_start_monitoring`
- `attention_pet_overlay_capsule`
- `attention_pet_overlay_panel`
- `attention_pet_overlay_panel_dismiss`
- `attention_pet_timeout_sheet`
- `attention_pet_overlay_state:<pet-state>`
- `attention_pet_overlay_session_ms:<elapsed-millis>`

New E2E-visible controls should get stable IDs in the same file rather than relying on localized text.

## Current Limits

- The timeout state is automated, but the timeout bottom sheet actions (`休息一下`, `再加 5 分钟`) still need a dedicated E2E case.
- Session persistence and extension grant consumption are still next-iteration scope.

## Add A New E2E Case

For every new user-facing monitoring feature:

1. Add or reuse stable IDs in `AttentionPetTestIds`.
2. Write an `androidTest` under `app/src/androidTest/java/com/attentionpet/e2e`.
3. Prefer UI Automator for overlay/system/cross-app behavior.
4. Prefer Compose UI Test for pure in-app Compose behavior.
5. Run `.\scripts\run-e2e.ps1`.
6. Keep unit tests and `:app:assembleDebug` passing too.
