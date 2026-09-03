package com.catovicajdin.expensetracker.data.entity

import androidx.room.Entity

/**
 * Records that a threshold notification already fired, so it only fires once per month per scope.
 * categoryId uses OVERALL_CATEGORY_ID to represent the whole-month budget rather than one category.
 */
@Entity(tableName = "budget_alerts", primaryKeys = ["yearMonth", "categoryId", "threshold"])
data class BudgetAlertEntity(
    val yearMonth: String,
    val categoryId: Long,
    val threshold: Int,
) {
    companion object {
        const val OVERALL_CATEGORY_ID = -1L
    }
}
