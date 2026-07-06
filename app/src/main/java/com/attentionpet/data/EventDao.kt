package com.attentionpet.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface EventDao {
    @Insert
    suspend fun insertExtension(entity: ExtensionEventEntity): Long

    @Query("SELECT * FROM extension_event WHERE sessionId = :sessionId")
    suspend fun extensionForSession(sessionId: Long): ExtensionEventEntity?

    @Insert
    suspend fun insertTimeoutAction(entity: TimeoutActionEventEntity): Long

    @Query("SELECT * FROM overlay_position WHERE id = 1")
    suspend fun overlayPosition(): OverlayPositionEntity?

    @Upsert
    suspend fun upsertOverlayPosition(entity: OverlayPositionEntity)
}
