package com.attentionpet.overlay

import android.content.Context
import android.graphics.Rect
import androidx.core.graphics.Insets
import androidx.window.layout.WindowMetricsCalculator

data class OverlaySafeBounds(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
) {
    val height: Int = bottom - top
}

fun overlaySafeBounds(
    context: Context,
    systemInsets: Insets = Insets.NONE,
    cutoutInsets: Insets = Insets.NONE
): OverlaySafeBounds {
    val bounds: Rect = WindowMetricsCalculator.getOrCreate().computeMaximumWindowMetrics(context).bounds
    return OverlaySafeBounds(
        left = bounds.left + maxOf(systemInsets.left, cutoutInsets.left),
        top = bounds.top + maxOf(systemInsets.top, cutoutInsets.top),
        right = bounds.right - maxOf(systemInsets.right, cutoutInsets.right),
        bottom = bounds.bottom - maxOf(systemInsets.bottom, cutoutInsets.bottom)
    )
}
