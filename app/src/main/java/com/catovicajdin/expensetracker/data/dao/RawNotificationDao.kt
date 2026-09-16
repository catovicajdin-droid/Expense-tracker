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

    /**
     * Whether this exact notification text was already recorded. Identity is the text, not the
     * arrival time: the bank re-issues its notifications with a fresh postTime, so the same
     * transaction can arrive looking brand new hours after the original. Matched in SQL so a wide
     * window costs one scan rather than loading every row of it into memory.
     */
    @Query(
        """
        SELECT EXISTS(
            SELECT 1 FROM raw_notifications
            WHERE packageName = :packageName
              AND title = :title
              AND body = :body
              AND postedAt BETWEEN :fromMillis AND :toMillis
        )
        """
    )
    suspend fun hasMatching(
        packageName: String,
        title: String,
        body: String,
        fromMillis: Long,
        toMillis: Long,
    ): Boolean

    @Query("UPDATE raw_notifications SET parseStatus = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)
}
