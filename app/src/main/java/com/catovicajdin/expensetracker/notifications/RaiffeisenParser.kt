package com.catovicajdin.expensetracker.notifications

/**
 * Parses Raiffeisen BiH's (RMB app) "Odlazna transakcija" push body, e.g.:
 * "Sa Vašeg računa 1613000120682023 izvršena je isplata u iznosu od 30.00 BAM.
 *  Opis:ATM BASCARSIJA >SARAJEVO BA"
 *
 * Unlike Intesa the amount uses a '.' decimal separator, and there is no balance to read. The
 * description after "Opis:" is free text (merchant, ATM, city) and the shade truncates it, so only
 * its presence is loosely allowed - nothing after the amount is relied on.
 *
 * Built from screenshots of the iOS lock screen; the Android push is expected to carry the same text.
 */
object RaiffeisenParser : BankParser {

    private const val TITLE_OUTGOING = "Odlazna transakcija"

    private val OUTGOING_REGEX = Regex(
        """^Sa Vašeg računa \d+ izvršena je isplata u iznosu od (\d+(?:,\d{3})*\.\d{2}) (\p{L}{3})\.""" +
            """(?:\s*Opis:.*)?$""",
        RegexOption.DOT_MATCHES_ALL,
    )

    override fun isTransactionTitle(title: String): Boolean = title.trim() == TITLE_OUTGOING

    override fun parse(title: String, body: String): ParseOutcome {
        if (!isTransactionTitle(title)) {
            return ParseOutcome.Failure("Unrecognized title: '$title'")
        }

        val match = OUTGOING_REGEX.find(body.trim())
            ?: return ParseOutcome.Failure("Body did not match the expected 'Odlazna transakcija' pattern")

        val (amountRaw, currency) = match.destructured
        val amount = parseDotDecimal(amountRaw)
            ?: return ParseOutcome.Failure("Could not parse amount: '$amountRaw'")

        if (amount <= 0.0) {
            return ParseOutcome.Failure("Non-positive amount: $amount")
        }
        // A card payment abroad (Disney+ in EUR) reports the foreign amount, not what left the BAM
        // account - so it goes to review, where the form suggests the converted figure.
        if (currency != "BAM") {
            return ParseOutcome.Failure("Unexpected currency (expected BAM): $currency")
        }

        return ParseOutcome.Success(ParsedTransaction(amount = amount, currency = currency))
    }
}
