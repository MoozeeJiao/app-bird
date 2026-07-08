package com.attentionpet.ui

import com.attentionpet.permissions.PermissionSnapshot
import com.attentionpet.permissions.RequiredPermission
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeScreenCopyTest {
    @Test
    fun visibleChineseCopyUsesIntendedStrings() {
        assertEquals("\u5C0F\u9E1F\u966A\u4F60\u5B88\u4F4F\u65F6\u95F4\u8FB9\u754C", HomeScreenCopy.subtitle)
        assertEquals("\u5F00\u59CB\u966A\u4F34", HomeScreenCopy.startCta)
        assertEquals("\u53D7\u9650 App", HomeScreenCopy.targetCardTitle)
        assertEquals("\u9009\u62E9", HomeScreenCopy.pickTargetCta)
        assertEquals("\u672A\u9009\u62E9 App", HomeScreenCopy.emptyTargetLabel)
        assertEquals("\u6BCF\u65E5\u603B\u9650\u5236", HomeScreenCopy.dailySliderLabel)
        assertEquals("\u5355\u6B21\u8FDE\u7EED\u9650\u5236", HomeScreenCopy.sessionSliderLabel)
        assertEquals("\u8FC7\u53BB 5 \u5C0F\u65F6\u9650\u5236", HomeScreenCopy.rollingSliderLabel)
        assertEquals("\u5206\u949F", HomeScreenCopy.minutesSuffix)
        assertEquals("\u4F7F\u7528\u60C5\u51B5\u6743\u9650", HomeScreenCopy.usagePermissionTitle)
        assertEquals("\u53BB\u5F00\u542F", HomeScreenCopy.usagePermissionCta)
        assertEquals("\u60AC\u6D6E\u7A97\u6743\u9650", HomeScreenCopy.overlayPermissionTitle)
        assertEquals("\u53BB\u5F00\u542F", HomeScreenCopy.overlayPermissionCta)
        assertEquals("\u5DF2\u5F00\u542F", HomeScreenCopy.grantedLabel)
        assertEquals("\u8FD8\u5DEE %d \u6B65\u624D\u80FD\u5F00\u59CB\u966A\u4F34", HomeScreenCopy.missingPermissionFormat)
        assertEquals("\u6743\u9650\u5DF2\u5F00\u542F\uFF0C\u5C0F\u9E1F\u53EF\u4EE5\u5728\u76EE\u6807 App \u6253\u5F00\u65F6\u51FA\u73B0\u3002", HomeScreenCopy.permissionsReady)
        assertEquals("\u6B63\u5728\u966A\u4F34", HomeScreenCopy.activeCta)
        assertEquals("\u6253\u5F00\u76EE\u6807 App \u540E\uFF0C\u5C0F\u9E1F\u4F1A\u51FA\u73B0\u5728\u5C4F\u5E55\u8FB9\u7F18\u3002", HomeScreenCopy.activeHint)
        assertEquals("\u9009\u62E9\u53D7\u9650 App", HomeScreenCopy.targetPickerTitle)
    }

    @Test
    fun ruleValueTextUsesMinutesSuffix() {
        assertEquals(
            "\u6BCF\u65E5\u603B\u9650\u5236  60 \u5206\u949F",
            HomeScreenCopy.ruleValueText(HomeScreenCopy.dailySliderLabel, 60)
        )
    }

    @Test
    fun permissionStatusTextSummarizesReadiness() {
        assertEquals(
            "\u8FD8\u5DEE 2 \u6B65\u624D\u80FD\u5F00\u59CB\u966A\u4F34",
            HomeScreenCopy.permissionStatusText(PermissionSnapshot(usageAccessGranted = false, overlayGranted = false))
        )
        assertEquals(
            "\u8FD8\u5DEE 1 \u6B65\u624D\u80FD\u5F00\u59CB\u966A\u4F34",
            HomeScreenCopy.permissionStatusText(PermissionSnapshot(usageAccessGranted = true, overlayGranted = false))
        )
        assertEquals(
            HomeScreenCopy.permissionsReady,
            HomeScreenCopy.permissionStatusText(PermissionSnapshot(usageAccessGranted = true, overlayGranted = true))
        )
    }

    @Test
    fun startButtonTextReflectsMonitoringStatus() {
        assertEquals(HomeScreenCopy.startCta, HomeScreenCopy.startButtonText(MonitoringStatus.IDLE))
        assertEquals(HomeScreenCopy.startingCta, HomeScreenCopy.startButtonText(MonitoringStatus.STARTING))
        assertEquals(HomeScreenCopy.activeCta, HomeScreenCopy.startButtonText(MonitoringStatus.ACTIVE))
        assertEquals(HomeScreenCopy.retryCta, HomeScreenCopy.startButtonText(MonitoringStatus.ERROR))
    }

    @Test
    fun permissionGuideCopyExplainsNextStep() {
        assertEquals("\u5F00\u542F\u4F7F\u7528\u60C5\u51B5\u6743\u9650", HomeScreenCopy.permissionStepTitle(RequiredPermission.USAGE_ACCESS))
        assertEquals("\u5F00\u542F\u60AC\u6D6E\u7A97\u6743\u9650", HomeScreenCopy.permissionStepTitle(RequiredPermission.OVERLAY))
    }
}
