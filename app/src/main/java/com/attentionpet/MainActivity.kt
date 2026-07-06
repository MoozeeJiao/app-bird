package com.attentionpet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.attentionpet.permissions.PermissionSnapshot
import com.attentionpet.permissions.PermissionState
import com.attentionpet.service.AttentionMonitorService
import com.attentionpet.ui.AttentionPetTheme
import com.attentionpet.ui.AppPicker
import com.attentionpet.ui.HomeScreen
import com.attentionpet.ui.HomeUiState
import com.attentionpet.ui.LaunchableApp
import com.attentionpet.ui.MainViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels {
        MainViewModel.factory((application as AttentionPetApp).repository)
    }

    private var permissionSnapshot by mutableStateOf(PermissionSnapshot(false, false))
    private var showAppPicker by mutableStateOf(false)
    private var launchableApps by mutableStateOf<List<LaunchableApp>>(emptyList())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        render()
    }

    override fun onResume() {
        super.onResume()
        permissionSnapshot = PermissionState.snapshot(this)
    }

    private fun render() {
        setContent {
            val homeConfig by viewModel.homeConfig.collectAsStateWithLifecycle()

            AttentionPetTheme {
                HomeScreen(
                    state = HomeUiState(
                        permissionSnapshot = permissionSnapshot,
                        targetAppLabel = homeConfig.targetAppLabel,
                        dailyLimitMinutes = homeConfig.dailyLimitMinutes,
                        sessionLimitMinutes = homeConfig.sessionLimitMinutes,
                        rollingWindowLimitMinutes = homeConfig.rollingWindowLimitMinutes
                    ),
                    availableApps = launchableApps,
                    showAppPicker = showAppPicker,
                    onOpenUsageAccess = { startActivity(PermissionState.usageAccessSettingsIntent()) },
                    onOpenOverlayPermission = { startActivity(PermissionState.overlaySettingsIntent(this)) },
                    onPickTargetApp = {
                        launchableApps = AppPicker.launchableApps(this)
                        showAppPicker = true
                    },
                    onDismissAppPicker = { showAppPicker = false },
                    onTargetAppSelected = { app ->
                        viewModel.onTargetAppSelected(app)
                        showAppPicker = false
                    },
                    onStartMonitoring = {
                        viewModel.onStartMonitoring {
                            AttentionMonitorService.start(this@MainActivity)
                        }
                    },
                    onDailyChanged = viewModel::onDailyLimitChanged,
                    onSessionChanged = viewModel::onSessionLimitChanged,
                    onRollingChanged = viewModel::onRollingWindowLimitChanged
                )
            }
        }
    }
}
