# Attention Pet UI Repair Design

Date: 2026-07-08
Status: A direction approved by user; ready for implementation planning after user review

## Goal

Repair the first installed Android build so it feels like a real Attention Pet product and the basic monitoring flow is understandable and verifiable on device.

This design covers:

- New simple geometric app icon direction.
- Home screen visual redesign using the approved A direction, "quiet companion".
- Target app picker redesign with real app icons in the list.
- Permission guidance that appears when needed instead of exposing two primary permission buttons on the home screen.
- Start monitoring feedback and failure states so tapping "开始陪伴" never appears to do nothing.

## Approved Visual Direction

Use A direction, "安静陪伴型".

Characteristics:

- The bird is the first brand signal, but the UI stays quiet and practical.
- Geometry stays simple: round bird body, small wing, small beak, soft cheek color, compact head tuft.
- Color palette stays close to the existing product palette: mint, deep ink, white, soft warm warning colors.
- Avoid decorative gradients, large marketing-style hero sections, and overly cute game-like treatment.
- Cards use tight radii and restrained shadows so the app feels like a tool with a companion, not a toy.

## App Icon

Use a new geometric bird launcher icon.

Default icon:

- Dark ink rounded square background.
- Centered round mint bird.
- White belly, small beak, two eyes, one head tuft.
- Must remain legible at launcher size and notification-adjacent surfaces.

Implementation notes:

- Replace the current generic ring launcher foreground.
- Keep the existing adaptive icon structure.
- Create a matching small notification icon that is simple and monochrome-safe.
- If more than one icon variant is generated during implementation, keep A as the default candidate and only add alternates as local assets if they are needed for review.

## Home Screen

The home screen should no longer show two large permission cards with "去开启" buttons as the first interaction.

Structure:

1. Compact top bar with small bird mark, `Attention Pet`, and a short status line.
2. Quiet companion panel:
   - Shows the bird.
   - Shows the current readiness/status copy.
   - Shows a small progress-style visual tied to remaining time or readiness.
3. Permission readiness strip:
   - If permissions are complete: "权限已开启，小鸟可以在目标 App 打开时出现。"
   - If permissions are missing: "还差 1/2 步才能开始陪伴。"
   - The strip has one action label such as "去完成", not two competing buttons.
4. Primary CTA:
   - Label before start: "开始陪伴".
   - Enabled when a target app is selected; if permissions are missing, tapping it opens the guided permission flow instead of silently doing nothing.
   - Label/state after successful service start: "正在陪伴".
5. Target app section:
   - Shows selected app icon and label.
   - Action label: "更换".
6. Rule controls:
   - Daily total limit.
   - Single continuous limit.
   - Past 5-hour window limit.
   - Preserve current min/max constraints unless implementation discovers a broken constraint.

## Target App Picker

Replace the current centered AlertDialog with a bottom sheet.

Required behavior:

- Title: "选择受限 App".
- Search field at top for filtering by app label.
- Rows show:
  - Real app icon loaded from the package manager.
  - App label.
  - Optional package name in muted text only if labels collide.
  - Checkmark for the currently selected app.
- Tapping a row immediately selects that app and dismisses the sheet.
- "取消" closes without changing selection.
- Empty state says no matching launchable app was found.
- Loading should not block home rendering; the app list can be loaded when the sheet opens.

Fallback behavior:

- If an app icon cannot be loaded, show a colored square with the first app-label character.
- If the label is blank, show package name as the label.

Implementation boundary:

- Extend `LaunchableApp` to carry icon data suitable for Compose rendering, or introduce a UI model wrapper that includes icon data.
- Keep package manager querying inside `AppPicker`; keep composable rendering inside `HomeScreen` or a small picker composable.

## Permission Guidance

Permissions should be contextual and sequential.

Required permissions for MVP:

- Usage Access: needed to know when the target app is foreground.
- Display over other apps: needed to show the edge companion.

Home behavior:

- Do not show two primary permission buttons on the main screen.
- Show a compact readiness strip summarizing what remains.
- If the user taps "开始陪伴" while permissions are missing, open a guided bottom sheet.

Guided bottom sheet:

