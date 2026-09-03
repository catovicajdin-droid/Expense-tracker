package com.catovicajdin.expensetracker.ui

sealed class Screen {
    data object TransactionList : Screen()
    data object NeedsReview : Screen()
    data class Categorize(val transactionId: Long) : Screen()
}
