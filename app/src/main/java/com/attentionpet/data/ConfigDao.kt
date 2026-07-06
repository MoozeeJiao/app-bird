package com.attentionpet.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ConfigDao {
    @Query("SELECT * FROM target_app_config WHERE id = 1")
    fun targetApp(): Flow<TargetAppConfigEntity?>

    @Query("SELECT * FROM limit_config WHERE id = 1")
    fun limits(): Flow<LimitConfigEntity?>

    @Upsert
    suspend fun upsertTargetApp(entity: TargetAppConfigEntity)

    @Upsert
    suspend fun upsertLimits(entity: LimitConfigEntity)
}
