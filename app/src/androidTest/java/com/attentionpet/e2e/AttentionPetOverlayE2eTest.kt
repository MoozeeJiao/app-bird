package com.attentionpet.e2e

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.attentionpet.AttentionPetApp
import com.attentionpet.AttentionPetTestIds
import com.attentionpet.MainActivity
import java.io.FileInputStream
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
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
        grantRequiredPermissions()
        seedTargetConfig()
        launchHome()
    }

    @Test
    fun startsMonitoringAndShowsCapsulePanelOverlay() {
        assertTrue(
            "Start button should become visible after permissions are granted",
            device.wait(Until.hasObject(By.desc(AttentionPetTestIds.START_MONITORING)), TIMEOUT_MILLIS)
        )

        device.findObject(By.desc(AttentionPetTestIds.START_MONITORING)).click()

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

    private fun grantRequiredPermissions() {
        executeShell("appops set $packageName GET_USAGE_STATS allow")
        executeShell("appops set $packageName SYSTEM_ALERT_WINDOW allow")
        executeShell("pm grant $packageName android.permission.POST_NOTIFICATIONS")
    }

    private fun seedTargetConfig() {
        val app = instrumentation.targetContext.applicationContext as AttentionPetApp
        runBlocking {
            app.repository.saveHomeConfig(
                packageName = packageName,
                displayName = "Attention Pet",
                dailyMinutes = 10,
                sessionMinutes = 5,
                rollingWindowLimitMinutes = 5
            )
        }
    }

    private fun launchHome() {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        context.startActivity(intent)
        device.wait(Until.hasObject(By.pkg(packageName).depth(0)), TIMEOUT_MILLIS)
    }

    private fun executeShell(command: String) {
        instrumentation.uiAutomation.executeShellCommand(command).use { descriptor ->
            FileInputStream(descriptor.fileDescriptor).bufferedReader().use { it.readText() }
        }
    }

    private companion object {
        const val TIMEOUT_MILLIS = 15_000L
    }
}
