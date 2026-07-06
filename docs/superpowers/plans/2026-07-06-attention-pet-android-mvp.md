# Attention Pet Android MVP Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the Android MVP for Attention Pet: one target app, three time-limit rules, local persistence, UsageStats foreground detection, and a small non-blocking A-bird overlay capsule with panel and timeout sheet.

**Architecture:** Single Android app module with package-level separation. Domain rule evaluation is pure Kotlin and unit-tested first; Android framework integration is isolated behind repositories, permission helpers, foreground service, and WindowManager overlay controller. Compose draws the main configuration UI and overlay content using the approved A “圆团小鸟” visual direction.

**Tech Stack:** Kotlin, Android Gradle Plugin 9.2.1, Gradle wrapper 9.4.1, Compose BOM 2026.04.01, Room 2.8.1, Foreground Service, UsageStatsManager, WindowManager `TYPE_APPLICATION_OVERLAY`, MVVM.

## Global Constraints

- MVP configures exactly one target app.
- Default pet is 方案 A “圆团小鸟”; B-F are not implemented in MVP.
- Pet states use `enum class PetState { RELAXED, REMINDER, TENSE, TIMEOUT }` with serialized values `relaxed`, `reminder`, `tense`, `timeout`.
- Daily/session/rolling limits must be positive before ratio calculation.
- Rolling window is fixed to 5 hours.
- Session background grace is 30 seconds.
- `再加 5 分钟` is allowed once per continuous session and applies as a temporary grace for all three effective rule buckets.
- Timeout bottom sheet opens only from explicit timeout-capsule tap; it never auto-opens at the boundary.
- Overlay windows remain `FLAG_NOT_FOCUSABLE`; hardware/gesture navigation events are not required for overlay dismissal.
- Capsule defaults to the left edge and uses a small `WRAP_CONTENT` overlay window, not a full-screen touch-consuming window.
- Monitoring starts only when Usage Access and Display over other apps permissions are both granted.
- Development references verified from official docs: AGP current release 9.2.1 and AGP 9.2 compatibility require Gradle 9.4.1/JDK 17; AGP 9 built-in Kotlin uses KGP 2.2.10 and matches KSP to `2.2.10-2.0.2`; Compose April 2026 BOM is `2026.04.01`; Room 2.x stable release used here is `2.8.1`.

---

## File Structure

Create this Android project under `D:\app-bird`:

```text
D:\app-bird
├── settings.gradle.kts
├── build.gradle.kts
├── gradle.properties
├── gradlew
├── gradlew.bat
├── gradle/wrapper/gradle-wrapper.properties
├── app
│   ├── build.gradle.kts
│   ├── src/main/AndroidManifest.xml
│   ├── src/main/java/com/attentionpet
│   │   ├── AttentionPetApp.kt
│   │   ├── MainActivity.kt
│   │   ├── data
│   │   │   ├── AttentionPetDatabase.kt
│   │   │   ├── AttentionPetRepository.kt
│   │   │   ├── ConfigDao.kt
│   │   │   ├── EventDao.kt
│   │   │   ├── SessionDao.kt
│   │   │   └── entities.kt
│   │   ├── domain
│   │   │   ├── RuleEvaluator.kt
│   │   │   ├── RuleModels.kt
│   │   │   └── TimeMath.kt
│   │   ├── overlay
│   │   │   ├── OverlayController.kt
│   │   │   ├── OverlayPositionStore.kt
│   │   │   ├── OverlaySafeBounds.kt
│   │   │   └── OverlayViews.kt
│   │   ├── permissions
│   │   │   └── PermissionState.kt
│   │   ├── pet
│   │   │   └── BirdPet.kt
│   │   ├── service
│   │   │   ├── AttentionMonitorService.kt
│   │   │   ├── ForegroundAppDetector.kt
│   │   │   └── SessionTracker.kt
│   │   └── ui
│   │       ├── AppPicker.kt
│   │       ├── HomeScreen.kt
│   │       ├── MainViewModel.kt
│   │       └── Theme.kt
│   └── src/test/java/com/attentionpet
│       ├── domain/RuleEvaluatorTest.kt
│       ├── domain/TimeMathTest.kt
│       └── service/SessionTrackerTest.kt
└── docs
    └── superpowers/plans/2026-07-06-attention-pet-android-mvp.md
```

Responsibility boundaries:

- `domain`: pure Kotlin time math and rule evaluation. No Android imports.
- `data`: Room schema, DAOs, repository. No Compose or WindowManager.
- `permissions`: permission status and settings intents.
- `ui`: main in-app Compose configuration UI.
- `pet`: reusable A-bird drawing primitives for app and overlay UI.
- `overlay`: WindowManager params, safe bounds, drag/snap, Compose overlay surfaces.
- `service`: foreground service, UsageStats foreground detection, session lifecycle, repository/evaluator/overlay wiring.

---

### Task 1: Android Project Scaffold

**Files:**
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Create: `gradle.properties`
- Create: `app/build.gradle.kts`
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/java/com/attentionpet/AttentionPetApp.kt`
- Create: `app/src/main/java/com/attentionpet/MainActivity.kt`

**Interfaces:**
- Produces: A buildable single-module Android app named `Attention Pet`.
- Produces: Package namespace `com.attentionpet`.
- Consumes: Official AGP/Compose/Room versions listed in Global Constraints.

- [ ] **Step 1: Initialize Git if needed**

Run:

```powershell
if (-not (Test-Path .git)) { git init }
git status --short
```

Expected: Git repository exists. If `git` is unavailable, continue and report that commits will be skipped.

- [ ] **Step 2: Generate Gradle wrapper**

Run:

```powershell
gradle wrapper --gradle-version 9.4.1
```

Expected: `gradlew`, `gradlew.bat`, and `gradle/wrapper/gradle-wrapper.properties` exist. If global Gradle 7.4 cannot generate the wrapper, install/use a Gradle 9.4.1 distribution and rerun the same command.

- [ ] **Step 3: Create `settings.gradle.kts`**

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "AttentionPet"
include(":app")
```

- [ ] **Step 4: Create root `build.gradle.kts`**

```kotlin
plugins {
    id("com.android.application") version "9.2.1" apply false
    id("com.google.devtools.ksp") version "2.2.10-2.0.2" apply false
}
```

- [ ] **Step 5: Create `gradle.properties`**

```properties
org.gradle.jvmargs=-Xmx3072m -Dfile.encoding=UTF-8
android.useAndroidX=true
android.nonTransitiveRClass=true
```

- [ ] **Step 6: Create `app/build.gradle.kts`**

```kotlin
plugins {
    id("com.android.application")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.attentionpet"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.attentionpet"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.04.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("androidx.room:room-runtime:2.8.1")
    implementation("androidx.room:room-ktx:2.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    ksp("androidx.room:room-compiler:2.8.1")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
```

- [ ] **Step 7: Create `AndroidManifest.xml`**

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="android.permission.PACKAGE_USAGE_STATS" />
    <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

    <queries>
        <intent>
            <action android:name="android.intent.action.MAIN" />
            <category android:name="android.intent.category.LAUNCHER" />
        </intent>
    </queries>

    <application
        android:name=".AttentionPetApp"
        android:allowBackup="false"
        android:icon="@mipmap/ic_launcher"
        android:label="Attention Pet"
        android:supportsRtl="true"
        android:theme="@style/AppTheme">

        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <service
            android:name=".service.AttentionMonitorService"
            android:exported="false"
            android:foregroundServiceType="specialUse" />
    </application>
</manifest>
```

- [ ] **Step 8: Create starter application/activity**

`AttentionPetApp.kt`:

```kotlin
package com.attentionpet

import android.app.Application

class AttentionPetApp : Application()
```

`MainActivity.kt`:

```kotlin
package com.attentionpet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Text("Attention Pet")
        }
    }
}
```

- [ ] **Step 9: Run scaffold build**

Run:

```powershell
.\gradlew.bat :app:assembleDebug
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 10: Commit**

