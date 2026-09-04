package com.catovicajdin.expensetracker.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.catovicajdin.expensetracker.data.CategoryTotal
import com.catovicajdin.expensetracker.data.TransactionRow
import com.catovicajdin.expensetracker.data.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Insert
    suspend fun insert(transaction: TransactionEntity): Long

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun byId(id: Long): TransactionEntity?

    /**
     * All filters are optional and combine with AND - a null/empty parameter is simply skipped.
     * Category matching is "any of categoryIds" (OR across categories) - categoryCount must be
     * categoryIds.size, passed separately since Room can't call .size on a bound List in SQL. Tag
     * matching has two modes: matchAllTags=false means "has any of tagIds" (OR across tags),
     * matchAllTags=true means "has every one of tagIds" (AND across tags), same reasoning for
     * tagCount.
     */
    @Query(
        """
        SELECT transactions.*, raw_notifications.packageName as source
        FROM transactions
        JOIN raw_notifications ON raw_notifications.id = transactions.rawNotificationId
        WHERE (:categoryCount = 0 OR categoryId IN (:categoryIds))
        AND (:fromMillis IS NULL OR transactions.postedAt >= :fromMillis)
        AND (:toMillis IS NULL OR transactions.postedAt <= :toMillis)
        AND (:minAmount IS NULL OR amount >= :minAmount)
        AND (:maxAmount IS NULL OR amount <= :maxAmount)
        AND (
            :tagCount = 0
            OR (NOT :matchAllTags AND transactions.id IN (SELECT transactionId FROM transaction_tags WHERE tagId IN (:tagIds)))
            OR (:matchAllTags AND (
                SELECT COUNT(DISTINCT tagId) FROM transaction_tags
                WHERE transactionId = transactions.id AND tagId IN (:tagIds)
            ) = :tagCount)
        )
        ORDER BY transactions.postedAt DESC
        """
    )
    fun filteredWithSource(
        categoryIds: List<Long>,
        categoryCount: Int,
        fromMillis: Long?,
        toMillis: Long?,
        minAmount: Double?,
        maxAmount: Double?,
        tagIds: List<Long>,
        matchAllTags: Boolean,
        tagCount: Int,
    ): Flow<List<TransactionRow>>

    @Query(
        """
        SELECT transactions.*, raw_notifications.packageName as source
        FROM transactions
        JOIN raw_notifications ON raw_notifications.id = transactions.rawNotificationId
        WHERE transactions.id = :id
        """
    )
    suspend fun byIdWithSource(id: Long): TransactionRow?

    @Query("SELECT * FROM transactions ORDER BY postedAt DESC LIMIT :limit")
    fun recent(limit: Int): Flow<List<TransactionEntity>>

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
    suspend fun assignCategory(id: Long, categoryId: Long?)

    /** Leaves the originating raw_notifications row intact - only the transaction itself is removed. */
    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE transactions SET postedAt = :postedAt WHERE id = :id")
    suspend fun updatePostedAt(id: Long, postedAt: Long)

    @Query(
        """
        SELECT categoryId, SUM(amount) as total
        FROM transactions
        WHERE postedAt BETWEEN :fromMillis AND :toMillis AND categoryId IS NOT NULL
        GROUP BY categoryId
        ORDER BY total DESC
        """
    )
    fun categoryTotals(fromMillis: Long, toMillis: Long): Flow<List<CategoryTotal>>

    @Query(
        """
        SELECT COALESCE(SUM(amount), 0.0) FROM transactions
        WHERE postedAt BETWEEN :fromMillis AND :toMillis
        """
    )
    fun totalSpent(fromMillis: Long, toMillis: Long): Flow<Double>
}
