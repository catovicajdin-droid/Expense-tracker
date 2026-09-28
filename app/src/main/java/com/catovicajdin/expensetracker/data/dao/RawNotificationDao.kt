package com.catovicajdin.expensetracker.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.catovicajdin.expensetracker.data.StatementImportSummary
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

    @Query("SELECT COUNT(*) FROM raw_notifications WHERE packageName = :packageName")
    suspend fun countForPackage(packageName: String): Int

    /**
     * How many rows came from statements covering this period. Every import run gets its own source
     * key so it can be undone on its own, so a whole period spans several of them and has to be
     * counted by prefix.
     */
    @Query("SELECT COUNT(*) FROM raw_notifications WHERE packageName LIKE :prefix || '%'")
    suspend fun countForPackagePrefix(prefix: String): Int

    /**
     * One row per statement import, newest first: what it covers, when it was run, and what it put
     * in the ledger. Amounts come from the transactions themselves, so a run whose rows were since
     * deleted by hand reports what is actually left of it.
     */
    @Query(
        """
        SELECT r.packageName AS sourceKey,
               COUNT(t.id) AS transactionCount,
               COALESCE(SUM(t.amount), 0.0) AS total,
               MIN(r.postedAt) AS firstPostedAt,
               MAX(r.postedAt) AS lastPostedAt,
               MAX(r.id) AS lastRowId
        FROM raw_notifications r
        LEFT JOIN transactions t ON t.rawNotificationId = r.id
        WHERE r.packageName LIKE 'statement:%'
        GROUP BY r.packageName
        ORDER BY lastRowId DESC
        """
    )
    fun statementImports(): Flow<List<StatementImportSummary>>

    @Query("DELETE FROM transactions WHERE rawNotificationId IN (SELECT id FROM raw_notifications WHERE packageName = :sourceKey)")
    suspend fun deleteTransactionsForSource(sourceKey: String)

    @Query("DELETE FROM raw_notifications WHERE packageName = :sourceKey")
    suspend fun deleteSource(sourceKey: String)

    /**
     * Undoes one import: its transactions first, then the statement lines they came from, in a
     * single transaction so a failure part-way cannot leave rows behind with nothing to trace them
     * to. Tag links go with the transactions, which the schema cascades.
     */
    @Transaction
    suspend fun deleteImport(sourceKey: String) {
        deleteTransactionsForSource(sourceKey)
        deleteSource(sourceKey)
    }

    @Query("UPDATE raw_notifications SET parseStatus = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)
}