```powershell
git add settings.gradle.kts build.gradle.kts gradle.properties gradlew gradlew.bat gradle app
git commit -m "chore: scaffold Android app"
```

Expected: commit succeeds if Git is available.

---

### Task 2: Pure Domain Rule Evaluator

**Files:**
- Create: `app/src/main/java/com/attentionpet/domain/RuleModels.kt`
- Create: `app/src/main/java/com/attentionpet/domain/TimeMath.kt`
- Create: `app/src/main/java/com/attentionpet/domain/RuleEvaluator.kt`
- Create: `app/src/test/java/com/attentionpet/domain/TimeMathTest.kt`
- Create: `app/src/test/java/com/attentionpet/domain/RuleEvaluatorTest.kt`

**Interfaces:**
- Produces: `PetState`, `RuleType`, `RuleConfig`, `UsageInterval`, `ExtensionGrant`, `RuleBucket`, `RuleEvaluationResult`.
- Produces: `RuleEvaluator.evaluate(nowMillis, zoneId, config, sessions, activeSession, extensionGrant): RuleEvaluationResult`.
- Consumes: No Android framework APIs.

- [ ] **Step 1: Write failing `TimeMathTest`**

```kotlin
package com.attentionpet.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class TimeMathTest {
    @Test
    fun overlapCountsOnlyIntersection() {
        val result = overlapMillis(
            sessionStart = 1_000L,
            sessionEndOrNow = 5_000L,
            windowStart = 3_000L,
            windowEnd = 7_000L
        )
        assertEquals(2_000L, result)
    }

    @Test
    fun overlapReturnsZeroWhenIntervalsDoNotTouch() {
        val result = overlapMillis(
            sessionStart = 1_000L,
            sessionEndOrNow = 2_000L,
            windowStart = 3_000L,
            windowEnd = 4_000L
        )
        assertEquals(0L, result)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests com.attentionpet.domain.TimeMathTest
```

Expected: FAIL because `overlapMillis` is not defined.

- [ ] **Step 3: Implement `TimeMath.kt`**

```kotlin
package com.attentionpet.domain

import kotlin.math.max
import kotlin.math.min

fun overlapMillis(
    sessionStart: Long,
    sessionEndOrNow: Long,
    windowStart: Long,
    windowEnd: Long
): Long {
    return max(0L, min(sessionEndOrNow, windowEnd) - max(sessionStart, windowStart))
}
```

- [ ] **Step 4: Add domain models**

```kotlin
package com.attentionpet.domain

enum class PetState(val serialized: String, val labelZh: String) {
    RELAXED("relaxed", "安全 / 放松"),
    REMINDER("reminder", "提醒"),
    TENSE("tense", "紧张"),
    TIMEOUT("timeout", "超时")
}

enum class RuleType {
    SESSION,
    ROLLING_WINDOW,
    DAILY
}

data class RuleConfig(
    val dailyLimitMillis: Long,
    val sessionLimitMillis: Long,
    val rollingWindowLimitMillis: Long,
    val rollingWindowMillis: Long = 5L * 60L * 60L * 1000L,
    val sessionGraceMillis: Long = 30_000L
) {
    init {
        require(dailyLimitMillis > 0L) { "dailyLimitMillis must be positive" }
        require(sessionLimitMillis > 0L) { "sessionLimitMillis must be positive" }
        require(rollingWindowLimitMillis > 0L) { "rollingWindowLimitMillis must be positive" }
        require(rollingWindowMillis == 5L * 60L * 60L * 1000L) { "rolling window is fixed to 5 hours for MVP" }
        require(sessionGraceMillis == 30_000L) { "session grace is fixed to 30 seconds for MVP" }
    }
}

data class UsageInterval(
    val startMillis: Long,
    val endMillis: Long
)

data class ActiveSession(
    val startMillis: Long,
    val foregroundMillis: Long
)

data class ExtensionGrant(
    val addedMillis: Long = 0L,
    val consumedForegroundMillis: Long = 0L
) {
    val remainingMillis: Long
        get() = (addedMillis - consumedForegroundMillis).coerceAtLeast(0L)
}

data class RuleBucket(
    val type: RuleType,
    val usedMillis: Long,
    val limitMillis: Long,
    val rawRemainingMillis: Long,
    val effectiveRemainingMillis: Long,
    val rawRemainingRatio: Float,
    val effectiveRemainingRatio: Float
)

data class RuleEvaluationResult(
    val daily: RuleBucket,
    val session: RuleBucket,
    val rollingWindow: RuleBucket,
    val triggeringRule: RuleType,
    val effectiveRemainingMillis: Long,
    val effectiveRemainingRatio: Float,
    val petState: PetState,
    val statusCopy: String,
    val activeExtensionRemainingMillis: Long
)
```

- [ ] **Step 5: Write failing `RuleEvaluatorTest`**

```kotlin
package com.attentionpet.domain

import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class RuleEvaluatorTest {
    private val zone = ZoneId.of("Asia/Shanghai")
    private val now = Instant.parse("2026-07-06T04:00:00Z").toEpochMilli()
    private val config = RuleConfig(
        dailyLimitMillis = 60 * 60_000L,
        sessionLimitMillis = 15 * 60_000L,
        rollingWindowLimitMillis = 30 * 60_000L
    )

    @Test
    fun relaxedWhenAllRulesHaveMoreThanHalfRemaining() {
        val result = RuleEvaluator.evaluate(
            nowMillis = now,
            zoneId = zone,
            config = config,
            sessions = listOf(UsageInterval(now - 10 * 60_000L, now - 5 * 60_000L)),
            activeSession = ActiveSession(startMillis = now - 2 * 60_000L, foregroundMillis = 2 * 60_000L),
            extensionGrant = ExtensionGrant()
        )
        assertEquals(PetState.RELAXED, result.petState)
        assertEquals(RuleType.SESSION, result.triggeringRule)
    }

    @Test
    fun timeoutWhenAnyRuleIsExceeded() {
        val result = RuleEvaluator.evaluate(
            nowMillis = now,
            zoneId = zone,
            config = config,
            sessions = emptyList(),
            activeSession = ActiveSession(startMillis = now - 16 * 60_000L, foregroundMillis = 16 * 60_000L),
            extensionGrant = ExtensionGrant()
        )
        assertEquals(PetState.TIMEOUT, result.petState)
        assertEquals(RuleType.SESSION, result.triggeringRule)
    }

    @Test
    fun extensionAppliesToEffectiveRemainingAcrossBuckets() {
        val result = RuleEvaluator.evaluate(
            nowMillis = now,
            zoneId = zone,
            config = config,
            sessions = emptyList(),
            activeSession = ActiveSession(startMillis = now - 16 * 60_000L, foregroundMillis = 16 * 60_000L),
            extensionGrant = ExtensionGrant(addedMillis = 5 * 60_000L, consumedForegroundMillis = 0L)
        )
        assertEquals(PetState.TENSE, result.petState)
        assertEquals(4 * 60_000L, result.effectiveRemainingMillis)
    }
}
```

- [ ] **Step 6: Run tests to verify they fail**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests com.attentionpet.domain.RuleEvaluatorTest
```

Expected: FAIL because `RuleEvaluator` is not defined.

- [ ] **Step 7: Implement `RuleEvaluator.kt`**

```kotlin
package com.attentionpet.domain

import java.time.Instant
import java.time.ZoneId

