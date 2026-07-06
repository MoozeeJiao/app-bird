package com.attentionpet.permissions

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Process
import android.provider.Settings

data class PermissionSnapshot(
    val usageAccessGranted: Boolean,
    val overlayGranted: Boolean
) {
    val canStartMonitoring: Boolean = usageAccessGranted && overlayGranted
}

object PermissionState {
    fun snapshot(context: Context): PermissionSnapshot {
        return PermissionSnapshot(
            usageAccessGranted = hasUsageAccess(context),
            overlayGranted = Settings.canDrawOverlays(context)
        )
    }

    fun usageAccessSettingsIntent(): Intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)

    fun overlaySettingsIntent(context: Context): Intent {
        return Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
    }

    private fun hasUsageAccess(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }
}
