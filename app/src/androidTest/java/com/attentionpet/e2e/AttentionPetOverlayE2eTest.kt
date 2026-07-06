package com.attentionpet.e2e

import android.content.Context
import android.content.Intent
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.attentionpet.AttentionPetApp
import com.attentionpet.AttentionPetTestIds
import com.attentionpet.MainActivity
import com.attentionpet.data.AttentionPetDatabase
import com.attentionpet.domain.PetState
import com.attentionpet.service.AttentionMonitorService
import java.io.FileInputStream
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AttentionPetOverlayE2eTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val device = UiDevice.getInstance(instrumentation)
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val packageName = context.packageName

    @Before
    fun setUp() {
        stopMonitoring()
        grantRequiredPermissions()
        resetAppStorage()
        seedTargetConfig()
        launchHome()
    }

    @Test
    fun startsMonitoringFixtureTargetAndShowsCapsulePanelOverlay() {
        startMonitoringFromHome()
        launchFixtureTarget()

        assertTrue(
            "Capsule overlay should appear while the configured target package is foreground",
            device.wait(Until.hasObject(By.desc(AttentionPetTestIds.OVERLAY_CAPSULE)), TIMEOUT_MILLIS)
        )

        device.findObject(By.desc(AttentionPetTestIds.OVERLAY_CAPSULE)).click()

        assertTrue(
            "Expanded overlay panel should appear after tapping the capsule",
            device.wait(Until.hasObject(By.desc(AttentionPetTestIds.OVERLAY_PANEL)), TIMEOUT_MILLIS)
        )
    }

    @Test
    fun foregroundTimeIncreasesWhileFixtureTargetStaysOpen() {
        startMonitoringFromHome()
        launchFixtureTarget()

        assertTrue(
            "Capsule overlay should appear for the fixture target app",
            device.wait(Until.hasObject(By.desc(AttentionPetTestIds.OVERLAY_CAPSULE)), TIMEOUT_MILLIS)
        )

        val firstElapsedMillis = openPanelAndReadElapsedMillis()
        waitForElapsedMillisAtLeast(firstElapsedMillis + 2_000L)
    }

    @Test
    fun fixtureTargetCanDriveEveryOverlayPetState() {
        assertFixtureStateForRollingUsage(usedMillis = 0L, expectedState = PetState.RELAXED)
        assertFixtureStateForRollingUsage(usedMillis = 3 * 60_000L, expectedState = PetState.REMINDER)
        assertFixtureStateForRollingUsage(usedMillis = 4 * 60_000L + 10_000L, expectedState = PetState.TENSE)
        assertFixtureStateForRollingUsage(usedMillis = 5 * 60_000L + 10_000L, expectedState = PetState.TIMEOUT)
    }

    private fun assertFixtureStateForRollingUsage(usedMillis: Long, expectedState: PetState) {
        stopMonitoring()
        resetAppStorage()
        seedTargetConfig()
        seedHistoricalFixtureUsage(usedMillis)
        launchHome()
        startMonitoringFromHome()
        launchFixtureTarget()

        assertTrue(
            "Capsule overlay should expose ${expectedState.serialized} state for fixture target",
            device.wait(
                Until.hasObject(By.desc(overlayStateDescription(expectedState))),
                STATE_TIMEOUT_MILLIS
            )
        )
    }

    private fun grantRequiredPermissions() {
        executeShell("appops set $packageName GET_USAGE_STATS allow")
        executeShell("appops set $packageName SYSTEM_ALERT_WINDOW allow")
        executeShell("pm grant $packageName android.permission.POST_NOTIFICATIONS")
    }

    private fun seedTargetConfig() {
        val app = instrumentation.targetContext.applicationContext as AttentionPetApp
        runBlocking {
            app.repository.saveHomeConfig(
                packageName = FIXTURE_PACKAGE,
                displayName = "Mock Shorts",
                dailyMinutes = 60,
                sessionMinutes = 5,
                rollingWindowLimitMinutes = 5
            )
        }
    }

    private fun seedHistoricalFixtureUsage(usedMillis: Long) {
        if (usedMillis <= 0L) return
        val app = instrumentation.targetContext.applicationContext as AttentionPetApp
        val endMillis = System.currentTimeMillis() - 10_000L
        val startMillis = endMillis - usedMillis
        runBlocking {
            val sessionId = app.repository.openSession(FIXTURE_PACKAGE, startMillis)
            app.repository.closeSession(
                sessionId = sessionId,
                endMillis = endMillis,
                foregroundDurationMillis = usedMillis,
                closeReason = "e2e_seed"
            )
        }
    }

    private fun resetAppStorage() {
        val database = Room.databaseBuilder(
            instrumentation.targetContext,
            AttentionPetDatabase::class.java,
            "attention-pet.db"
        ).build()
        try {
            database.clearAllTables()
        } finally {
            database.close()
        }
    }

    private fun launchHome() {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        context.startActivity(intent)
        device.wait(Until.hasObject(By.pkg(packageName).depth(0)), TIMEOUT_MILLIS)
    }

    private fun startMonitoringFromHome() {
        assertTrue(
            "Start button should become visible after permissions are granted",
            device.wait(Until.hasObject(By.desc(AttentionPetTestIds.START_MONITORING)), TIMEOUT_MILLIS)
        )
        device.findObject(By.desc(AttentionPetTestIds.START_MONITORING)).click()
    }

    private fun launchFixtureTarget() {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(FIXTURE_PACKAGE)
        assertNotNull(
            "Fixture target app $FIXTURE_PACKAGE should be installed before E2E runs",
            launchIntent
        )

        context.startActivity(
            launchIntent!!.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
        assertTrue(
            "Fixture target app should move to foreground",
            device.wait(Until.hasObject(By.pkg(FIXTURE_PACKAGE).depth(0)), TIMEOUT_MILLIS)
        )
    }

    private fun stopMonitoring() {
        context.stopService(Intent(context, AttentionMonitorService::class.java))
        device.pressHome()
    }

    private fun openPanelAndReadElapsedMillis(): Long {
        dismissPanelIfVisible()
        assertTrue(
            "Capsule should be visible before opening panel",
            device.wait(Until.hasObject(By.desc(AttentionPetTestIds.OVERLAY_CAPSULE)), TIMEOUT_MILLIS)
        )
        device.findObject(By.desc(AttentionPetTestIds.OVERLAY_CAPSULE)).click()
        assertTrue(
            "Expanded panel should appear after tapping capsule",
            device.wait(Until.hasObject(By.desc(AttentionPetTestIds.OVERLAY_PANEL)), TIMEOUT_MILLIS)
        )
        val elapsedNode = device.wait(
            Until.findObject(By.descStartsWith(AttentionPetTestIds.OVERLAY_SESSION_MILLIS_PREFIX)),
            TIMEOUT_MILLIS
        )
        assertNotNull("Expanded panel should expose current session elapsed millis", elapsedNode)
        return elapsedNode!!.contentDescription
            .removePrefix(AttentionPetTestIds.OVERLAY_SESSION_MILLIS_PREFIX)
            .toLong()
    }

    private fun waitForElapsedMillisAtLeast(expectedMillis: Long) {
        val deadline = System.currentTimeMillis() + STATE_TIMEOUT_MILLIS
        var lastElapsedMillis = -1L
        while (System.currentTimeMillis() < deadline) {
            val elapsedMillis = openPanelAndReadElapsedMillis()
            lastElapsedMillis = elapsedMillis
            if (elapsedMillis >= expectedMillis) return
            Thread.sleep(1_000L)
        }

        assertNotEquals(
            "Current session elapsed millis should have been sampled at least once",
            -1L,
            lastElapsedMillis
        )
        fail("Expected elapsed millis to reach $expectedMillis, last value was $lastElapsedMillis")
    }

    private fun dismissPanelIfVisible() {
        device.findObject(By.desc(AttentionPetTestIds.OVERLAY_PANEL_DISMISS))?.click()
        device.wait(Until.gone(By.desc(AttentionPetTestIds.OVERLAY_PANEL)), 2_000L)
    }

    private fun overlayStateDescription(state: PetState): String {
        return AttentionPetTestIds.overlayState(state)
    }

    private fun executeShell(command: String) {
        instrumentation.uiAutomation.executeShellCommand(command).use { descriptor ->
            FileInputStream(descriptor.fileDescriptor).bufferedReader().use { it.readText() }
        }
    }

    private companion object {
        const val FIXTURE_PACKAGE = "com.attentionpet.fixture.shortvideo"
        const val TIMEOUT_MILLIS = 15_000L
        const val STATE_TIMEOUT_MILLIS = 75_000L
    }
}
