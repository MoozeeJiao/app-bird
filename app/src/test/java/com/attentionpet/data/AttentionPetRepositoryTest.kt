package com.attentionpet.data

import com.attentionpet.domain.UsageInterval
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AttentionPetRepositoryTest {
    @Test
    fun ensureDefaultMvpConfigCreatesMissingTargetAndLimits() = runTest {
        val repository = AttentionPetRepository(FakeConfigDao(), FakeSessionDao(), FakeEventDao())

        val snapshot = repository.ensureDefaultMvpConfig()

        assertEquals("com.ss.android.ugc.aweme", snapshot.targetApp.packageName)
        assertEquals("\u6296\u97F3", snapshot.targetApp.displayName)
        assertEquals(true, snapshot.targetApp.enabled)
        assertEquals(60, snapshot.limits.dailyLimitMinutes)
        assertEquals(15, snapshot.limits.sessionLimitMinutes)
        assertEquals(5, snapshot.limits.rollingWindowHours)
        assertEquals(30, snapshot.limits.rollingWindowLimitMinutes)
    }

    @Test
    fun saveHomeConfigPersistsTargetAndLimitRowsTogether() = runTest {
        val repository = AttentionPetRepository(FakeConfigDao(), FakeSessionDao(), FakeEventDao())

        val snapshot = repository.saveHomeConfig(
            packageName = "com.example.video",
            displayName = "\u89C6\u9891",
            dailyMinutes = 45,
            sessionMinutes = 12,
            rollingWindowLimitMinutes = 25
        )

        assertEquals("com.example.video", snapshot.targetApp.packageName)
        assertEquals("\u89C6\u9891", snapshot.targetApp.displayName)
        assertEquals(true, snapshot.targetApp.enabled)
        assertEquals(45, snapshot.limits.dailyLimitMinutes)
        assertEquals(12, snapshot.limits.sessionLimitMinutes)
        assertEquals(25, snapshot.limits.rollingWindowLimitMinutes)
    }

    @Test
    fun readsAndWritesOverlayPosition() = runTest {
        val eventDao = FakeEventDao()
        val repository = AttentionPetRepository(FakeConfigDao(), FakeSessionDao(), eventDao)

        assertNull(repository.overlayPosition())

        val position = OverlayPositionEntity(
            edge = "right",
            verticalRatio = 0.42f,
            updatedAtMillis = 123_000L
        )

        repository.saveOverlayPosition(position)

        assertEquals(position, repository.overlayPosition())
    }

    @Test
    fun usageIntervalsMapsOverlappingSessionsWithNowForActiveSessions() = runTest {
        val sessionDao = FakeSessionDao()
        val repository = AttentionPetRepository(FakeConfigDao(), sessionDao, FakeEventDao())
        sessionDao.overlappingSessions = listOf(
            UsageSessionEntity(
                id = 1L,
                packageName = "com.example.target",
                startMillis = 1_000L,
                endMillis = 2_000L,
                foregroundDurationMillis = 1_000L,
                closeReason = "closed"
            ),
            UsageSessionEntity(
                id = 2L,
                packageName = "com.example.target",
                startMillis = 3_000L,
                endMillis = null,
                foregroundDurationMillis = 500L,
                closeReason = "active"
            )
        )

        val intervals = repository.usageIntervals(
            packageName = "com.example.target",
            windowStart = 0L,
            windowEnd = 10_000L,
            nowMillis = 4_000L
        )

        assertEquals(
            listOf(
                UsageInterval(startMillis = 1_000L, endMillis = 2_000L),
                UsageInterval(startMillis = 3_000L, endMillis = 4_000L)
            ),
            intervals
        )
        assertEquals(
            OverlapQuery("com.example.target", 0L, 10_000L),
            sessionDao.lastOverlapQuery
        )
    }

    @Test
    fun recordExtensionReturnsExistingIdWhenDuplicateInsertIsIgnored() = runTest {
        val eventDao = FakeEventDao()
        val repository = AttentionPetRepository(FakeConfigDao(), FakeSessionDao(), eventDao)
        val existing = ExtensionEventEntity(
            id = 42L,
            sessionId = 7L,
            timestampMillis = 90_000L,
            addedMinutes = 5,
            consumedForegroundMillis = 0L
        )
        eventDao.extensionResponses.add(null)
        eventDao.extensionResponses.add(existing)
        eventDao.insertExtensionResult = -1L

        val id = repository.recordExtension(sessionId = 7L, timestampMillis = 100_000L)

        assertEquals(42L, id)
    }
}

private class FakeConfigDao : ConfigDao {
    private val targetApp = MutableStateFlow<TargetAppConfigEntity?>(null)
    private val limits = MutableStateFlow<LimitConfigEntity?>(null)

    override fun targetApp(): Flow<TargetAppConfigEntity?> = targetApp

    override fun limits(): Flow<LimitConfigEntity?> = limits

    override suspend fun upsertTargetApp(entity: TargetAppConfigEntity) {
        targetApp.value = entity
    }

    override suspend fun upsertLimits(entity: LimitConfigEntity) {
        limits.value = entity
    }
}

private data class OverlapQuery(
    val packageName: String,
    val windowStart: Long,
    val windowEnd: Long
)

private class FakeSessionDao : SessionDao {
    private val sessions = mutableMapOf<Long, UsageSessionEntity>()
    private var nextId = 1L

    var overlappingSessions: List<UsageSessionEntity> = emptyList()
    var lastOverlapQuery: OverlapQuery? = null

    override suspend fun insert(entity: UsageSessionEntity): Long {
        val id = nextId++
        sessions[id] = entity.copy(id = id)
        return id
    }

    override suspend fun update(entity: UsageSessionEntity) {
        sessions[entity.id] = entity
    }

    override suspend fun getById(id: Long): UsageSessionEntity? = sessions[id]

    override suspend fun sessionsOverlapping(
        packageName: String,
        windowStart: Long,
        windowEnd: Long
    ): List<UsageSessionEntity> {
        lastOverlapQuery = OverlapQuery(packageName, windowStart, windowEnd)
        return overlappingSessions
    }
}

private class FakeEventDao : EventDao {
    private var overlayPosition: OverlayPositionEntity? = null
    private var extension: ExtensionEventEntity? = null
    private var nextExtensionId = 1L

    val extensionResponses = mutableListOf<ExtensionEventEntity?>()
    var insertExtensionResult: Long? = null

    override suspend fun insertExtension(entity: ExtensionEventEntity): Long {
        insertExtensionResult?.let { return it }

        val id = nextExtensionId++
        extension = entity.copy(id = id)
        return id
    }

    override suspend fun extensionForSession(sessionId: Long): ExtensionEventEntity? {
        if (extensionResponses.isNotEmpty()) {
            return extensionResponses.removeAt(0)
        }

        return extension?.takeIf { it.sessionId == sessionId }
    }

    override suspend fun insertTimeoutAction(entity: TimeoutActionEventEntity): Long = 1L

    override suspend fun overlayPosition(): OverlayPositionEntity? = overlayPosition

    override suspend fun upsertOverlayPosition(entity: OverlayPositionEntity) {
        overlayPosition = entity
    }
}
