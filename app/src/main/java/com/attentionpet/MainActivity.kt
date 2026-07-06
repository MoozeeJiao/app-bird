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
import com.attentionpet.ui.HomeScreen
import com.attentionpet.ui.HomeScreenCopy
import com.attentionpet.ui.HomeUiState

class MainActivity : ComponentActivity() {
    private var permissionSnapshot by mutableStateOf(PermissionSnapshot(false, false))

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
                        targetAppLabel = HomeScreenCopy.emptyTargetLabel,
                        dailyLimitMinutes = 60,
                        sessionLimitMinutes = 15,
                        rollingWindowLimitMinutes = 30
                    ),
                    onOpenUsageAccess = { startActivity(PermissionState.usageAccessSettingsIntent()) },
                    onOpenOverlayPermission = { startActivity(PermissionState.overlaySettingsIntent(this)) },
                    onPickTargetApp = {},
                    onStartMonitoring = {},
                    onDailyChanged = {},
                    onSessionChanged = {},
                    onRollingChanged = {}
                )
            }
        }
    }
}
