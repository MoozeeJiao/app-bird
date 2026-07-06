# Attention Pet Design Spec

Date: 2026-07-04
Status: Stage 1 visual direction approved, Android implementation not started

## Product Goal

Attention Pet is an Android attention companion for short-video overuse. It does not hard-block users by default. It helps users sense time passing through a small, persistent, edge-attached pet capsule.

Core principle:

> A cute bird helps the user keep a time boundary, without judging or abruptly interrupting them.

## Target User

Users who repeatedly open short-video feeds and lose track of time in apps such as Douyin, TikTok, Instagram Reels, and YouTube Shorts.

## Approved Character Direction

The default pet is 方案 A, “圆团小鸟”.

Character traits:

- Minimal geometric 2D bird.
- Round body, short wing, small triangular beak, light cheek color, one small head tuft.
- Friendly and soft rather than childish or game-like.
- Must remain legible inside a `40-46dp` tall floating capsule.

State mapping:

- Relaxed: mint green body, subtle breathing, remaining time above 50%.
- Reminder: yellow-green body, slightly more alert, remaining time 20%-50%.
- Tense: warm yellow/orange body, light wing movement, remaining time below 20%.
- Timeout: orange-red body, raised tuft, anxious but cute expression, time limit exceeded.

B-F bird variants stay in the prototype as future skin or brand exploration options. They are not part of the MVP default implementation.

## Key User Experience

The user configures one target app and time rules once for MVP. After that, the service works when that target app is in the foreground. Multiple target apps remain out of scope for MVP even though the visual language can support them later.

Primary behavior:

1. When a target short-video app is foregrounded, Attention Pet starts tracking usage.
2. A small floating capsule appears at the screen edge.
3. The capsule shows the bird, remaining time, and a tiny progress bar.
4. The capsule can be dragged vertically and snaps to the nearest screen edge.
5. Tapping the capsule expands a lightweight panel.
6. Near a limit, the bird and capsule color become more noticeable without opening a large modal.
7. After timeout, the default behavior remains the red edge capsule. The timeout bottom sheet appears only after the user taps the timeout capsule.
8. The timeout sheet offers “休息一下” and “再加 5 分钟”. It is dismissible and never force-stops, locks, or blocks the target app.

## Timeout Prompt Policy

`TimeoutPromptPolicy` keeps timeout handling aligned with the companion principle:

- Timeout does not auto-open a blocking modal or bottom sheet.
- Timeout first appears as the red edge capsule with the anxious A-bird and an overage label such as `+2m`.
- The bottom sheet opens only from an explicit user tap on the timeout capsule.
- The sheet is dismissible through drag down or outside-tap dismissal. MVP overlay windows stay non-focusable and do not handle hardware or gesture navigation events.
- The app records timeout actions as `dismissed`, `restSelected`, or `extensionSelected`.
- The app must not repeatedly present the sheet without another user tap.
- The app must never force-stop, lock, or prevent returning to the target app.

## Time Rules

MVP supports three configured rules:

- Daily total limit: example, 60 minutes per day.
- Single continuous session limit: example, 15 minutes.
- Rolling time window limit: example, 30 minutes within the past 5 hours.

Enabled limit invariants:

- MVP requires all three configured limits to be positive.
- `dailyLimitMillis > 0`, `sessionLimitMillis > 0`, and `rollingWindowLimitMillis > 0`.
- Disabled-rule semantics are out of scope for MVP.
- Rule evaluator tests must include validation for zero/negative limits and reject them before ratio calculation.

All usage is calculated in milliseconds internally. UI may round display values to minutes.

Interval overlap formula:

- For any stored or active session and any measurement window, use:
  `overlapMillis = max(0, min(sessionEndOrNow, windowEnd) - max(sessionStart, windowStart))`.
- `sessionEndOrNow` is the persisted session end time for closed sessions, or `now` for the active foreground session.
- Daily and rolling-window buckets must use this exact overlap formula so partially overlapping sessions are counted consistently.

Daily total:

- Daily boundary is the device local date from `ZoneId.systemDefault()`.
- Daily used time is the sum of all target-app session overlap with `[localStartOfDay, now]`, including the active session when a target app is foregrounded.
- Daily remaining is `dailyLimitMillis - dailyUsedMillis`.

Continuous session:

- A session starts when the configured target app enters foreground.
- Leaving the target app for `<= 30 seconds` keeps the same continuous session. This grace avoids breaking a session for notification shade, permission flow, or brief app switches.
- Leaving the target app for `> 30 seconds` closes the session at the last foreground timestamp.
- Session remaining is `sessionLimitMillis - currentSessionForegroundMillis`.