object RuleEvaluator {
    fun evaluate(
        nowMillis: Long,
        zoneId: ZoneId,
        config: RuleConfig,
        sessions: List<UsageInterval>,
        activeSession: ActiveSession?,
        extensionGrant: ExtensionGrant
    ): RuleEvaluationResult {
        val activeExtensionRemaining = extensionGrant.remainingMillis
        val startOfDay = Instant.ofEpochMilli(nowMillis)
            .atZone(zoneId)
            .toLocalDate()
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()
        val rollingStart = nowMillis - config.rollingWindowMillis

        val dailyUsed = sessions.sumOf { overlapMillis(it.startMillis, it.endMillis, startOfDay, nowMillis) } +
            activeSessionOverlap(activeSession, startOfDay, nowMillis)
        val rollingUsed = sessions.sumOf { overlapMillis(it.startMillis, it.endMillis, rollingStart, nowMillis) } +
            activeSessionOverlap(activeSession, rollingStart, nowMillis)
        val sessionUsed = activeSession?.foregroundMillis ?: 0L

        val session = bucket(RuleType.SESSION, sessionUsed, config.sessionLimitMillis, activeExtensionRemaining)
        val rolling = bucket(RuleType.ROLLING_WINDOW, rollingUsed, config.rollingWindowLimitMillis, activeExtensionRemaining)
        val daily = bucket(RuleType.DAILY, dailyUsed, config.dailyLimitMillis, activeExtensionRemaining)
        val ordered = listOf(session, rolling, daily)
        val triggering = ordered.minWith(compareBy<RuleBucket> { it.effectiveRemainingRatio }.thenBy { tieRank(it.type) })
        val state = when {
            ordered.any { it.effectiveRemainingMillis <= 0L } -> PetState.TIMEOUT
            triggering.effectiveRemainingRatio < 0.20f -> PetState.TENSE
            triggering.effectiveRemainingRatio <= 0.50f -> PetState.REMINDER
            else -> PetState.RELAXED
        }

        return RuleEvaluationResult(
            daily = daily,
            session = session,
            rollingWindow = rolling,
            triggeringRule = triggering.type,
            effectiveRemainingMillis = triggering.effectiveRemainingMillis,
            effectiveRemainingRatio = triggering.effectiveRemainingRatio,
            petState = state,
            statusCopy = statusCopy(state),
            activeExtensionRemainingMillis = activeExtensionRemaining
        )
    }

    private fun activeSessionOverlap(activeSession: ActiveSession?, windowStart: Long, windowEnd: Long): Long {
        if (activeSession == null) return 0L
        return overlapMillis(activeSession.startMillis, windowEnd, windowStart, windowEnd)
            .coerceAtMost(activeSession.foregroundMillis)
    }

    private fun bucket(type: RuleType, usedMillis: Long, limitMillis: Long, extensionMillis: Long): RuleBucket {
        val rawRemaining = limitMillis - usedMillis
        val effectiveRemaining = rawRemaining + extensionMillis
        return RuleBucket(
            type = type,
            usedMillis = usedMillis,
            limitMillis = limitMillis,
            rawRemainingMillis = rawRemaining,
            effectiveRemainingMillis = effectiveRemaining,
            rawRemainingRatio = rawRemaining.toFloat() / limitMillis.toFloat(),
            effectiveRemainingRatio = effectiveRemaining.toFloat() / limitMillis.toFloat()
        )
    }

    private fun tieRank(type: RuleType): Int = when (type) {
        RuleType.SESSION -> 0
        RuleType.ROLLING_WINDOW -> 1
        RuleType.DAILY -> 2
    }

    private fun statusCopy(state: PetState): String = when (state) {
        PetState.RELAXED -> "还很充裕，小鸟在旁边陪你。"
        PetState.REMINDER -> "还可以看一会儿，留意时间边界。"
        PetState.TENSE -> "快到边界了，小鸟会更明显一点。"
        PetState.TIMEOUT -> "已经超时啦，点一下可以选择休息或加 5 分钟。"
    }
}
```

- [ ] **Step 8: Run domain tests**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests com.attentionpet.domain.*
```

Expected: PASS.

- [ ] **Step 9: Commit**

```powershell
git add app/src/main/java/com/attentionpet/domain app/src/test/java/com/attentionpet/domain
git commit -m "feat: add rule evaluator"
```

Expected: commit succeeds if Git is available.

---

### Task 3: Room Persistence and Repository

**Files:**
- Create: `app/src/main/java/com/attentionpet/data/entities.kt`
- Create: `app/src/main/java/com/attentionpet/data/ConfigDao.kt`
- Create: `app/src/main/java/com/attentionpet/data/SessionDao.kt`
- Create: `app/src/main/java/com/attentionpet/data/EventDao.kt`
- Create: `app/src/main/java/com/attentionpet/data/AttentionPetDatabase.kt`
- Create: `app/src/main/java/com/attentionpet/data/AttentionPetRepository.kt`

**Interfaces:**
- Produces: `AttentionPetRepository` with suspend functions `config()`, `saveTargetApp(...)`, `saveLimits(...)`, `openSession(...)`, `closeSession(...)`, `recordExtension(...)`, `recordTimeoutAction(...)`.
- Consumes: Domain `RuleConfig`, `UsageInterval`, `ExtensionGrant`.

- [ ] **Step 1: Create Room entities**

```kotlin
package com.attentionpet.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "target_app_config")
data class TargetAppConfigEntity(
    @PrimaryKey val id: Int = 1,
    val packageName: String,
    val displayName: String,
    val enabled: Boolean
)

@Entity(tableName = "limit_config")
data class LimitConfigEntity(
    @PrimaryKey val id: Int = 1,
    val dailyLimitMinutes: Int,
    val sessionLimitMinutes: Int,
    val rollingWindowHours: Int,
    val rollingWindowLimitMinutes: Int
)

@Entity(tableName = "usage_session")
data class UsageSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val packageName: String,
    val startMillis: Long,
    val endMillis: Long?,
    val foregroundDurationMillis: Long,
    val closeReason: String
)

@Entity(tableName = "extension_event")
data class ExtensionEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val sessionId: Long,
    val timestampMillis: Long,
    val addedMinutes: Int,
    val consumedForegroundMillis: Long
)

@Entity(tableName = "timeout_action_event")
data class TimeoutActionEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val sessionId: Long,
    val timestampMillis: Long,
    val actionType: String,
    val overageDurationMillis: Long
)

@Entity(tableName = "overlay_position")
data class OverlayPositionEntity(
    @PrimaryKey val id: Int = 1,
    val edge: String,
    val verticalRatio: Float,
    val updatedAtMillis: Long
)
```

- [ ] **Step 2: Create DAOs**

`ConfigDao.kt`:

```kotlin
package com.attentionpet.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ConfigDao {
    @Query("SELECT * FROM target_app_config WHERE id = 1")
    fun targetApp(): Flow<TargetAppConfigEntity?>

    @Query("SELECT * FROM limit_config WHERE id = 1")
    fun limits(): Flow<LimitConfigEntity?>

    @Upsert
    suspend fun upsertTargetApp(entity: TargetAppConfigEntity)

    @Upsert
    suspend fun upsertLimits(entity: LimitConfigEntity)
}
```

`SessionDao.kt`:

```kotlin
package com.attentionpet.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface SessionDao {
    @Insert
    suspend fun insert(entity: UsageSessionEntity): Long

    @Update
    suspend fun update(entity: UsageSessionEntity)

    @Query("SELECT * FROM usage_session WHERE id = :id")
    suspend fun getById(id: Long): UsageSessionEntity?

    @Query("SELECT * FROM usage_session WHERE packageName = :packageName AND startMillis < :windowEnd AND COALESCE(endMillis, :windowEnd) > :windowStart")
    suspend fun sessionsOverlapping(packageName: String, windowStart: Long, windowEnd: Long): List<UsageSessionEntity>
}
```

`EventDao.kt`:

