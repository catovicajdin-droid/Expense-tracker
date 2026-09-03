package com.catovicajdin.expensetracker.ui

enum class TagMatchMode { ANY, ALL }

data class TransactionFilter(
    val categoryId: Long? = null,
    val fromMillis: Long? = null,
    val toMillis: Long? = null,
    val minAmount: Double? = null,
    val maxAmount: Double? = null,
    val tagIds: Set<Long> = emptySet(),
    val tagMatchMode: TagMatchMode = TagMatchMode.ANY,
)
