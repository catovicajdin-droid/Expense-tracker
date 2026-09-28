package com.catovicajdin.expensetracker.data

import com.catovicajdin.expensetracker.data.dao.BudgetDao
import com.catovicajdin.expensetracker.data.entity.CategoryBudgetEntity
import com.catovicajdin.expensetracker.data.entity.MonthlyBudgetEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * A month with no budgets of its own borrows them from the nearest month that has some, so budgets
 * apply from the moment a month is reached or a statement fills one in, rather than every month
 * having to be set up by hand.
 *
 * Inheritance is decided per month rather than per category: a month is either borrowing wholesale
 * or standing on its own. Falling back category by category would mean deliberately clearing one
 * budget quietly resurrected last month's figure for it.
 *
 * Nothing is written on read. The borrowed rows are stamped with the month being viewed so the
 * screens treat them as its own, and only an edit makes them real - see
 * [BudgetDao.setCategoryBudgetMaterializing].
 */
fun BudgetDao.effectiveCategoryBudgets(yearMonth: String): Flow<List<CategoryBudgetEntity>> =
    categoryBudgetsFlow(yearMonth).map { own ->
        if (own.isNotEmpty()) return@map own
        val source = nearestBudgetedMonth(yearMonth) ?: return@map emptyList()
        categoryBudgetsOnce(source).map { it.copy(yearMonth = yearMonth) }
    }

/** The overall budget, borrowed from the same month the category budgets come from. */
fun BudgetDao.effectiveMonthlyBudget(yearMonth: String): Flow<MonthlyBudgetEntity?> =
    monthlyBudgetFlow(yearMonth).map { own ->
        if (own != null) return@map own
        val source = nearestBudgetedMonth(yearMonth) ?: return@map null
        monthlyBudgetOnce(source)?.copy(yearMonth = yearMonth)
    }
