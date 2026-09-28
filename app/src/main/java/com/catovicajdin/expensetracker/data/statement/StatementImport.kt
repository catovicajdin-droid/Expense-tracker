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
    /** Position in the statement. Identifies a row for an override that applies to it alone. */
    val id: Int,
    /** The statement line as written, kept with the transaction so it can be traced back. */
    val description: String,
    /** The same line without the bank's standing wording - what to show when naming this one row. */
    val shortDescription: String,
    val amount: Double,
    /** The day the bank moved the money, which is the day the transaction is filed under. */
    val postingDate: LocalDate,
    /** The day the card was used. Earlier than the posting date by a day or three. */
    val transactionDate: LocalDate,
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
    /** Rows the card was used on before the statement's own period - filed under the day they were posted. */
    val transactedEarlier: Int,
) {
    val expenseCount: Int get() = groups.sumOf { it.rows.size }
}

/**
 * What the user decided for a merchant or for particular rows of it. Nothing chosen imports as
 * uncategorized rather than blocking the import.
 *
 * Tags are held as ids, never as names still waiting to be created: a tag typed here is created at
 * once, so it is immediately offered to every other merchant in the same review and shows as
 * selected where it was typed.
 */
data class GroupChoice(
    val categoryId: Long? = null,
    val tagIds: Set<Long> = emptySet(),
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
        val rows = expenses.mapIndexed { index, row ->
            // Filed under the posting date, the column the statement is ordered by: that is when the
            // money left the account, and it keeps each statement's month whole. Using the card date
            // instead would scatter the first few rows of every statement into the month before,
            // which is not what importing "July" is understood to mean.
            val postedAt = row.orderDate.atTime(NOON_HOUR, 0).atZone(zone).toInstant().toEpochMilli()
            val amount = StatementParser.round2(row.net)
            // Matched against what is already recorded on the card date, though: a notification
            // arrived when the card was used, not when the bank got round to posting it.
            val usedAt = row.transactionDate.atTime(NOON_HOUR, 0).atZone(zone).toInstant().toEpochMilli()
            ImportRow(
                id = index,
                description = row.description,
                shortDescription = stripPrefix(row.description, prefix),
                amount = amount,
                postingDate = row.orderDate,
                transactionDate = row.transactionDate,
                postedAt = postedAt,
                looksAlreadyPresent = existing.any {
                    abs(it.amount - amount) < 0.005 &&
                        abs(it.postedAt - usedAt) <= DUPLICATE_DAYS * 24 * 60 * 60 * 1000
                },
            )
        }

        // What the app has been told before, keyed the same way this statement is grouped: previous
        // imports, and any notification whose text names a merchant. Built from the transactions
        // themselves rather than from saved rules, so it follows every later correction - put a
        // merchant in a different category in the ledger and the next import follows suit. Oldest
        // first, so the most recent decision is the one that survives into the map.
        val learned = db.transactionDao().categorizedSourceTexts()
            .associate { merchantKey(it.body, prefix) to it.categoryId }

        val groups = rows.groupBy { merchantKey(it.description, prefix) }
            .map { (key, groupRows) ->
                val label = groupRows.groupingBy { stripPrefix(it.description, prefix) }
                    .eachCount().maxByOrNull { it.value }?.key ?: key
                MerchantGroup(
                    key = key,
                    label = label,
                    rows = groupRows.sortedBy { it.postedAt },
                    suggestedCategoryId = learned[key],
                )
            }
            .sortedByDescending { it.total }

        val period = statement.rows.groupingBy { YearMonth.from(it.orderDate) }.eachCount()
            .maxByOrNull { it.value }?.key ?: YearMonth.from(statement.postingFirst)
        // Each run gets its own key so it can be undone on its own; importing the same month twice
        // must not produce one batch that can only be removed wholesale.
        val sourceKey = "statement:$period:${System.currentTimeMillis()}"

        return ImportPlan(
            statement = statement,
            groups = groups,
            nonExpenseCount = statement.rows.size - expenses.size,
            duplicateCount = rows.count { it.looksAlreadyPresent },
            sourceKey = sourceKey,
            alreadyImportedFromPeriod = db.rawNotificationDao().countForPackagePrefix("statement:$period"),
            currency = db.transactionDao().mostRecentCurrency() ?: "BAM",
            transactedEarlier = expenses.count { YearMonth.from(it.transactionDate) < period },
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
        rowChoices: Map<Int, GroupChoice>,
        droppedRowIds: Set<Int>,
        skipDuplicates: Boolean,
    ): Int {
        val db = AppDatabase.get(context)
        var imported = 0
        for (group in plan.groups) {
            for (row in group.rows) {
                if (row.id in droppedRowIds) continue
                if (skipDuplicates && row.looksAlreadyPresent) continue
                // A row set on its own wins over what the merchant was given; otherwise it follows
                // the group, which is the point of grouping in the first place.
                val choice = rowChoices[row.id] ?: choices[group.key] ?: GroupChoice()
                val tagIds = choice.tagIds
                val rawId = db.rawNotificationDao().insert(
                    RawNotificationEntity(
                        packageName = plan.sourceKey,
                        title = row.postingDate.toString(),
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
