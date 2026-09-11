package com.catovicajdin.expensetracker.ui

import com.catovicajdin.expensetracker.data.entity.CategoryEntity

/**
 * How a per-category list is ordered. Shared by the budget dashboard's grid and the edit-budgets
 * list so the two offer the same choices, though they default differently: the dashboard opens on
 * what's most urgent (% spent), while edit-budgets opens on the order you arranged yourself.
 */
enum class BudgetSort(val label: String) {
    CATEGORY_ORDER("Category order"),
    PERCENT_SPENT("% of budget spent"),
    AMOUNT_SPENT("Amount spent"),
    BUDGET_SIZE("Budget size"),
}

/**
 * Applies [sort] to [items], descending for every measure-based option. Categories with no budget
 * set have no percentage and no size to rank by, so they sink to the bottom of those two rather
 * than being treated as zero and mixed in among real values.
 */
fun <T> List<T>.sortedForBudget(
    sort: BudgetSort,
    category: (T) -> CategoryEntity,
    spent: (T) -> Double,
    budget: (T) -> Double?,
): List<T> = when (sort) {
    BudgetSort.CATEGORY_ORDER -> sortedBy { category(it).sortOrder }
    BudgetSort.AMOUNT_SPENT -> sortedByDescending { spent(it) }
    BudgetSort.BUDGET_SIZE -> sortedByDescending { budget(it) ?: Double.NEGATIVE_INFINITY }
    BudgetSort.PERCENT_SPENT -> sortedByDescending {
        val b = budget(it)
        if (b != null && b > 0.0) spent(it) / b else Double.NEGATIVE_INFINITY
    }
}
