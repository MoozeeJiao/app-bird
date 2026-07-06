package com.attentionpet.overlay

import android.content.Context

enum class OverlayEdge { LEFT, RIGHT }

data class OverlayPosition(
    val edge: OverlayEdge = OverlayEdge.LEFT,
    val verticalRatio: Float = 0.22f
)

class OverlayPositionStore(context: Context) {
    private val prefs = context.getSharedPreferences("overlay_position", Context.MODE_PRIVATE)

    fun load(): OverlayPosition {
        val edge = if (prefs.getString("edge", "LEFT") == "RIGHT") OverlayEdge.RIGHT else OverlayEdge.LEFT
        val ratio = prefs.getFloat("verticalRatio", 0.22f).coerceIn(0f, 1f)
        return OverlayPosition(edge, ratio)
    }

    fun save(position: OverlayPosition) {
        prefs.edit()
            .putString("edge", position.edge.name)
            .putFloat("verticalRatio", position.verticalRatio.coerceIn(0f, 1f))
            .apply()
    }
}