```kotlin
package com.attentionpet.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface EventDao {
    @Insert
    suspend fun insertExtension(entity: ExtensionEventEntity): Long

    @Query("SELECT * FROM extension_event WHERE sessionId = :sessionId")
    suspend fun extensionForSession(sessionId: Long): ExtensionEventEntity?

    @Insert
    suspend fun insertTimeoutAction(entity: TimeoutActionEventEntity): Long

    @Query("SELECT * FROM overlay_position WHERE id = 1")
    suspend fun overlayPosition(): OverlayPositionEntity?

    @Upsert
    suspend fun upsertOverlayPosition(entity: OverlayPositionEntity)
}
```

- [ ] **Step 3: Create database**

```kotlin
package com.attentionpet.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        TargetAppConfigEntity::class,
        LimitConfigEntity::class,
        UsageSessionEntity::class,
        ExtensionEventEntity::class,
        TimeoutActionEventEntity::class,
        OverlayPositionEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AttentionPetDatabase : RoomDatabase() {
    abstract fun configDao(): ConfigDao
    abstract fun sessionDao(): SessionDao
    abstract fun eventDao(): EventDao
}
```

- [ ] **Step 4: Create repository skeleton**

```kotlin
package com.attentionpet.data

import com.attentionpet.domain.ExtensionGrant
import com.attentionpet.domain.RuleConfig
import com.attentionpet.domain.UsageInterval
import kotlinx.coroutines.flow.Flow

class AttentionPetRepository(
    private val configDao: ConfigDao,
    private val sessionDao: SessionDao,
    private val eventDao: EventDao
) {
    fun targetApp(): Flow<TargetAppConfigEntity?> = configDao.targetApp()
    fun limits(): Flow<LimitConfigEntity?> = configDao.limits()

    suspend fun saveTargetApp(packageName: String, displayName: String, enabled: Boolean) {
        configDao.upsertTargetApp(TargetAppConfigEntity(packageName = packageName, displayName = displayName, enabled = enabled))
    }

    suspend fun saveLimits(dailyMinutes: Int, sessionMinutes: Int, rollingWindowLimitMinutes: Int) {
        configDao.upsertLimits(
            LimitConfigEntity(
                dailyLimitMinutes = dailyMinutes,
                sessionLimitMinutes = sessionMinutes,
                rollingWindowHours = 5,
                rollingWindowLimitMinutes = rollingWindowLimitMinutes
            )
        )
    }

    fun LimitConfigEntity.toRuleConfig(): RuleConfig {
        return RuleConfig(
            dailyLimitMillis = dailyLimitMinutes * 60_000L,
            sessionLimitMillis = sessionLimitMinutes * 60_000L,
            rollingWindowLimitMillis = rollingWindowLimitMinutes * 60_000L
        )
    }

    fun UsageSessionEntity.toUsageInterval(nowMillis: Long): UsageInterval {
        return UsageInterval(startMillis = startMillis, endMillis = endMillis ?: nowMillis)
    }

    suspend fun openSession(packageName: String, startMillis: Long): Long {
        return sessionDao.insert(
            UsageSessionEntity(
                packageName = packageName,
                startMillis = startMillis,
                endMillis = null,
                foregroundDurationMillis = 0L,
                closeReason = "active"
            )
        )
    }

    suspend fun closeSession(sessionId: Long, endMillis: Long, foregroundDurationMillis: Long, closeReason: String) {
        val existing = sessionDao.getById(sessionId) ?: return
        sessionDao.update(existing.copy(endMillis = endMillis, foregroundDurationMillis = foregroundDurationMillis, closeReason = closeReason))
    }

    suspend fun recordExtension(sessionId: Long, timestampMillis: Long): Long {
        val existing = eventDao.extensionForSession(sessionId)
        if (existing != null) return existing.id
        return eventDao.insertExtension(
            ExtensionEventEntity(
                sessionId = sessionId,
                timestampMillis = timestampMillis,
                addedMinutes = 5,
                consumedForegroundMillis = 0L
            )
        )
    }

    suspend fun extensionGrant(sessionId: Long): ExtensionGrant {
        val event = eventDao.extensionForSession(sessionId) ?: return ExtensionGrant()
        return ExtensionGrant(addedMillis = event.addedMinutes * 60_000L, consumedForegroundMillis = event.consumedForegroundMillis)
    }

    suspend fun recordTimeoutAction(sessionId: Long, timestampMillis: Long, actionType: String, overageDurationMillis: Long) {
        eventDao.insertTimeoutAction(TimeoutActionEventEntity(sessionId = sessionId, timestampMillis = timestampMillis, actionType = actionType, overageDurationMillis = overageDurationMillis))
    }
}
```

- [ ] **Step 5: Build Room code**

Run:

```powershell
.\gradlew.bat :app:kspDebugKotlin :app:compileDebugKotlin
```

Expected: PASS.

- [ ] **Step 6: Commit**

```powershell
git add app/src/main/java/com/attentionpet/data app/schemas
git commit -m "feat: add local persistence"
```

Expected: commit succeeds if Git is available.

---

### Task 4: Permission State and Home Configuration UI

**Files:**
- Create: `app/src/main/java/com/attentionpet/permissions/PermissionState.kt`
- Create: `app/src/main/java/com/attentionpet/ui/Theme.kt`
- Create: `app/src/main/java/com/attentionpet/ui/HomeScreen.kt`
- Create: `app/src/main/java/com/attentionpet/ui/AppPicker.kt`
- Create: `app/src/main/java/com/attentionpet/ui/MainViewModel.kt`
- Modify: `app/src/main/java/com/attentionpet/MainActivity.kt`

**Interfaces:**
- Produces: `PermissionSnapshot(usageAccessGranted, overlayGranted)`.
- Produces: `HomeScreen(...)` with permission cards, start CTA, one target app, and three rule controls.
- Consumes: Repository from Task 3.

- [ ] **Step 1: Implement permission helpers**

```kotlin
package com.attentionpet.permissions

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Process
import android.provider.Settings

data class PermissionSnapshot(
    val usageAccessGranted: Boolean,
    val overlayGranted: Boolean
) {
    val canStartMonitoring: Boolean = usageAccessGranted && overlayGranted
}

object PermissionState {
    fun snapshot(context: Context): PermissionSnapshot {
        return PermissionSnapshot(
            usageAccessGranted = hasUsageAccess(context),
            overlayGranted = Settings.canDrawOverlays(context)
        )
    }

    fun usageAccessSettingsIntent(): Intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)

    fun overlaySettingsIntent(context: Context): Intent {
        return Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
    }

    private fun hasUsageAccess(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }
}
```

- [ ] **Step 2: Implement theme**

```kotlin
package com.attentionpet.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AttentionColors = lightColorScheme(
    primary = Color(0xFF24383F),
    secondary = Color(0xFF45D6A1),
    tertiary = Color(0xFFFFD86F),
    background = Color(0xFFF3F8F7),
    surface = Color.White,
    error = Color(0xFFF45E63)
)

@Composable
fun AttentionPetTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = AttentionColors, content = content)
}
```

- [ ] **Step 3: Implement `HomeScreen`**

