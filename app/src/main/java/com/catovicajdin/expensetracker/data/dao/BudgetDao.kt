package com.catovicajdin.expensetracker.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
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

    /** Returns -1 if this (yearMonth, categoryId, threshold) alert was already recorded - the caller's cue to skip notifying again. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun recordAlert(alert: BudgetAlertEntity): Long
}
