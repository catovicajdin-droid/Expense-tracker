package com.catovicajdin.expensetracker.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val isQuickPick: Boolean = false,
    val sortOrder: Int = 0,
    val colorHex: String = "#607D8B",
)
