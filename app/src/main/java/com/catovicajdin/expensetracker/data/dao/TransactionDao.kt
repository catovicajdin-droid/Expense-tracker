package com.catovicajdin.expensetracker.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.catovicajdin.expensetracker.data.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Insert
    suspend fun insert(transaction: TransactionEntity): Long

    @Query("SELECT * FROM transactions ORDER BY postedAt DESC")
    fun all(): Flow<List<TransactionEntity>>

    @Query("UPDATE transactions SET categoryId = :categoryId WHERE id = :id")
    suspend fun assignCategory(id: Long, categoryId: Long)
}
