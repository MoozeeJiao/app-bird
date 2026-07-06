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

    @Test
    fun overlapReturnsZeroWhenEndpointOnlyTouchesWindow() {
        val result = overlapMillis(
            sessionStart = 1_000L,
            sessionEndOrNow = 3_000L,
            windowStart = 3_000L,
            windowEnd = 5_000L
        )
        assertEquals(0L, result)
    }

    @Test
    fun overlapCountsFullSessionWhenContainedByWindow() {
        val result = overlapMillis(
            sessionStart = 2_000L,
            sessionEndOrNow = 4_000L,
            windowStart = 1_000L,
            windowEnd = 5_000L
        )
        assertEquals(2_000L, result)
    }

    @Test
    fun overlapCountsFullWindowWhenContainedBySession() {
        val result = overlapMillis(
            sessionStart = 1_000L,
            sessionEndOrNow = 5_000L,
            windowStart = 2_000L,
            windowEnd = 4_000L
        )
        assertEquals(2_000L, result)
    }
}
