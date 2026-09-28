package com.catovicajdin.expensetracker.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.catovicajdin.expensetracker.data.entity.BudgetAlertEntity
import com.catovicajdin.expensetracker.data.entity.CategoryBudgetEntity
import com.catovicajdin.expensetracker.data.entity.MonthlyBudgetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Query("SELECT * FROM monthly_budgets WHERE yearMonth = :yearMonth")
    fun monthlyBudgetFlow(yearMonth: String): Flow<MonthlyBudgetEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setMonthlyBudget(budget: MonthlyBudgetEntity)

    @Query("SELECT * FROM category_budgets WHERE yearMonth = :yearMonth")
    fun categoryBudgetsFlow(yearMonth: String): Flow<List<CategoryBudgetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setCategoryBudget(budget: CategoryBudgetEntity)

    /** Most recent earlier month with any category budgets set, to suggest carrying them forward. */
    @Query("SELECT DISTINCT yearMonth FROM category_budgets WHERE yearMonth < :beforeYearMonth ORDER BY yearMonth DESC LIMIT 1")
    suspend fun mostRecentBudgetedMonth(beforeYearMonth: String): String?

    @Query("SELECT * FROM category_budgets WHERE yearMonth = :yearMonth")
    suspend fun categoryBudgetsOnce(yearMonth: String): List<CategoryBudgetEntity>

    @Query("SELECT * FROM monthly_budgets WHERE yearMonth = :yearMonth")
    suspend fun monthlyBudgetOnce(yearMonth: String): MonthlyBudgetEntity?

    /**
     * The month an unbudgeted one borrows its figures from: the nearest month that has any,
     * preferring the month *after*, since a later month reflects a more current view of what things
     * cost than an older one. Lets a month opened for the first time - or filled in by a statement
     * import - show budgets without every month being set up by hand.
     */
    @Query(
        """
        SELECT yearMonth FROM category_budgets
        WHERE yearMonth <> :yearMonth
        GROUP BY yearMonth
        ORDER BY CASE WHEN yearMonth > :yearMonth THEN 0 ELSE 1 END,
                 CASE WHEN yearMonth > :yearMonth THEN yearMonth END ASC,
                 yearMonth DESC
        LIMIT 1
        """
    )
    suspend fun nearestBudgetedMonth(yearMonth: String): String?

    /**
     * Writes the inherited figures into this month before the edit lands. Without it, setting one
     * category in a month that was only borrowing would give that month a row of its own, stop the
     * inheritance, and silently drop every other budget it was showing.
     */
    @Transaction
    suspend fun setCategoryBudgetMaterializing(yearMonth: String, categoryId: Long, amount: Double) {
        if (categoryBudgetsOnce(yearMonth).isEmpty()) {
            nearestBudgetedMonth(yearMonth)?.let { source ->
                categoryBudgetsOnce(source).forEach { setCategoryBudget(it.copy(yearMonth = yearMonth)) }
            }
        }
        setCategoryBudget(CategoryBudgetEntity(yearMonth, categoryId, amount))
    }

    /** Returns -1 if this (yearMonth, categoryId, threshold) alert was already recorded - the caller's cue to skip notifying again. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun recordAlert(alert: BudgetAlertEntity): Long
}
