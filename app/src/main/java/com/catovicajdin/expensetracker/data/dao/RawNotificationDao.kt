package com.catovicajdin.expensetracker.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.catovicajdin.expensetracker.data.entity.RawNotificationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RawNotificationDao {
    @Insert
    suspend fun insert(raw: RawNotificationEntity): Long

    @Query("SELECT * FROM raw_notifications WHERE parseStatus = 'NEEDS_REVIEW' ORDER BY postedAt DESC")
    fun needsReview(): Flow<List<RawNotificationEntity>>

    @Query(
        """
        SELECT * FROM raw_notifications
        WHERE packageName = :packageName AND postedAt BETWEEN :fromMillis AND :toMillis
        ORDER BY postedAt DESC
        """
    )
    suspend fun findRecentForDedup(packageName: String, fromMillis: Long, toMillis: Long): List<RawNotificationEntity>

    @Query("UPDATE raw_notifications SET parseStatus = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)
}
