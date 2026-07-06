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
    fun extensionAppliesToEffectiveRemainingAcrossBuckets() {
        val result = RuleEvaluator.evaluate(
            nowMillis = now,
            zoneId = zone,
            config = config,
            sessions = emptyList(),
            activeSession = ActiveSession(startMillis = now - 16 * 60_000L, foregroundMillis = 16 * 60_000L),
            extensionGrant = ExtensionGrant(addedMillis = 5 * 60_000L, consumedForegroundMillis = 0L)
        )
        assertEquals(PetState.TENSE, result.petState)
        assertEquals(4 * 60_000L, result.effectiveRemainingMillis)
    }
}
