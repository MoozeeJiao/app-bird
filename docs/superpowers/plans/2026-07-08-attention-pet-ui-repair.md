# Attention Pet UI Repair Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Repair the installed Attention Pet Android build with the approved A visual direction, contextual permission guidance, visible start-monitoring feedback, and an app picker with real app icons.

**Architecture:** Keep the single Android app module and existing package boundaries. Add small UI state models and pure helpers in `ui`/`permissions`, keep PackageManager querying in `AppPicker`, keep monitoring service wiring behind `MainViewModel` and `MainActivity`.

**Tech Stack:** Kotlin, Jetpack Compose Material3, Android PackageManager, Android Drawable/ImageBitmap conversion, Room-backed existing config persistence, JUnit unit tests.

## Global Constraints

- Use A direction, "安静陪伴型".
- Target app picker rows must show real app icons, with first-character fallback if loading fails.
- Missing permissions must be guided contextually instead of shown as two main-screen permission buttons.
- Tapping "开始陪伴" must always produce visible feedback.
- The overlay capsule still appears only when the configured target app is foreground.
- Implementation starts with failing tests before production code changes.
- Do not modify unrelated dirty files such as `.github/workflows/android-debug-apk.yml`.

---

## File Structure

- `app/src/main/java/com/attentionpet/ui/HomeScreen.kt`: home UI state, copy, permission guide sheet, app picker sheet, visual layout.
- `app/src/main/java/com/attentionpet/ui/MainViewModel.kt`: monitoring status state and start-monitoring success/error transitions.
- `app/src/main/java/com/attentionpet/ui/AppPicker.kt`: launchable app query, icon loading, filtering helper.
- `app/src/main/java/com/attentionpet/permissions/PermissionState.kt`: missing-permission labels and next-step helper.
- `app/src/main/java/com/attentionpet/MainActivity.kt`: local sheet state, settings launch error handling, service start wiring.
- `app/src/main/res/drawable/ic_launcher_foreground.xml`: new geometric bird launcher foreground.
- `app/src/main/res/drawable/ic_stat_attention_pet.xml`: matching notification glyph.
- `app/src/main/res/values/colors.xml`: launcher background color.
- Tests under `app/src/test/java/com/attentionpet/ui` and `app/src/test/java/com/attentionpet/permissions`.

## Task 1: App Picker Model, Icons, And Filtering

**Files:**
- Modify: `app/src/main/java/com/attentionpet/ui/AppPicker.kt`
- Modify: `app/src/main/java/com/attentionpet/ui/HomeScreen.kt`
- Test: `app/src/test/java/com/attentionpet/ui/AppPickerTest.kt`
- Test: `app/src/test/java/com/attentionpet/ui/HomeConfigStateTest.kt`

**Interfaces:**
- Produces: `data class LaunchableApp(val packageName: String, val label: String, val icon: Drawable? = null)`
- Produces: `AppPicker.filterLaunchableApps(apps: List<LaunchableApp>, query: String): List<LaunchableApp>`
- Consumes: existing `HomeConfigState.selectTarget(app: LaunchableApp)`

- [ ] **Step 1: Write failing AppPicker tests**

Add tests for `icon` constructor support and search filtering:

```kotlin
@Test
fun launchableAppCanCarryIconForPickerRows() {
    val app = LaunchableApp(packageName = "com.example", label = "Example", icon = null)
    assertEquals("com.example", app.packageName)
    assertEquals("Example", app.label)
    assertEquals(null, app.icon)
}

@Test
fun filtersLaunchableAppsByLabelCaseInsensitively() {
    val apps = listOf(
        LaunchableApp("com.video", "Bilibili"),
        LaunchableApp("com.chat", "ChatGPT"),
        LaunchableApp("com.browser", "Chrome")
    )
    assertEquals(listOf("com.chat"), AppPicker.filterLaunchableApps(apps, "chat").map { it.packageName })
}
```

- [ ] **Step 2: Run tests and verify RED**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests com.attentionpet.ui.AppPickerTest --tests com.attentionpet.ui.HomeConfigStateTest
```

Expected: FAIL because `LaunchableApp.icon` and `AppPicker.filterLaunchableApps` do not exist.

- [ ] **Step 3: Implement model and filter**

Update `LaunchableApp` and `AppPicker.launchableApps` so each row carries `it.loadIcon(context.packageManager)`.

Add:

```kotlin
fun filterLaunchableApps(apps: List<LaunchableApp>, query: String): List<LaunchableApp> {
    val normalized = query.trim().lowercase()
    if (normalized.isEmpty()) return apps
    return apps.filter {
        it.label.lowercase().contains(normalized) || it.packageName.lowercase().contains(normalized)
    }
}
```

- [ ] **Step 4: Run tests and verify GREEN**

Run the same targeted test command. Expected: PASS.

## Task 2: Permission Readiness And Monitoring Status

**Files:**
- Modify: `app/src/main/java/com/attentionpet/permissions/PermissionState.kt`
- Modify: `app/src/main/java/com/attentionpet/ui/HomeScreen.kt`
- Modify: `app/src/main/java/com/attentionpet/ui/MainViewModel.kt`
- Test: `app/src/test/java/com/attentionpet/permissions/PermissionSnapshotTest.kt`
- Test: `app/src/test/java/com/attentionpet/ui/HomeScreenCopyTest.kt`
- Test: `app/src/test/java/com/attentionpet/ui/MainViewModelTest.kt`

**Interfaces:**
- Produces: `enum class RequiredPermission { USAGE_ACCESS, OVERLAY }`
- Produces: `PermissionSnapshot.missingPermissions(): List<RequiredPermission>`
- Produces: `PermissionSnapshot.nextMissingPermission(): RequiredPermission?`
- Produces: `enum class MonitoringStatus { IDLE, STARTING, ACTIVE, ERROR }`
- Produces: `MainViewModel.monitoringStatus: StateFlow<MonitoringStatus>`

- [ ] **Step 1: Write failing tests**

Add tests that missing permissions are ordered usage-first, home copy uses new strings, and ViewModel transitions active/error:

```kotlin
@Test
fun missingPermissionsAreReportedUsageFirst() {
    val snapshot = PermissionSnapshot(usageAccessGranted = false, overlayGranted = false)
    assertEquals(listOf(RequiredPermission.USAGE_ACCESS, RequiredPermission.OVERLAY), snapshot.missingPermissions())
    assertEquals(RequiredPermission.USAGE_ACCESS, snapshot.nextMissingPermission())
}

