package com.catovicajdin.expensetracker.notifications

sealed class ParseOutcome {
    data class Success(val transaction: ParsedTransaction) : ParseOutcome()
    data class Failure(val reason: String) : ParseOutcome()
}

/** Balance and limit are only filled by banks whose push actually reports them (Intesa). */
data class ParsedTransaction(
    val amount: Double,
    val currency: String,
    val availableBalance: Double? = null,
    val balanceCurrency: String? = null,
    val availableLimit: Double? = null,
    val limitCurrency: String? = null,
)

/**
 * One bank's push format. Each implementation is deliberately strict (anchored, exact wording) so a
 * change on the bank's side falls through to Failure rather than silently mis-extracting a number.
 */
interface BankParser {
    /**
     * Whether this title announces a debit. A body that then fails to parse goes to needs-review;
     * any other title (logins, promotions, incoming payments) is simply ignored.
     */
    fun isTransactionTitle(title: String): Boolean

    fun parse(title: String, body: String): ParseOutcome
}

object TransactionParser {

    /** The fixed BAM/EUR peg - a foreign-currency debit lands in the account at about this rate. */
    private const val BAM_PER_EUR = 1.95583

    /**
     * Each bank's own phrasing for the transaction amount, used to seed the needs-review accept form:
     * Intesa "u iznosu 4,10 BAM", Raiffeisen "u iznosu od 30.00 BAM", UniCredit "transakciju 16.10 BAM".
     */
    private val AMOUNT_PHRASES: List<Pair<Regex, (String) -> Double?>> = listOf(
        Regex("""u iznosu ([\d.]+,\d{2})(?: (\p{L}{3}))?""") to ::parseCommaDecimal,
        Regex("""u iznosu od (\d+(?:,\d{3})*\.\d{2})(?: (\p{L}{3}))?""") to ::parseDotDecimal,
        Regex("""transakciju (\d+(?:,\d{3})*\.\d{2})(?: (\p{L}{3}))?""") to ::parseDotDecimal,
    )
    private val ANY_AMOUNT_REGEX = Regex("""[\d.]+,\d{2}""")

    /**
     * Best guess at the transaction amount in a body no parser could handle, to prefill the
     * needs-review accept form. Prefers a bank's amount phrasing and only falls back to the first
     * amount-shaped number, because a body typically carries several (the amount, the remaining
     * balance, the remaining instalment limit) and the first one isn't always the right one. A EUR
     * amount is converted at the peg, since the form takes BAM. A guess either way - the caller must
     * leave it editable.
     */
    fun suggestAmount(body: String): Double? {
        for ((regex, parseRaw) in AMOUNT_PHRASES) {
            val match = regex.find(body) ?: continue
            val amount = parseRaw(match.groupValues[1]) ?: continue
            val inBam = if (match.groupValues[2] == "EUR") amount * BAM_PER_EUR else amount
            return inBam.takeIf { it > 0.0 }
        }
        val raw = ANY_AMOUNT_REGEX.find(body)?.value ?: return null
        return parseCommaDecimal(raw)?.takeIf { it > 0.0 }
    }
}

/** "1.075,86" (bs/hr/sr formatting: '.' thousands separator, ',' decimal separator) -> 1075.86 */
internal fun parseCommaDecimal(raw: String): Double? =
    raw.replace(".", "").replace(",", ".").toDoubleOrNull()

/** "1,075.86" (English formatting, used by Raiffeisen and UniCredit pushes) -> 1075.86 */
internal fun parseDotDecimal(raw: String): Double? =
    raw.replace(",", "").toDoubleOrNull()