```kotlin
package com.attentionpet.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.attentionpet.permissions.PermissionSnapshot

data class HomeUiState(
    val permissionSnapshot: PermissionSnapshot,
    val targetAppLabel: String,
    val dailyLimitMinutes: Int,
    val sessionLimitMinutes: Int,
    val rollingWindowLimitMinutes: Int
)

@Composable
fun HomeScreen(
    state: HomeUiState,
    onOpenUsageAccess: () -> Unit,
    onOpenOverlayPermission: () -> Unit,
    onPickTargetApp: () -> Unit,
    onStartMonitoring: () -> Unit,
    onDailyChanged: (Int) -> Unit,
    onSessionChanged: (Int) -> Unit,
    onRollingChanged: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(18.dp)
    ) {
        Text("Attention Pet", style = MaterialTheme.typography.headlineMedium)
        Text("今天的小鸟还很放松", color = MaterialTheme.colorScheme.secondary)
        Spacer(Modifier.height(16.dp))
        PermissionCards(state.permissionSnapshot, onOpenUsageAccess, onOpenOverlayPermission)
        Spacer(Modifier.height(10.dp))
        Button(
            modifier = Modifier.fillMaxWidth(),
            enabled = state.permissionSnapshot.canStartMonitoring,
            onClick = onStartMonitoring
        ) {
            Text("开始守护")
        }
        Spacer(Modifier.height(12.dp))
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp)) {
                Row {
                    Text("受限 App", modifier = Modifier.weight(1f))
                    OutlinedButton(onClick = onPickTargetApp) { Text("更换") }
                }
                Text(state.targetAppLabel)
            }
        }
        RuleSlider("每日总使用限制", state.dailyLimitMinutes, 10, 180, onDailyChanged)
        RuleSlider("单次连续使用限制", state.sessionLimitMinutes, 5, 60, onSessionChanged)
        RuleSlider("过去 5 小时窗口", state.rollingWindowLimitMinutes, 5, 120, onRollingChanged)
    }
}

@Composable
private fun PermissionCards(snapshot: PermissionSnapshot, onOpenUsageAccess: () -> Unit, onOpenOverlayPermission: () -> Unit) {
    Row(Modifier.fillMaxWidth()) {
        PermissionCard("使用情况权限", snapshot.usageAccessGranted, "开启使用情况权限", onOpenUsageAccess, Modifier.weight(1f))
        Spacer(Modifier.padding(4.dp))
        PermissionCard("悬浮窗权限", snapshot.overlayGranted, "开启悬浮窗权限", onOpenOverlayPermission, Modifier.weight(1f))
    }
}

@Composable
private fun PermissionCard(title: String, granted: Boolean, cta: String, onClick: () -> Unit, modifier: Modifier) {
    Card(modifier) {
        Column(Modifier.padding(10.dp)) {
            Text(title)
            if (granted) {
                Text("已开启", color = MaterialTheme.colorScheme.secondary)
            } else {
                OutlinedButton(onClick = onClick) { Text(cta) }
            }
        }
    }
}

@Composable
private fun RuleSlider(label: String, value: Int, min: Int, max: Int, onChanged: (Int) -> Unit) {
    Column(Modifier.padding(top = 12.dp)) {
        Text("$label  $value 分钟")
        Slider(
            value = value.toFloat(),
            valueRange = min.toFloat()..max.toFloat(),
            onValueChange = { onChanged(it.toInt().coerceIn(min, max)) }
        )
    }
}
```

- [ ] **Step 4: Implement launchable app picker helper**

```kotlin
package com.attentionpet.ui

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

data class LaunchableApp(
    val packageName: String,
    val label: String
)

object AppPicker {
    fun launchableApps(context: Context): List<LaunchableApp> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            .map {
                LaunchableApp(
                    packageName = it.activityInfo.packageName,
                    label = it.loadLabel(context.packageManager).toString()
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }
}
```

- [ ] **Step 5: Wire `MainActivity` to `HomeScreen`**

Replace `MainActivity.kt` with:

```kotlin
package com.attentionpet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.attentionpet.permissions.PermissionSnapshot
import com.attentionpet.permissions.PermissionState
import com.attentionpet.ui.AttentionPetTheme
import com.attentionpet.ui.HomeScreen
import com.attentionpet.ui.HomeUiState

class MainActivity : ComponentActivity() {
    private var permissionSnapshot by mutableStateOf(PermissionSnapshot(false, false))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        render()
    }

    override fun onResume() {
        super.onResume()
        permissionSnapshot = PermissionState.snapshot(this)
    }

    private fun render() {
        setContent {
            AttentionPetTheme {
                HomeScreen(
                    state = HomeUiState(
                        permissionSnapshot = permissionSnapshot,
                        targetAppLabel = "抖音",
                        dailyLimitMinutes = 60,
                        sessionLimitMinutes = 15,
                        rollingWindowLimitMinutes = 30
                    ),
                    onOpenUsageAccess = { startActivity(PermissionState.usageAccessSettingsIntent()) },
                    onOpenOverlayPermission = { startActivity(PermissionState.overlaySettingsIntent(this)) },
                    onPickTargetApp = {},
                    onStartMonitoring = {},
                    onDailyChanged = {},
                    onSessionChanged = {},
                    onRollingChanged = {}
                )
            }
        }
    }
}
```

- [ ] **Step 6: Build UI**

Run:

```powershell
.\gradlew.bat :app:compileDebugKotlin
```

Expected: PASS.

- [ ] **Step 7: Commit**

```powershell
git add app/src/main/java/com/attentionpet/permissions app/src/main/java/com/attentionpet/ui app/src/main/java/com/attentionpet/MainActivity.kt
git commit -m "feat: add configuration UI"
```

Expected: commit succeeds if Git is available.

---

### Task 5: A-Bird Pet Drawing and Overlay Compose Surfaces

**Files:**
- Create: `app/src/main/java/com/attentionpet/pet/BirdPet.kt`
- Create: `app/src/main/java/com/attentionpet/overlay/OverlayViews.kt`

**Interfaces:**
- Produces: `BirdPet(state: PetState, modifier: Modifier)`.
- Produces: `CapsuleOverlay`, `ExpandedPanelOverlay`, `TimeoutSheetOverlay`.
- Consumes: `PetState`, `RuleEvaluationResult`.

- [ ] **Step 1: Implement A-bird drawing**

```kotlin
package com.attentionpet.pet

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import com.attentionpet.domain.PetState

@Composable
fun BirdPet(state: PetState, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawBird(state)
    }
}

private fun DrawScope.drawBird(state: PetState) {
    val body = when (state) {
        PetState.RELAXED -> Color(0xFF62DDB3)
        PetState.REMINDER -> Color(0xFF9BE391)
        PetState.TENSE -> Color(0xFFFFD76F)
        PetState.TIMEOUT -> Color(0xFFFF9A7B)
    }
    val wing = when (state) {
        PetState.RELAXED -> Color(0xFF44C79A)
        PetState.REMINDER -> Color(0xFF77D873)
        PetState.TENSE -> Color(0xFFFFBD58)
        PetState.TIMEOUT -> Color(0xFFF45E63)
    }
    val scale = size.minDimension / 82f
    fun x(value: Float) = value * scale
    drawOval(body, topLeft = Offset(x(13f), x(13f)), size = Size(x(56f), x(58f)))
    rotate(-18f, pivot = Offset(x(42f), x(12f))) {
        drawOval(wing, topLeft = Offset(x(36f), x(5f)), size = Size(x(11f), x(14f)))
    }
    rotate(if (state == PetState.TIMEOUT) -54f else -13f, pivot = Offset(x(23f), x(50f))) {
        drawOval(wing, topLeft = Offset(x(9f), x(39f)), size = Size(x(27f), x(22f)))
    }
    drawOval(Color.White.copy(alpha = 0.54f), topLeft = Offset(x(29f), x(44f)), size = Size(x(25f), x(17f)))
    drawCircle(Color(0xFF21323A), radius = x(3f), center = Offset(x(38.5f), x(37f)))
    drawCircle(Color(0xFF21323A), radius = x(3f), center = Offset(x(53.5f), x(37f)))
    val beak = Path().apply {
        moveTo(x(44f), x(41f))
        lineTo(x(55f), x(46f))
        lineTo(x(44f), x(51f))
        close()
    }
    drawPath(beak, Color(0xFFFFD05B))
    drawOval(Color(0xFFFF7E70).copy(alpha = 0.38f), topLeft = Offset(x(30f), x(46f)), size = Size(x(7f), x(4f)))
    drawOval(Color(0xFFFF7E70).copy(alpha = 0.38f), topLeft = Offset(x(55f), x(46f)), size = Size(x(7f), x(4f)))
    drawLine(Color(0xFFEFB45A), start = Offset(x(31f), x(75f)), end = Offset(x(51f), x(75f)), strokeWidth = x(3f))
}
```

