package com.catovicajdin.expensetracker.data

/**
 * A 14-color categorical palette validated against the CVD/lightness/contrast checks documented
 * in the dataviz method (adjacent-pair mode, dark surface) - lightness band and chroma floor both
 * pass; only one adjacent pair sits in the CVD-warn band rather than a hard fail, which is
 * acceptable there because every category is always shown with a text label and a letter avatar,
 * never color alone.
 */
object CategoryColors {
    val byName: Map<String, String> = mapOf(
        "Phone Bill" to "#b31b1b",
        "Misc" to "#9e6618",
        "Food ordering" to "#838d15",
        "Subscriptions" to "#2c7b12",
        "Padel" to "#16924a",
        "Groceries" to "#19a499",
        "Coffee" to "#2a94df",
        "Gas Bill" to "#385ae1",
        "Parents" to "#5f45e3",
        "Donating" to "#8621de",
        "Bills" to "#c221db",
        "Date nights" to "#cf1fac",
        "Pets" to "#c51d71",
        "DM" to "#ba1c3b",
    )
}
