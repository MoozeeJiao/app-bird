package com.attentionpet.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.attentionpet.AttentionPetTestIds
import com.attentionpet.domain.PetState
import com.attentionpet.domain.RuleBucket
import com.attentionpet.domain.RuleEvaluationResult
import com.attentionpet.pet.BirdPet
import kotlin.math.abs

internal object TimeoutSheetCopy {
    const val title = "\u5DF2\u7ECF\u8D85\u65F6\u5566"
    const val question = "\u8981\u4E0D\u8981\u4F11\u606F\u4E00\u4E0B\uFF1F"
    const val defaultFeedback = "\u5C0F\u9E1F\u6709\u70B9\u7740\u6025\u4E86\uFF0C\u5148\u628A\u65F6\u95F4\u8FB9\u754C\u6536\u56DE\u6765\u3002"
    const val restButton = "\u4F11\u606F\u4E00\u4E0B"
    const val extendButton = "\u518D\u52A0 5 \u5206\u949F"
}

@Composable
fun CapsuleOverlay(
    result: RuleEvaluationResult,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(topEnd = 999.dp, bottomEnd = 999.dp)
    Box(
        modifier = modifier
            .width(112.dp)
            .height(46.dp)
    ) {
        Row(
            modifier = Modifier
                .width(112.dp)
                .height(46.dp)
                .semantics { contentDescription = AttentionPetTestIds.OVERLAY_CAPSULE }
                .clip(shape)
                .background(stateColor(result.petState).copy(alpha = 0.78f))
                .clickable(onClick = onClick)
                .padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BirdPet(result.petState, Modifier.size(28.dp))
            Column(Modifier.padding(start = 6.dp)) {
                Text(
                    text = displayRemaining(result),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF21323A)
                )
                Spacer(Modifier.height(3.dp))
                LinearProgressIndicator(
                    progress = { result.effectiveRemainingRatio.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .width(56.dp)
                        .height(5.dp),
                    color = progressColor(result.petState),
                    trackColor = Color(0x3320323C)
                )
            }
        }
        Spacer(
            modifier = Modifier
                .size(1.dp)
                .semantics { contentDescription = AttentionPetTestIds.overlayState(result.petState) }
        )
    }
}

@Composable
fun ExpandedPanelOverlay(
    result: RuleEvaluationResult,
    currentSessionText: String,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(276.dp)
            .heightIn(min = 184.dp)
            .semantics { contentDescription = AttentionPetTestIds.OVERLAY_PANEL }
            .background(Color.White.copy(alpha = 0.92f), RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BirdPet(result.petState, Modifier.size(44.dp))
            Column(
                modifier = Modifier
                    .padding(start = 10.dp)
                    .weight(1f)
            ) {
                Text(
                    text = "\u5C0F\u9E1F\u5728\u65C1\u8FB9\u966A\u4F60\u770B\u7740\u65F6\u95F4",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color(0xFF21323A)
                )
                Text(
                    text = result.petState.labelZh,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF52636B)
                )
            }
            TextButton(
                modifier = Modifier.semantics { contentDescription = AttentionPetTestIds.OVERLAY_PANEL_DISMISS },
                onClick = onDismiss
            ) {
                Text("\u6536\u8D77")
            }
        }
        Spacer(Modifier.height(10.dp))
        expandedPanelMetricLines(result, currentSessionText).forEachIndexed { index, line ->
            if (index > 0) {
                Spacer(Modifier.height(5.dp))
            }
            Text(
                text = line,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF32434B)
            )
        }
        Spacer(
            modifier = Modifier
                .size(1.dp)
                .semantics { contentDescription = AttentionPetTestIds.overlaySessionMillis(result.session.usedMillis) }
        )
    }
}

@Composable
fun TimeoutSheetOverlay(
    onRest: () -> Unit,
    onExtend: () -> Unit,
    feedbackText: String = TimeoutSheetCopy.defaultFeedback
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 296.dp)
            .semantics { contentDescription = AttentionPetTestIds.TIMEOUT_SHEET }
            .background(Color(0xFFFFFAFA), RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BirdPet(PetState.TIMEOUT, Modifier.size(84.dp))
        Spacer(Modifier.height(8.dp))
        Text(
            text = TimeoutSheetCopy.title,
            style = MaterialTheme.typography.headlineSmall,
            color = Color(0xFF21323A)
        )
        Text(
            text = TimeoutSheetCopy.question,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF52636B)
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = feedbackText,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF8B4A42)
        )
        Spacer(Modifier.height(18.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 360.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(onClick = onRest, modifier = Modifier.weight(1f)) {
                Text(TimeoutSheetCopy.restButton)
            }
            Button(onClick = onExtend, modifier = Modifier.weight(1f)) {
                Text(TimeoutSheetCopy.extendButton)
            }
        }
    }
}

internal fun displayRemaining(result: RuleEvaluationResult): String {
    val minutes = abs(result.effectiveRemainingMillis / 60_000L)
    return if (result.petState == PetState.TIMEOUT) "+${minutes}m" else "${minutes}m"
}

internal fun expandedPanelMetricLines(
    result: RuleEvaluationResult,
    currentSessionText: String
): List<String> = listOf(
    "\u4ECA\u65E5 ${bucketUsageCopy(result.daily)}",
    "\u8FD1 5 \u5C0F\u65F6 ${bucketUsageCopy(result.rollingWindow)}",
    "\u5F53\u524D\u8FDE\u7EED\u4F7F\u7528 $currentSessionText",
    "\u72B6\u6001 ${result.statusCopy}"
)

private fun bucketUsageCopy(bucket: RuleBucket): String {
    return "${minutes(bucket.usedMillis)} / ${minutes(bucket.limitMillis)} \u5206\u949F"
}

private fun minutes(millis: Long): Long = millis / 60_000L

private fun stateColor(state: PetState): Color = when (state) {
    PetState.RELAXED -> Color(0xFFE8FFF5)
    PetState.REMINDER -> Color(0xFFFFF7D2)
    PetState.TENSE -> Color(0xFFFFF1D8)
    PetState.TIMEOUT -> Color(0xFFFFE2E2)
}

private fun progressColor(state: PetState): Color = when (state) {
    PetState.RELAXED -> Color(0xFF45D6A1)
    PetState.REMINDER -> Color(0xFFFFD86F)
    PetState.TENSE -> Color(0xFFFF9A5E)
    PetState.TIMEOUT -> Color(0xFFF45E63)
}