Rolling 5-hour window:

- The rolling window is fixed to 5 hours for MVP.
- Window range is `[now - 5 hours, now]`.
- Rolling used time is the sum of target-app session overlaps with that window, including the active session when a target app is foregrounded.
- Rolling remaining is `rollingWindowLimitMillis - rollingUsedMillis`.

Rule urgency:

- Each rule produces `usedMillis`, `limitMillis`, `rawRemainingMillis`, `effectiveRemainingMillis`, `rawRemainingRatio`, and `effectiveRemainingRatio`.
- `rawRemainingMillis = limitMillis - usedMillis`.
- `effectiveRemainingMillis = rawRemainingMillis + activeExtensionRemainingMillis`.
- `rawRemainingRatio = rawRemainingMillis / limitMillis`.
- `effectiveRemainingRatio = effectiveRemainingMillis / limitMillis`.
- The triggering rule is the rule with the lowest `effectiveRemainingRatio`.
- Tie-break order is `session`, then `rollingWindow`, then `daily`, because session limits are the most immediate user-facing boundary.
- If any effective remaining time is `<= 0`, state is `timeout`.
- Else if the lowest effective ratio is `< 0.20`, state is `tense`.
- Else if the lowest effective ratio is `>= 0.20` and `<= 0.50`, state is `reminder`.
- Else state is `relaxed`.

## Stage 1 Prototype

Prototype files:

- `prototype/index.html`
- `docs/attention-pet-stage1-ui.md`

The prototype contains:

- Home/configuration screen.
- Safe floating capsule.
- Near-limit floating capsule.
- Timeout floating capsule.
- Expanded capsule panel.
- Timeout bottom sheet.
- Bird character candidates A-F, with A selected as default.
- Permission status cards for Usage Access and Display over other apps.
- One selected target app for MVP.

## Android MVP Scope

Technology stack:

- Kotlin.
- Jetpack Compose for app UI and overlay-rendered Compose content where practical.
- Room for local persistence.
- Foreground Service for active monitoring.
- UsageStatsManager for foreground app detection.
- WindowManager Overlay for edge capsule and expanded panel.
- MVVM architecture.

MVP features:

- Configure one target app.
- Set daily total limit.
- Set single continuous usage limit.
- Set the usage limit for a fixed rolling 5-hour window.
- Detect whether the target app is foregrounded.
- Show the edge floating bird capsule while the target app is active.
- Switch bird state and capsule color based on time status.
- Support tapping the capsule to show the expanded panel.
- Support timeout bottom sheet actions: rest and add 5 minutes.
- Store configuration and usage data locally.

## Architecture

Recommended modules inside a single Android app module for MVP:

- `data`: Room entities, DAO, repositories.
- `domain`: rule evaluation, time-window calculations, status model.
- `service`: foreground service, UsageStats polling, overlay lifecycle.
- `overlay`: WindowManager host views, Compose capsule/panel/sheet UI.
- `ui`: main app configuration screens in Compose.
- `pet`: bird state model and drawing primitives.

The MVP can stay as one Gradle app module with package-level separation. A multi-module setup is unnecessary until the feature set grows.

## Data Model

Core persisted data:

- `TargetAppConfig`: package name, display name, enabled flag.
- `LimitConfig`: daily limit minutes, session limit minutes, fixed rolling window hours value of 5, rolling window limit minutes.
- `UsageSession`: target package, start time, end time, foreground duration, start timestamp, end timestamp, close reason.
- `ExtensionEvent`: related session id, timestamp, added minutes, consumed foreground duration, expires when the session closes.
- `TimeoutActionEvent`: related session id, timestamp, action type `dismissed | restSelected | extensionSelected`, overage duration at action time.
- `OverlayPosition`: edge side, vertical position ratio, last updated timestamp.
- `PermissionStateSnapshot`: usage access granted, overlay permission granted, foreground service enabled.

Derived runtime data:

- Current foreground target package.
- Current session duration.
- Today total usage.
- Rolling window usage.
- Remaining time per rule.
- Effective pet state.
- Active extension remaining for the current session.

## State Names

Use stable Kotlin-facing state names throughout implementation:

Kotlin enum:

```kotlin
enum class PetState {
    RELAXED,
    REMINDER,
    TENSE,
    TIMEOUT
}
```

