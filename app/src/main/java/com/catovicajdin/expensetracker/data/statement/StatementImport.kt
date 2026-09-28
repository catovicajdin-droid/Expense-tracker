package com.catovicajdin.expensetracker.data.statement

import android.content.Context
import com.catovicajdin.expensetracker.data.AppDatabase
import com.catovicajdin.expensetracker.data.entity.RawNotificationEntity
import com.catovicajdin.expensetracker.data.entity.TransactionEntity
import com.catovicajdin.expensetracker.data.entity.TransactionTagCrossRef
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import kotlin.math.abs

/** One statement line that would become a transaction. */
data class ImportRow(
    val description: String,
    val amount: Double,
    val date: LocalDate,
    val postedAt: Long,
    /** The app already has a transaction of this amount around this date - almost certainly the same one. */
    val looksAlreadyPresent: Boolean,
)

/**
 * Every line from the same merchant, so one decision covers all of them. On a real statement this is
 * the difference between 125 choices and 57 - and a handful of those cover most of the rows.
 */
data class MerchantGroup(
    val key: String,
    val label: String,
    val rows: List<ImportRow>,
    /** The category this merchant's transactions were given last time, if the app has seen it before. */
    val suggestedCategoryId: Long?,
) {
    val total: Double get() = rows.sumOf { it.amount }
    val newCount: Int get() = rows.count { !it.looksAlreadyPresent }
}

data class ImportPlan(
    val statement: ParsedStatement,
    val groups: List<MerchantGroup>,
    /** Credits and reversals: money in, so not spending. Never imported. */
    val nonExpenseCount: Int,
    val duplicateCount: Int,
    val sourceKey: String,
    /** Rows already imported from a statement covering this same period. */
    val alreadyImportedFromPeriod: Int,
    val currency: String,
) {
    val expenseCount: Int get() = groups.sumOf { it.rows.size }
    fun importableCount(skipDuplicates: Boolean): Int =
        if (skipDuplicates) groups.sumOf { it.newCount } else expenseCount
}

/** What the user decided for one merchant group. Nothing chosen imports as uncategorized, not blocked. */
data class GroupChoice(
    val categoryId: Long? = null,
    val tagIds: Set<Long> = emptySet(),
    val newTagNames: List<String> = emptyList(),
)

object StatementImport {

    /** A statement's date is a day, not a moment; noon keeps it in that day whatever the zone does. */
    private const val NOON_HOUR = 12

    /** Posting and purchase dates differ by a day or two, so a match nearby is still the same purchase. */
    private const val DUPLICATE_DAYS = 3L

    suspend fun plan(context: Context, statement: ParsedStatement): ImportPlan {
        val db = AppDatabase.get(context)
        val zone = ZoneId.systemDefault()
        val expenses = statement.rows.filter { it.isExpense }

        val existing = if (expenses.isEmpty()) emptyList() else {
            val from = expenses.minOf { it.transactionDate }.minusDays(DUPLICATE_DAYS)
                .atStartOfDay(zone).toInstant().toEpochMilli()
            val to = expenses.maxOf { it.transactionDate }.plusDays(DUPLICATE_DAYS)
                .atTime(23, 59, 59).atZone(zone).toInstant().toEpochMilli()
            db.transactionDao().betweenOnce(from, to)
        }

        val prefix = commonPrefix(expenses.map { it.description })
        val rows = expenses.map { row ->
            val postedAt = row.transactionDate.atTime(NOON_HOUR, 0).atZone(zone).toInstant().toEpochMilli()
            val amount = StatementParser.round2(row.net)
            ImportRow(
                description = row.description,
                amount = amount,
                date = row.transactionDate,
                postedAt = postedAt,
                looksAlreadyPresent = existing.any {
                    abs(it.amount - amount) < 0.005 &&
                        abs(it.postedAt - postedAt) <= DUPLICATE_DAYS * 24 * 60 * 60 * 1000
                },
            )
        }

        val groups = rows.groupBy { merchantKey(it.description, prefix) }
            .map { (key, groupRows) ->
                // The label is a description as the bank wrote it, punctuation and all, so it is what
                // the suggestion is looked up by - the key has had its dots stripped to group
                // "P-076" with "P-078", which would no longer match anything stored.
                val label = groupRows.groupingBy { stripPrefix(it.description, prefix) }
                    .eachCount().maxByOrNull { it.value }?.key ?: key
                MerchantGroup(
                    key = key,
                    label = label,
                    rows = groupRows.sortedBy { it.postedAt },
                    suggestedCategoryId = db.transactionDao().suggestedCategoryForDescription(label.uppercase()),
                )
            }
            .sortedByDescending { it.total }

        val period = statement.rows.groupingBy { YearMonth.from(it.orderDate) }.eachCount()
            .maxByOrNull { it.value }?.key ?: YearMonth.from(statement.firstDate)
        val sourceKey = "statement:$period"

        return ImportPlan(
            statement = statement,
            groups = groups,
            nonExpenseCount = statement.rows.size - expenses.size,
            duplicateCount = rows.count { it.looksAlreadyPresent },
            sourceKey = sourceKey,
            alreadyImportedFromPeriod = db.rawNotificationDao().countForPackage(sourceKey),
            currency = db.transactionDao().mostRecentCurrency() ?: "BAM",
        )
    }

