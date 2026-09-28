package com.catovicajdin.expensetracker.data

/** What a transaction came from, and where it was filed - the raw material for suggesting a category. */
data class CategorizedSourceText(val body: String, val categoryId: Long)
