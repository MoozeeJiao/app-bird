package com.attentionpet.pet

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.attentionpet.domain.PetState
import org.junit.Assert.assertEquals
import org.junit.Test

class BirdPetTest {
    @Test
    fun birdPaletteProgressesFromGreenToYellowToOrangeToRed() {
        assertPalette(
            state = PetState.RELAXED,
            body = 0xFF62DDB3,
            wing = 0xFF44C79A
        )
        assertPalette(
            state = PetState.REMINDER,
            body = 0xFFFFD76F,
            wing = 0xFFFFBD58
        )
        assertPalette(
            state = PetState.TENSE,
            body = 0xFFFFB066,
            wing = 0xFFFF8A4C
        )
        assertPalette(
            state = PetState.TIMEOUT,
            body = 0xFFFF9A7B,
            wing = 0xFFF45E63
        )
    }

    private fun assertPalette(state: PetState, body: Long, wing: Long) {
        val palette = birdPaletteFor(state)

        assertEquals(body, palette.body.argbLong())
        assertEquals(wing, palette.wing.argbLong())
    }

    private fun Color.argbLong(): Long = toArgb().toLong() and 0xFFFFFFFFL
}
