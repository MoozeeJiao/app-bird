package com.attentionpet.service

import android.app.usage.UsageStatsManager
import android.app.usage.UsageEvents
import android.content.Context

class ForegroundAppDetector internal constructor(
    private val eventSource: ForegroundEventSource,
    private val bootstrapLookbackMillis: Long = DEFAULT_BOOTSTRAP_LOOKBACK_MILLIS,
    private val overlapMillis: Long = DEFAULT_QUERY_OVERLAP_MILLIS
) {
    constructor(context: Context) : this(AndroidForegroundEventSource(context))

    private var lastForegroundPackage: String? = null
    private var lastQueryEndMillis: Long? = null

    fun currentForegroundPackage(nowMillis: Long): String? {
        val queryEndMillis = nowMillis.coerceAtLeast(0L)
        val queryStartMillis = queryStartMillis(queryEndMillis).coerceAtMost(queryEndMillis)
        val transitions = eventSource.queryEvents(queryStartMillis, queryEndMillis)
            .sortedBy { it.timestampMillis }

        transitions.forEach { transition ->
            when (transition.type) {
                ForegroundTransition.Type.FOREGROUND -> lastForegroundPackage = transition.packageName
                ForegroundTransition.Type.BACKGROUND -> {
                    if (lastForegroundPackage == transition.packageName) {
                        lastForegroundPackage = null
                    }
                }
            }
        }
        if (transitions.isEmpty()) {
            val latestUsageSnapshot = eventSource.queryUsageSnapshots(queryStartMillis, queryEndMillis)
                .maxByOrNull { it.lastTimeUsedMillis }
            if (latestUsageSnapshot != null) {
                lastForegroundPackage = latestUsageSnapshot.packageName
            }
        }

        lastQueryEndMillis = maxOf(lastQueryEndMillis ?: queryEndMillis, queryEndMillis)
        return lastForegroundPackage
    }

    private fun queryStartMillis(queryEndMillis: Long): Long {
        val previousQueryEnd = lastQueryEndMillis
        return if (previousQueryEnd == null) {
            (queryEndMillis - bootstrapLookbackMillis).coerceAtLeast(0L)
        } else {
            (previousQueryEnd - overlapMillis).coerceAtLeast(0L)
        }
    }

    private companion object {
        const val DEFAULT_BOOTSTRAP_LOOKBACK_MILLIS = 24L * 60L * 60L * 1000L
        const val DEFAULT_QUERY_OVERLAP_MILLIS = 1_000L
    }
}

internal interface ForegroundEventSource {
    fun queryEvents(startMillis: Long, endMillis: Long): List<ForegroundTransition>

    fun queryUsageSnapshots(startMillis: Long, endMillis: Long): List<ForegroundUsageSnapshot>
}

internal data class ForegroundUsageSnapshot(
    val packageName: String,
    val lastTimeUsedMillis: Long
)

internal data class ForegroundTransition(
    val packageName: String,
    val type: Type,
    val timestampMillis: Long
) {
    enum class Type { FOREGROUND, BACKGROUND }
}

private class AndroidForegroundEventSource(context: Context) : ForegroundEventSource {
    private val usageStatsManager =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    @Suppress("DEPRECATION")
    override fun queryEvents(startMillis: Long, endMillis: Long): List<ForegroundTransition> {
        val events = usageStatsManager.queryEvents(startMillis, endMillis)
        val event = UsageEvents.Event()
        val transitions = mutableListOf<ForegroundTransition>()

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            when (event.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND -> transitions += ForegroundTransition(
                    packageName = event.packageName,
                    type = ForegroundTransition.Type.FOREGROUND,
                    timestampMillis = event.timeStamp
                )
                UsageEvents.Event.MOVE_TO_BACKGROUND -> transitions += ForegroundTransition(
                    packageName = event.packageName,
                    type = ForegroundTransition.Type.BACKGROUND,
                    timestampMillis = event.timeStamp
                )
            }
        }

        return transitions
    }

    override fun queryUsageSnapshots(startMillis: Long, endMillis: Long): List<ForegroundUsageSnapshot> {
        return usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_BEST,
            startMillis,
            endMillis
        ).mapNotNull { usageStats ->
            val packageName = usageStats.packageName ?: return@mapNotNull null
            ForegroundUsageSnapshot(
                packageName = packageName,
                lastTimeUsedMillis = usageStats.lastTimeUsed
            )
        }
    }
}
