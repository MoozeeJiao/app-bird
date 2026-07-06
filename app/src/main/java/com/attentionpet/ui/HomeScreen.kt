package com.attentionpet.ui

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

@Composable
fun HomeScreen(
    state: HomeUiState,
    onOpenUsageAccess: () -> Unit,
    onOpenOverlayPermission: () -> Unit,
    onPickTargetApp: () -> Unit,
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
        Text("浠婂ぉ鐨勫皬楦熻繕寰堟斁鏉?", color = MaterialTheme.colorScheme.secondary)
        Spacer(Modifier.height(16.dp))
        PermissionCards(state.permissionSnapshot, onOpenUsageAccess, onOpenOverlayPermission)
        Spacer(Modifier.height(10.dp))
        Button(
            modifier = Modifier.fillMaxWidth(),
            enabled = state.permissionSnapshot.canStartMonitoring,
            onClick = onStartMonitoring
        ) {
            Text("寮€濮嬪畧鎶?")
        }
        Spacer(Modifier.height(12.dp))
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp)) {
                Row {
                    Text("鍙楅檺 App", modifier = Modifier.weight(1f))
                    OutlinedButton(onClick = onPickTargetApp) { Text("鏇存崲") }
                }
                Text(state.targetAppLabel)
            }
        }
        RuleSlider("姣忔棩鎬讳娇鐢ㄩ檺鍒?", state.dailyLimitMinutes, 10, 180, onDailyChanged)
        RuleSlider("鍗曟杩炵画浣跨敤闄愬埗", state.sessionLimitMinutes, 5, 60, onSessionChanged)
        RuleSlider("杩囧幓 5 灏忔椂绐楀彛", state.rollingWindowLimitMinutes, 5, 120, onRollingChanged)
    }
}

@Composable
private fun PermissionCards(snapshot: PermissionSnapshot, onOpenUsageAccess: () -> Unit, onOpenOverlayPermission: () -> Unit) {
    Row(Modifier.fillMaxWidth()) {
        PermissionCard("浣跨敤鎯呭喌鏉冮檺", snapshot.usageAccessGranted, "寮€鍚娇鐢ㄦ儏鍐垫潈闄?", onOpenUsageAccess, Modifier.weight(1f))
        Spacer(Modifier.padding(4.dp))
        PermissionCard("鎮诞绐楁潈闄?", snapshot.overlayGranted, "寮€鍚偓娴獥鏉冮檺", onOpenOverlayPermission, Modifier.weight(1f))
    }
}

@Composable
private fun PermissionCard(title: String, granted: Boolean, cta: String, onClick: () -> Unit, modifier: Modifier) {
    Card(modifier) {
        Column(Modifier.padding(10.dp)) {
            Text(title)
            if (granted) {
                Text("宸插紑鍚?", color = MaterialTheme.colorScheme.secondary)
            } else {
                OutlinedButton(onClick = onClick) { Text(cta) }
            }
        }
    }
}

@Composable
private fun RuleSlider(label: String, value: Int, min: Int, max: Int, onChanged: (Int) -> Unit) {
    Column(Modifier.padding(top = 12.dp)) {
        Text("$label  $value 鍒嗛挓")
        Slider(
            value = value.toFloat(),
            valueRange = min.toFloat()..max.toFloat(),
            onValueChange = { onChanged(it.toInt().coerceIn(min, max)) }
        )
    }
}