1. Explain why the missing permission is needed in one sentence.
2. Show missing permissions as a two-step checklist.
3. Primary action opens the next missing permission setting.
4. Usage Access is requested before overlay when both are missing.
5. Returning from Settings refreshes permission state in `onResume`.
6. When all required permissions are granted, dismiss the sheet or switch it to a "ready" state with the "开始陪伴" CTA.

Failure and edge states:

- If the user returns without granting permission, keep the sheet open or show the readiness strip still missing the step.
- If a permission is revoked after monitoring started, the app should stop or mark monitoring unavailable and show a clear status.
- If Android refuses to open a settings intent, show an error message and keep the user on the home screen.

## Start Monitoring Feedback

Tapping "开始陪伴" must always produce visible feedback.

Flow:

1. If permissions are missing, open the permission guidance sheet.
2. If no target app is selected, open the target app picker or show a target-app-required status.
3. If ready, persist current config and start `AttentionMonitorService`.
4. Immediately update home UI to a "starting" state.
5. After service start is requested, show:
   - "正在陪伴 [App label]"
   - "打开目标 App 后，小鸟会出现在屏幕边缘。"
6. If service start throws, show a visible error state with a retry action.

Important product clarification:

- The edge capsule appears when the configured target app is foreground.
- The home screen must explain this after start so users do not interpret "no overlay on home" as no reaction.

## Monitoring Functionality Check

The installed build complaint says "开始陪伴没有任何反应". Investigation shows the current click path calls `MainViewModel.onStartMonitoring` and `AttentionMonitorService.start`, but the UI has no started state and the overlay only appears when the target app is foreground.

The implementation should verify both possibilities:

- UI feedback problem: fix with explicit starting/started/error states.
- Runtime problem: confirm service start, notification, foreground detection, and overlay appearance still work after UI changes.

No root-cause assumption should be made without verification.

## Component Changes

Likely affected production files:

- `app/src/main/res/drawable/ic_launcher_foreground.xml`
- `app/src/main/res/drawable/ic_stat_attention_pet.xml`
- `app/src/main/res/values/colors.xml`
- `app/src/main/java/com/attentionpet/ui/Theme.kt`
- `app/src/main/java/com/attentionpet/ui/HomeScreen.kt`
- `app/src/main/java/com/attentionpet/ui/AppPicker.kt`
- `app/src/main/java/com/attentionpet/ui/MainViewModel.kt`
- `app/src/main/java/com/attentionpet/MainActivity.kt`
- Possibly `AttentionPetTestIds.kt` for stable selectors.

The service should only be changed if tests or manual verification show an actual runtime bug.

## State Model

Add an explicit home monitoring state:

- `Idle`: not started.
- `NeedsPermission`: one or both permissions missing.
- `Starting`: config is being saved and service start was requested.
- `Active`: service start request completed; user should open target app to see the capsule.
- `Error`: service start failed or configuration is invalid.

This state can be represented inside `HomeUiState` or derived from ViewModel state plus `PermissionSnapshot`.

## Testing Strategy

Use TDD for implementation.

Unit and UI-facing tests should cover:

- Permission readiness copy for both granted, one missing, and both missing.
- Tapping start while permissions are missing opens the permission guidance path instead of doing nothing.
- Tapping start when ready transitions through starting/active state and invokes service start.
- Service start errors surface as an error state.
- App picker rows expose label, package name, selected state, and icon/fallback data.
- App picker search filters by label.
- Existing config persistence tests still pass.

Instrumentation or manual verification should cover:

- On a device, return from Usage Access settings refreshes the home screen.
- Return from overlay settings refreshes the home screen.
- Starting monitoring shows a foreground notification.
- Opening the configured target app shows the overlay capsule.
- App picker list displays real installed app icons.

## Out Of Scope

- Multiple target apps.
- Pet customization skins beyond the approved default icon.
- Strict blocking mode.
- Analytics dashboard.
- Reworking timeout sheet behavior beyond preserving current MVP behavior.

## Review Gate

After this spec is approved, create an implementation plan. Implementation must start with failing tests for the permission/start-monitoring and app-picker behavior before editing production code.
