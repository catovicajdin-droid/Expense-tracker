package com.catovicajdin.expensetracker.data

/**
 * Transactions that came out of the same bank notification text more than once - the shape the two
 * old duplication bugs left behind (a re-posted notification, and two ingests racing each other).
 *
 * [ids] is a comma-separated list of transaction ids, oldest first, because SQLite's GROUP_CONCAT
 * has no typed equivalent in Room. [keepId] is the one to keep: the first recorded.
 */
data class DuplicateGroup(
    val ids: String,
    val copies: Int,
    val title: String,
    val body: String,
    val amount: Double,
    val currency: String,
    val firstPostedAt: Long,
    val lastPostedAt: Long,
) {
    val idList: List<Long> get() = ids.split(',').mapNotNull { it.trim().toLongOrNull() }
    val keepId: Long? get() = idList.minOrNull()
    val extraIds: List<Long> get() = idList.filter { it != keepId }
}
