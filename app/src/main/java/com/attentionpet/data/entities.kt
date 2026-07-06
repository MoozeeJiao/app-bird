package com.attentionpet.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "target_app_config")
data class TargetAppConfigEntity(
    @PrimaryKey val id: Int = 1,
    val packageName: String,
    val displayName: String,
    val enabled: Boolean
)

@Entity(tableName = "limit_config")
data class LimitConfigEntity(
    @PrimaryKey val id: Int = 1,
    val dailyLimitMinutes: Int,
    val sessionLimitMinutes: Int,
    val rollingWindowHours: Int,
    val rollingWindowLimitMinutes: Int
)

@Entity(tableName = "usage_session")
data class UsageSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val packageName: String,
    val startMillis: Long,
    val endMillis: Long?,
    val foregroundDurationMillis: Long,
    val closeReason: String
)

@Entity(tableName = "extension_event")
data class ExtensionEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val sessionId: Long,
    val timestampMillis: Long,
    val addedMinutes: Int,
    val consumedForegroundMillis: Long
)

@Entity(tableName = "timeout_action_event")
data class TimeoutActionEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val sessionId: Long,
    val timestampMillis: Long,
    val actionType: String,
    val overageDurationMillis: Long
)

@Entity(tableName = "overlay_position")
data class OverlayPositionEntity(
    @PrimaryKey val id: Int = 1,
    val edge: String,
    val verticalRatio: Float,
    val updatedAtMillis: Long
)
