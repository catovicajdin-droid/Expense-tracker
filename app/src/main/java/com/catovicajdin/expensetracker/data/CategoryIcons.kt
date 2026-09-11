package com.catovicajdin.expensetracker.data

/**
 * The starting glyph for each seeded category. Only the seeder and MIGRATION_4_5 read this now -
 * every category carries its own `icon` column once created, so nothing looks an icon up by name
 * at display time.
 */
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
}
