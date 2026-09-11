package com.catovicajdin.expensetracker.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.catovicajdin.expensetracker.data.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Insert
    suspend fun insert(category: CategoryEntity): Long

    @Query("SELECT * FROM categories ORDER BY sortOrder")
    fun all(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun byId(id: Long): CategoryEntity?

    @Query("SELECT * FROM categories WHERE isQuickPick = 1 ORDER BY sortOrder LIMIT :limit")
    suspend fun quickPicks(limit: Int): List<CategoryEntity>

    @Query("SELECT COUNT(*) FROM categories WHERE name = :name AND id != :excludingId")
    suspend fun countByName(name: String, excludingId: Long = -1L): Int

    @Query("SELECT COALESCE(MAX(sortOrder), -1) + 1 FROM categories")
    suspend fun nextSortOrder(): Int

    @Query("UPDATE categories SET name = :name, icon = :icon, colorHex = :colorHex WHERE id = :id")
    suspend fun updateDetails(id: Long, name: String, icon: String, colorHex: String)

    @Query("UPDATE categories SET isQuickPick = :isQuickPick WHERE id = :id")
    suspend fun updateQuickPick(id: Long, isQuickPick: Boolean)

    /** Creates the category and returns its id, ordered after every existing one. */
    @Transaction
    suspend fun create(name: String, icon: String, colorHex: String): Long =
        insert(
            CategoryEntity(
                name = name,
                icon = icon,
                colorHex = colorHex,
                sortOrder = nextSortOrder(),
            )
        )

    /**
     * transactions.categoryId has no foreign key to this table, so nothing cascades - deleting a
     * row without this cleanup would leave transactions pointing at an id that no longer exists.
     * Their spend is kept and simply becomes uncategorized; the category's budgets and the
     * already-fired budget alerts keyed to it go with it.
     */
    @Transaction
    suspend fun deleteAndDetach(id: Long) {
        clearCategoryFromTransactions(id)
        deleteCategoryBudgets(id)
        deleteBudgetAlerts(id)
        deleteCategory(id)
    }

    @Query("UPDATE transactions SET categoryId = NULL WHERE categoryId = :id")
    suspend fun clearCategoryFromTransactions(id: Long)

    @Query("DELETE FROM category_budgets WHERE categoryId = :id")
    suspend fun deleteCategoryBudgets(id: Long)

    @Query("DELETE FROM budget_alerts WHERE categoryId = :id")
    suspend fun deleteBudgetAlerts(id: Long)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteCategory(id: Long)
}
