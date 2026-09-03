package com.catovicajdin.expensetracker.data.entity

import androidx.room.Entity

@Entity(tableName = "category_budgets", primaryKeys = ["yearMonth", "categoryId"])
data class CategoryBudgetEntity(
    val yearMonth: String,
    val categoryId: Long,
    val amount: Double,
)
