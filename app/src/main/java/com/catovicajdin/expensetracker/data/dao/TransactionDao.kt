package com.catovicajdin.expensetracker.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.catovicajdin.expensetracker.data.CategorizedSourceText
import com.catovicajdin.expensetracker.data.CategoryTotal
import com.catovicajdin.expensetracker.data.DuplicateGroup
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
     * "Uncategorized" is its own switch rather than an id, since having no category cannot be
     * expressed as one: with it on and no categories picked, only uncategorized rows come back; with
     * categories picked too, it widens that selection rather than narrowing it.
     *
     * Tag matching has two modes: matchAllTags=false means "has any of tagIds" (OR across tags),
     * matchAllTags=true means "has every one of tagIds" (AND across tags), same reasoning for
     * tagCount.
     */
    @Query(
        """
        SELECT transactions.*, raw_notifications.packageName as source
        FROM transactions
        JOIN raw_notifications ON raw_notifications.id = transactions.rawNotificationId
        WHERE (
            (:categoryCount = 0 AND :includeUncategorized = 0)
            OR categoryId IN (:categoryIds)
            OR (:includeUncategorized = 1 AND categoryId IS NULL)
        )
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
        includeUncategorized: Boolean,
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

    /**
     * The months that actually have transactions, newest first, as "YYYY-MM" keys. Computed in the
     * device's own zone ('localtime') so a late-evening purchase lands in the month the rest of the
     * app puts it in - [com.catovicajdin.expensetracker.data.MonthRange] slices by local midnight too.
     */
    @Query(
        """
        SELECT DISTINCT strftime('%Y-%m', postedAt / 1000, 'unixepoch', 'localtime') AS ym
        FROM transactions
        ORDER BY ym DESC
        """
    )
    fun monthsWithData(): Flow<List<String>>

    /**
     * Transactions recorded more than once from the same notification text - what the re-post and
     * the ingest race left behind before either was fixed. Both fixes were forward-only, so rows
     * already written stay until something clears them.
     *
     * Two guards keep honest rows out of the list. Statement rows are excluded: two identical lines
     * on a statement are two real purchases, not one recorded twice. And every copy in a group has
     * to come from a *different* raw notification, which is what duplication looks like - a split
     * puts two rows against the one notification, and an even split would otherwise look identical.
     */
    @Query(
        """
        SELECT GROUP_CONCAT(t.id) AS ids, COUNT(*) AS copies,
               r.title AS title, r.body AS body,
               MIN(t.amount) AS amount, MIN(t.currency) AS currency,
               MIN(t.postedAt) AS firstPostedAt, MAX(t.postedAt) AS lastPostedAt
        FROM transactions t
        JOIN raw_notifications r ON r.id = t.rawNotificationId
        WHERE r.packageName NOT LIKE 'statement%'
        GROUP BY r.packageName, r.title, r.body, t.amount
        HAVING COUNT(*) > 1 AND COUNT(DISTINCT t.rawNotificationId) = COUNT(*)
        ORDER BY MAX(t.postedAt) DESC
        """
    )
    fun duplicateGroups(): Flow<List<DuplicateGroup>>

    /** Leaves the originating raw_notifications rows intact, same as [delete]. */
    @Query("DELETE FROM transactions WHERE id IN (:ids)")
    suspend fun deleteAll(ids: List<Long>)

    /** Everything posted in a window, for checking an import against transactions already recorded. */
    @Query("SELECT * FROM transactions WHERE postedAt BETWEEN :from AND :to")
    suspend fun betweenOnce(from: Long, to: Long): List<TransactionEntity>

    /** What an import should record its amounts in, taken from what is already here. */
    @Query("SELECT currency FROM transactions ORDER BY postedAt DESC LIMIT 1")
    suspend fun mostRecentCurrency(): String?

    /**
     * The source text of every categorized transaction, oldest first, so an import can work out what
     * a merchant was filed under before. Statement rows keep their line, and a notification keeps
     * its message, so both teach the next import - and because this reads the transactions rather
     * than a table of saved rules, correcting a category in the ledger corrects the suggestion too.
     */
    @Query(
        """
        SELECT r.body AS body, t.categoryId AS categoryId
        FROM transactions t
        JOIN raw_notifications r ON r.id = t.rawNotificationId
        WHERE t.categoryId IS NOT NULL
        ORDER BY t.postedAt ASC
        """
    )
    suspend fun categorizedSourceTexts(): List<CategorizedSourceText>
}
