package com.catovicajdin.expensetracker.ui

import com.catovicajdin.expensetracker.data.entity.CategoryEntity

/**
 * How a per-category list is ordered. Shared by the budget dashboard's grid and the edit-budgets
 * list so the two offer the same choices, though they default differently: the dashboard opens on
 * what's most urgent (% spent), while edit-budgets opens on the plain A-Z listing.
 *
 * This is a per-screen view preference only - it never writes anything back, so changing it on one
 * screen can't disturb another. The underlying list is always alphabetical (CategoryDao.all).
 */
enum class BudgetSort(val label: String) {
    ALPHABETICAL("A–Z"),
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
    BudgetSort.ALPHABETICAL -> sortedBy { category(it).name.lowercase() }
    BudgetSort.AMOUNT_SPENT -> sortedByDescending { spent(it) }
    BudgetSort.BUDGET_SIZE -> sortedByDescending { budget(it) ?: Double.NEGATIVE_INFINITY }
    BudgetSort.PERCENT_SPENT -> sortedByDescending {
        val b = budget(it)
        if (b != null && b > 0.0) spent(it) / b else Double.NEGATIVE_INFINITY
    }
}