    /**
     * Writes the chosen rows. Each one keeps its statement line verbatim in raw_notifications, the
     * way a notification's text is kept - it is what a transaction can be traced back to, and what
     * lets the next import suggest a category for a merchant seen before.
     */
    suspend fun commit(
        context: Context,
        plan: ImportPlan,
        choices: Map<String, GroupChoice>,
        skipDuplicates: Boolean,
    ): Int {
        val db = AppDatabase.get(context)
        var imported = 0
        for (group in plan.groups) {
            val choice = choices[group.key] ?: GroupChoice()
            val tagIds = choice.tagIds + choice.newTagNames.map { db.tagDao().getOrCreate(it) }
            for (row in group.rows) {
                if (skipDuplicates && row.looksAlreadyPresent) continue
                val rawId = db.rawNotificationDao().insert(
                    RawNotificationEntity(
                        packageName = plan.sourceKey,
                        title = row.date.toString(),
                        body = row.description,
                        postedAt = row.postedAt,
                        parseStatus = "PARSED",
                        parserVersion = 0,
                    ),
                )
                val transactionId = db.transactionDao().insert(
                    TransactionEntity(
                        rawNotificationId = rawId,
                        amount = row.amount,
                        currency = plan.currency,
                        availableBalance = 0.0,
                        postedAt = row.postedAt,
                        categoryId = choice.categoryId,
                    ),
                )
                tagIds.forEach { db.tagDao().addTagToTransaction(TransactionTagCrossRef(transactionId, it)) }
                imported++
            }
        }
        return imported
    }

    /**
     * Banks label card purchases with the same words on every line - "Kupovina VISA karticom - ",
     * and whatever the equivalent is elsewhere. That prefix is learned from the document rather than
     * listed here: whatever comes before the first " - " on a fifth or more of the lines is the
     * bank's own wording, not a merchant, and drops out so the merchants group properly.
     */
    private fun commonPrefix(descriptions: List<String>): String? {
        if (descriptions.isEmpty()) return null
        val counts = descriptions
            .filter { it.contains(" - ") }
            .map { it.substringBefore(" - ").trim().uppercase() }
            .filter { it.isNotEmpty() }
            .groupingBy { it }
            .eachCount()
        val best = counts.maxByOrNull { it.value } ?: return null
        return if (best.value * 5 >= descriptions.size) best.key else null
    }

    private fun stripPrefix(description: String, prefix: String?): String {
        if (prefix == null) return description.trim()
        val head = description.substringBefore(" - ").trim()
        return if (head.uppercase() == prefix) description.substringAfter(" - ").trim() else description.trim()
    }

    /**
     * What counts as "the same merchant": the description without the bank's wording, upper-cased and
     * with digits dropped, so the terminal number in a chain's name doesn't split it into one group
     * per till.
     */
    private fun merchantKey(description: String, prefix: String?): String =
        stripPrefix(description, prefix)
            .uppercase()
            .filter { it.isLetter() || it.isWhitespace() }
            .split(" ")
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .take(40)
            .ifEmpty { "UNLABELLED" }
}
