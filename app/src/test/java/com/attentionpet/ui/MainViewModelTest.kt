package com.attentionpet.ui

import com.attentionpet.MainDispatcherRule
import com.attentionpet.data.AttentionPetRepository
import com.attentionpet.data.ConfigDao
import com.attentionpet.data.EventDao
import com.attentionpet.data.ExtensionEventEntity
import com.attentionpet.data.LimitConfigEntity
import com.attentionpet.data.OverlayPositionEntity
import com.attentionpet.data.SessionDao
import com.attentionpet.data.TargetAppConfigEntity
import com.attentionpet.data.TimeoutActionEventEntity
import com.attentionpet.data.UsageSessionEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun initializesDefaultMvpConfigWhenRepositoryIsEmpty() = runTest {
        val repository = AttentionPetRepository(FakeConfigDao(), FakeSessionDao(), FakeEventDao())
        val viewModel = MainViewModel(
            repository = repository,
            ioDispatcher = StandardTestDispatcher(testScheduler)
        )

        advanceUntilIdle()

        val target = repository.targetApp().first()
        val limits = repository.limits().first()
        assertEquals("com.ss.android.ugc.aweme", target?.packageName)
        assertEquals("\u6296\u97F3", target?.displayName)
        assertEquals(true, target?.enabled)
        assertEquals(60, limits?.dailyLimitMinutes)
        assertEquals(15, limits?.sessionLimitMinutes)
        assertEquals(30, limits?.rollingWindowLimitMinutes)
        assertEquals("com.ss.android.ugc.aweme", viewModel.homeConfig.value.selectedTargetPackageName)
        assertEquals("\u6296\u97F3", viewModel.homeConfig.value.targetAppLabel)
    }

    @Test
    fun startMonitoringPersistsCurrentUiConfigBeforeCallback() = runTest {
        val repository = AttentionPetRepository(FakeConfigDao(), FakeSessionDao(), FakeEventDao())
        val viewModel = MainViewModel(
            repository = repository,
            ioDispatcher = StandardTestDispatcher(testScheduler)
        )
        advanceUntilIdle()

        viewModel.onTargetAppSelected(LaunchableApp(packageName = "com.example.video", label = "\u89C6\u9891"))
        viewModel.onDailyLimitChanged(45)
        viewModel.onSessionLimitChanged(12)
        viewModel.onRollingWindowLimitChanged(25)

        var started = false
        viewModel.onStartMonitoring {
            started = true
        }
        advanceUntilIdle()

        assertTrue(started)
        val target = repository.targetApp().first()
        val limits = repository.limits().first()
        assertEquals("com.example.video", target?.packageName)
        assertEquals("\u89C6\u9891", target?.displayName)
        assertEquals(45, limits?.dailyLimitMinutes)
        assertEquals(12, limits?.sessionLimitMinutes)
        assertEquals(25, limits?.rollingWindowLimitMinutes)
    }

    @Test
    fun startMonitoringMovesStatusToActiveAfterCallback() = runTest {
        val repository = AttentionPetRepository(FakeConfigDao(), FakeSessionDao(), FakeEventDao())
        val viewModel = MainViewModel(
            repository = repository,
            ioDispatcher = StandardTestDispatcher(testScheduler)
        )
        advanceUntilIdle()

        var started = false
        viewModel.onStartMonitoring {
            started = true
        }
        assertEquals(MonitoringStatus.STARTING, viewModel.monitoringStatus.value)

        advanceUntilIdle()

        assertTrue(started)
        assertEquals(MonitoringStatus.ACTIVE, viewModel.monitoringStatus.value)
    }

    @Test
    fun startMonitoringErrorsAreVisibleInStatus() = runTest {
        val repository = AttentionPetRepository(FakeConfigDao(), FakeSessionDao(), FakeEventDao())
        val viewModel = MainViewModel(
            repository = repository,
            ioDispatcher = StandardTestDispatcher(testScheduler)
        )
        advanceUntilIdle()

        viewModel.onStartMonitoring {
            error("service unavailable")
        }
        assertEquals(MonitoringStatus.STARTING, viewModel.monitoringStatus.value)

        advanceUntilIdle()

        assertEquals(MonitoringStatus.ERROR, viewModel.monitoringStatus.value)
    }

    @Test
    fun delayedDefaultInitializationDoesNotOverwriteUserSelectionBeforeStart() = runTest {
        val configDao = DelayedConfigDao()
        val repository = AttentionPetRepository(configDao, FakeSessionDao(), FakeEventDao())
        val viewModel = MainViewModel(
            repository = repository,
            ioDispatcher = StandardTestDispatcher(testScheduler)
        )

        viewModel.onTargetAppSelected(LaunchableApp(packageName = "com.example.fast", label = "\u5FEB\u901F"))
        configDao.releaseInitialRead()
        advanceUntilIdle()

        viewModel.onStartMonitoring {}
        advanceUntilIdle()

        assertEquals("com.example.fast", viewModel.homeConfig.value.selectedTargetPackageName)
        assertEquals("\u5FEB\u901F", viewModel.homeConfig.value.targetAppLabel)
        assertEquals("com.example.fast", repository.targetApp().first()?.packageName)
    }

    @Test
    fun stalePendingEditSaveCannotOverwriteConfigSavedForStart() = runTest {
        val configDao = BlockingOldTargetConfigDao()
        val repository = AttentionPetRepository(configDao, FakeSessionDao(), FakeEventDao())
        val viewModel = MainViewModel(
            repository = repository,
            ioDispatcher = StandardTestDispatcher(testScheduler)
        )
        advanceUntilIdle()

        viewModel.onTargetAppSelected(LaunchableApp(packageName = "com.example.old", label = "\u65E7\u914D\u7F6E"))
        advanceUntilIdle()
        assertTrue(configDao.oldSaveStarted.isCompleted)

        viewModel.onTargetAppSelected(LaunchableApp(packageName = "com.example.current", label = "\u5F53\u524D\u914D\u7F6E"))
        var started = false
        viewModel.onStartMonitoring {
            started = true
        }
        advanceUntilIdle()

        assertTrue(started)
        assertEquals("com.example.current", repository.targetApp().first()?.packageName)

        configDao.releaseOldSave()
        advanceUntilIdle()

        assertEquals("com.example.current", repository.targetApp().first()?.packageName)
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

private class DelayedConfigDao : ConfigDao {
    private val release = CompletableDeferred<Unit>()
    private val targetApp = MutableStateFlow<TargetAppConfigEntity?>(null)
    private val limits = MutableStateFlow<LimitConfigEntity?>(null)

    fun releaseInitialRead() {
        release.complete(Unit)
    }

    override fun targetApp(): Flow<TargetAppConfigEntity?> = flow {
        release.await()
        emit(targetApp.value)
    }

    override fun limits(): Flow<LimitConfigEntity?> = flow {
        release.await()
        emit(limits.value)
    }

    override suspend fun upsertTargetApp(entity: TargetAppConfigEntity) {
        targetApp.value = entity
    }

    override suspend fun upsertLimits(entity: LimitConfigEntity) {
        limits.value = entity
    }
}

private class BlockingOldTargetConfigDao : ConfigDao {
    val oldSaveStarted = CompletableDeferred<Unit>()
    private val releaseOldSave = CompletableDeferred<Unit>()
    private val targetApp = MutableStateFlow<TargetAppConfigEntity?>(
        TargetAppConfigEntity(
            packageName = AttentionPetRepository.DEFAULT_TARGET_PACKAGE_NAME,
            displayName = AttentionPetRepository.DEFAULT_TARGET_DISPLAY_NAME,
            enabled = true
        )
    )
    private val limits = MutableStateFlow<LimitConfigEntity?>(
        LimitConfigEntity(
            dailyLimitMinutes = AttentionPetRepository.DEFAULT_DAILY_LIMIT_MINUTES,
            sessionLimitMinutes = AttentionPetRepository.DEFAULT_SESSION_LIMIT_MINUTES,
            rollingWindowHours = AttentionPetRepository.DEFAULT_ROLLING_WINDOW_HOURS,
            rollingWindowLimitMinutes = AttentionPetRepository.DEFAULT_ROLLING_WINDOW_LIMIT_MINUTES
        )
    )

    fun releaseOldSave() {
        releaseOldSave.complete(Unit)
    }

    override fun targetApp(): Flow<TargetAppConfigEntity?> = targetApp

    override fun limits(): Flow<LimitConfigEntity?> = limits

    override suspend fun upsertTargetApp(entity: TargetAppConfigEntity) {
        if (entity.packageName == "com.example.old" && !oldSaveStarted.isCompleted) {
            oldSaveStarted.complete(Unit)
            releaseOldSave.await()
        }
        targetApp.value = entity
    }

    override suspend fun upsertLimits(entity: LimitConfigEntity) {
        limits.value = entity
    }
}

private class FakeSessionDao : SessionDao {
    override suspend fun insert(entity: UsageSessionEntity): Long = 1L

    override suspend fun update(entity: UsageSessionEntity) = Unit

    override suspend fun getById(id: Long): UsageSessionEntity? = null

    override suspend fun sessionsOverlapping(
        packageName: String,
        windowStart: Long,
        windowEnd: Long
    ): List<UsageSessionEntity> = emptyList()
}

private class FakeEventDao : EventDao {
    override suspend fun insertExtension(entity: ExtensionEventEntity): Long = 1L

    override suspend fun extensionForSession(sessionId: Long): ExtensionEventEntity? = null

    override suspend fun insertTimeoutAction(entity: TimeoutActionEventEntity): Long = 1L

    override suspend fun overlayPosition(): OverlayPositionEntity? = null

    override suspend fun upsertOverlayPosition(entity: OverlayPositionEntity) = Unit
}
