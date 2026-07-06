package com.attentionpet.service

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context

class ForegroundAppDetector(context: Context) {
    private val usageStatsManager =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    @Suppress("DEPRECATION")
    fun currentForegroundPackage(nowMillis: Long): String? {
        val events = usageStatsManager.queryEvents((nowMillis - LOOKBACK_MILLIS).coerceAtLeast(0L), nowMillis)
        val event = UsageEvents.Event()
        var current: String? = null

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            when (event.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND -> current = event.packageName
                UsageEvents.Event.MOVE_TO_BACKGROUND -> if (current == event.packageName) current = null
            }
        }

        return current
    }

    private companion object {
        const val LOOKBACK_MILLIS = 10_000L
    }
}
