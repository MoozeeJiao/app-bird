package com.attentionpet.domain

enum class PetState(val serialized: String, val labelZh: String) {
    RELAXED("relaxed", "\u7039\u590A\u53CF / \u93C0\u70AC\u6F97"),
    REMINDER("reminder", "\u93BB\u6130\u554B"),
    TENSE("tense", "\u7EF1\u0443\u7D36"),
    TIMEOUT("timeout", "\u74D2\u546E\u6902")
}

enum class RuleType {
    SESSION,
    ROLLING_WINDOW,
    DAILY
}

data class RuleConfig(
    val dailyLimitMillis: Long,
    val sessionLimitMillis: Long,
    val rollingWindowLimitMillis: Long,
    val rollingWindowMillis: Long = 5L * 60L * 60L * 1000L,
    val sessionGraceMillis: Long = 30_000L
) {
    init {
        require(dailyLimitMillis > 0L) { "dailyLimitMillis must be positive" }
        require(sessionLimitMillis > 0L) { "sessionLimitMillis must be positive" }
        require(rollingWindowLimitMillis > 0L) { "rollingWindowLimitMillis must be positive" }
        require(rollingWindowMillis == 5L * 60L * 60L * 1000L) { "rolling window is fixed to 5 hours for MVP" }
        require(sessionGraceMillis == 30_000L) { "session grace is fixed to 30 seconds for MVP" }
    }
}

data class UsageInterval(
    val startMillis: Long,
    val endMillis: Long
)

data class ActiveSession(
    val startMillis: Long,
    val foregroundMillis: Long
)

data class ExtensionGrant(
    val addedMillis: Long = 0L,
    val consumedForegroundMillis: Long = 0L
) {
    val remainingMillis: Long
        get() = (addedMillis - consumedForegroundMillis).coerceAtLeast(0L)
}

data class RuleBucket(
    val type: RuleType,
    val usedMillis: Long,
    val limitMillis: Long,
    val rawRemainingMillis: Long,
    val effectiveRemainingMillis: Long,
    val rawRemainingRatio: Float,
    val effectiveRemainingRatio: Float
)

data class RuleEvaluationResult(
    val daily: RuleBucket,
    val session: RuleBucket,
    val rollingWindow: RuleBucket,
    val triggeringRule: RuleType,
    val effectiveRemainingMillis: Long,
    val effectiveRemainingRatio: Float,
    val petState: PetState,
    val statusCopy: String,
    val activeExtensionRemainingMillis: Long
)
