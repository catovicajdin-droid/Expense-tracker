package com.catovicajdin.expensetracker.notifications

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.catovicajdin.expensetracker.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Handles a tap on one of the notification's quick-category buttons - assigns the category and dismisses. */
class CategoryActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val transactionId = intent.getLongExtra(EXTRA_TRANSACTION_ID, -1L)
        val categoryId = intent.getLongExtra(EXTRA_CATEGORY_ID, -1L)
        if (transactionId < 0 || categoryId < 0) return

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                AppDatabase.get(context.applicationContext).transactionDao()
                    .assignCategory(transactionId, categoryId)
                BudgetAlerts.checkCategory(context.applicationContext, categoryId)
                context.getSystemService(NotificationManager::class.java).cancel(transactionId.toInt())
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val EXTRA_TRANSACTION_ID = "extra_transaction_id"
        const val EXTRA_CATEGORY_ID = "extra_category_id"
    }
}