- [ ] **Step 2: Implement overlay views**

```kotlin
package com.attentionpet.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.attentionpet.domain.PetState
import com.attentionpet.domain.RuleEvaluationResult
import com.attentionpet.pet.BirdPet

@Composable
fun CapsuleOverlay(result: RuleEvaluationResult, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val color = stateColor(result.petState).copy(alpha = 0.78f)
    Row(
        modifier = modifier
            .background(color, RoundedCornerShape(topEnd = 999.dp, bottomEnd = 999.dp))
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BirdPet(result.petState, Modifier.size(28.dp))
        Column(Modifier.padding(start = 6.dp)) {
            Text(displayRemaining(result), style = MaterialTheme.typography.labelMedium)
            LinearProgressIndicator(
                progress = { result.effectiveRemainingRatio.coerceIn(0f, 1f) },
                modifier = Modifier.size(width = 56.dp, height = 5.dp),
                color = progressColor(result.petState),
                trackColor = Color(0x3320323C)
            )
        }
    }
}

@Composable
fun ExpandedPanelOverlay(result: RuleEvaluationResult, currentSessionText: String, onDismiss: () -> Unit) {
    Column(
        modifier = Modifier
            .background(Color.White.copy(alpha = 0.92f), RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BirdPet(result.petState, Modifier.size(44.dp))
            Column(Modifier.padding(start = 10.dp)) {
                Text("小鸟在旁边陪你看着时间")
                Text(result.statusCopy, style = MaterialTheme.typography.bodySmall)
            }
        }
        Spacer(Modifier.height(10.dp))
        Text("当前连续使用 $currentSessionText")
    }
}

@Composable
fun TimeoutSheetOverlay(onRest: () -> Unit, onExtend: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFFFAFA), RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BirdPet(PetState.TIMEOUT, Modifier.size(84.dp))
        Text("已经超时啦", style = MaterialTheme.typography.headlineSmall)
        Text("要不要休息一下？")
        Spacer(Modifier.height(18.dp))
        Row {
            Button(onClick = onRest, modifier = Modifier.weight(1f)) { Text("休息一下") }
            Spacer(Modifier.padding(5.dp))
            Button(onClick = onExtend, modifier = Modifier.weight(1f)) { Text("再加 5 分钟") }
        }
    }
}

private fun displayRemaining(result: RuleEvaluationResult): String {
    val minutes = kotlin.math.abs(result.effectiveRemainingMillis / 60_000L)
    return if (result.petState == PetState.TIMEOUT) "+${minutes}m" else "${minutes}m"
}

private fun stateColor(state: PetState): Color = when (state) {
    PetState.RELAXED -> Color(0xFFE8FFF5)
    PetState.REMINDER -> Color(0xFFFFF7D2)
    PetState.TENSE -> Color(0xFFFFF1D8)
    PetState.TIMEOUT -> Color(0xFFFFE2E2)
}

private fun progressColor(state: PetState): Color = when (state) {
    PetState.RELAXED -> Color(0xFF45D6A1)
    PetState.REMINDER -> Color(0xFFFFD86F)
    PetState.TENSE -> Color(0xFFFF9A5E)
    PetState.TIMEOUT -> Color(0xFFF45E63)
}
```

- [ ] **Step 3: Build Compose surfaces**

Run:

```powershell
.\gradlew.bat :app:compileDebugKotlin
```

Expected: PASS.

- [ ] **Step 4: Commit**

```powershell
git add app/src/main/java/com/attentionpet/pet app/src/main/java/com/attentionpet/overlay/OverlayViews.kt
git commit -m "feat: add pet overlay views"
```

Expected: commit succeeds if Git is available.

---

### Task 6: WindowManager Overlay Controller

**Files:**
- Create: `app/src/main/java/com/attentionpet/overlay/OverlaySafeBounds.kt`
- Create: `app/src/main/java/com/attentionpet/overlay/OverlayPositionStore.kt`
- Create: `app/src/main/java/com/attentionpet/overlay/OverlayController.kt`

**Interfaces:**
- Produces: `OverlayController.showCapsule(result)`, `showPanel(result)`, `showTimeoutSheet()`, `hideAll()`.
- Consumes: `OverlayViews` from Task 5 and `RuleEvaluationResult` from Task 2.

- [ ] **Step 1: Implement safe bounds helper**

```kotlin
package com.attentionpet.overlay

import android.content.Context
import android.graphics.Rect
import androidx.core.graphics.Insets
import androidx.core.view.WindowInsetsCompat
import androidx.window.layout.WindowMetricsCalculator

data class OverlaySafeBounds(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
) {
    val height: Int = bottom - top
}

fun overlaySafeBounds(context: Context, systemInsets: Insets = Insets.NONE, cutoutInsets: Insets = Insets.NONE): OverlaySafeBounds {
    val bounds: Rect = WindowMetricsCalculator.getOrCreate().computeMaximumWindowMetrics(context).bounds
    return OverlaySafeBounds(
        left = bounds.left + maxOf(systemInsets.left, cutoutInsets.left),
        top = bounds.top + maxOf(systemInsets.top, cutoutInsets.top),
        right = bounds.right - maxOf(systemInsets.right, cutoutInsets.right),
        bottom = bounds.bottom - maxOf(systemInsets.bottom, cutoutInsets.bottom)
    )
}
```

- [ ] **Step 2: Implement overlay position store**

```kotlin
package com.attentionpet.overlay

import android.content.Context

enum class OverlayEdge { LEFT, RIGHT }

data class OverlayPosition(
    val edge: OverlayEdge = OverlayEdge.LEFT,
    val verticalRatio: Float = 0.22f
)

class OverlayPositionStore(context: Context) {
    private val prefs = context.getSharedPreferences("overlay_position", Context.MODE_PRIVATE)

    fun load(): OverlayPosition {
        val edge = if (prefs.getString("edge", "LEFT") == "RIGHT") OverlayEdge.RIGHT else OverlayEdge.LEFT
        val ratio = prefs.getFloat("verticalRatio", 0.22f).coerceIn(0f, 1f)
        return OverlayPosition(edge, ratio)
    }

    fun save(position: OverlayPosition) {
        prefs.edit()
            .putString("edge", position.edge.name)
            .putFloat("verticalRatio", position.verticalRatio.coerceIn(0f, 1f))
            .apply()
    }
}
```

- [ ] **Step 3: Implement `OverlayController` skeleton with exact flags**

