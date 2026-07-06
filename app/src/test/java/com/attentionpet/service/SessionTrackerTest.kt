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

    @Test
    fun ignoresNonIncreasingTargetSamples() {
        val tracker = SessionTracker(targetPackage = "com.target")

        tracker.onForegroundSample("com.target", 1_000L)
        val earlierState = tracker.onForegroundSample("com.target", 900L)
        val laterState = tracker.onForegroundSample("com.target", 1_100L)

        assertEquals(0L, earlierState.foregroundDurationMillis)
        assertEquals(100L, laterState.foregroundDurationMillis)
    }

    @Test
    fun startsFreshSessionAfterClosure() {
        val tracker = SessionTracker(targetPackage = "com.target")

        tracker.onForegroundSample("com.target", 0L)
        tracker.onForegroundSample(null, 31_000L)
        val newSession = tracker.onForegroundSample("com.target", 40_000L)

        assertEquals(SessionTracker.Status.ACTIVE, newSession.status)
        assertEquals(40_000L, newSession.activeStartMillis)
        assertEquals(0L, newSession.foregroundDurationMillis)
    }
}
