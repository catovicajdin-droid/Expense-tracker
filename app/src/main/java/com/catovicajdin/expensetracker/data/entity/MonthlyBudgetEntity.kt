package com.catovicajdin.expensetracker.data.entity

import androidx.room.Entity

@Entity(tableName = "monthly_budgets", primaryKeys = ["yearMonth"])
data class MonthlyBudgetEntity(
    val yearMonth: String,
    val totalBudget: Double,
)
