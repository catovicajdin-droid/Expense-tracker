package com.catovicajdin.expensetracker.data

data class TagTotal(
    val tagId: Long,
    val tagName: String,
    val total: Double,
    val count: Int,
)
