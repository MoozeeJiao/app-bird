package com.attentionpet.data

import com.attentionpet.domain.ExtensionGrant
import com.attentionpet.domain.RuleConfig
import com.attentionpet.domain.UsageInterval
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

data class AttentionPetConfigSnapshot(
    val targetApp: TargetAppConfigEntity,
    val limits: LimitConfigEntity
)

class AttentionPetRepository(
    private val configDao: ConfigDao,
    private val sessionDao: SessionDao,
    private val eventDao: EventDao
) {
    fun targetApp(): Flow<TargetAppConfigEntity?> = configDao.targetApp()
    fun limits(): Flow<LimitConfigEntity?> = configDao.limits()

    suspend fun config(): RuleConfig? = configDao.limits().first()?.toRuleConfig()

    suspend fun ensureDefaultMvpConfig(): AttentionPetConfigSnapshot {
        val target = configDao.targetApp().first() ?: defaultTargetApp().also {
            configDao.upsertTargetApp(it)
        }
        val limits = configDao.limits().first() ?: defaultLimits().also {
            configDao.upsertLimits(it)
        }
        return AttentionPetConfigSnapshot(targetApp = target, limits = limits)
    }

    suspend fun saveHomeConfig(
        packageName: String,
        displayName: String,
        dailyMinutes: Int,
        sessionMinutes: Int,
        rollingWindowLimitMinutes: Int
    ): AttentionPetConfigSnapshot {
        val target = TargetAppConfigEntity(
            packageName = packageName,
            displayName = displayName,
            enabled = true
        )
        val limits = LimitConfigEntity(
            dailyLimitMinutes = dailyMinutes,
            sessionLimitMinutes = sessionMinutes,
            rollingWindowHours = DEFAULT_ROLLING_WINDOW_HOURS,
            rollingWindowLimitMinutes = rollingWindowLimitMinutes
        )

        configDao.upsertTargetApp(target)
        configDao.upsertLimits(limits)
        return AttentionPetConfigSnapshot(targetApp = target, limits = limits)
    }

    suspend fun overlayPosition(): OverlayPositionEntity? = eventDao.overlayPosition()

    suspend fun saveOverlayPosition(entity: OverlayPositionEntity) {
        eventDao.upsertOverlayPosition(entity)
    }

    suspend fun saveTargetApp(packageName: String, displayName: String, enabled: Boolean) {
        configDao.upsertTargetApp(TargetAppConfigEntity(packageName = packageName, displayName = displayName, enabled = enabled))
    }

    suspend fun saveLimits(dailyMinutes: Int, sessionMinutes: Int, rollingWindowLimitMinutes: Int) {
        configDao.upsertLimits(
            LimitConfigEntity(
                dailyLimitMinutes = dailyMinutes,
                sessionLimitMinutes = sessionMinutes,
                rollingWindowHours = 5,
                rollingWindowLimitMinutes = rollingWindowLimitMinutes
            )
        )
    }

    fun LimitConfigEntity.toRuleConfig(): RuleConfig {
        return RuleConfig(
            dailyLimitMillis = dailyLimitMinutes * 60_000L,
            sessionLimitMillis = sessionLimitMinutes * 60_000L,
            rollingWindowLimitMillis = rollingWindowLimitMinutes * 60_000L
        )
    }

    fun UsageSessionEntity.toUsageInterval(nowMillis: Long): UsageInterval {
        return UsageInterval(startMillis = startMillis, endMillis = endMillis ?: nowMillis)
    }

    suspend fun usageIntervals(
        packageName: String,
        windowStart: Long,
        windowEnd: Long,
        nowMillis: Long
    ): List<UsageInterval> {
        return sessionDao.sessionsOverlapping(packageName, windowStart, windowEnd)
            .map { it.toUsageInterval(nowMillis) }
    }

    suspend fun openSession(packageName: String, startMillis: Long): Long {
        return sessionDao.insert(
            UsageSessionEntity(
                packageName = packageName,
                startMillis = startMillis,
                endMillis = null,
                foregroundDurationMillis = 0L,
                closeReason = "active"
            )
        )
    }

    suspend fun closeSession(sessionId: Long, endMillis: Long, foregroundDurationMillis: Long, closeReason: String) {
        val existing = sessionDao.getById(sessionId) ?: return
        sessionDao.update(existing.copy(endMillis = endMillis, foregroundDurationMillis = foregroundDurationMillis, closeReason = closeReason))
    }

    suspend fun recordExtension(sessionId: Long, timestampMillis: Long): Long {
        val existing = eventDao.extensionForSession(sessionId)
        if (existing != null) return existing.id
        val insertedId = eventDao.insertExtension(
            ExtensionEventEntity(
                sessionId = sessionId,
                timestampMillis = timestampMillis,
                addedMinutes = 5,
                consumedForegroundMillis = 0L
            )
        )
        if (insertedId != -1L) return insertedId

        return checkNotNull(eventDao.extensionForSession(sessionId)) {
            "Extension insert ignored but no existing extension found for session $sessionId"
        }.id
    }

    suspend fun extensionGrant(sessionId: Long): ExtensionGrant {
        val event = eventDao.extensionForSession(sessionId) ?: return ExtensionGrant()
        return ExtensionGrant(addedMillis = event.addedMinutes * 60_000L, consumedForegroundMillis = event.consumedForegroundMillis)
    }

    suspend fun recordTimeoutAction(sessionId: Long, timestampMillis: Long, actionType: String, overageDurationMillis: Long) {
        eventDao.insertTimeoutAction(TimeoutActionEventEntity(sessionId = sessionId, timestampMillis = timestampMillis, actionType = actionType, overageDurationMillis = overageDurationMillis))
    }

    private fun defaultTargetApp(): TargetAppConfigEntity {
        return TargetAppConfigEntity(
            packageName = DEFAULT_TARGET_PACKAGE_NAME,
            displayName = DEFAULT_TARGET_DISPLAY_NAME,
            enabled = true
        )
    }

    private fun defaultLimits(): LimitConfigEntity {
        return LimitConfigEntity(
            dailyLimitMinutes = DEFAULT_DAILY_LIMIT_MINUTES,
            sessionLimitMinutes = DEFAULT_SESSION_LIMIT_MINUTES,
            rollingWindowHours = DEFAULT_ROLLING_WINDOW_HOURS,
            rollingWindowLimitMinutes = DEFAULT_ROLLING_WINDOW_LIMIT_MINUTES
        )
    }

    companion object {
        const val DEFAULT_TARGET_PACKAGE_NAME = "com.ss.android.ugc.aweme"
        const val DEFAULT_TARGET_DISPLAY_NAME = "\u6296\u97F3"
        const val DEFAULT_DAILY_LIMIT_MINUTES = 60
        const val DEFAULT_SESSION_LIMIT_MINUTES = 15
        const val DEFAULT_ROLLING_WINDOW_LIMIT_MINUTES = 30
        const val DEFAULT_ROLLING_WINDOW_HOURS = 5
    }
}
