package com.attentionpet.domain

import java.time.Instant
import java.time.ZoneId

object RuleEvaluator {
    fun evaluate(
        nowMillis: Long,
        zoneId: ZoneId,
        config: RuleConfig,
        sessions: List<UsageInterval>,
        activeSession: ActiveSession?,
        extensionGrant: ExtensionGrant
    ): RuleEvaluationResult {
        val activeExtensionRemaining = if (activeSession == null) 0L else extensionGrant.remainingMillis
        val startOfDay = Instant.ofEpochMilli(nowMillis)
            .atZone(zoneId)
            .toLocalDate()
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()
        val rollingStart = nowMillis - config.rollingWindowMillis

        val dailyUsed = sessions.sumOf { overlapMillis(it.startMillis, it.endMillis, startOfDay, nowMillis) } +
            activeSessionOverlap(activeSession, startOfDay, nowMillis)
        val rollingUsed = sessions.sumOf { overlapMillis(it.startMillis, it.endMillis, rollingStart, nowMillis) } +
            activeSessionOverlap(activeSession, rollingStart, nowMillis)
        val sessionUsed = activeSession?.foregroundMillis ?: 0L

        val session = bucket(RuleType.SESSION, sessionUsed, config.sessionLimitMillis, activeExtensionRemaining)
        val rolling = bucket(RuleType.ROLLING_WINDOW, rollingUsed, config.rollingWindowLimitMillis, activeExtensionRemaining)
        val daily = bucket(RuleType.DAILY, dailyUsed, config.dailyLimitMillis, activeExtensionRemaining)
        val ordered = listOf(session, rolling, daily)
        val triggering = ordered.minWith(compareBy<RuleBucket> { it.effectiveRemainingMillis }.thenBy { tieRank(it.type) })
        val state = when {
            ordered.any { it.effectiveRemainingMillis <= 0L } -> PetState.TIMEOUT
            triggering.effectiveRemainingRatio < 0.20f -> PetState.TENSE
            triggering.effectiveRemainingRatio <= 0.50f -> PetState.REMINDER
            else -> PetState.RELAXED
        }

        return RuleEvaluationResult(
            daily = daily,
            session = session,
            rollingWindow = rolling,
            triggeringRule = triggering.type,
            effectiveRemainingMillis = triggering.effectiveRemainingMillis,
            effectiveRemainingRatio = triggering.effectiveRemainingRatio,
            petState = state,
            statusCopy = statusCopy(state),
            activeExtensionRemainingMillis = activeExtensionRemaining
        )
    }

    private fun activeSessionOverlap(activeSession: ActiveSession?, windowStart: Long, windowEnd: Long): Long {
        if (activeSession == null) return 0L
        return overlapMillis(activeSession.startMillis, windowEnd, windowStart, windowEnd)
            .coerceAtMost(activeSession.foregroundMillis)
    }

    private fun bucket(type: RuleType, usedMillis: Long, limitMillis: Long, extensionMillis: Long): RuleBucket {
        val rawRemaining = limitMillis - usedMillis
        val effectiveRemaining = rawRemaining + extensionMillis
        return RuleBucket(
            type = type,
            usedMillis = usedMillis,
            limitMillis = limitMillis,
            rawRemainingMillis = rawRemaining,
            effectiveRemainingMillis = effectiveRemaining,
            rawRemainingRatio = rawRemaining.toFloat() / limitMillis.toFloat(),
            effectiveRemainingRatio = effectiveRemaining.toFloat() / limitMillis.toFloat()
        )
    }

    private fun tieRank(type: RuleType): Int = when (type) {
        RuleType.SESSION -> 0
        RuleType.ROLLING_WINDOW -> 1
        RuleType.DAILY -> 2
    }

    private fun statusCopy(state: PetState): String = when (state) {
        PetState.RELAXED -> "\u8FD8\u5F88\u5145\u88D5\uFF0C\u5C0F\u9E1F\u5728\u65C1\u8FB9\u966A\u4F60\u3002"
        PetState.REMINDER -> "\u8FD8\u53EF\u4EE5\u770B\u4E00\u4F1A\u513F\uFF0C\u7559\u610F\u65F6\u95F4\u8FB9\u754C\u3002"
        PetState.TENSE -> "\u5FEB\u5230\u8FB9\u754C\u4E86\uFF0C\u5C0F\u9E1F\u4F1A\u66F4\u660E\u663E\u4E00\u70B9\u3002"
        PetState.TIMEOUT -> "\u5DF2\u7ECF\u8D85\u65F6\u5566\uFF0C\u70B9\u4E00\u4E0B\u53EF\u4EE5\u9009\u62E9\u4F11\u606F\u6216\u52A0 5 \u5206\u949F\u3002"
    }
}
