package com.catovicajdin.expensetracker.data.statement

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.roundToInt

/** One drawn character and where it sits on the page - what a PDF actually stores. */
data class Glyph(val text: String, val x0: Float, val x1: Float, val top: Float, val page: Int)

data class StatementRow(
    val orderDate: LocalDate,
    val transactionDate: LocalDate,
    /** Money out. Negative on a reversal, which is the bank crediting a debit back. */
    val debit: Double,
    /** Money in. */
    val credit: Double,
    val balance: Double,
    val description: String,
) {
    /** Positive means it cost money. Zero or less is income or a reversal, not an expense. */
    val net: Double get() = debit - credit
    val isExpense: Boolean get() = net > 0.0
}

data class ParsedStatement(
    val rows: List<StatementRow>,
    val opening: Double,
    val closing: Double,
    /** How many rows disagreed with the running balance. Zero means every figure was read correctly. */
    val mismatches: Int,
) {
    val reconciles: Boolean get() = mismatches == 0

    /**
     * The statement's own span: the days the bank posted these, which is what its period covers and
     * what it is ordered by. The card dates run earlier - a purchase made on the last days of one
     * month is posted in the next - so they are not what the statement is "from and to".
     */
    val postingFirst: LocalDate get() = rows.minOf { it.orderDate }
    val postingLast: LocalDate get() = rows.maxOf { it.orderDate }
    val totalDebits: Double get() = rows.sumOf { it.debit }
    val totalCredits: Double get() = rows.sumOf { it.credit }
}

sealed interface ParseResult {
    data class Success(val statement: ParsedStatement) : ParseResult
    /** [detail] is shown to the user, so it says what was wrong rather than naming an exception. */
    data class Failure(val detail: String) : ParseResult
}

/**
 * Turns the characters of a bank statement back into transactions.
 *
 * A PDF stores "draw this character at x, y" and nothing else: no table, no columns, no rows. Text
 * extraction alone gives the dates, the amounts and the descriptions as three detached runs, in
 * whatever order the file happens to hold them. So this works from the coordinates instead - group
 * characters into words, words into lines, and discover where the amount columns are by where the
 * numbers line up.
 *
 * Nothing here is specific to one bank. Which column is money out, which is money in and which is
 * the running balance is not assumed, and neither is whether the file writes "1,234.56" or
 * "1.234,56". Every combination is tried, and the one kept is the one where each row's balance
 * equals the row before it, minus what went out, plus what came in - all the way down the statement.
 * A wrong reading cannot satisfy that for a hundred rows in a row, so the check does not just
 * validate the parse at the end: it is what configures it.
 */
object StatementParser {

    private val DATE = Regex("""^\d{2}\.\d{2}\.\d{4}$""")
    private val NUMBER_DOT_DECIMAL = Regex("""^-?\d{1,3}(,\d{3})*\.\d{2}$""")
    private val NUMBER_COMMA_DECIMAL = Regex("""^-?\d{1,3}(\.\d{3})*,\d{2}$""")
    private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    /** Characters whose tops differ by less than this are on the same line. Well under any line spacing. */
    private const val LINE_TOLERANCE = 2.5f

    /** A horizontal gap wider than this ends a word. Letters within a word sit all but touching. */
    private const val WORD_GAP = 2.0f

    /** Right edges within this of each other belong to the same column - the columns are right-aligned. */
    private const val COLUMN_TOLERANCE = 5.0f

    /** A balance that is out by less than half a cent is rounding, not a misread. */
    private const val CENT = 0.005

