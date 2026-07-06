package com.attentionpet.ui

import android.content.pm.PackageManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class AppPickerTest {
    @Test
    fun launcherQueryDoesNotUseDefaultOnlyFiltering() {
        assertEquals(0, AppPicker.launcherQueryFlags)
        assertNotEquals(PackageManager.MATCH_DEFAULT_ONLY, AppPicker.launcherQueryFlags)
    }
}
