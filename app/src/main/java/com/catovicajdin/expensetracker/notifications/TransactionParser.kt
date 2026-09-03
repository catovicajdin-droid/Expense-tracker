package com.catovicajdin.expensetracker.notifications

import com.catovicajdin.expensetracker.Constants

sealed class ParseOutcome {
    data class Success(val transaction: ParsedTransaction) : ParseOutcome()
    data class Failure(val reason: String) : ParseOutcome()
}

data class ParsedTransaction(
    val amount: Double,
    val currency: String,
    val availableBalance: Double,
    val balanceCurrency: String,
    val availableLimit: Double,
    val limitCurrency: String,
)

/**
 * Parses the "Nova transakcija" push body, e.g.:
 * "Evidentiran je odliv u iznosu 4,10 BAM - Kartična transakcija. Raspoloživo stanje je: 620,70 BAM,
 *  raspoloživo rate: 1.075,86 BAM."
 *
 * Deliberately strict (anchored, exact wording) so that any change on the bank's side falls through
 * to Failure rather than silently mis-extracting a number. Callers must keep the raw text regardless
 * of the outcome here - this function only classifies it.
 */
object TransactionParser {

    private val CARD_TRANSACTION_REGEX = Regex(
        """^Evidentiran je odliv u iznosu ([\d.]+,\d{2}) (\p{L}{3})\s*-\s*Kartična transakcija\.\s*""" +
            """Raspoloživo stanje je:\s*([\d.]+,\d{2}) (\p{L}{3}),\s*""" +
            """raspoloživo rate:\s*([\d.]+,\d{2}) (\p{L}{3})\.$"""
    )

    fun parse(title: String, body: String): ParseOutcome {
        if (title.trim() != Constants.NOTIFICATION_TITLE_TRANSACTION) {
            return ParseOutcome.Failure("Unrecognized title: '$title'")
        }

        val match = CARD_TRANSACTION_REGEX.find(body.trim())
            ?: return ParseOutcome.Failure("Body did not match the expected 'Nova transakcija' pattern")

        val groups = match.groupValues
        val amountRaw = groups[1]
        val amountCcy = groups[2]
        val balanceRaw = groups[3]
        val balanceCcy = groups[4]
        val limitRaw = groups[5]
        val limitCcy = groups[6]

        val amount = parseAmount(amountRaw)
            ?: return ParseOutcome.Failure("Could not parse amount: '$amountRaw'")
        val balance = parseAmount(balanceRaw)
            ?: return ParseOutcome.Failure("Could not parse balance: '$balanceRaw'")
        val limit = parseAmount(limitRaw)
            ?: return ParseOutcome.Failure("Could not parse limit: '$limitRaw'")

        if (amount <= 0.0) {
            return ParseOutcome.Failure("Non-positive amount: $amount")
        }
        if (amountCcy != "BAM" || balanceCcy != "BAM" || limitCcy != "BAM") {
            return ParseOutcome.Failure("Unexpected currency (expected BAM): $amountCcy/$balanceCcy/$limitCcy")
        }

        return ParseOutcome.Success(
            ParsedTransaction(
                amount = amount,
                currency = amountCcy,
                availableBalance = balance,
                balanceCurrency = balanceCcy,
                availableLimit = limit,
                limitCurrency = limitCcy,
            )
        )
    }

    /** "1.075,86" (bs/hr/sr formatting: '.' thousands separator, ',' decimal separator) -> 1075.86 */
    private fun parseAmount(raw: String): Double? =
        raw.replace(".", "").replace(",", ".").toDoubleOrNull()
}
