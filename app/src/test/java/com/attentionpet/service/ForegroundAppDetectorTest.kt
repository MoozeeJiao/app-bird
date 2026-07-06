package com.attentionpet.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ForegroundAppDetectorTest {
    @Test
    fun keepsForegroundPackageWhenNoNewEventsArriveAfterInitialDetection() {
        val source = FakeForegroundEventSource(
            ForegroundTransition("com.target", ForegroundTransition.Type.FOREGROUND, 500L)
        )
        val detector = ForegroundAppDetector(
            eventSource = source,
            bootstrapLookbackMillis = 10_000L,
            overlapMillis = 1_000L
        )

        assertEquals("com.target", detector.currentForegroundPackage(2_000L))
        assertEquals("com.target", detector.currentForegroundPackage(60_000L))
    }

    @Test
    fun bootstrapsFromHistoryBeyondIncrementalPollingWindow() {
        val source = FakeForegroundEventSource(
            ForegroundTransition("com.target", ForegroundTransition.Type.FOREGROUND, 20_000L)
        )
        val detector = ForegroundAppDetector(
            eventSource = source,
            bootstrapLookbackMillis = 60_000L,
            overlapMillis = 1_000L
        )

        assertEquals("com.target", detector.currentForegroundPackage(50_000L))
        assertEquals(0L to 50_000L, source.queries.single())
    }

    @Test
    fun clearsForegroundPackageWhenBackgroundEventArrives() {
        val source = FakeForegroundEventSource(
            ForegroundTransition("com.target", ForegroundTransition.Type.FOREGROUND, 1_000L),
            ForegroundTransition("com.target", ForegroundTransition.Type.BACKGROUND, 3_000L)
        )
        val detector = ForegroundAppDetector(
            eventSource = source,
            bootstrapLookbackMillis = 10_000L,
            overlapMillis = 1_000L
        )

        assertNull(detector.currentForegroundPackage(4_000L))
        assertNull(detector.currentForegroundPackage(8_000L))
    }

    @Test
    fun returnsNullWhenInitialQueryHasNoEvents() {
        val detector = ForegroundAppDetector(
            eventSource = FakeForegroundEventSource(),
            bootstrapLookbackMillis = 10_000L,
            overlapMillis = 1_000L
        )

        assertNull(detector.currentForegroundPackage(5_000L))
    }
}

private class FakeForegroundEventSource(
    vararg transitions: ForegroundTransition
) : ForegroundEventSource {
    val queries = mutableListOf<Pair<Long, Long>>()
    private val transitions = transitions.toList()

    override fun queryEvents(startMillis: Long, endMillis: Long): List<ForegroundTransition> {
        queries += startMillis to endMillis
        return transitions.filter { it.timestampMillis in startMillis..endMillis }
    }
}
