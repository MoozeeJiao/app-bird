package com.attentionpet.data

import com.attentionpet.domain.ExtensionGrant
import com.attentionpet.domain.RuleConfig
import com.attentionpet.domain.UsageInterval
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class AttentionPetRepository(
    private val configDao: ConfigDao,
    private val sessionDao: SessionDao,
    private val eventDao: EventDao
) {
    fun targetApp(): Flow<TargetAppConfigEntity?> = configDao.targetApp()
    fun limits(): Flow<LimitConfigEntity?> = configDao.limits()

    suspend fun config(): RuleConfig? = configDao.limits().first()?.toRuleConfig()

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
        return eventDao.insertExtension(
            ExtensionEventEntity(
                sessionId = sessionId,
                timestampMillis = timestampMillis,
                addedMinutes = 5,
                consumedForegroundMillis = 0L
            )
        )
    }

    suspend fun extensionGrant(sessionId: Long): ExtensionGrant {
        val event = eventDao.extensionForSession(sessionId) ?: return ExtensionGrant()
        return ExtensionGrant(addedMillis = event.addedMinutes * 60_000L, consumedForegroundMillis = event.consumedForegroundMillis)
    }

    suspend fun recordTimeoutAction(sessionId: Long, timestampMillis: Long, actionType: String, overageDurationMillis: Long) {
        eventDao.insertTimeoutAction(TimeoutActionEventEntity(sessionId = sessionId, timestampMillis = timestampMillis, actionType = actionType, overageDurationMillis = overageDurationMillis))
    }
}
