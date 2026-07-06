package com.attentionpet.overlay

import com.attentionpet.domain.PetState
import com.attentionpet.domain.RuleBucket
import com.attentionpet.domain.RuleEvaluationResult
import com.attentionpet.domain.RuleType
import org.junit.Assert.assertEquals
import org.junit.Test

class OverlayViewsTest {
    @Test
    fun capsuleRemainingShowsMinutesBeforeTimeout() {
        val result = result(
            petState = PetState.RELAXED,
            effectiveRemainingMillis = 8 * 60_000L
        )

        assertEquals("8m", displayRemaining(result))
    }

    @Test
    fun capsuleRemainingShowsOvertimeMinutesWithPlus() {
        val result = result(
            petState = PetState.TIMEOUT,
            effectiveRemainingMillis = -6 * 60_000L
        )

        assertEquals("+6m", displayRemaining(result))
    }

    @Test
    fun expandedPanelMetricLinesShowTodayRollingSessionAndStatus() {
        val result = result(
            daily = bucket(RuleType.DAILY, usedMinutes = 42, limitMinutes = 90),
            rollingWindow = bucket(RuleType.ROLLING_WINDOW, usedMinutes = 18, limitMinutes = 30),
            session = bucket(RuleType.SESSION, usedMinutes = 11, limitMinutes = 15),
            petState = PetState.TENSE,
            statusCopy = "\u5FEB\u5230\u8FB9\u754C\u4E86"
        )

        assertEquals(
            listOf(
                "\u4ECA\u65E5 42 / 90 \u5206\u949F",
                "\u8FD1 5 \u5C0F\u65F6 18 / 30 \u5206\u949F",
                "\u5F53\u524D\u8FDE\u7EED\u4F7F\u7528 \u8FDE\u7EED 11 \u5206\u949F",
                "\u72B6\u6001 \u5FEB\u5230\u8FB9\u754C\u4E86"
            ),
            expandedPanelMetricLines(result, currentSessionText = "\u8FDE\u7EED 11 \u5206\u949F")
        )
    }

    private fun result(
        daily: RuleBucket = bucket(RuleType.DAILY, usedMinutes = 10, limitMinutes = 60),
        session: RuleBucket = bucket(RuleType.SESSION, usedMinutes = 5, limitMinutes = 15),
        rollingWindow: RuleBucket = bucket(RuleType.ROLLING_WINDOW, usedMinutes = 12, limitMinutes = 30),
        petState: PetState,
        effectiveRemainingMillis: Long = session.effectiveRemainingMillis,
        statusCopy: String = "\u8FD8\u5F88\u5145\u88D5\uFF0C\u5C0F\u9E1F\u5728\u65C1\u8FB9\u966A\u4F60\u3002"
    ): RuleEvaluationResult = RuleEvaluationResult(
        daily = daily,
        session = session,
        rollingWindow = rollingWindow,
        triggeringRule = RuleType.SESSION,
        effectiveRemainingMillis = effectiveRemainingMillis,
        effectiveRemainingRatio = 0.5f,
        petState = petState,
        statusCopy = statusCopy,
        activeExtensionRemainingMillis = 0L
    )

    private fun bucket(type: RuleType, usedMinutes: Long, limitMinutes: Long): RuleBucket {
        val usedMillis = usedMinutes * 60_000L
        val limitMillis = limitMinutes * 60_000L
        val remainingMillis = limitMillis - usedMillis
        return RuleBucket(
            type = type,
            usedMillis = usedMillis,
            limitMillis = limitMillis,
            rawRemainingMillis = remainingMillis,
            effectiveRemainingMillis = remainingMillis,
            rawRemainingRatio = remainingMillis.toFloat() / limitMillis.toFloat(),
            effectiveRemainingRatio = remainingMillis.toFloat() / limitMillis.toFloat()
        )
    }
}