| Kotlin enum | Serialized value | Chinese UI label | Meaning |
| --- | --- | --- | --- |
| `RELAXED` | `relaxed` | 安全 / 放松 | Remaining effective ratio is above 50%. |
| `REMINDER` | `reminder` | 提醒 | Remaining effective ratio is 20%-50%. |
| `TENSE` | `tense` | 紧张 | Remaining effective ratio is below 20%, but no rule is exceeded. |
| `TIMEOUT` | `timeout` | 超时 | One or more effective remaining values are `<= 0`. |

## Rule Evaluation

The rule evaluator receives current time, current active session, persisted sessions, extension events, and config. It returns a pure Kotlin `RuleEvaluationResult`.

`RuleEvaluationResult` fields:

- `daily: RuleBucket`
- `session: RuleBucket`
- `rollingWindow: RuleBucket`
- `triggeringRule: RuleType`
- `effectiveRemainingMillis: Long`
- `effectiveRemainingRatio: Float`
- `petState: PetState`
- `statusCopy: String`
- `activeExtensionRemainingMillis: Long`

`RuleBucket` fields:

- `type: RuleType`
- `usedMillis: Long`
- `limitMillis: Long`
- `rawRemainingMillis: Long`
- `effectiveRemainingMillis: Long`
- `rawRemainingRatio: Float`
- `effectiveRemainingRatio: Float`

Add-5-minute extension semantics:

- “再加 5 分钟” creates one `ExtensionEvent` for the current continuous session.
- MVP allows one extension per continuous session.
- The extension is a temporary grace allowance for the current timeout episode inside the current continuous session, not a mutation of the base daily/session/window limits.
- While active, the extension applies to effective remaining calculations for all three rule buckets. This is intentional: if daily or rolling-window limits triggered timeout, the user still receives a real 5-minute grace period.
- `activeExtensionRemainingMillis = max(0, extensionAddedMillis - extensionConsumedForegroundMillis)`.
- For each rule bucket, `effectiveRemainingMillis = rawRemainingMillis + activeExtensionRemainingMillis`.
- For each rule bucket, `effectiveRemainingRatio = effectiveRemainingMillis / limitMillis`.
- `triggeringRule` is calculated from effective ratios after extension is applied.
- The extension is consumed only while the target app is foregrounded.
- When the session closes, unused extension time expires.
- Persisted usage still records the real foreground duration; extension events are stored separately for audit and future statistics.

Status copy examples:

- Relaxed: “还很充裕，小鸟在旁边陪你。”
- Reminder: “还可以看一会儿，留意时间边界。”
- Tense: “快到边界了，小鸟会更明显一点。”
- Timeout: “已经超时啦，点一下可以选择休息或加 5 分钟。”

The evaluator must be pure Kotlin logic so it can be unit tested without Android framework dependencies.

## Overlay Behavior

### Overlay Runtime Contract

All overlay surfaces use `WindowManager.LayoutParams` with `type = TYPE_APPLICATION_OVERLAY` on supported Android versions and `format = PixelFormat.TRANSLUCENT`. The app must never create a full-screen touch-consuming overlay for the capsule or panel.

Per-surface layout params:

| Surface | Width / Height | Gravity | Flags | Outside Touch | Insets / Cutout |
| --- | --- | --- | --- | --- | --- |
| Capsule | `WRAP_CONTENT` / `WRAP_CONTENT` | `TOP | START` or `TOP | END`, based on persisted edge | `FLAG_NOT_FOCUSABLE | FLAG_NOT_TOUCH_MODAL | FLAG_LAYOUT_IN_SCREEN` | Not watched; touches outside the capsule window pass through to the target app | Clamp rect into `safeBounds`; default top `safeBounds.top + 108dp` |
| Expanded panel | `WRAP_CONTENT` / `WRAP_CONTENT`, max content width `min(300dp, screenWidth - 32dp)` | same edge and top anchor as capsule | `FLAG_NOT_FOCUSABLE | FLAG_NOT_TOUCH_MODAL | FLAG_WATCH_OUTSIDE_TOUCH | FLAG_LAYOUT_IN_SCREEN` | `ACTION_OUTSIDE` dismisses panel; outside touch still reaches target app | Clamp rect into `safeBounds`; if panel would cross bottom, shift upward |
| Timeout sheet | `MATCH_PARENT` / `WRAP_CONTENT` | `BOTTOM` | `FLAG_NOT_FOCUSABLE | FLAG_NOT_TOUCH_MODAL | FLAG_WATCH_OUTSIDE_TOUCH | FLAG_LAYOUT_IN_SCREEN` | `ACTION_OUTSIDE` or drag down dismisses sheet; outside touch should not be consumed beyond dismissal signal | Bottom aligns to `safeBounds.bottom`; height target about `300dp`, max `45%` of `safeBounds.height` |
| Optional visual scrim | `MATCH_PARENT` / `MATCH_PARENT` | `FILL` | `FLAG_NOT_FOCUSABLE | FLAG_NOT_TOUCHABLE | FLAG_LAYOUT_IN_SCREEN` | No touch handling; purely visual and may be omitted in MVP | Draw only within display bounds; opacity must stay light |

