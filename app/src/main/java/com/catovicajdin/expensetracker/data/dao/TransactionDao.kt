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

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun byId(id: Long): TransactionEntity?

    /** All filters are optional and combine with AND - a null parameter is simply skipped. */
    @Query(
        """
        SELECT * FROM transactions
        WHERE (:categoryId IS NULL OR categoryId = :categoryId)
        AND (:fromMillis IS NULL OR postedAt >= :fromMillis)
        AND (:toMillis IS NULL OR postedAt <= :toMillis)
        AND (:minAmount IS NULL OR amount >= :minAmount)
        AND (:maxAmount IS NULL OR amount <= :maxAmount)
        ORDER BY postedAt DESC
        """
    )
    fun filtered(
        categoryId: Long?,
        fromMillis: Long?,
        toMillis: Long?,
        minAmount: Double?,
        maxAmount: Double?,
    ): Flow<List<TransactionEntity>>

    /** Most recent category assigned to a transaction of this exact amount, if any - powers the categorize suggestion. */
    @Query(
        """
        SELECT categoryId FROM transactions
        WHERE amount = :amount AND categoryId IS NOT NULL
        ORDER BY postedAt DESC LIMIT 1
        """
    )
    suspend fun suggestedCategoryForAmount(amount: Double): Long?

    @Query("UPDATE transactions SET categoryId = :categoryId WHERE id = :id")
    suspend fun assignCategory(id: Long, categoryId: Long)
}
