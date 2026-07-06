package com.attentionpet.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface SessionDao {
    @Insert
    suspend fun insert(entity: UsageSessionEntity): Long

    @Update
    suspend fun update(entity: UsageSessionEntity)

    @Query("SELECT * FROM usage_session WHERE id = :id")
    suspend fun getById(id: Long): UsageSessionEntity?

    @Query("SELECT * FROM usage_session WHERE packageName = :packageName AND startMillis < :windowEnd AND COALESCE(endMillis, :windowEnd) > :windowStart")
    suspend fun sessionsOverlapping(packageName: String, windowStart: Long, windowEnd: Long): List<UsageSessionEntity>
}