`safeBounds` formula:

- `windowBounds = WindowMetricsCalculator.currentWindowMetrics.bounds`.
- `systemInsets = WindowInsetsCompat.Type.systemBars()` resolved to left/top/right/bottom.
- `cutoutInsets = DisplayCutoutCompat.safeInsets`, or zero if no cutout exists.
- `safeLeft = windowBounds.left + max(systemInsets.left, cutoutInsets.left)`.
- `safeTop = windowBounds.top + max(systemInsets.top, cutoutInsets.top)`.
- `safeRight = windowBounds.right - max(systemInsets.right, cutoutInsets.right)`.
- `safeBottom = windowBounds.bottom - max(systemInsets.bottom, cutoutInsets.bottom)`.
- Clamp every overlay rect so `left >= safeLeft`, `top >= safeTop`, `right <= safeRight`, and `bottom <= safeBottom`.

Implementation notes:

- Capsule and expanded panel should be separate overlay windows so the panel can listen for outside-touch dismissal without changing capsule touch bounds.
- Timeout sheet should be separate from the capsule/panel windows. Showing the sheet hides the expanded panel but may leave the capsule state in memory.
- If the optional scrim is used, it must be `FLAG_NOT_TOUCHABLE`; it cannot become the layer that blocks the short-video UI.
- MVP overlay surfaces are dismissed through explicit overlay gestures or outside-touch signals only; all overlay windows remain `FLAG_NOT_FOCUSABLE`.

Default capsule:

- Height `40-46dp`.
- Width `112-136dp`.
- Edge-attached with only inner-side rounded corners visible.
- Semi-transparent background.
- Shows bird, remaining minutes, and micro progress bar.
- Defaults to the left edge to avoid common short-video right-side action rails.
- If the user drags it to the right edge, persist that choice but clamp vertical position away from the bottom navigation area and common right-side action cluster.
- Uses a `WindowManager.LayoutParams` window sized to capsule content, not a full-screen overlay.
- Window type is `TYPE_APPLICATION_OVERLAY` on supported Android versions.
- Capsule flags include non-focusable behavior so it does not steal keyboard or app focus.
- Touch handling is limited to capsule bounds. Areas outside the overlay window pass through to the underlying app.
- Drag starts after an `8dp` movement threshold. On release, snap to the nearest horizontal edge and persist edge side plus vertical ratio.
- Recalculate position on orientation change, display cutout changes, and status/navigation bar inset changes.
- Hide and remove the overlay window when the target app leaves foreground for longer than the session grace period or when overlay permission is revoked.

Expanded panel:

- Width `252dp` on a 390dp-wide phone, with `maxWidth = min(300dp, screenWidth - 32dp)`.
- Anchored to the same screen edge as the capsule, visually attached to the edge rather than centered over video.
- Shows today usage, rolling-window usage, current session duration, and status copy.
- Opens only after the user taps the capsule.
- It may temporarily cover video content only after that explicit tap.
- Dismisses when tapping the capsule again, receiving an outside-touch signal, or when target app leaves foreground.
- Outside touches should not be consumed except for the minimal outside-touch signal needed to dismiss the panel.

Timeout sheet:

- Bottom-aligned overlay with light scrim.
- Shows timeout bird, “已经超时啦”, “要不要休息一下？”.
- Actions: “休息一下” and “再加 5 分钟”.
- Opens only after the user taps the timeout capsule. MVP does not auto-open the sheet at the limit boundary.
- Dismissible by drag down or tapping outside.
- Shows at most once per timeout episode unless the user taps the timeout capsule again.
- “休息一下” records a `restSelected` event and dismisses the sheet. If the implementation chooses to open Android Home, it must be user-initiated from this button and must never force-stop, lock, or prevent returning to the target app.
- “再加 5 分钟” creates the session-only extension described in Rule Evaluation and dismisses the sheet.
- Dismissing the sheet without choosing an action records `dismissed` and returns to the timeout capsule.