    fun parse(glyphs: List<Glyph>): ParseResult {
        if (glyphs.isEmpty()) {
            return ParseResult.Failure(
                "There is no text in this PDF - it is likely a scan or a photo of a statement, " +
                    "which would need character recognition to read.",
            )
        }

        val lines = glyphs.groupBy { it.page }
            .toSortedMap()
            .flatMap { (page, pageGlyphs) -> linesOf(pageGlyphs, page) }

        val transactionLines = lines.filter { it.startsTransaction }
        if (transactionLines.isEmpty()) {
            return ParseResult.Failure("No transactions found - this does not look like an account statement.")
        }

        val columns = amountColumns(transactionLines)
        if (columns.size != 3) {
            return ParseResult.Failure(
                "Expected three amount columns (out, in, balance) but found ${columns.size}. " +
                    "This statement's layout is not one the app can read yet.",
            )
        }

        // Try every reading of the three columns under both number formats, and keep the one whose
        // running balance holds. Only one can, so this identifies the layout rather than guessing it.
        var best: ParsedStatement? = null
        for (decimalComma in listOf(false, true)) {
            for (roles in COLUMN_ROLES) {
                val candidate = build(lines, transactionLines, columns, roles, decimalComma) ?: continue
                if (candidate.reconciles) return ParseResult.Success(candidate)
                if (best == null || candidate.mismatches < best.mismatches) best = candidate
            }
        }

        return best?.let { ParseResult.Success(it) }
            ?: ParseResult.Failure("The amounts in this statement could not be read.")
    }

    /** (out, in, balance) as indexes into the three columns, left to right. */
    private val COLUMN_ROLES = listOf(
        Triple(0, 1, 2), Triple(1, 0, 2),
        Triple(0, 2, 1), Triple(2, 0, 1),
        Triple(1, 2, 0), Triple(2, 1, 0),
    )

    private data class Word(val text: String, val x0: Float, val x1: Float, val top: Float, val page: Int)

    /**
     * [startsTransaction] is decided as the line is built: a date at the far left with amounts after
     * it opens a transaction, anything else is a heading, a footer, or a description that wrapped.
     */
    private class Line(
        val words: List<Word>,
        val page: Int,
        val top: Float,
        val startsTransaction: Boolean,
    )

    private fun startsTransaction(words: List<Word>): Boolean {
        val first = words.firstOrNull() ?: return false
        return DATE.matches(first.text) && first.x0 < 60f && words.count { isNumber(it.text) } >= 3
    }

    /**
     * Characters into words, words into lines. Grouping is against each line's first character
     * rather than the previous one, so a page of slightly drifting baselines can't chain together
     * into one line.
     */
    private fun linesOf(pageGlyphs: List<Glyph>, page: Int): List<Line> {
        val byTop = pageGlyphs.sortedBy { it.top }
        val rows = mutableListOf<MutableList<Glyph>>()
        for (glyph in byTop) {
            val row = rows.lastOrNull()
            if (row != null && abs(glyph.top - row.first().top) <= LINE_TOLERANCE) row.add(glyph)
            else rows.add(mutableListOf(glyph))
        }
        return rows.map { row ->
            val words = mutableListOf<Word>()
            var current: Word? = null
            for (glyph in row.sortedBy { it.x0 }) {
                if (glyph.text.isBlank()) {
                    current?.let { words.add(it) }
                    current = null
                    continue
                }
                val open = current
                current = if (open != null && glyph.x0 - open.x1 <= WORD_GAP) {
                    open.copy(text = open.text + glyph.text, x1 = glyph.x1)
                } else {
                    open?.let { words.add(it) }
                    Word(glyph.text, glyph.x0, glyph.x1, glyph.top, page)
                }
            }
            current?.let { words.add(it) }
            Line(words, page, row.first().top, startsTransaction(words))
        }
    }

