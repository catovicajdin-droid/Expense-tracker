package com.catovicajdin.expensetracker.ui

data class TransactionFilter(
    val categoryId: Long? = null,
    val fromMillis: Long? = null,
    val toMillis: Long? = null,
    val minAmount: Double? = null,
    val maxAmount: Double? = null,
    val tagId: Long? = null,
)
