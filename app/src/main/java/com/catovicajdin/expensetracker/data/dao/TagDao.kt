package com.catovicajdin.expensetracker.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.catovicajdin.expensetracker.data.TagTotal
import com.catovicajdin.expensetracker.data.TransactionTagName
import com.catovicajdin.expensetracker.data.entity.TagEntity
import com.catovicajdin.expensetracker.data.entity.TransactionTagCrossRef
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {
    @Query("SELECT * FROM tags ORDER BY name")
    fun all(): Flow<List<TagEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(tag: TagEntity): Long

    @Query("SELECT * FROM tags WHERE name = :name LIMIT 1")
    suspend fun findByName(name: String): TagEntity?

    /** Creates the tag if it doesn't already exist (by name), returns its id either way. */
    @Transaction
    suspend fun getOrCreate(name: String): Long {
        findByName(name)?.let { return it.id }
        insert(TagEntity(name = name))
        return findByName(name)!!.id
    }

    @Query("SELECT tagId FROM transaction_tags WHERE transactionId = :transactionId")
    suspend fun tagIdsForTransaction(transactionId: Long): List<Long>

    /** Cascades: transaction_tags rows referencing this tag are removed automatically (ON DELETE CASCADE). */
    @Query("DELETE FROM tags WHERE id = :id")
    suspend fun delete(id: Long)

    /** One-time cleanup for tags saved with a leading "#" before entry stripped it - OR IGNORE so a
     * collision with an already-clean tag of the same name just drops the duplicate instead of
     * failing the whole statement. */
    @Query("UPDATE OR IGNORE tags SET name = TRIM(name, '#') WHERE name LIKE '#%'")
    suspend fun stripLeadingHashFromNames()

    @Query(
        """
        SELECT tt.transactionId as transactionId, t.name as tagName
        FROM transaction_tags tt
        JOIN tags t ON t.id = tt.tagId
        """
    )
    fun allTransactionTagNames(): Flow<List<TransactionTagName>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addTagToTransaction(crossRef: TransactionTagCrossRef)

    @Query("DELETE FROM transaction_tags WHERE transactionId = :transactionId")
    suspend fun clearTagsForTransaction(transactionId: Long)

    @Query(
        """
        SELECT t.id as tagId, t.name as tagName, SUM(tx.amount) as total, COUNT(tx.id) as count
        FROM tags t
        JOIN transaction_tags tt ON tt.tagId = t.id
        JOIN transactions tx ON tx.id = tt.transactionId
        WHERE tx.postedAt BETWEEN :fromMillis AND :toMillis
        GROUP BY t.id
        ORDER BY total DESC
        """
    )
    fun tagTotals(fromMillis: Long, toMillis: Long): Flow<List<TagTotal>>

    /** Replaces a transaction's full tag set in one go - simpler for the edit dialog than diffing. */
    @Transaction
    suspend fun replaceTagsForTransaction(transactionId: Long, tagIds: Set<Long>) {
        clearTagsForTransaction(transactionId)
        tagIds.forEach { tagId -> addTagToTransaction(TransactionTagCrossRef(transactionId, tagId)) }
    }
}
