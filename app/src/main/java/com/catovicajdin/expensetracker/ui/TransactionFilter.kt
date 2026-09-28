package com.catovicajdin.expensetracker.ui

enum class TagMatchMode { ANY, ALL }

data class TransactionFilter(
    val categoryIds: Set<Long> = emptySet(),
    /** Transactions with no category at all - not a category, so it cannot live in [categoryIds]. */
    val includeUncategorized: Boolean = false,
    val fromMillis: Long? = null,
    val toMillis: Long? = null,
    val minAmount: Double? = null,
    val maxAmount: Double? = null,
    val tagIds: Set<Long> = emptySet(),
    val tagMatchMode: TagMatchMode = TagMatchMode.ANY,
)
