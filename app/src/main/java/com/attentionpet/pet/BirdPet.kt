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

private fun DrawScope.drawBird(state: PetState, scale: Float) {
    val body = when (state) {
        PetState.RELAXED -> Color(0xFF62DDB3)
        PetState.REMINDER -> Color(0xFF9BE391)
        PetState.TENSE -> Color(0xFFFFD76F)
        PetState.TIMEOUT -> Color(0xFFFF9A7B)
    }
    val wing = when (state) {
        PetState.RELAXED -> Color(0xFF44C79A)
        PetState.REMINDER -> Color(0xFF77D873)
        PetState.TENSE -> Color(0xFFFFBD58)
        PetState.TIMEOUT -> Color(0xFFF45E63)
    }
    fun x(value: Float) = value * scale

    drawOval(body, topLeft = Offset(x(13f), x(13f)), size = Size(x(56f), x(58f)))
    rotate(-18f, pivot = Offset(x(42f), x(12f))) {
        drawOval(wing, topLeft = Offset(x(36f), x(5f)), size = Size(x(11f), x(14f)))
    }
    rotate(if (state == PetState.TIMEOUT) -54f else -13f, pivot = Offset(x(23f), x(50f))) {
        drawOval(wing, topLeft = Offset(x(9f), x(39f)), size = Size(x(27f), x(22f)))
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
