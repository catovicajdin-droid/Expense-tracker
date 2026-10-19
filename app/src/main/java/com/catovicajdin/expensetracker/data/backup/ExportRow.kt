package com.catovicajdin.expensetracker.data.backup

/** One transaction flattened for the CSV export: names instead of ids, tags joined into one cell. */
data class ExportRow(
    val postedAt: Long,
    val amount: Double,
    val currency: String,
    val category: String?,
    val tags: String?,
    val source: String,
    val details: String,
)
