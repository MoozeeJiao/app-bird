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
