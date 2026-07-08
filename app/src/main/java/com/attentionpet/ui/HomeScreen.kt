package com.attentionpet.ui

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.attentionpet.AttentionPetTestIds
import com.attentionpet.domain.PetState
import com.attentionpet.permissions.PermissionSnapshot
import com.attentionpet.permissions.RequiredPermission
import com.attentionpet.pet.BirdPet

data class HomeUiState(
    val permissionSnapshot: PermissionSnapshot,
    val targetAppLabel: String,
    val dailyLimitMinutes: Int,
    val sessionLimitMinutes: Int,
    val rollingWindowLimitMinutes: Int,
    val monitoringStatus: MonitoringStatus = MonitoringStatus.IDLE,
    val targetAppIcon: Drawable? = null
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
    showPermissionGuide: Boolean = false,
    onOpenUsageAccess: () -> Unit,
    onOpenOverlayPermission: () -> Unit,
    onOpenPermissionGuide: () -> Unit = {},
    onDismissPermissionGuide: () -> Unit = {},
    onOpenNextPermission: () -> Unit = {},
    onPickTargetApp: () -> Unit,
    onDismissAppPicker: () -> Unit = {},
    onTargetAppSelected: (LaunchableApp) -> Unit = {},
    onStartMonitoring: () -> Unit,
    onDailyChanged: (Int) -> Unit,
    onSessionChanged: (Int) -> Unit,
    onRollingChanged: (Int) -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                TopBar()
            }
            item {
                CompanionPanel(state)
            }
            item {
                PermissionReadinessStrip(
                    snapshot = state.permissionSnapshot,
                    onClick = onOpenPermissionGuide
                )
            }
            item {
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .semantics { contentDescription = AttentionPetTestIds.START_MONITORING },
                    enabled = state.monitoringStatus != MonitoringStatus.STARTING,
                    onClick = onStartMonitoring
                ) {
                    Text(HomeScreenCopy.startButtonText(state.monitoringStatus))
                }
            }
            if (state.monitoringStatus == MonitoringStatus.ACTIVE || state.monitoringStatus == MonitoringStatus.ERROR) {
                item {
                    MonitoringStatusStrip(state)
                }
            }
            item {
                TargetAppCard(
                    label = state.targetAppLabel,
                    icon = state.targetAppIcon,
                    onPickTargetApp = onPickTargetApp
                )
            }
            item {
                RuleSlider(HomeScreenCopy.dailySliderLabel, state.dailyLimitMinutes, 10, 180, onDailyChanged)
            }
            item {
                RuleSlider(HomeScreenCopy.sessionSliderLabel, state.sessionLimitMinutes, 5, 60, onSessionChanged)
            }
            item {
                RuleSlider(HomeScreenCopy.rollingSliderLabel, state.rollingWindowLimitMinutes, 5, 120, onRollingChanged)
            }
        }

        if (showAppPicker) {
            AppPickerSheet(
                apps = availableApps,
                onDismiss = onDismissAppPicker,
                onTargetAppSelected = onTargetAppSelected
            )
        }

        if (showPermissionGuide) {
            PermissionGuideSheet(
                snapshot = state.permissionSnapshot,
                onDismiss = onDismissPermissionGuide,
                onOpenNextPermission = onOpenNextPermission,
                onOpenUsageAccess = onOpenUsageAccess,
                onOpenOverlayPermission = onOpenOverlayPermission
            )
        }
    }
}

