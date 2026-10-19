package com.catovicajdin.expensetracker.notifications

/**
 * Parses UniCredit BiH's (m-ba app) "Odobrena karticna transakcija" push body, e.g.:
 * "Imate transakciju 16.10 BAM na HOLDINA D.O.O. SARAJEV       SARAJEVO koristeći svoju karticu
 *  5352********3434."
 *
 * The amount uses a '.' decimal separator and there is no balance to read. The merchant between
 * "na" and "koristeći" is free text padded with runs of spaces. The title is unaccented ("karticna")
 * while the body is accented ("koristeći"); both are matched either way, since a bank fixing one
 * spelling shouldn't send every transaction to review.
 *
 * Built from screenshots of the iOS lock screen; the Android push is expected to carry the same text.
 */
object UniCreditParser : BankParser {

    private val TITLE_REGEX = Regex("""^Odobrena karti[cč]na transakcija$""")

    private val CARD_TRANSACTION_REGEX = Regex(
        """^Imate transakciju (\d+(?:,\d{3})*\.\d{2}) (\p{L}{3}) na .+? koriste[cćč]i svoju karticu [\d*]+\.?$""",
        RegexOption.DOT_MATCHES_ALL,
    )

    override fun isTransactionTitle(title: String): Boolean = TITLE_REGEX.matches(title.trim())

    override fun parse(title: String, body: String): ParseOutcome {
        if (!isTransactionTitle(title)) {
            return ParseOutcome.Failure("Unrecognized title: '$title'")
        }

        val match = CARD_TRANSACTION_REGEX.find(body.trim())
            ?: return ParseOutcome.Failure("Body did not match the expected 'Odobrena karticna transakcija' pattern")

        val (amountRaw, currency) = match.destructured
        val amount = parseDotDecimal(amountRaw)
            ?: return ParseOutcome.Failure("Could not parse amount: '$amountRaw'")

        if (amount <= 0.0) {
            return ParseOutcome.Failure("Non-positive amount: $amount")
        }
        if (currency != "BAM") {
            return ParseOutcome.Failure("Unexpected currency (expected BAM): $currency")
        }

        return ParseOutcome.Success(ParsedTransaction(amount = amount, currency = currency))
    }
}
