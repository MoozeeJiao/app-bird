package com.attentionpet.ui

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable

data class LaunchableApp(
    val packageName: String,
    val label: String,
    val icon: Drawable? = null
)

object AppPicker {
    internal const val launcherQueryFlags = 0

    fun launchableApps(context: Context): List<LaunchableApp> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return context.packageManager.queryIntentActivities(intent, launcherQueryFlags)
            .map {
                val label = it.loadLabel(context.packageManager).toString()
                LaunchableApp(
                    packageName = it.activityInfo.packageName,
                    label = label.ifBlank { it.activityInfo.packageName },
                    icon = runCatching { it.loadIcon(context.packageManager) }.getOrNull()
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }

    fun filterLaunchableApps(apps: List<LaunchableApp>, query: String): List<LaunchableApp> {
        val normalizedQuery = query.trim().lowercase()
        if (normalizedQuery.isEmpty()) return apps
        return apps.filter { app ->
            app.label.lowercase().contains(normalizedQuery) ||
                app.packageName.lowercase().contains(normalizedQuery)
        }
    }
}
