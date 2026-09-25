package com.catovicajdin.expensetracker.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
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

    /** Recategorizes a whole selection in one statement, so the list redraws once rather than per row. */
    @Query("UPDATE transactions SET categoryId = :categoryId WHERE id IN (:ids)")
    suspend fun assignCategoryToAll(ids: List<Long>, categoryId: Long?)

    @Query("UPDATE transactions SET amount = :amount, categoryId = :categoryId WHERE id = :id")
    suspend fun updateAmountAndCategory(id: Long, amount: Double, categoryId: Long?)

    /**
     * Carves [splitAmount] off a transaction into a second one - a cash withdrawal where only part
     * of it went on the thing you actually bought, the rest still being cash in hand.
     *
     * The new row keeps the original's rawNotificationId, date and currency, so both halves still
     * trace back to the one bank notification and the ledger's total is unchanged: the original is
     * reduced by exactly what the new one takes. @Transaction so a failure can't leave money
     * duplicated or missing between the two writes.
     *
     * Returns the new transaction's id, or null if the amount doesn't leave something on both
     * sides - splitting off all of it, or none, is not a split.
     */
    @Transaction
    suspend fun splitOff(
        id: Long,
        splitAmount: Double,
        splitCategoryId: Long?,
        remainderCategoryId: Long?,
    ): Long? {
        val original = byId(id) ?: return null
        if (splitAmount <= 0.0 || splitAmount >= original.amount) return null
        updateAmountAndCategory(id, original.amount - splitAmount, remainderCategoryId)
        return insert(
            original.copy(id = 0, amount = splitAmount, categoryId = splitCategoryId),
        )
    }

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