@Composable
private fun TopBar() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            BirdPet(PetState.RELAXED, Modifier.size(30.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text("Attention Pet", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(HomeScreenCopy.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
        }
    }
}

@Composable
private fun CompanionPanel(state: HomeUiState) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(86.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFEAFBF4)),
                contentAlignment = Alignment.Center
            ) {
                BirdPet(PetState.RELAXED, Modifier.size(70.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(HomeScreenCopy.companionTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    text = when (state.monitoringStatus) {
                        MonitoringStatus.ACTIVE -> HomeScreenCopy.activeHint
                        MonitoringStatus.ERROR -> HomeScreenCopy.startErrorHint
                        else -> HomeScreenCopy.companionBody
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE6EFEC))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(if (state.permissionSnapshot.canStartMonitoring) 0.68f else 0.28f)
                            .height(7.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondary)
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionReadinessStrip(snapshot: PermissionSnapshot, onClick: () -> Unit) {
    val ready = snapshot.canStartMonitoring
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !ready, onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = if (ready) Color(0xFFEFFAF6) else Color(0xFFFFFAF2)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = HomeScreenCopy.permissionStatusText(snapshot),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                color = if (ready) Color(0xFF2B6659) else Color(0xFF76523A)
            )
            if (!ready) {
                Text(HomeScreenCopy.completePermissionsCta, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun MonitoringStatusStrip(state: HomeUiState) {
    val isError = state.monitoringStatus == MonitoringStatus.ERROR
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = if (isError) Color(0xFFFFECEC) else Color(0xFFEFFAF6)
    ) {
        Text(
            text = if (isError) HomeScreenCopy.startErrorHint else HomeScreenCopy.activeStatusText(state.targetAppLabel),
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodySmall,
            color = if (isError) Color(0xFF8C3E43) else Color(0xFF2B6659)
        )
    }
}

@Composable
private fun TargetAppCard(label: String, icon: Drawable?, onPickTargetApp: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(HomeScreenCopy.targetCardTitle, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                OutlinedButton(onClick = onPickTargetApp) { Text(HomeScreenCopy.changeTargetCta) }
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIcon(label = label, icon = icon)
                Spacer(Modifier.width(10.dp))
                Text(label.ifBlank { HomeScreenCopy.emptyTargetLabel }, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
private fun RuleSlider(label: String, value: Int, min: Int, max: Int, onChanged: (Int) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Row {
                Text(label, modifier = Modifier.weight(1f))
                Text("$value ${HomeScreenCopy.minutesSuffix}", fontWeight = FontWeight.Bold)
            }
            Slider(
                value = value.toFloat(),
                valueRange = min.toFloat()..max.toFloat(),
                onValueChange = { onChanged(it.toInt().coerceIn(min, max)) }
            )
        }
    }
}

@Composable
private fun AppPickerSheet(
    apps: List<LaunchableApp>,
    onDismiss: () -> Unit,
    onTargetAppSelected: (LaunchableApp) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filteredApps = AppPicker.filterLaunchableApps(apps, query)
    val duplicatedLabels = apps.groupBy { it.label }.filterValues { it.size > 1 }.keys

    BottomSheet(onDismiss = onDismiss) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(HomeScreenCopy.targetPickerTitle, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            TextButton(onClick = onDismiss) { Text(HomeScreenCopy.dismissPickerCta) }
        }
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text(HomeScreenCopy.searchAppLabel) }
        )
        Spacer(Modifier.height(10.dp))
        if (filteredApps.isEmpty()) {
            Text(HomeScreenCopy.emptyAppSearchLabel, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            LazyColumn(
                modifier = Modifier.heightIn(max = 360.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(filteredApps, key = { it.packageName }) { app ->
                    AppPickerRow(
                        app = app,
                        showPackageName = app.label in duplicatedLabels,
                        onClick = { onTargetAppSelected(app) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AppPickerRow(app: LaunchableApp, showPackageName: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppIcon(label = app.label, icon = app.icon)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(app.label, style = MaterialTheme.typography.bodyLarge)
            if (showPackageName) {
                Text(app.packageName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun PermissionGuideSheet(
    snapshot: PermissionSnapshot,
    onDismiss: () -> Unit,
    onOpenNextPermission: () -> Unit,
    onOpenUsageAccess: () -> Unit,
    onOpenOverlayPermission: () -> Unit
) {
    val missing = snapshot.missingPermissions()
    BottomSheet(onDismiss = onDismiss) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(HomeScreenCopy.permissionGuideTitle, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            TextButton(onClick = onDismiss) { Text(HomeScreenCopy.dismissPickerCta) }
        }
        Text(HomeScreenCopy.permissionGuideBody, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        PermissionStepRow(
            permission = RequiredPermission.USAGE_ACCESS,
            granted = snapshot.usageAccessGranted,
            onClick = onOpenUsageAccess
        )
        PermissionStepRow(
            permission = RequiredPermission.OVERLAY,
            granted = snapshot.overlayGranted,
            onClick = onOpenOverlayPermission
        )
        Spacer(Modifier.height(12.dp))
        Button(
            modifier = Modifier.fillMaxWidth(),
            enabled = missing.isNotEmpty(),
            onClick = onOpenNextPermission
        ) {
            Text(if (missing.isEmpty()) HomeScreenCopy.startCta else HomeScreenCopy.permissionStepTitle(missing.first()))
        }
    }
}

@Composable
private fun PermissionStepRow(permission: RequiredPermission, granted: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = !granted, onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(if (granted) MaterialTheme.colorScheme.secondary else Color(0xFFFFF0D8)),
            contentAlignment = Alignment.Center
        ) {
            Text(if (granted) "\u2713" else "!", color = if (granted) Color.White else Color(0xFF76523A))
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(HomeScreenCopy.permissionStepTitle(permission), fontWeight = FontWeight.Bold)
            Text(HomeScreenCopy.permissionStepDescription(permission), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(if (granted) HomeScreenCopy.grantedLabel else HomeScreenCopy.usagePermissionCta, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun BottomSheet(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.32f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = {}),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                content = content
            )
        }
    }
}

@Composable
private fun AppIcon(label: String, icon: Drawable?) {
    val bitmap = remember(icon) {
        runCatching { icon?.toBitmap(width = 64, height = 64)?.asImageBitmap() }.getOrNull()
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(8.dp))
        )
    } else {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label.trim().take(1).ifBlank { "A" },
                color = Color.White,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

internal object HomeScreenCopy {
    const val subtitle = "\u5C0F\u9E1F\u966A\u4F60\u5B88\u4F4F\u65F6\u95F4\u8FB9\u754C"
    const val companionTitle = "\u4ECA\u5929\u8FD8\u5F88\u4ECE\u5BB9"
    const val companionBody = "\u6253\u5F00\u53D7\u9650 App \u65F6\uFF0C\u5C0F\u9E1F\u4F1A\u5F85\u5728\u5C4F\u5E55\u8FB9\u7F18\u63D0\u9192\u65F6\u95F4\u3002"
    const val startCta = "\u5F00\u59CB\u966A\u4F34"
    const val startingCta = "\u6B63\u5728\u542F\u52A8"
    const val activeCta = "\u6B63\u5728\u966A\u4F34"
    const val retryCta = "\u91CD\u8BD5\u5F00\u59CB"
    const val targetCardTitle = "\u53D7\u9650 App"
    const val pickTargetCta = "\u9009\u62E9"
    const val changeTargetCta = "\u66F4\u6362"
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
    const val missingPermissionFormat = "\u8FD8\u5DEE %d \u6B65\u624D\u80FD\u5F00\u59CB\u966A\u4F34"
    const val permissionsReady = "\u6743\u9650\u5DF2\u5F00\u542F\uFF0C\u5C0F\u9E1F\u53EF\u4EE5\u5728\u76EE\u6807 App \u6253\u5F00\u65F6\u51FA\u73B0\u3002"
    const val activeHint = "\u6253\u5F00\u76EE\u6807 App \u540E\uFF0C\u5C0F\u9E1F\u4F1A\u51FA\u73B0\u5728\u5C4F\u5E55\u8FB9\u7F18\u3002"
    const val startErrorHint = "\u542F\u52A8\u5931\u8D25\uFF0C\u8BF7\u68C0\u67E5\u6743\u9650\u540E\u91CD\u8BD5\u3002"
    const val targetPickerTitle = "\u9009\u62E9\u53D7\u9650 App"
    const val searchAppLabel = "\u641C\u7D22 App \u540D\u79F0"
    const val emptyAppSearchLabel = "\u6CA1\u6709\u627E\u5230\u5339\u914D\u7684 App"
    const val completePermissionsCta = "\u53BB\u5B8C\u6210"
    const val permissionGuideTitle = "\u5B8C\u6210\u966A\u4F34\u524D\u7684\u51C6\u5907"
    const val permissionGuideBody = "\u9700\u8981\u8FD9\u4E9B\u6743\u9650\uFF0C\u5C0F\u9E1F\u624D\u80FD\u77E5\u9053\u76EE\u6807 App \u662F\u5426\u5728\u524D\u53F0\uFF0C\u5E76\u5F85\u5728\u5C4F\u5E55\u8FB9\u7F18\u63D0\u9192\u4F60\u3002"
    const val dismissPickerCta = "\u53D6\u6D88"

    fun ruleValueText(label: String, value: Int): String = "$label  $value $minutesSuffix"

    fun permissionStatusText(snapshot: PermissionSnapshot): String {
        val missingCount = snapshot.missingPermissions().size
        return if (missingCount == 0) permissionsReady else missingPermissionFormat.format(missingCount)
    }

    fun startButtonText(status: MonitoringStatus): String = when (status) {
        MonitoringStatus.IDLE -> startCta
        MonitoringStatus.STARTING -> startingCta
        MonitoringStatus.ACTIVE -> activeCta
        MonitoringStatus.ERROR -> retryCta
    }

    fun permissionStepTitle(permission: RequiredPermission): String = when (permission) {
        RequiredPermission.USAGE_ACCESS -> "\u5F00\u542F\u4F7F\u7528\u60C5\u51B5\u6743\u9650"
        RequiredPermission.OVERLAY -> "\u5F00\u542F\u60AC\u6D6E\u7A97\u6743\u9650"
    }

    fun permissionStepDescription(permission: RequiredPermission): String = when (permission) {
        RequiredPermission.USAGE_ACCESS -> "\u7528\u6765\u5224\u65AD\u53D7\u9650 App \u4EC0\u4E48\u65F6\u5019\u5728\u524D\u53F0\u3002"
        RequiredPermission.OVERLAY -> "\u7528\u6765\u8BA9\u5C0F\u9E1F\u663E\u793A\u5728\u5C4F\u5E55\u8FB9\u7F18\u3002"
    }

    fun activeStatusText(targetAppLabel: String): String {
        return "\u6B63\u5728\u966A\u4F34 ${targetAppLabel.ifBlank { emptyTargetLabel }}\u3002$activeHint"
    }
}