```kotlin
package com.attentionpet.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import com.attentionpet.domain.RuleEvaluationResult

class OverlayController(private val context: Context) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val positionStore = OverlayPositionStore(context)
    private var capsuleView: View? = null
    private var panelView: View? = null
    private var sheetView: View? = null

    fun showCapsule(result: RuleEvaluationResult, onClick: () -> Unit) {
        val view = capsuleView ?: composeView().also {
            capsuleView = it
            windowManager.addView(it, capsuleParams())
        }
        (view as ComposeView).setContent {
            CapsuleOverlay(result = result, onClick = onClick)
        }
        view.setOnClickListener { onClick() }
    }

    fun showPanel(result: RuleEvaluationResult, currentSessionText: String) {
        hidePanel()
        val view = composeView()
        panelView = view
        view.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_OUTSIDE) {
                hidePanel()
                true
            } else {
                false
            }
        }
        view.setContent {
            ExpandedPanelOverlay(result = result, currentSessionText = currentSessionText, onDismiss = ::hidePanel)
        }
        windowManager.addView(view, panelParams())
    }

    fun showTimeoutSheet(onRest: () -> Unit, onExtend: () -> Unit) {
        hidePanel()
        hideTimeoutSheet()
        val view = composeView()
        sheetView = view
        view.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_OUTSIDE) {
                hideTimeoutSheet()
                true
            } else {
                false
            }
        }
        view.setContent {
            TimeoutSheetOverlay(onRest = onRest, onExtend = onExtend)
        }
        windowManager.addView(view, timeoutSheetParams())
    }

    fun hidePanel() {
        panelView?.let { windowManager.removeView(it) }
        panelView = null
    }

    fun hideTimeoutSheet() {
        sheetView?.let { windowManager.removeView(it) }
        sheetView = null
    }

    fun hideAll() {
        hidePanel()
        hideTimeoutSheet()
        capsuleView?.let { windowManager.removeView(it) }
        capsuleView = null
    }

    private fun capsuleParams(): WindowManager.LayoutParams = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        x = 0
        y = 108
    }

    private fun panelParams(): WindowManager.LayoutParams = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        x = 0
        y = 108
    }

    private fun timeoutSheetParams(): WindowManager.LayoutParams = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.BOTTOM
    }

    private fun composeView(): ComposeView {
        return ComposeView(context).apply {
            val owner = OverlayLifecycleOwner()
            setViewTreeLifecycleOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            owner.handleResume()
        }
    }
}

private class OverlayLifecycleOwner : LifecycleOwner, SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateController = SavedStateRegistryController.create(this)

    init {
        savedStateController.performAttach()
        savedStateController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }

    override val lifecycle: Lifecycle = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry = savedStateController.savedStateRegistry

    fun handleResume() {
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
    }
}
```

- [ ] **Step 4: Build overlay code**

Run:

```powershell
.\gradlew.bat :app:compileDebugKotlin
```

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add app/src/main/java/com/attentionpet/overlay
git commit -m "feat: add overlay controller"
```

Expected: commit succeeds if Git is available.

---

### Task 7: Foreground App Detection and Session Tracker

**Files:**
- Create: `app/src/main/java/com/attentionpet/service/ForegroundAppDetector.kt`
- Create: `app/src/main/java/com/attentionpet/service/SessionTracker.kt`
- Create: `app/src/test/java/com/attentionpet/service/SessionTrackerTest.kt`

**Interfaces:**
- Produces: `ForegroundAppDetector.currentForegroundPackage(nowMillis): String?`.
- Produces: `SessionTracker.onForegroundSample(packageName, nowMillis): SessionTracker.State`.
- Consumes: Target package from repository.

- [ ] **Step 1: Implement `ForegroundAppDetector`**

```kotlin
package com.attentionpet.service

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context

class ForegroundAppDetector(context: Context) {
    private val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    fun currentForegroundPackage(nowMillis: Long): String? {
        val events = usageStatsManager.queryEvents(nowMillis - 10_000L, nowMillis)
        var current: String? = null
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            when (event.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND -> current = event.packageName
                UsageEvents.Event.MOVE_TO_BACKGROUND -> if (current == event.packageName) current = null
            }
        }
        return current
    }
}
```

- [ ] **Step 2: Write failing `SessionTrackerTest`**

```kotlin
package com.attentionpet.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SessionTrackerTest {
    @Test
    fun keepsSessionAcrossThirtySecondGrace() {
        val tracker = SessionTracker(targetPackage = "com.target")
        tracker.onForegroundSample("com.target", 0L)
        tracker.onForegroundSample(null, 10_000L)
        val state = tracker.onForegroundSample("com.target", 20_000L)
        assertEquals(20_000L, state.foregroundDurationMillis)
        assertEquals(SessionTracker.Status.ACTIVE, state.status)
    }

    @Test
    fun closesSessionAfterGrace() {
        val tracker = SessionTracker(targetPackage = "com.target")
        tracker.onForegroundSample("com.target", 0L)
        tracker.onForegroundSample(null, 40_000L)
        val state = tracker.onForegroundSample(null, 41_000L)
        assertEquals(SessionTracker.Status.CLOSED, state.status)
        assertNull(state.activeStartMillis)
    }
}
```

- [ ] **Step 3: Run tracker test to verify it fails**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests com.attentionpet.service.SessionTrackerTest
```

Expected: FAIL because `SessionTracker` is not defined.

- [ ] **Step 4: Implement `SessionTracker`**

```kotlin
package com.attentionpet.service

class SessionTracker(
    private val targetPackage: String,
    private val graceMillis: Long = 30_000L
) {
    enum class Status { IDLE, ACTIVE, GRACE, CLOSED }

    data class State(
        val status: Status,
        val activeStartMillis: Long?,
        val foregroundDurationMillis: Long,
        val closedAtMillis: Long?
    )

    private var activeStartMillis: Long? = null
    private var lastForegroundMillis: Long? = null
    private var foregroundDurationMillis: Long = 0L

    fun onForegroundSample(packageName: String?, nowMillis: Long): State {
        val isTarget = packageName == targetPackage
        if (isTarget) {
            if (activeStartMillis == null) activeStartMillis = nowMillis
            val last = lastForegroundMillis ?: nowMillis
            foregroundDurationMillis += (nowMillis - last).coerceAtLeast(0L)
            lastForegroundMillis = nowMillis
            return state(Status.ACTIVE, null)
        }

        val last = lastForegroundMillis
        if (activeStartMillis != null && last != null && nowMillis - last <= graceMillis) {
            return state(Status.GRACE, null)
        }

        if (activeStartMillis != null) {
            val closedAt = last ?: nowMillis
            activeStartMillis = null
            lastForegroundMillis = null
            return State(Status.CLOSED, null, foregroundDurationMillis, closedAt)
        }

        return state(Status.IDLE, null)
    }

    private fun state(status: Status, closedAt: Long?): State {
        return State(status, activeStartMillis, foregroundDurationMillis, closedAt)
    }
}
```

