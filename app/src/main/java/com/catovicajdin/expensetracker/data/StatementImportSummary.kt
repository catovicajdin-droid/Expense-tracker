package com.catovicajdin.expensetracker.data

/**
 * What one statement import left in the ledger, so it can be reviewed and undone as a unit.
 *
 * [sourceKey] is "statement:<period>:<when it was run>" - the period for reading, the timestamp so
 * two imports of the same month stay separable.
 */
data class StatementImportSummary(
    val sourceKey: String,
    val transactionCount: Int,
    val total: Double,
    val firstPostedAt: Long,
    val lastPostedAt: Long,
    val lastRowId: Long,
) {
    /** "2026-07" out of "statement:2026-07:1759000000000". */
    val period: String get() = sourceKey.split(":").getOrNull(1).orEmpty()
}
