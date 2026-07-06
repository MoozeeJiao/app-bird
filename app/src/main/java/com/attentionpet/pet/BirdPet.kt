package com.attentionpet.pet

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import com.attentionpet.domain.PetState

@Composable
fun BirdPet(state: PetState, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val scale = size.minDimension / 82f
        val drawingSize = 82f * scale
        translate(
            left = (size.width - drawingSize) / 2f,
            top = (size.height - drawingSize) / 2f
        ) {
            drawBird(state, scale)
        }
    }
}

internal data class BirdPalette(
    val body: Color,
    val wing: Color
)

internal fun birdPaletteFor(state: PetState): BirdPalette = when (state) {
    PetState.RELAXED -> BirdPalette(
        body = Color(0xFF62DDB3),
        wing = Color(0xFF44C79A)
    )
    PetState.REMINDER -> BirdPalette(
        body = Color(0xFFFFD76F),
        wing = Color(0xFFFFBD58)
    )
    PetState.TENSE -> BirdPalette(
        body = Color(0xFFFFB066),
        wing = Color(0xFFFF8A4C)
    )
    PetState.TIMEOUT -> BirdPalette(
        body = Color(0xFFFF9A7B),
        wing = Color(0xFFF45E63)
    )
}

private fun DrawScope.drawBird(state: PetState, scale: Float) {
    val palette = birdPaletteFor(state)
    fun x(value: Float) = value * scale

    drawOval(palette.body, topLeft = Offset(x(13f), x(13f)), size = Size(x(56f), x(58f)))
    rotate(-18f, pivot = Offset(x(42f), x(12f))) {
        drawOval(palette.wing, topLeft = Offset(x(36f), x(5f)), size = Size(x(11f), x(14f)))
    }
    rotate(if (state == PetState.TIMEOUT) -54f else -13f, pivot = Offset(x(23f), x(50f))) {
        drawOval(palette.wing, topLeft = Offset(x(9f), x(39f)), size = Size(x(27f), x(22f)))
    }
    drawOval(Color.White.copy(alpha = 0.54f), topLeft = Offset(x(29f), x(44f)), size = Size(x(25f), x(17f)))
    drawCircle(Color(0xFF21323A), radius = x(3f), center = Offset(x(38.5f), x(37f)))
    drawCircle(Color(0xFF21323A), radius = x(3f), center = Offset(x(53.5f), x(37f)))

    val beak = Path().apply {
        moveTo(x(44f), x(41f))
        lineTo(x(55f), x(46f))
        lineTo(x(44f), x(51f))
        close()
    }
    drawPath(beak, Color(0xFFFFD05B))
    drawOval(Color(0xFFFF7E70).copy(alpha = 0.38f), topLeft = Offset(x(30f), x(46f)), size = Size(x(7f), x(4f)))
    drawOval(Color(0xFFFF7E70).copy(alpha = 0.38f), topLeft = Offset(x(55f), x(46f)), size = Size(x(7f), x(4f)))
    drawLine(Color(0xFFEFB45A), start = Offset(x(31f), x(75f)), end = Offset(x(51f), x(75f)), strokeWidth = x(3f))
}
