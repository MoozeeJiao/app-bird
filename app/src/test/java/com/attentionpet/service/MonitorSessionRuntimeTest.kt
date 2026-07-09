package com.attentionpet.service

import org.junit.Assert.assertEquals
import org.junit.Test

class MonitorSessionRuntimeTest {
    @Test
    fun resetsTrackingWhenTargetPackageChangesWhileServiceKeepsRunning() {
        val runtime = MonitorSessionRuntime()

        runtime.sample(
            targetPackage = "com.old",
            graceMillis = 30_000L,
            foregroundPackage = "com.old",
            nowMillis = 1_000L
        )
        val state = runtime.sample(
            targetPackage = "com.new",
            graceMillis = 30_000L,
            foregroundPackage = "com.new",
            nowMillis = 20_000L
        )

        assertEquals(SessionTracker.Status.ACTIVE, state.status)
        assertEquals(20_000L, state.activeStartMillis)
        assertEquals(0L, state.foregroundDurationMillis)
    }
}
