package com.attentionpet.overlay

import org.junit.Assert.assertEquals
import org.junit.Test

class OverlayPositionTest {
    @Test
    fun defaultPositionAnchorsLeftAtTwentyTwoPercent() {
        val position = OverlayPosition()

        assertEquals(OverlayEdge.LEFT, position.edge)
        assertEquals(0.22f, position.verticalRatio, 0f)
    }
}
