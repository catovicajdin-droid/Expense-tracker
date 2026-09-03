package com.catovicajdin.expensetracker.data

import com.catovicajdin.expensetracker.Constants
import com.catovicajdin.expensetracker.data.entity.RawNotificationEntity
import com.catovicajdin.expensetracker.data.entity.TransactionEntity
import com.catovicajdin.expensetracker.notifications.ParseOutcome
import com.catovicajdin.expensetracker.notifications.TransactionParser

class NotificationRepository(private val db: AppDatabase) {

    /**
     * Stores the raw notification unconditionally, then attempts to parse it. A parse failure never
     * loses data: the raw row is kept, flagged NEEDS_REVIEW with the failure reason attached.
     * Returns the created transaction id, or null if this notification wasn't a parsed transaction
     * (needs review, ignored, or a duplicate delivery).
     */
    suspend fun ingest(packageName: String, title: String, body: String, postedAt: Long): Long? {
        if (isDuplicate(packageName, title, body, postedAt)) return null

        val outcome = TransactionParser.parse(title, body)

        val status = when (outcome) {
            is ParseOutcome.Success -> "PARSED"
            is ParseOutcome.Failure ->
                if (title.trim() == Constants.NOTIFICATION_TITLE_TRANSACTION) "NEEDS_REVIEW" else "IGNORED"
        }
        val failureReason = (outcome as? ParseOutcome.Failure)?.reason

        val rawId = db.rawNotificationDao().insert(
            RawNotificationEntity(
                packageName = packageName,
                title = title,
                body = body,
                postedAt = postedAt,
                parseStatus = status,
                parserVersion = Constants.PARSER_VERSION,
                failureReason = failureReason,
            )
        )

        if (outcome is ParseOutcome.Success) {
            val t = outcome.transaction
            return db.transactionDao().insert(
                TransactionEntity(
                    rawNotificationId = rawId,
                    amount = t.amount,
                    currency = t.currency,
                    availableBalance = t.availableBalance,
                    postedAt = postedAt,
                )
            )
        }
        return null
    }

    /** Android occasionally redelivers/updates the same notification; catch it via a tight time+content window. */
    private suspend fun isDuplicate(packageName: String, title: String, body: String, postedAt: Long): Boolean {
        val windowMillis = 60_000L
        val recent = db.rawNotificationDao().findRecentForDedup(
            packageName, postedAt - windowMillis, postedAt + windowMillis,
        )
        return recent.any { it.title == title && it.body == body }
    }

    /**
     * Manually-entered transactions (cash, anything the bank never pushes a notification for) still
     * get a raw_notifications row, so every transaction traces back to one uniformly - it's just
     * marked as coming from "manual" rather than the bank's package.
     */
    suspend fun insertManual(amount: Double, categoryId: Long?, postedAt: Long): Long {
        val rawId = db.rawNotificationDao().insert(
            RawNotificationEntity(
                packageName = "manual",
                title = "Manual entry",
                body = "Manually added transaction",
                postedAt = postedAt,
                parseStatus = "PARSED",
                parserVersion = Constants.PARSER_VERSION,
            )
        )
        return db.transactionDao().insert(
            TransactionEntity(
                rawNotificationId = rawId,
                amount = amount,
                currency = "BAM",
                availableBalance = 0.0,
                postedAt = postedAt,
                categoryId = categoryId,
            )
        )
    }
}
