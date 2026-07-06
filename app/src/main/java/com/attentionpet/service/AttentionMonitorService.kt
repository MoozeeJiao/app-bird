package com.attentionpet.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.attentionpet.R
import com.attentionpet.domain.ActiveSession
import com.attentionpet.domain.ExtensionGrant
import com.attentionpet.domain.PetState
import com.attentionpet.domain.RuleConfig
import com.attentionpet.domain.RuleEvaluator
import com.attentionpet.overlay.OverlayController
import java.time.ZoneId
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AttentionMonitorService : Service() {
    private val serviceJob = SupervisorJob()
    private val scope = CoroutineScope(serviceJob + Dispatchers.IO)
    private var pollingJob: Job? = null
    private lateinit var detector: ForegroundAppDetector
    private lateinit var tracker: SessionTracker
    private lateinit var overlayController: OverlayController

    override fun onCreate() {
        super.onCreate()
        detector = ForegroundAppDetector(this)
        tracker = SessionTracker(TARGET_PACKAGE, RULE_CONFIG.sessionGraceMillis)
        overlayController = OverlayController(this)
        ensureNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, notification())
        if (pollingJob?.isActive != true) {
            pollingJob = scope.launch {
                pollForegroundApp()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        pollingJob?.cancel()
        serviceJob.cancel()
        overlayController.hideAll()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private suspend fun pollForegroundApp() {
        while (coroutineContext.isActive) {
            val nowMillis = System.currentTimeMillis()
            val foregroundPackage = detector.currentForegroundPackage(nowMillis)
            val sessionState = tracker.onForegroundSample(foregroundPackage, nowMillis)

            if (sessionState.activeStartMillis != null) {
                val result = RuleEvaluator.evaluate(
                    nowMillis = nowMillis,
                    zoneId = ZoneId.systemDefault(),
                    config = RULE_CONFIG,
                    sessions = emptyList(),
                    activeSession = ActiveSession(
                        startMillis = sessionState.activeStartMillis,
                        foregroundMillis = sessionState.foregroundDurationMillis
                    ),
                    extensionGrant = ExtensionGrant()
                )
                withContext(Dispatchers.Main.immediate) {
                    overlayController.showCapsule(result) {
                        if (result.petState == PetState.TIMEOUT) {
                            overlayController.showTimeoutSheet(
                                onRest = { overlayController.hideTimeoutSheet() },
                                onExtend = { overlayController.hideTimeoutSheet() }
                            )
                        } else {
                            overlayController.showPanel(
                                result = result,
                                currentSessionText = sessionMinutesText(sessionState.foregroundDurationMillis)
                            )
                        }
                    }
                }
            } else {
                withContext(Dispatchers.Main.immediate) {
                    overlayController.hideAll()
                }
            }

            delay(POLL_INTERVAL_MILLIS)
        }
    }

    private fun ensureNotificationChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "\u4E13\u6CE8\u76D1\u6D4B",
                NotificationManager.IMPORTANCE_LOW
            )
        )
    }

    private fun notification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_attention_pet)
            .setContentTitle("Attention Pet \u6B63\u5728\u5B88\u62A4\u65F6\u95F4\u8FB9\u754C")
            .setContentText("\u5C0F\u9E1F\u4F1A\u5728\u76EE\u6807 App \u6253\u5F00\u65F6\u51FA\u73B0\u5728\u5C4F\u5E55\u8FB9\u7F18")
            .setOngoing(true)
            .build()
    }

    private fun sessionMinutesText(foregroundDurationMillis: Long): String {
        return "${foregroundDurationMillis / MILLIS_PER_MINUTE} \u5206\u949F"
    }

    companion object {
        private const val CHANNEL_ID = "attention_pet_monitor"
        private const val NOTIFICATION_ID = 1
        private const val TARGET_PACKAGE = "com.ss.android.ugc.aweme"
        private const val POLL_INTERVAL_MILLIS = 1_000L
        private const val MILLIS_PER_MINUTE = 60_000L
        private val RULE_CONFIG = RuleConfig(
            dailyLimitMillis = 60L * MILLIS_PER_MINUTE,
            sessionLimitMillis = 15L * MILLIS_PER_MINUTE,
            rollingWindowLimitMillis = 30L * MILLIS_PER_MINUTE
        )

        fun start(context: Context) {
            context.startForegroundService(Intent(context, AttentionMonitorService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, AttentionMonitorService::class.java))
        }
    }
}
