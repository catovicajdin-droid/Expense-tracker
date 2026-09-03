package com.catovicajdin.expensetracker.data

/** Accepts both "100.05" and "100,05" - the comma-decimal keyboard is the norm for this locale. */
fun parseAmountInput(text: String): Double? = text.replace(',', '.').toDoubleOrNull()
