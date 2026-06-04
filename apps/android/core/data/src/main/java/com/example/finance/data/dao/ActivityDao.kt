package com.example.finance.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.finance.data.entity.ActivityLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityDao {
    @Query("SELECT * FROM activity_logs WHERE user_id = :userId ORDER BY created_at DESC LIMIT 20")
    fun getRecentActivity(userId: Int): Flow<List<ActivityLogEntity>>

    @Query("SELECT * FROM activity_logs WHERE user_id = :userId ORDER BY created_at DESC, id DESC")
    suspend fun getAllActivity(userId: Int): List<ActivityLogEntity>

    @Insert
    suspend fun insertLog(log: ActivityLogEntity)

    @Insert
    suspend fun insertLogs(logs: List<ActivityLogEntity>)

    @Query("DELETE FROM activity_logs")
    suspend fun deleteAll()
}
