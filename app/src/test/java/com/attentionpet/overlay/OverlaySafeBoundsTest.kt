package com.attentionpet.overlay

import org.junit.Assert.assertEquals
import org.junit.Test

class OverlaySafeBoundsTest {
    @Test
    fun heightIsBottomMinusTop() {
        val bounds = OverlaySafeBounds(
            left = 12,
            top = 24,
            right = 480,
            bottom = 824
        )

        assertEquals(800, bounds.height)
    }
}
