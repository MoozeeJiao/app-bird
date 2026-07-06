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
        val activeExtensionRemaining = extensionGrant.remainingMillis
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
            ordered.any { it.rawRemainingMillis <= 0L } -> PetState.TENSE
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
        PetState.RELAXED -> "\u6769\u6A3A\u7DE2\u934F\u5470\uE5DA\u951B\u5C7D\u76AC\u6966\u71B7\u6E6A\u93C3\u4F7D\u7ADF\u95C4\uE043\u7D98\u9286?"
        PetState.REMINDER -> "\u6769\u6A3A\u5F72\u6D60\u30E7\u6E45\u6D93\u20AC\u6D7C\u6C2C\u52B9\u951B\u5C80\u6680\u93B0\u5FD4\u6902\u95C2\u78CB\u7ADF\u9423\u5C7B\u20AC?"
        PetState.TENSE -> "\u8E47\uE0A2\u57CC\u6748\u572D\u666B\u6D5C\u55ED\u7D1D\u704F\u5FDB\u7B29\u6D7C\u6C2D\u6D3F\u93C4\u5EA2\u6A09\u6D93\u20AC\u9410\u5E7F\u20AC?"
        PetState.TIMEOUT -> "\u5BB8\u832C\u7CA1\u74D2\u546E\u6902\u935F\uFE3C\u7D1D\u9410\u901B\u7AF4\u6D93\u5B2A\u5F72\u6D60\u30E9\u20AC\u590B\u5AE8\u6D7C\u621E\u4F05\u93B4\u6827\u59DE 5 \u9352\u55DB\u6313\u9286?"
    }
}
