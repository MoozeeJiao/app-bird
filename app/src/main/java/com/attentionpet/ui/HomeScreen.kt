package com.attentionpet.ui

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.attentionpet.permissions.PermissionSnapshot

data class HomeUiState(
    val permissionSnapshot: PermissionSnapshot,
    val targetAppLabel: String,
    val dailyLimitMinutes: Int,
    val sessionLimitMinutes: Int,
    val rollingWindowLimitMinutes: Int
)

internal data class HomeConfigState(
    val selectedTargetPackageName: String? = null,
    val targetAppLabel: String = HomeScreenCopy.emptyTargetLabel,
    val dailyLimitMinutes: Int = 60,
    val sessionLimitMinutes: Int = 15,
    val rollingWindowLimitMinutes: Int = 30
) {
    fun selectTarget(app: LaunchableApp): HomeConfigState {
        return copy(
            selectedTargetPackageName = app.packageName,
            targetAppLabel = app.label.ifBlank { HomeScreenCopy.emptyTargetLabel }
        )
    }

    fun updateDailyLimit(minutes: Int): HomeConfigState {
        return copy(dailyLimitMinutes = minutes.coerceIn(10, 180))
    }

    fun updateSessionLimit(minutes: Int): HomeConfigState {
        return copy(sessionLimitMinutes = minutes.coerceIn(5, 60))
    }

    fun updateRollingWindowLimit(minutes: Int): HomeConfigState {
        return copy(rollingWindowLimitMinutes = minutes.coerceIn(5, 120))
    }
}

@Composable
fun HomeScreen(
    state: HomeUiState,
    availableApps: List<LaunchableApp> = emptyList(),
    showAppPicker: Boolean = false,
    onOpenUsageAccess: () -> Unit,
    onOpenOverlayPermission: () -> Unit,
    onPickTargetApp: () -> Unit,
    onDismissAppPicker: () -> Unit = {},
    onTargetAppSelected: (LaunchableApp) -> Unit = {},
    onStartMonitoring: () -> Unit,
    onDailyChanged: (Int) -> Unit,
    onSessionChanged: (Int) -> Unit,
    onRollingChanged: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(18.dp)
    ) {
        Text("Attention Pet", style = MaterialTheme.typography.headlineMedium)
        Text(HomeScreenCopy.subtitle, color = MaterialTheme.colorScheme.secondary)
        Spacer(Modifier.height(16.dp))
        PermissionCards(state.permissionSnapshot, onOpenUsageAccess, onOpenOverlayPermission)
        Spacer(Modifier.height(10.dp))
        Button(
            modifier = Modifier.fillMaxWidth(),
            enabled = state.permissionSnapshot.canStartMonitoring,
            onClick = onStartMonitoring
        ) {
            Text(HomeScreenCopy.startCta)
        }
        Spacer(Modifier.height(12.dp))
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp)) {
                Row {
                    Text(HomeScreenCopy.targetCardTitle, modifier = Modifier.weight(1f))
                    OutlinedButton(onClick = onPickTargetApp) { Text(HomeScreenCopy.pickTargetCta) }
                }
                Text(state.targetAppLabel.ifBlank { HomeScreenCopy.emptyTargetLabel })
            }
        }
        RuleSlider(HomeScreenCopy.dailySliderLabel, state.dailyLimitMinutes, 10, 180, onDailyChanged)
        RuleSlider(HomeScreenCopy.sessionSliderLabel, state.sessionLimitMinutes, 5, 60, onSessionChanged)
        RuleSlider(HomeScreenCopy.rollingSliderLabel, state.rollingWindowLimitMinutes, 5, 120, onRollingChanged)
    }

    if (showAppPicker) {
        AppPickerDialog(
            apps = availableApps,
            onDismiss = onDismissAppPicker,
            onTargetAppSelected = onTargetAppSelected
        )
    }
}

@Composable
private fun PermissionCards(snapshot: PermissionSnapshot, onOpenUsageAccess: () -> Unit, onOpenOverlayPermission: () -> Unit) {
    Row(Modifier.fillMaxWidth()) {
        PermissionCard(
            HomeScreenCopy.usagePermissionTitle,
            snapshot.usageAccessGranted,
            HomeScreenCopy.usagePermissionCta,
            onOpenUsageAccess,
            Modifier.weight(1f)
        )
        Spacer(Modifier.padding(4.dp))
        PermissionCard(
            HomeScreenCopy.overlayPermissionTitle,
            snapshot.overlayGranted,
            HomeScreenCopy.overlayPermissionCta,
            onOpenOverlayPermission,
            Modifier.weight(1f)
        )
    }
}

@Composable
private fun PermissionCard(title: String, granted: Boolean, cta: String, onClick: () -> Unit, modifier: Modifier) {
    Card(modifier) {
        Column(Modifier.padding(10.dp)) {
            Text(title)
            if (granted) {
                Text(HomeScreenCopy.grantedLabel, color = MaterialTheme.colorScheme.secondary)
            } else {
                OutlinedButton(onClick = onClick) { Text(cta) }
            }
        }
    }
}

@Composable
private fun RuleSlider(label: String, value: Int, min: Int, max: Int, onChanged: (Int) -> Unit) {
    Column(Modifier.padding(top = 12.dp)) {
        Text(HomeScreenCopy.ruleValueText(label, value))
        Slider(
            value = value.toFloat(),
            valueRange = min.toFloat()..max.toFloat(),
            onValueChange = { onChanged(it.toInt().coerceIn(min, max)) }
        )
    }
}

@Composable
private fun AppPickerDialog(
    apps: List<LaunchableApp>,
    onDismiss: () -> Unit,
    onTargetAppSelected: (LaunchableApp) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(HomeScreenCopy.targetCardTitle) },
        text = {
            if (apps.isEmpty()) {
                Text(HomeScreenCopy.emptyTargetLabel)
            } else {
                LazyColumn {
                    items(apps) { app ->
                        TextButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { onTargetAppSelected(app) }
                        ) {
                            Text(app.label)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(HomeScreenCopy.dismissPickerCta)
            }
        }
    )
}

internal object HomeScreenCopy {
    const val subtitle = "\u5C0F\u9E1F\u966A\u4F60\u5B88\u4F4F\u65F6\u95F4\u8FB9\u754C"
    const val startCta = "\u5F00\u59CB\u966A\u4F34"
    const val targetCardTitle = "\u53D7\u9650 App"
    const val pickTargetCta = "\u9009\u62E9"
    const val emptyTargetLabel = "\u672A\u9009\u62E9 App"
    const val dailySliderLabel = "\u6BCF\u65E5\u603B\u9650\u5236"
    const val sessionSliderLabel = "\u5355\u6B21\u8FDE\u7EED\u9650\u5236"
    const val rollingSliderLabel = "\u8FC7\u53BB 5 \u5C0F\u65F6\u9650\u5236"
    const val minutesSuffix = "\u5206\u949F"
    const val usagePermissionTitle = "\u4F7F\u7528\u60C5\u51B5\u6743\u9650"
    const val usagePermissionCta = "\u53BB\u5F00\u542F"
    const val overlayPermissionTitle = "\u60AC\u6D6E\u7A97\u6743\u9650"
    const val overlayPermissionCta = "\u53BB\u5F00\u542F"
    const val grantedLabel = "\u5DF2\u5F00\u542F"
    const val dismissPickerCta = "\u53D6\u6D88"

    fun ruleValueText(label: String, value: Int): String = "$label  $value $minutesSuffix"
}
