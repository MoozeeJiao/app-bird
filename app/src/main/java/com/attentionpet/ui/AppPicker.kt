package com.attentionpet.ui

import android.content.Context
import android.content.Intent

data class LaunchableApp(
    val packageName: String,
    val label: String
)

object AppPicker {
    internal const val launcherQueryFlags = 0

    fun launchableApps(context: Context): List<LaunchableApp> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return context.packageManager.queryIntentActivities(intent, launcherQueryFlags)
            .map {
                LaunchableApp(
                    packageName = it.activityInfo.packageName,
                    label = it.loadLabel(context.packageManager).toString()
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }
}
