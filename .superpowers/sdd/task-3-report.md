# Task 3 Report: Room Persistence and Repository

## Status

Completed.

## Files Changed

- `app/src/main/java/com/attentionpet/data/entities.kt`
- `app/src/main/java/com/attentionpet/data/ConfigDao.kt`
- `app/src/main/java/com/attentionpet/data/SessionDao.kt`
- `app/src/main/java/com/attentionpet/data/EventDao.kt`
- `app/src/main/java/com/attentionpet/data/AttentionPetDatabase.kt`
- `app/src/main/java/com/attentionpet/data/AttentionPetRepository.kt`
- `app/build.gradle.kts`
- `app/schemas/com.attentionpet.data.AttentionPetDatabase/1.json`

## Commit

- `c9c8365 feat: add local persistence`

## Commands Run

```powershell
$env:ANDROID_HOME='D:\Android\Sdk'; .\gradlew.bat :app:kspDebugKotlin :app:compileDebugKotlin
```

First run succeeded but emitted Room's schema export warning because no schema location was configured.

```powershell
$env:ANDROID_HOME='D:\Android\Sdk'; .\gradlew.bat :app:kspDebugKotlin :app:compileDebugKotlin
```

Final result: `BUILD SUCCESSFUL in 2s`; 7 actionable tasks, 2 executed, 5 up-to-date.

```powershell
$env:ANDROID_HOME='D:\Android\Sdk'; .\gradlew.bat :app:testDebugUnitTest
```

Final result: `BUILD SUCCESSFUL in 2s`; 26 actionable tasks, 5 executed, 21 up-to-date.

```powershell
git diff --check
```

Result: exit 0, with Git CRLF conversion warnings only.

```powershell
git add app/src/main/java/com/attentionpet/data app/schemas app/build.gradle.kts; git commit -m "feat: add local persistence"
```

Result: commit `c9c8365`.

## Build/Test Summary

- Room/KSP debug compile passed.
- Debug Kotlin compile passed.
- Debug unit tests passed.
- Room schema exported to `app/schemas/com.attentionpet.data.AttentionPetDatabase/1.json`.

## Self-Review Notes

- Implemented the Task 3 entities, DAOs, database, and repository in the data package.
- Added `AttentionPetRepository.config()` returning persisted limits as `RuleConfig?` to satisfy the brief's repository interface list.
- Kept extension recording idempotent per session as specified by the provided repository skeleton.
- Added only the minimal `ksp { arg("room.schemaLocation", "$projectDir/schemas") }` build config needed for `exportSchema = true` and `app/schemas` generation.
- Did not modify domain, UI, service, overlay, prototype, or docs files.

## Concerns

- No new DAO/repository unit tests were added because Task 3 scoped ownership to the data-layer implementation files and requested only the existing unit test command after Room compile.
- Once-per-session extension enforcement beyond recording one extension event remains outside Task 3, matching the provided context.

---

# Task 3 Fix Report: Blocking Review Findings

## Status

Completed.

## Files Changed

- `app/src/main/java/com/attentionpet/data/entities.kt`
- `app/src/main/java/com/attentionpet/data/EventDao.kt`
- `app/src/main/java/com/attentionpet/data/AttentionPetRepository.kt`
- `app/src/test/java/com/attentionpet/data/AttentionPetRepositoryTest.kt`
- `app/schemas/com.attentionpet.data.AttentionPetDatabase/1.json`
- `.superpowers/sdd/task-3-report.md`

## Commit

- To be committed with message `fix: address persistence review`.

## Commands Run

```powershell
$env:ANDROID_HOME='D:\Android\Sdk'; .\gradlew.bat :app:testDebugUnitTest --tests com.attentionpet.data.AttentionPetRepositoryTest
```

Initial result: failed as expected before production changes with unresolved repository methods `overlayPosition`, `saveOverlayPosition`, and `usageIntervals`.

```powershell
$env:ANDROID_HOME='D:\Android\Sdk'; .\gradlew.bat :app:testDebugUnitTest --tests com.attentionpet.data.AttentionPetRepositoryTest
```

Final focused result: `BUILD SUCCESSFUL in 8s`; 26 actionable tasks, 8 executed, 18 up-to-date.

```powershell
$env:ANDROID_HOME='D:\Android\Sdk'; .\gradlew.bat :app:kspDebugKotlin :app:compileDebugKotlin
```

Final required compile result: `BUILD SUCCESSFUL in 1s`; 7 actionable tasks, 7 up-to-date.

```powershell
$env:ANDROID_HOME='D:\Android\Sdk'; .\gradlew.bat :app:testDebugUnitTest
```

Final required unit test result: `BUILD SUCCESSFUL in 2s`; 26 actionable tasks, 1 executed, 25 up-to-date.

```powershell
git diff --check
```

Result: exit 0, with Git CRLF conversion warnings only.

## Build/Test Summary

- Repository tests cover overlay position read/write, overlapping session mapping to `UsageInterval`, and duplicate extension insert fallback.
- Room/KSP debug compile passed.
- Debug Kotlin compile passed.
- Debug unit tests passed.
- Room schema regenerated with unique index `index_extension_event_sessionId`.

## Self-Review Notes

- Added `AttentionPetRepository.overlayPosition()` and `saveOverlayPosition(OverlayPositionEntity)`.
- Added `AttentionPetRepository.usageIntervals(packageName, windowStart, windowEnd, nowMillis)` using `SessionDao.sessionsOverlapping(...)` and `toUsageInterval(nowMillis)`.
- Added a unique Room index on `ExtensionEventEntity.sessionId`.
- Changed extension insert to `OnConflictStrategy.IGNORE` and made repository return either the new id or the existing id after an ignored duplicate insert.
- Made `extensionForSession` deterministic with `ORDER BY id LIMIT 1`.
- Did not modify UI, service, overlay, or domain files.

## Concerns

- The repository throws if an extension insert is ignored but the follow-up lookup cannot find the conflicting row; with the unique index this should indicate database inconsistency rather than a normal race path.