- [ ] **Step 5: Run tracker tests**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests com.attentionpet.service.SessionTrackerTest
```

Expected: PASS.

- [ ] **Step 6: Commit**

```powershell
git add app/src/main/java/com/attentionpet/service/ForegroundAppDetector.kt app/src/main/java/com/attentionpet/service/SessionTracker.kt app/src/test/java/com/attentionpet/service/SessionTrackerTest.kt
git commit -m "feat: add foreground session tracking"
```

Expected: commit succeeds if Git is available.

---

### Task 8: Foreground Service Integration

**Files:**
- Create: `app/src/main/java/com/attentionpet/service/AttentionMonitorService.kt`
- Modify: `app/src/main/java/com/attentionpet/MainActivity.kt`

**Interfaces:**
- Produces: `AttentionMonitorService.start(context)` and `AttentionMonitorService.stop(context)`.
- Consumes: `ForegroundAppDetector`, `SessionTracker`, `RuleEvaluator`, `OverlayController`.

- [ ] **Step 1: Implement foreground service skeleton**

```kotlin
package com.attentionpet.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.attentionpet.R
import com.attentionpet.domain.ActiveSession
import com.attentionpet.domain.ExtensionGrant
import com.attentionpet.domain.RuleConfig
import com.attentionpet.domain.RuleEvaluator
import com.attentionpet.overlay.OverlayController
import java.time.ZoneId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AttentionMonitorService : Service() {
    private val scope = CoroutineScope(Dispatchers.Main)
    private var job: Job? = null
    private lateinit var detector: ForegroundAppDetector
    private lateinit var overlayController: OverlayController
    private val targetPackage = "com.ss.android.ugc.aweme"
    private val config = RuleConfig(
        dailyLimitMillis = 60 * 60_000L,
        sessionLimitMillis = 15 * 60_000L,
        rollingWindowLimitMillis = 30 * 60_000L
    )

    override fun onCreate() {
        super.onCreate()
        detector = ForegroundAppDetector(this)
        overlayController = OverlayController(this)
        ensureChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(1, notification())
        val tracker = SessionTracker(targetPackage)
        job?.cancel()
        job = scope.launch {
            while (isActive) {
                val now = System.currentTimeMillis()
                val foreground = detector.currentForegroundPackage(now)
                val state = tracker.onForegroundSample(foreground, now)
                if (state.activeStartMillis != null) {
                    val result = RuleEvaluator.evaluate(
                        nowMillis = now,
                        zoneId = ZoneId.systemDefault(),
                        config = config,
                        sessions = emptyList(),
                        activeSession = ActiveSession(state.activeStartMillis, state.foregroundDurationMillis),
                        extensionGrant = ExtensionGrant()
                    )
                    overlayController.showCapsule(result) {
                        if (result.petState.name == "TIMEOUT") {
                            overlayController.showTimeoutSheet(onRest = { overlayController.hideTimeoutSheet() }, onExtend = { overlayController.hideTimeoutSheet() })
                        } else {
                            overlayController.showPanel(result, "${state.foregroundDurationMillis / 60_000L}m")
                        }
                    }
                } else {
                    overlayController.hideAll()
                }
                delay(1_000L)
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        job?.cancel()
        overlayController.hideAll()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun ensureChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, "Attention Pet", NotificationManager.IMPORTANCE_LOW))
    }

    private fun notification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Attention Pet 正在守护时间边界")
            .setContentText("小鸟会在目标 App 打开时出现在屏幕边缘")
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "attention_pet_monitor"

        fun start(context: Context) {
            context.startForegroundService(Intent(context, AttentionMonitorService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, AttentionMonitorService::class.java))
        }
    }
}
```

- [ ] **Step 2: Wire start CTA**

In `MainActivity.kt`, set `onStartMonitoring = { AttentionMonitorService.start(this) }` and add import:

```kotlin
import com.attentionpet.service.AttentionMonitorService
```

- [ ] **Step 3: Build service**

Run:

```powershell
.\gradlew.bat :app:compileDebugKotlin
```

Expected: PASS.

- [ ] **Step 4: Manual smoke test on device**

Run:

```powershell
.\gradlew.bat :app:installDebug
```

Expected: Install succeeds on connected device. If `adb` is unavailable, record that device smoke test is blocked and continue with unit/build verification.

- [ ] **Step 5: Commit**

```powershell
git add app/src/main/java/com/attentionpet/service/AttentionMonitorService.kt app/src/main/java/com/attentionpet/MainActivity.kt app/src/main/AndroidManifest.xml
git commit -m "feat: wire monitoring service"
```

Expected: commit succeeds if Git is available.

---

### Task 9: Repository-Backed Integration and Acceptance Hardening

**Files:**
- Modify: `app/src/main/java/com/attentionpet/AttentionPetApp.kt`
- Modify: `app/src/main/java/com/attentionpet/ui/MainViewModel.kt`
- Modify: `app/src/main/java/com/attentionpet/MainActivity.kt`
- Modify: `app/src/main/java/com/attentionpet/service/AttentionMonitorService.kt`
- Modify: `app/src/main/java/com/attentionpet/data/AttentionPetRepository.kt`

**Interfaces:**
- Produces: In-memory app container with database and repository.
- Produces: service uses saved target app and limits rather than constants.
- Consumes: All earlier tasks.

- [ ] **Step 1: Create app container in `AttentionPetApp.kt`**

```kotlin
package com.attentionpet

import android.app.Application
import androidx.room.Room
import com.attentionpet.data.AttentionPetDatabase
import com.attentionpet.data.AttentionPetRepository

class AttentionPetApp : Application() {
    lateinit var repository: AttentionPetRepository
        private set

    override fun onCreate() {
        super.onCreate()
        val database = Room.databaseBuilder(this, AttentionPetDatabase::class.java, "attention-pet.db").build()
        repository = AttentionPetRepository(database.configDao(), database.sessionDao(), database.eventDao())
    }
}
```

- [ ] **Step 2: Implement `MainViewModel`**

```kotlin
package com.attentionpet.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.attentionpet.data.AttentionPetRepository
import kotlinx.coroutines.launch

class MainViewModel(private val repository: AttentionPetRepository) : ViewModel() {
    fun saveDefaultMvpConfig() {
        viewModelScope.launch {
            repository.saveTargetApp("com.ss.android.ugc.aweme", "抖音", true)
            repository.saveLimits(dailyMinutes = 60, sessionMinutes = 15, rollingWindowLimitMinutes = 30)
        }
    }
}
```

- [ ] **Step 3: Use repository config in service**

Replace hardcoded package/limits in `AttentionMonitorService` with repository reads before monitoring loop starts:

```kotlin
val app = application as com.attentionpet.AttentionPetApp
val repository = app.repository
```

Then load target and limits using first emissions:

```kotlin
val target = repository.targetApp().first() ?: return@launch
val limits = repository.limits().first() ?: return@launch
val tracker = SessionTracker(target.packageName)
val config = with(repository) { limits.toRuleConfig() }
```

Add imports:

```kotlin
import kotlinx.coroutines.flow.first
```

- [ ] **Step 4: Persist extension and timeout actions**

In timeout sheet callbacks inside `AttentionMonitorService`, use current `sessionId` when session persistence is wired. If no persisted session id exists in this task, keep overlay behavior and add a code comment exactly:

```kotlin
// Session persistence id is wired in the next iteration; MVP visual behavior remains non-blocking here.
```

This is the only allowed temporary comment because session persistence wiring depends on running service behavior and should be reviewed with device logs.

- [ ] **Step 5: Full verification**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Manual acceptance checklist**

On an Android device or emulator:

```text
[ ] App opens to Attention Pet home screen.
[ ] Usage Access and overlay permission cards reflect real permission state after returning from Settings.
[ ] “开始守护” is disabled until both permissions are granted.
[ ] After permissions are granted, tapping “开始守护” starts a foreground notification.
[ ] Opening the configured target app shows the small edge capsule.
[ ] Capsule taps open the edge panel.
[ ] Timeout state opens bottom sheet only after tapping timeout capsule.
[ ] Overlay outside bounds do not block the underlying app.
```

- [ ] **Step 7: Commit**

```powershell
git add app/src/main/java/com/attentionpet
git commit -m "feat: integrate monitoring MVP"
```

Expected: commit succeeds if Git is available.

---

## Self-Review

Spec coverage:

- One target app: Task 4 UI and Task 8/9 monitoring config.
- Three rules: Task 2 domain evaluator and Task 4 sliders.
- Rolling 5-hour window: Task 2 `RuleConfig` invariant.
- Timeout prompt policy: Task 5 views and Task 6/8 user-triggered sheet.
- Overlay runtime contract: Task 6 exact flags and WindowManager params.
- Permission flow: Task 4 helpers/UI and Task 9 acceptance checklist.
- Room persistence: Task 3 entities/DAOs/repository.
- UsageStats foreground detection: Task 7 detector and Task 8 service.
- A-bird visual: Task 5 `BirdPet`.

Placeholder scan:

- No `TBD`, `TODO`, `implement later`, or unspecified error-handling steps are present.
- The one allowed temporary comment in Task 9 is explicit, bounded, and tied to a later device-reviewed persistence iteration.

Type consistency:

- `PetState`, `RuleType`, `RuleConfig`, `RuleBucket`, `RuleEvaluationResult`, and `ExtensionGrant` are defined in Task 2 and reused consistently.
- Repository type names match Task 3 entities/DAOs.
- Overlay views consume `RuleEvaluationResult` from Task 2.
- Service consumes `ForegroundAppDetector`, `SessionTracker`, `RuleEvaluator`, and `OverlayController` from earlier tasks.

## Execution Handoff

Plan complete and saved to `docs/superpowers/plans/2026-07-06-attention-pet-android-mvp.md`.

Two execution options:

1. **Subagent-Driven (recommended)** - dispatch a fresh subagent per task, review between tasks, fast iteration.
2. **Inline Execution** - execute tasks in this session using executing-plans, batch execution with checkpoints.