### MVP Acceptance Matrix

| State | Trigger | Capsule | Panel Copy | Timeout Sheet |
| --- | --- | --- | --- | --- |
| relaxed | lowest effective ratio `> 0.50` | Mint bird, mint progress, remaining minutes | “还很充裕，小鸟在旁边陪你。” | Not available |
| reminder | lowest effective ratio `0.20..0.50` | Yellow-green bird/progress | “还可以看一会儿，留意时间边界。” | Not available |
| tense | lowest effective ratio `< 0.20` and no rule exceeded | Orange progress, nervous bird | “快到边界了，小鸟会更明显一点。” | Not available |
| timeout | any effective remaining `<= 0` | Red progress, anxious bird, overage display such as `+2m` | “已经超时啦，点一下可以选择休息或加 5 分钟。” | Opens only from timeout capsule tap |

## Error Handling

Required permission states:

- MVP requires both Usage Access and Display over other apps before monitoring can be enabled. This keeps the MVP behavior aligned with the core product promise: tracking plus visible companion.
- The prototype shows the “both granted” state. Missing-permission states are specified here for Android implementation.
- Foreground service stopped by system: service should restart when app is explicitly enabled, within Android background execution limits.

Permission-state matrix:

| Usage Access | Overlay Permission | Home UI | Active permission action | Start Monitoring | On Return |
| --- | --- | --- | --- | --- | --- |
| granted | granted | Two compact granted-state cards above rule controls | “开始守护” | Enabled | Re-query permissions in `onResume`; keep enabled |
| missing | granted | Usage Access warning card above rule controls; overlay card shows granted | “开启使用情况权限” -> `Settings.ACTION_USAGE_ACCESS_SETTINGS` | Disabled | Re-query; if granted, show both granted |
| granted | missing | Overlay warning card above rule controls; usage card shows granted | “开启悬浮窗权限” -> `Settings.ACTION_MANAGE_OVERLAY_PERMISSION` with app package URI | Disabled | Re-query; if granted, show both granted |
| missing | missing | Two warning cards, Usage Access first | First CTA opens Usage Access, second opens Overlay settings | Disabled | Re-query each permission independently |

Permission copy:

- Usage missing: “需要使用情况权限，才能知道目标 App 什么时候在前台。”
- Overlay missing: “需要悬浮窗权限，小鸟才能待在屏幕边缘提醒你。”
- Both granted: “权限已开启，小鸟可以在目标 App 打开时出现。”

Home CTA placement:

- The primary CTA sits immediately below the permission cards and above the target-app selector.
- In the both-granted state, the CTA label is “开始守护” and is enabled.
- If any required permission is missing, the same CTA position remains visible but disabled. The active action is the missing-permission card CTA above it.
- The Stage 1 prototype shows the both-granted state; the permission-state matrix above is the implementation source of truth for missing-permission variants.

Data handling:

- If session end is missed because the process is killed, close the active session on next launch/service start using last known timestamp.
- If UsageStats returns no reliable foreground app, keep the previous state briefly, then hide overlay if uncertainty continues.

## Testing Strategy

Unit tests:

- Rule evaluator status thresholds.
- Daily total calculation.
- Rolling 5-hour window inclusion/exclusion.
- Rolling-window partial-overlap calculation.
- Daily reset at device local midnight.
- Session continuity with 30-second background grace.
- Session limit calculation.
- Add-5-minutes extension effect.

Instrumentation/manual tests:

- Permission grant flow.
- Target app foreground detection.
- Overlay appears, drags, snaps, expands, and dismisses.
- Overlay outside bounds do not block the target app.
- Timeout bottom sheet actions.

Visual checks:

- Capsule does not cover the central video content.
- Bird remains legible at small size.
- Relaxed/reminder/tense/timeout colors match the prototype.

## Out Of Scope For MVP

- Multiple simultaneous target app groups.
- Cloud sync.
- Account system.
- Gamified leveling.
- Complex pet customization.
- Strict blocking mode.
- Analytics dashboard beyond basic today/window/session stats.

## Open Implementation Gate

Before Android development starts, review this spec and confirm:

- 方案 A remains the default bird.
- MVP starts with one target app.
- Rolling window is fixed to 5 hours for MVP.
- “再加 5 分钟” is allowed once per continuous session after timeout.
- Timeout bottom sheet is user-triggered from the timeout capsule, not auto-opened.

After confirmation, the next Superpowers step is to create an implementation plan.
