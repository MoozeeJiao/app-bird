package com.attentionpet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.attentionpet.permissions.PermissionSnapshot
import com.attentionpet.permissions.PermissionState
import com.attentionpet.ui.AttentionPetTheme
import com.attentionpet.ui.AppPicker
import com.attentionpet.ui.HomeConfigState
import com.attentionpet.ui.HomeScreen
import com.attentionpet.ui.HomeUiState
import com.attentionpet.ui.LaunchableApp

class MainActivity : ComponentActivity() {
    private var permissionSnapshot by mutableStateOf(PermissionSnapshot(false, false))
    private var homeConfig by mutableStateOf(HomeConfigState())
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
                        homeConfig = homeConfig.selectTarget(app)
                        showAppPicker = false
                    },
                    onStartMonitoring = {},
                    onDailyChanged = { homeConfig = homeConfig.updateDailyLimit(it) },
                    onSessionChanged = { homeConfig = homeConfig.updateSessionLimit(it) },
                    onRollingChanged = { homeConfig = homeConfig.updateRollingWindowLimit(it) }
                )
            }
        }
    }
}
