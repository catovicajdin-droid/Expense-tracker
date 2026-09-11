package com.catovicajdin.expensetracker.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val isQuickPick: Boolean = false,
    val sortOrder: Int = 0,
    val colorHex: String = DEFAULT_COLOR,
    /**
     * Stored per row rather than looked up from CategoryIcons by name: once categories can be
     * created and renamed in-app, a name-keyed map has no answer for them.
     */
    val icon: String = DEFAULT_ICON,
) {
    companion object {
        const val DEFAULT_ICON = "🏷️"
        const val DEFAULT_COLOR = "#607D8B"
    }
}
