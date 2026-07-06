package com.attentionpet.domain

import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class RuleEvaluatorTest {
    private val zone = ZoneId.of("Asia/Shanghai")
    private val now = Instant.parse("2026-07-06T04:00:00Z").toEpochMilli()
    private val config = RuleConfig(
        dailyLimitMillis = 60 * 60_000L,
        sessionLimitMillis = 15 * 60_000L,
        rollingWindowLimitMillis = 30 * 60_000L
    )
    private val localStartOfDay = Instant.parse("2026-07-05T16:00:00Z").toEpochMilli()

    @Test
    fun petStateLabelsUseIntendedChineseCopy() {
        assertEquals("\u5B89\u5168 / \u653E\u677E", PetState.RELAXED.labelZh)
        assertEquals("\u63D0\u9192", PetState.REMINDER.labelZh)
        assertEquals("\u7D27\u5F20", PetState.TENSE.labelZh)
        assertEquals("\u8D85\u65F6", PetState.TIMEOUT.labelZh)
    }

    @Test
    fun relaxedWhenAllRulesHaveMoreThanHalfRemaining() {
        val result = RuleEvaluator.evaluate(
            nowMillis = now,
            zoneId = zone,
            config = config,
            sessions = listOf(UsageInterval(now - 10 * 60_000L, now - 5 * 60_000L)),
            activeSession = ActiveSession(startMillis = now - 2 * 60_000L, foregroundMillis = 2 * 60_000L),
            extensionGrant = ExtensionGrant()
        )
        assertEquals(PetState.RELAXED, result.petState)
        assertEquals(RuleType.SESSION, result.triggeringRule)
    }

    @Test
    fun relaxedStatusCopyUsesIntendedChineseCopy() {
        val result = RuleEvaluator.evaluate(
            nowMillis = now,
            zoneId = zone,
            config = config,
            sessions = listOf(UsageInterval(now - 10 * 60_000L, now - 5 * 60_000L)),
            activeSession = ActiveSession(startMillis = now - 2 * 60_000L, foregroundMillis = 2 * 60_000L),
            extensionGrant = ExtensionGrant()
        )
        assertEquals("\u8FD8\u5F88\u5145\u88D5\uFF0C\u5C0F\u9E1F\u5728\u65C1\u8FB9\u966A\u4F60\u3002", result.statusCopy)
    }

    @Test
    fun reminderStatusCopyUsesIntendedChineseCopy() {
        val result = RuleEvaluator.evaluate(
            nowMillis = now,
            zoneId = zone,
            config = config,
            sessions = emptyList(),
            activeSession = ActiveSession(startMillis = now - 10 * 60_000L, foregroundMillis = 10 * 60_000L),
            extensionGrant = ExtensionGrant()
        )
        assertEquals(PetState.REMINDER, result.petState)
        assertEquals("\u8FD8\u53EF\u4EE5\u770B\u4E00\u4F1A\u513F\uFF0C\u7559\u610F\u65F6\u95F4\u8FB9\u754C\u3002", result.statusCopy)
    }

    @Test
    fun tenseStatusCopyUsesIntendedChineseCopy() {
        val result = RuleEvaluator.evaluate(
            nowMillis = now,
            zoneId = zone,
            config = config,
            sessions = emptyList(),
            activeSession = ActiveSession(startMillis = now - 16 * 60_000L, foregroundMillis = 16 * 60_000L),
            extensionGrant = ExtensionGrant(addedMillis = 3 * 60_000L, consumedForegroundMillis = 0L)
        )
        assertEquals(PetState.TENSE, result.petState)
        assertEquals("\u5FEB\u5230\u8FB9\u754C\u4E86\uFF0C\u5C0F\u9E1F\u4F1A\u66F4\u660E\u663E\u4E00\u70B9\u3002", result.statusCopy)
    }

    @Test
    fun timeoutWhenAnyRuleIsExceeded() {
        val result = RuleEvaluator.evaluate(
            nowMillis = now,
            zoneId = zone,
            config = config,
            sessions = emptyList(),
            activeSession = ActiveSession(startMillis = now - 16 * 60_000L, foregroundMillis = 16 * 60_000L),
            extensionGrant = ExtensionGrant()
        )
        assertEquals(PetState.TIMEOUT, result.petState)
        assertEquals(RuleType.SESSION, result.triggeringRule)
    }

    @Test
    fun timeoutStatusCopyUsesIntendedChineseCopy() {
        val result = RuleEvaluator.evaluate(
            nowMillis = now,
            zoneId = zone,
            config = config,
            sessions = emptyList(),
            activeSession = ActiveSession(startMillis = now - 16 * 60_000L, foregroundMillis = 16 * 60_000L),
            extensionGrant = ExtensionGrant()
        )
        assertEquals(PetState.TIMEOUT, result.petState)
        assertEquals("\u5DF2\u7ECF\u8D85\u65F6\u5566\uFF0C\u70B9\u4E00\u4E0B\u53EF\u4EE5\u9009\u62E9\u4F11\u606F\u6216\u52A0 5 \u5206\u949F\u3002", result.statusCopy)
    }

    @Test
    fun extensionCoveredOverrunUsesReminderWhenEffectiveRatioIsBetweenTwentyAndFiftyPercent() {
        val result = RuleEvaluator.evaluate(
            nowMillis = now,
            zoneId = zone,
            config = config,
            sessions = emptyList(),
            activeSession = ActiveSession(startMillis = now - 16 * 60_000L, foregroundMillis = 16 * 60_000L),
            extensionGrant = ExtensionGrant(addedMillis = 5 * 60_000L, consumedForegroundMillis = 0L)
        )
        assertEquals(PetState.REMINDER, result.petState)
        assertEquals(4 * 60_000L, result.effectiveRemainingMillis)
    }

    @Test
    fun extensionCoveredOverrunUsesRelaxedWhenEffectiveRatioIsAboveFiftyPercent() {
        val result = RuleEvaluator.evaluate(
            nowMillis = now,
            zoneId = zone,
            config = config,
            sessions = emptyList(),
            activeSession = ActiveSession(startMillis = now - 16 * 60_000L, foregroundMillis = 16 * 60_000L),
            extensionGrant = ExtensionGrant(addedMillis = 10 * 60_000L, consumedForegroundMillis = 0L)
        )
        assertEquals(PetState.RELAXED, result.petState)
        assertEquals(9 * 60_000L, result.effectiveRemainingMillis)
    }

    @Test
    fun triggeringRuleUsesNearestEffectiveRemainingMillisWhenRatioDiffers() {
        val result = RuleEvaluator.evaluate(
            nowMillis = now,
            zoneId = zone,
            config = RuleConfig(
                dailyLimitMillis = 100 * 60_000L,
                sessionLimitMillis = 15 * 60_000L,
                rollingWindowLimitMillis = 30 * 60_000L
            ),
            sessions = listOf(UsageInterval(localStartOfDay + 30 * 60_000L, localStartOfDay + 105 * 60_000L)),
            activeSession = ActiveSession(startMillis = now - 5 * 60_000L, foregroundMillis = 5 * 60_000L),
            extensionGrant = ExtensionGrant()
        )
        assertEquals(10 * 60_000L, result.session.effectiveRemainingMillis)
        assertEquals(20 * 60_000L, result.daily.effectiveRemainingMillis)
        assertEquals(RuleType.SESSION, result.triggeringRule)
    }

    @Test
    fun triggeringRuleTieOrderPrefersSessionBeforeRollingBeforeDaily() {
        val result = RuleEvaluator.evaluate(
            nowMillis = now,
            zoneId = zone,
            config = config,
            sessions = listOf(UsageInterval(now - 20 * 60_000L, now - 5 * 60_000L)),
            activeSession = ActiveSession(startMillis = now - 5 * 60_000L, foregroundMillis = 5 * 60_000L),
            extensionGrant = ExtensionGrant()
        )
        assertEquals(10 * 60_000L, result.session.effectiveRemainingMillis)
        assertEquals(10 * 60_000L, result.rollingWindow.effectiveRemainingMillis)
        assertEquals(RuleType.SESSION, result.triggeringRule)
    }

    @Test
    fun dailyUsageCountsOnlyLocalDayOverlap() {
        val result = RuleEvaluator.evaluate(
            nowMillis = now,
            zoneId = zone,
            config = config,
            sessions = listOf(
                UsageInterval(localStartOfDay - 30 * 60_000L, localStartOfDay + 10 * 60_000L),
                UsageInterval(localStartOfDay - 120 * 60_000L, localStartOfDay - 60 * 60_000L)
            ),
            activeSession = null,
            extensionGrant = ExtensionGrant()
        )
        assertEquals(10 * 60_000L, result.daily.usedMillis)
    }

    @Test
    fun rollingWindowUsageCountsHistoricalOverlapInsideFixedFiveHourWindow() {
        val rollingStart = now - 5L * 60L * 60L * 1000L
        val result = RuleEvaluator.evaluate(
            nowMillis = now,
            zoneId = zone,
            config = config,
            sessions = listOf(
                UsageInterval(rollingStart - 30 * 60_000L, rollingStart - 5 * 60_000L),
                UsageInterval(rollingStart - 10 * 60_000L, rollingStart + 5 * 60_000L),
                UsageInterval(now - 30 * 60_000L, now - 20 * 60_000L)
            ),
            activeSession = null,
            extensionGrant = ExtensionGrant()
        )
        assertEquals(15 * 60_000L, result.rollingWindow.usedMillis)
    }

    @Test
    fun partiallyConsumedExtensionReducesActiveExtensionAndBucketEffectiveValues() {
        val result = RuleEvaluator.evaluate(
            nowMillis = now,
            zoneId = zone,
            config = config,
            sessions = emptyList(),
            activeSession = ActiveSession(startMillis = now - 16 * 60_000L, foregroundMillis = 16 * 60_000L),
            extensionGrant = ExtensionGrant(addedMillis = 5 * 60_000L, consumedForegroundMillis = 2 * 60_000L)
        )
        assertEquals(3 * 60_000L, result.activeExtensionRemainingMillis)
        assertEquals(2 * 60_000L, result.session.effectiveRemainingMillis)
        assertEquals(17 * 60_000L, result.rollingWindow.effectiveRemainingMillis)
        assertEquals(PetState.TENSE, result.petState)
    }
}
