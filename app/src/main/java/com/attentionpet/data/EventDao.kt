package com.attentionpet.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface EventDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertExtension(entity: ExtensionEventEntity): Long

    @Query("SELECT * FROM extension_event WHERE sessionId = :sessionId ORDER BY id LIMIT 1")
    suspend fun extensionForSession(sessionId: Long): ExtensionEventEntity?

    @Insert
    suspend fun insertTimeoutAction(entity: TimeoutActionEventEntity): Long

    @Query("SELECT * FROM overlay_position WHERE id = 1")
    suspend fun overlayPosition(): OverlayPositionEntity?

    @Upsert
    suspend fun upsertOverlayPosition(entity: OverlayPositionEntity)
}
