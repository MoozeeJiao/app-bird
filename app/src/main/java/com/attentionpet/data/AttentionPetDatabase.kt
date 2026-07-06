package com.attentionpet.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        TargetAppConfigEntity::class,
        LimitConfigEntity::class,
        UsageSessionEntity::class,
        ExtensionEventEntity::class,
        TimeoutActionEventEntity::class,
        OverlayPositionEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AttentionPetDatabase : RoomDatabase() {
    abstract fun configDao(): ConfigDao
    abstract fun sessionDao(): SessionDao
    abstract fun eventDao(): EventDao
}
