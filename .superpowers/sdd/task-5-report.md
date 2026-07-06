# Task 5 Report

## Status

Implemented Task 5: A-bird pet drawing and overlay Compose surfaces.

## Files Changed

- `app/src/main/java/com/attentionpet/pet/BirdPet.kt`
- `app/src/main/java/com/attentionpet/overlay/OverlayViews.kt`
- `app/src/test/java/com/attentionpet/overlay/OverlayViewsTest.kt`

## Commits

- `43368579f62644acb75862e1c2537ebf7ce1f248` - `feat: add pet overlay views`

## Commands Run

- `$env:ANDROID_HOME='D:\Android\Sdk'; .\gradlew.bat :app:testDebugUnitTest`
  - Baseline before Task 5 edits: `BUILD SUCCESSFUL in 1s`; `26 actionable tasks: 26 up-to-date`.
- `$env:ANDROID_HOME='D:\Android\Sdk'; .\gradlew.bat :app:testDebugUnitTest --tests com.attentionpet.overlay.OverlayViewsTest`
  - Red check before production files: failed at `:app:compileDebugUnitTestKotlin` with unresolved references for `displayRemaining` and `expandedPanelMetricLines`.
- `$env:ANDROID_HOME='D:\Android\Sdk'; .\gradlew.bat :app:testDebugUnitTest --tests com.attentionpet.overlay.OverlayViewsTest`
  - Green check after implementation: `BUILD SUCCESSFUL in 6s`; `26 actionable tasks: 9 executed, 17 up-to-date`.
- `git diff --check`
  - Passed with no whitespace errors.
- `$env:ANDROID_HOME='D:\Android\Sdk'; .\gradlew.bat :app:compileDebugKotlin`
  - Required build check: `BUILD SUCCESSFUL in 1s`; `7 actionable tasks: 7 up-to-date`.
- `$env:ANDROID_HOME='D:\Android\Sdk'; .\gradlew.bat :app:testDebugUnitTest`
  - Required test check: `BUILD SUCCESSFUL in 3s`; `26 actionable tasks: 1 executed, 25 up-to-date`.
- `git commit -m "feat: add pet overlay views"`
  - Commit succeeded as `43368579f62644acb75862e1c2537ebf7ce1f248`.

## Build/Test Summary

- `:app:compileDebugKotlin`: passed.
- `:app:testDebugUnitTest`: passed.
- Focused overlay helper tests: passed after red/green cycle.

## Self-Review Notes

- Kept production scope to `BirdPet.kt` and `OverlayViews.kt`; added only focused JVM tests for pure overlay helper copy/state behavior.
- Did not implement WindowManager overlay controller, safe bounds, drag, service, or foreground detection.
- Did not modify the home screen or unrelated UI.
- Used the approved rounded/geometric A-bird drawing values from the brief, with centering inside the Canvas so the bird remains inside its Compose box.
- Capsule, expanded panel, and timeout sheet use stable dimensions; capsule remains small and nonblocking.
- Timeout sheet renders the required Chinese copy: `已经超时啦`, `要不要休息一下？`, `休息一下`, and `再加 5 分钟`.

## Concerns

- A code-review subagent tool was not exposed in this session, so I could not dispatch the requested review-skill reviewer; I completed a local self-review instead.
- No visual/device render pass was run; verification was by Kotlin compile and JVM unit tests as required by the task workflow.

---

## Fix Report: Palette Review Finding

## Status

Fixed the blocking palette issue from review. `BirdPet` now uses an internal `BirdPalette` data class and `birdPaletteFor(state)` helper so palette behavior is covered directly by JVM tests.

## Files Changed

- `app/src/main/java/com/attentionpet/pet/BirdPet.kt`
- `app/src/test/java/com/attentionpet/pet/BirdPetTest.kt`

## Commands Run

- `$env:ANDROID_HOME='D:\Android\Sdk'; .\gradlew.bat :app:testDebugUnitTest --tests com.attentionpet.pet.BirdPetTest`
  - Red check before production change: failed at `:app:compileDebugUnitTestKotlin` with unresolved reference `birdPaletteFor`.
- `$env:ANDROID_HOME='D:\Android\Sdk'; .\gradlew.bat :app:testDebugUnitTest --tests com.attentionpet.pet.BirdPetTest`
  - Focused green check after fix: `BUILD SUCCESSFUL in 5s`; `26 actionable tasks: 8 executed, 18 up-to-date`.
- `$env:ANDROID_HOME='D:\Android\Sdk'; .\gradlew.bat :app:compileDebugKotlin`
  - Required build check: `BUILD SUCCESSFUL in 1s`; `7 actionable tasks: 7 up-to-date`.
- `$env:ANDROID_HOME='D:\Android\Sdk'; .\gradlew.bat :app:testDebugUnitTest`
  - Required test check: `BUILD SUCCESSFUL in 2s`; `26 actionable tasks: 1 executed, 25 up-to-date`.
- `git diff --check`
  - Passed with no whitespace errors.

## Build/Test Summary

- `:app:compileDebugKotlin`: passed.
- `:app:testDebugUnitTest`: passed.
- Focused palette regression test: passed after red/green cycle.

## Self-Review Notes

- Root cause was the original inline `when` palette mapping: `REMINDER` used green colors and `TENSE` used yellow colors.
- Palette now progresses as required: relaxed green, reminder yellow, tense orange, timeout orange-red/red.
- Drawing geometry, centering, and bounds were left intact.
- No WindowManager, service, safe-bounds, drag, or foreground-detection work was added.

## Concerns

- No visual/device render pass was run; verification was compile plus JVM tests.