    /**
     * Where the amount columns are, found by where the numbers end: the columns are right-aligned,
     * so their right edges stack up. A cluster has to appear on at least half the transaction lines
     * to count, which leaves out a stray number that happens to sit inside a description.
     */
    private fun amountColumns(transactionLines: List<Line>): List<Float> {
        val edges = transactionLines.flatMap { line -> line.words.filter { isNumber(it.text) }.map { it.x1 } }.sorted()
        val clusters = mutableListOf<MutableList<Float>>()
        for (edge in edges) {
            val cluster = clusters.lastOrNull()
            if (cluster != null && edge - cluster.last() <= COLUMN_TOLERANCE) cluster.add(edge)
            else clusters.add(mutableListOf(edge))
        }
        return clusters.filter { it.size >= transactionLines.size / 2 }.map { it.average().toFloat() }
    }

    private fun build(
        allLines: List<Line>,
        transactionLines: List<Line>,
        columns: List<Float>,
        roles: Triple<Int, Int, Int>,
        decimalComma: Boolean,
    ): ParsedStatement? {
        val (debitCol, creditCol, balanceCol) = roles
        val descriptionStart = columns.max() + 5f

        val rows = mutableListOf<StatementRow>()
        val rowLines = mutableListOf<Line>()
        for (line in transactionLines) {
            val amounts = arrayOfNulls<Double>(3)
            for (word in line.words) {
                if (!isNumber(word.text)) continue
                val value = toAmount(word.text, decimalComma) ?: return null
                val nearest = columns.indices.minByOrNull { abs(word.x1 - columns[it]) } ?: continue
                amounts[nearest] = value
            }
            val balance = amounts[balanceCol] ?: return null
            val dates = line.words.filter { DATE.matches(it.text) }.take(2)
            val orderDate = dates.firstOrNull()?.let { runCatching { LocalDate.parse(it.text, DATE_FORMAT) }.getOrNull() }
                ?: return null
            val transactionDate = dates.getOrNull(1)
                ?.let { runCatching { LocalDate.parse(it.text, DATE_FORMAT) }.getOrNull() }
                ?: orderDate
            rows.add(
                StatementRow(
                    orderDate = orderDate,
                    transactionDate = transactionDate,
                    debit = amounts[debitCol] ?: 0.0,
                    credit = amounts[creditCol] ?: 0.0,
                    balance = balance,
                    description = line.words.filter { it.x0 > descriptionStart }.joinToString(" ") { it.text },
                ),
            )
            rowLines.add(line)
        }
        if (rows.isEmpty()) return null

        // A line with no date that starts in the description column is a description that wrapped;
        // it belongs to the transaction above it, not to one of its own.
        val descriptions = rows.map { StringBuilder(it.description) }
        for (line in allLines) {
            if (line.startsTransaction) continue
            val first = line.words.firstOrNull() ?: continue
            if (first.x0 <= descriptionStart) continue
            val ownerIndex = rowLines.indexOfLast { it.page == line.page && it.top < line.top }
            if (ownerIndex >= 0) descriptions[ownerIndex].append(' ').append(line.words.joinToString(" ") { it.text })
        }

        val withDescriptions = rows.mapIndexed { index, row ->
            row.copy(description = descriptions[index].toString().trim())
        }

        val first = withDescriptions.first()
        val opening = first.balance + first.debit - first.credit
        var running = opening
        var mismatches = 0
        for (row in withDescriptions) {
            val expected = running - row.debit + row.credit
            if (abs(expected - row.balance) > CENT) mismatches++
            running = row.balance
        }
        return ParsedStatement(
            rows = withDescriptions,
            opening = opening,
            closing = withDescriptions.last().balance,
            mismatches = mismatches,
        )
    }

    private fun isNumber(text: String) =
        NUMBER_DOT_DECIMAL.matches(text) || NUMBER_COMMA_DECIMAL.matches(text)

    private fun toAmount(text: String, decimalComma: Boolean): Double? =
        if (decimalComma) text.replace(".", "").replace(',', '.').toDoubleOrNull()
        else text.replace(",", "").toDoubleOrNull()

    /** Rounds to whole cents, so summing a column of parsed amounts can't drift in binary floating point. */
    fun round2(value: Double): Double = (value * 100).roundToInt() / 100.0
}
