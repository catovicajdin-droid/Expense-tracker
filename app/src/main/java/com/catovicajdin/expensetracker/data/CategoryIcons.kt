package com.catovicajdin.expensetracker.data

/** An emoji glyph per default category - paired with the color and name everywhere, never standing in for them alone. */
object CategoryIcons {
    val byName: Map<String, String> = mapOf(
        "Phone Bill" to "📱",
        "Misc" to "🗂️",
        "Food ordering" to "🍔",
        "Subscriptions" to "🔁",
        "Padel" to "🎾",
        "Groceries" to "🛒",
        "Coffee" to "☕",
        "Gas Bill" to "⛽",
        "Parents" to "👪",
        "Donating" to "🎁",
        "Bills" to "🧾",
        "Date nights" to "💕",
        "Pets" to "🐾",
        "DM" to "🧴",
    )

    private const val DEFAULT_ICON = "🏷️"

    fun iconFor(name: String): String = byName[name] ?: DEFAULT_ICON
}
