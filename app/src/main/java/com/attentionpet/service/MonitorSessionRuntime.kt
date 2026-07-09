package com.attentionpet.service

internal class MonitorSessionRuntime {
    private var tracker: SessionTracker? = null
    private var targetPackage: String? = null
    private var graceMillis: Long? = null

    fun sample(
        targetPackage: String,
        graceMillis: Long,
        foregroundPackage: String?,
        nowMillis: Long
    ): SessionTracker.State {
        val activeTracker = if (this.targetPackage == targetPackage && this.graceMillis == graceMillis) {
            tracker
        } else {
            null
        } ?: SessionTracker(targetPackage, graceMillis).also {
            tracker = it
            this.targetPackage = targetPackage
            this.graceMillis = graceMillis
        }

        return activeTracker.onForegroundSample(foregroundPackage, nowMillis)
    }
}
