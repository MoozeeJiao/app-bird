package com.attentionpet.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HomeConfigStateTest {
    @Test
    fun defaultStateUsesEmptyTargetAndMvpLimitDefaults() {
        val state = HomeConfigState()

        assertNull(state.selectedTargetPackageName)
        assertEquals("\u672A\u9009\u62E9 App", state.targetAppLabel)
        assertEquals(60, state.dailyLimitMinutes)
        assertEquals(15, state.sessionLimitMinutes)
        assertEquals(30, state.rollingWindowLimitMinutes)
    }

    @Test
    fun selectingTargetUpdatesRenderedLabelAndPackage() {
        val state = HomeConfigState().selectTarget(
            LaunchableApp(
                packageName = "com.example.video",
                label = "\u89C6\u9891"
            )
        )

        assertEquals("com.example.video", state.selectedTargetPackageName)
        assertEquals("\u89C6\u9891", state.targetAppLabel)
    }

    @Test
    fun limitUpdatesChangeRenderedValuesAndCoerceToSliderRanges() {
        val state = HomeConfigState()
            .updateDailyLimit(240)
            .updateSessionLimit(1)
            .updateRollingWindowLimit(90)

        assertEquals(180, state.dailyLimitMinutes)
        assertEquals(5, state.sessionLimitMinutes)
        assertEquals(90, state.rollingWindowLimitMinutes)
    }
}
