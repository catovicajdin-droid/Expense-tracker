package com.catovicajdin.expensetracker.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BankParsersTest {

    private fun success(outcome: ParseOutcome): ParsedTransaction {
        assertTrue("expected success, got $outcome", outcome is ParseOutcome.Success)
        return (outcome as ParseOutcome.Success).transaction
    }

    private fun failure(outcome: ParseOutcome): String {
        assertTrue("expected failure, got $outcome", outcome is ParseOutcome.Failure)
        return (outcome as ParseOutcome.Failure).reason
    }

    @Test
    fun intesa_cardTransaction() {
        val t = success(
            IntesaParser.parse(
                "Nova transakcija",
                "Evidentiran je odliv u iznosu 4,10 BAM - Kartična transakcija. Raspoloživo stanje je: 620,70 BAM, " +
                    "raspoloživo rate: 1.075,86 BAM.",
            )
        )
        assertEquals(4.10, t.amount, 0.001)
        assertEquals(620.70, t.availableBalance!!, 0.001)
        assertEquals(1075.86, t.availableLimit!!, 0.001)
    }

    @Test
    fun raiffeisen_atmWithdrawal() {
        val t = success(
            RaiffeisenParser.parse(
                "Odlazna transakcija",
                "Sa Vašeg računa 1613000120682023 izvršena je isplata u iznosu od 30.00 BAM. Opis:ATM BASCARSIJA >SARAJEVO BA",
            )
        )
        assertEquals(30.00, t.amount, 0.001)
        assertEquals("BAM", t.currency)
    }

    @Test
    fun raiffeisen_descriptionOnNextLine() {
        val t = success(
            RaiffeisenParser.parse(
                "Odlazna transakcija",
                "Sa Vašeg računa 1613000120682023 izvršena je isplata u iznosu od 100.00 BAM.\nOpis:KJKP RAD CIGLANE PAUK>SARAJEVO BA",
            )
        )
        assertEquals(100.00, t.amount, 0.001)
    }

    @Test
    fun raiffeisen_thousandsSeparator() {
        val t = success(
            RaiffeisenParser.parse(
                "Odlazna transakcija",
                "Sa Vašeg računa 1613000120682023 izvršena je isplata u iznosu od 1,250.40 BAM. Opis:NEKI TRGOVAC",
            )
        )
        assertEquals(1250.40, t.amount, 0.001)
    }

    @Test
    fun raiffeisen_euroGoesToReview() {
        val body = "Sa Vašeg računa 1613000120682023 izvršena je isplata u iznosu od 9.99 EUR. Opis:DISNEY PLUS>LONDON GB"
        assertTrue(failure(RaiffeisenParser.parse("Odlazna transakcija", body)).contains("EUR"))
        assertTrue(RaiffeisenParser.isTransactionTitle("Odlazna transakcija"))
        assertEquals(9.99 * 1.95583, TransactionParser.suggestAmount(body)!!, 0.001)
    }

    @Test
    fun raiffeisen_otherTitlesAreNotTransactions() {
        assertFalse(RaiffeisenParser.isTransactionTitle("Dolazna transakcija"))
    }

    @Test
    fun unicredit_cardTransaction() {
        val t = success(
            UniCreditParser.parse(
                "Odobrena karticna transakcija",
                "Imate transakciju 16.10 BAM na HOLDINA D.O.O. SARAJEV       SARAJEVO koristeći svoju karticu 5352********3434.",
            )
        )
        assertEquals(16.10, t.amount, 0.001)
        assertEquals("BAM", t.currency)
    }

    @Test
    fun unicredit_accentedTitle() {
        assertTrue(UniCreditParser.isTransactionTitle("Odobrena kartična transakcija"))
        assertFalse(UniCreditParser.isTransactionTitle("Odbijena karticna transakcija"))
    }

    @Test
    fun unicredit_unexpectedWordingGoesToReview() {
        val body = "Transakcija 16.10 BAM odobrena na HOLDINA D.O.O."
        failure(UniCreditParser.parse("Odobrena karticna transakcija", body))
        assertNotNull(TransactionParser.suggestAmount("Imate transakciju 16.10 BAM na X"))
    }

    @Test
    fun suggestAmount_intesaStillWorks() {
        assertEquals(4.10, TransactionParser.suggestAmount("Evidentiran je odliv u iznosu 4,10 BAM - nešto novo.")!!, 0.001)
    }
}