@Test
fun startMonitoringMovesStatusToActiveAfterCallback() = runTest {
    val viewModel = MainViewModel(repository, ioDispatcher = StandardTestDispatcher(testScheduler))
    advanceUntilIdle()
    viewModel.onStartMonitoring {}
    advanceUntilIdle()
    assertEquals(MonitoringStatus.ACTIVE, viewModel.monitoringStatus.value)
}

@Test
fun startMonitoringErrorsAreVisibleInStatus() = runTest {
    val viewModel = MainViewModel(repository, ioDispatcher = StandardTestDispatcher(testScheduler))
    advanceUntilIdle()
    viewModel.onStartMonitoring { error("boom") }
    advanceUntilIdle()
    assertEquals(MonitoringStatus.ERROR, viewModel.monitoringStatus.value)
}
```

- [ ] **Step 2: Run tests and verify RED**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests com.attentionpet.permissions.PermissionSnapshotTest --tests com.attentionpet.ui.HomeScreenCopyTest --tests com.attentionpet.ui.MainViewModelTest
```

Expected: FAIL because permission helpers and monitoring status are missing.

- [ ] **Step 3: Implement permission helpers and monitoring state**

Add permission helper enum/functions in `PermissionState.kt`.

Add `MonitoringStatus`, `_monitoringStatus`, and error-safe `onStartMonitoring` transition in `MainViewModel`.

- [ ] **Step 4: Run tests and verify GREEN**

Run the same targeted test command. Expected: PASS.

## Task 3: A Direction Home UI And Bottom Sheets

**Files:**
- Modify: `app/src/main/java/com/attentionpet/ui/HomeScreen.kt`
- Modify: `app/src/main/java/com/attentionpet/MainActivity.kt`
- Modify: `app/src/main/java/com/attentionpet/AttentionPetTestIds.kt`
- Test: `app/src/test/java/com/attentionpet/ui/HomeScreenCopyTest.kt`

**Interfaces:**
- Consumes: `HomeUiState.permissionSnapshot`, `MonitoringStatus`, `LaunchableApp.icon`
- Produces: `showPermissionGuide` flow controlled by `MainActivity`
- Produces: bottom-sheet app picker with search and icons

- [ ] **Step 1: Add failing copy/test-id expectations**

Update `HomeScreenCopyTest` to require:

```kotlin
assertEquals("小鸟陪你守住边界", HomeScreenCopy.subtitle)
assertEquals("还差 %d 步才能开始陪伴", HomeScreenCopy.missingPermissionFormat)
assertEquals("正在陪伴", HomeScreenCopy.activeCta)
assertEquals("打开目标 App 后，小鸟会出现在屏幕边缘。", HomeScreenCopy.activeHint)
assertEquals("选择受限 App", HomeScreenCopy.targetPickerTitle)
```

- [ ] **Step 2: Run copy test and verify RED**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests com.attentionpet.ui.HomeScreenCopyTest
```

Expected: FAIL until new copy exists.

- [ ] **Step 3: Implement Compose UI**

Replace the old permission cards and AlertDialog with:

- A compact top row and companion panel.
- Permission readiness strip.
- Primary CTA that routes to permission guide when permissions are missing.
- Target app bottom sheet using real icons and `AppPicker.filterLaunchableApps`.
- Permission guide bottom sheet that opens the next missing settings page.

- [ ] **Step 4: Run copy tests and compile Kotlin**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests com.attentionpet.ui.HomeScreenCopyTest :app:compileDebugKotlin
```

Expected: PASS.

## Task 4: Icon Assets And Final Verification

**Files:**
- Modify: `app/src/main/res/drawable/ic_launcher_foreground.xml`
- Modify: `app/src/main/res/drawable/ic_stat_attention_pet.xml`
- Modify: `app/src/main/res/values/colors.xml`

**Interfaces:**
- Consumes: existing adaptive icon resource references.
- Produces: geometric bird launcher and notification assets.

- [ ] **Step 1: Replace icon vectors**

Use vector paths for the approved round mint bird and simple white notification glyph.

- [ ] **Step 2: Run full local verification**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: If an emulator/device is available, run e2e**

Run:

```powershell
.\scripts\run-e2e.ps1
```

Expected: existing overlay e2e still passes. If unavailable, report the blocker.

## Self-Review

Spec coverage:

- Icon direction: Task 4.
- A home screen direction: Task 3.
- App picker icons/search: Task 1 and Task 3.
- Contextual permissions: Task 2 and Task 3.
- Start monitoring feedback: Task 2 and Task 3.
- Runtime verification: Task 4.

Placeholder scan:

- No TBD/TODO placeholders remain.

Type consistency:

- `LaunchableApp.icon` is defined in Task 1 before Compose rendering in Task 3.
- `MonitoringStatus` is defined in Task 2 before home rendering in Task 3.
- Permission helpers are defined in Task 2 before MainActivity uses them in Task 3.
