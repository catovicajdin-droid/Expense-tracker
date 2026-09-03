package com.catovicajdin.expensetracker.ui

sealed class Screen {
    data object Home : Screen()
    data object Ledger : Screen()
    data object Filters : Screen()
    data object BudgetDashboard : Screen()
    data object BudgetSettings : Screen()
    data object NeedsReview : Screen()

    /** resolvingRawId set means this add-flow finishes a NEEDS_REVIEW row instead of starting fresh. */
    data class AddTransaction(
        val prefillCategoryId: Long? = null,
        val prefillPostedAt: Long? = null,
        val resolvingRawId: Long? = null,
    ) : Screen()

    data class Detail(val transactionId: Long) : Screen()
    data class Categorize(val transactionId: Long) : Screen()
}
