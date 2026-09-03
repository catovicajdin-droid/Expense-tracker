package com.catovicajdin.expensetracker.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.catovicajdin.expensetracker.Constants
import com.catovicajdin.expensetracker.R
import com.catovicajdin.expensetracker.data.AppDatabase
import com.catovicajdin.expensetracker.data.MonthRange
import com.catovicajdin.expensetracker.data.entity.BudgetAlertEntity
import kotlinx.coroutines.flow.first

/** Fires a one-time notification per month when overall or per-category spend crosses 50%/75% of its budget. */
object BudgetAlerts {

    private val THRESHOLDS = listOf(50, 75)

    private fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                Constants.CHANNEL_ID_BUDGET_ALERT,
                "Budget alerts",
                NotificationManager.IMPORTANCE_DEFAULT,
            )
        )
    }

    suspend fun checkOverall(context: Context) {
        val db = AppDatabase.get(context)
        val yearMonth = MonthRange.current()
        val budget = db.budgetDao().monthlyBudgetFlow(yearMonth).first() ?: return
        if (budget.totalBudget <= 0.0) return

        val (from, to) = MonthRange.millisRange(yearMonth)
        val spent = db.transactionDao().totalSpent(from, to).first()
        val percent = (spent / budget.totalBudget * 100).toInt()

        checkThresholds(context, yearMonth, BudgetAlertEntity.OVERALL_CATEGORY_ID, "your overall budget", percent)
    }

    suspend fun checkCategory(context: Context, categoryId: Long) {
        val db = AppDatabase.get(context)
        val yearMonth = MonthRange.current()
        val budget = db.budgetDao().categoryBudgetsFlow(yearMonth).first().find { it.categoryId == categoryId }
            ?: return
        if (budget.amount <= 0.0) return

        val (from, to) = MonthRange.millisRange(yearMonth)
        val spent = db.transactionDao().categoryTotals(from, to).first()
            .find { it.categoryId == categoryId }?.total ?: 0.0
        val percent = (spent / budget.amount * 100).toInt()

        val categoryName = db.categoryDao().byId(categoryId)?.name ?: "this category"
        checkThresholds(context, yearMonth, categoryId, categoryName, percent)
    }

    private suspend fun checkThresholds(context: Context, yearMonth: String, scopeId: Long, label: String, percent: Int) {
        val db = AppDatabase.get(context)
        THRESHOLDS.forEach { threshold ->
            if (percent >= threshold) {
                val inserted = db.budgetDao().recordAlert(BudgetAlertEntity(yearMonth, scopeId, threshold))
                if (inserted != -1L) {
                    notify(context, label, threshold, scopeId)
                }
            }
        }
    }

    private fun notify(context: Context, label: String, threshold: Int, scopeId: Long) {
        ensureChannel(context)
        val builder = NotificationCompat.Builder(context, Constants.CHANNEL_ID_BUDGET_ALERT)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("$threshold% of budget reached")
            .setContentText("You've used $threshold% of $label this month.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
        val notificationId = "budget_${scopeId}_$threshold".hashCode()
        context.getSystemService(NotificationManager::class.java).notify(notificationId, builder.build())
    }
}
