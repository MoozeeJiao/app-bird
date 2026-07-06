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

    @Test
    fun timeoutSheetCopyIncludesRequiredLabelsAndBirdFeedback() {
        assertEquals("\u5DF2\u7ECF\u8D85\u65F6\u5566", TimeoutSheetCopy.title)
        assertEquals("\u8981\u4E0D\u8981\u4F11\u606F\u4E00\u4E0B\uFF1F", TimeoutSheetCopy.question)
        assertEquals(
            "\u5C0F\u9E1F\u6709\u70B9\u7740\u6025\u4E86\uFF0C\u5148\u628A\u65F6\u95F4\u8FB9\u754C\u6536\u56DE\u6765\u3002",
            TimeoutSheetCopy.defaultFeedback
        )
        assertEquals("\u4F11\u606F\u4E00\u4E0B", TimeoutSheetCopy.restButton)
        assertEquals("\u518D\u52A0 5 \u5206\u949F", TimeoutSheetCopy.extendButton)
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
