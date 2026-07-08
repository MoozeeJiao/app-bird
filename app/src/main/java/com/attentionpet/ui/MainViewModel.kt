package com.attentionpet.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.attentionpet.data.AttentionPetConfigSnapshot
import com.attentionpet.data.AttentionPetRepository
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

enum class MonitoringStatus {
    IDLE,
    STARTING,
    ACTIVE,
    ERROR
}

class MainViewModel(
    private val repository: AttentionPetRepository,
    internal val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {
    private val _homeConfig = MutableStateFlow(defaultHomeConfig())
    internal val homeConfig: StateFlow<HomeConfigState> = _homeConfig.asStateFlow()
    private val _monitoringStatus = MutableStateFlow(MonitoringStatus.IDLE)
    val monitoringStatus: StateFlow<MonitoringStatus> = _monitoringStatus.asStateFlow()
    private val userEditedConfig = AtomicBoolean(false)
    private val configVersion = AtomicLong(0L)
    private val saveMutex = Mutex()
    private var pendingSaveJob: Job? = null

    private val initializationJob: Job = viewModelScope.launch(ioDispatcher) {
        val savedConfig = repository.ensureDefaultMvpConfig().toHomeConfigState()
        if (!userEditedConfig.get()) {
            _homeConfig.value = savedConfig
        }
    }

    fun onTargetAppSelected(app: LaunchableApp) {
        userEditedConfig.set(true)
        val updated = _homeConfig.value.selectTarget(app)
        _homeConfig.value = updated
        saveCurrentConfigAsync(updated, configVersion.incrementAndGet())
    }

    fun onDailyLimitChanged(minutes: Int) {
        userEditedConfig.set(true)
        val updated = _homeConfig.value.updateDailyLimit(minutes)
        _homeConfig.value = updated
        saveCurrentConfigAsync(updated, configVersion.incrementAndGet())
    }

    fun onSessionLimitChanged(minutes: Int) {
        userEditedConfig.set(true)
        val updated = _homeConfig.value.updateSessionLimit(minutes)
        _homeConfig.value = updated
        saveCurrentConfigAsync(updated, configVersion.incrementAndGet())
    }

    fun onRollingWindowLimitChanged(minutes: Int) {
        userEditedConfig.set(true)
        val updated = _homeConfig.value.updateRollingWindowLimit(minutes)
        _homeConfig.value = updated
        saveCurrentConfigAsync(updated, configVersion.incrementAndGet())
    }

    fun onStartMonitoring(startService: () -> Unit) {
        _monitoringStatus.value = MonitoringStatus.STARTING
        viewModelScope.launch {
            try {
                initializationJob.join()
                pendingSaveJob?.cancelAndJoin()
                configVersion.incrementAndGet()
                withContext(ioDispatcher) {
                    saveMutex.withLock {
                        repository.saveHomeConfig(_homeConfig.value)
                    }
                }
                startService()
                _monitoringStatus.value = MonitoringStatus.ACTIVE
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                _monitoringStatus.value = MonitoringStatus.ERROR
            }
        }
    }

    private fun saveCurrentConfigAsync(config: HomeConfigState, version: Long) {
        pendingSaveJob?.cancel()
        pendingSaveJob = viewModelScope.launch(ioDispatcher) {
            initializationJob.join()
            saveMutex.withLock {
                if (version == configVersion.get()) {
                    repository.saveHomeConfig(config)
                }
            }
        }
    }

    private suspend fun AttentionPetRepository.saveHomeConfig(config: HomeConfigState): AttentionPetConfigSnapshot {
        val packageName = config.selectedTargetPackageName ?: AttentionPetRepository.DEFAULT_TARGET_PACKAGE_NAME
        val displayName = when {
            config.targetAppLabel.isBlank() -> defaultDisplayName(packageName)
            config.targetAppLabel == HomeScreenCopy.emptyTargetLabel -> defaultDisplayName(packageName)
            else -> config.targetAppLabel
        }
        return saveHomeConfig(
            packageName = packageName,
            displayName = displayName,
            dailyMinutes = config.dailyLimitMinutes,
            sessionMinutes = config.sessionLimitMinutes,
            rollingWindowLimitMinutes = config.rollingWindowLimitMinutes
        )
    }

    private fun AttentionPetConfigSnapshot.toHomeConfigState(): HomeConfigState {
        return HomeConfigState(
            selectedTargetPackageName = targetApp.packageName,
            targetAppLabel = targetApp.displayName.ifBlank { targetApp.packageName },
            dailyLimitMinutes = limits.dailyLimitMinutes,
            sessionLimitMinutes = limits.sessionLimitMinutes,
            rollingWindowLimitMinutes = limits.rollingWindowLimitMinutes
        )
    }

    companion object {
        fun factory(repository: AttentionPetRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                        return MainViewModel(repository) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class ${modelClass.name}")
                }
            }
        }

        private fun defaultHomeConfig(): HomeConfigState {
            return HomeConfigState(
                selectedTargetPackageName = AttentionPetRepository.DEFAULT_TARGET_PACKAGE_NAME,
                targetAppLabel = AttentionPetRepository.DEFAULT_TARGET_DISPLAY_NAME,
                dailyLimitMinutes = AttentionPetRepository.DEFAULT_DAILY_LIMIT_MINUTES,
                sessionLimitMinutes = AttentionPetRepository.DEFAULT_SESSION_LIMIT_MINUTES,
                rollingWindowLimitMinutes = AttentionPetRepository.DEFAULT_ROLLING_WINDOW_LIMIT_MINUTES
            )
        }

        private fun defaultDisplayName(packageName: String): String {
            return if (packageName == AttentionPetRepository.DEFAULT_TARGET_PACKAGE_NAME) {
                AttentionPetRepository.DEFAULT_TARGET_DISPLAY_NAME
            } else {
                packageName
            }
        }
    }
}
