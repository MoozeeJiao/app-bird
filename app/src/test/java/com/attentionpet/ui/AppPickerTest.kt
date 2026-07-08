package com.attentionpet.ui

import android.content.pm.PackageManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppPickerTest {
    @Test
    fun launcherQueryDoesNotUseDefaultOnlyFiltering() {
        assertEquals(0, AppPicker.launcherQueryFlags)
        assertNotEquals(PackageManager.MATCH_DEFAULT_ONLY, AppPicker.launcherQueryFlags)
    }

    @Test
    fun launchableAppCanCarryIconForPickerRows() {
        val app = LaunchableApp(packageName = "com.example", label = "Example", icon = null)

        assertEquals("com.example", app.packageName)
        assertEquals("Example", app.label)
        assertNull(app.icon)
    }

    @Test
    fun filtersLaunchableAppsByLabelAndPackageCaseInsensitively() {
        val apps = listOf(
            LaunchableApp(packageName = "com.video", label = "Bilibili"),
            LaunchableApp(packageName = "com.chat", label = "ChatGPT"),
            LaunchableApp(packageName = "com.browser", label = "Chrome")
        )

        assertEquals(
            listOf("com.chat"),
            AppPicker.filterLaunchableApps(apps, "chat").map { it.packageName }
        )
        assertEquals(
            listOf("com.browser"),
            AppPicker.filterLaunchableApps(apps, "BROWSER").map { it.packageName }
        )
        assertEquals(apps, AppPicker.filterLaunchableApps(apps, " "))
    }
}
