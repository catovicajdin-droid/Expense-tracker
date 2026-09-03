package com.catovicajdin.expensetracker.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
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

    /** Replaces a transaction's full tag set in one go - simpler for the edit dialog than diffing. */
    @Transaction
    suspend fun replaceTagsForTransaction(transactionId: Long, tagIds: Set<Long>) {
        clearTagsForTransaction(transactionId)
        tagIds.forEach { tagId -> addTagToTransaction(TransactionTagCrossRef(transactionId, tagId)) }
    }
}
