package com.catovicajdin.expensetracker.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.catovicajdin.expensetracker.Constants
import com.catovicajdin.expensetracker.data.AppDatabase
import com.catovicajdin.expensetracker.data.NotificationRepository
import com.catovicajdin.expensetracker.notifications.CategorizeNotifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Debug-only stand-in for a bank push, so the notification flow can be tested without spending
 * money. Two modes:
 *
 *   adb shell am broadcast -a com.catovicajdin.expensetracker.debug.FAKE_TRANSACTION \
 *       -p com.catovicajdin.expensetracker [--es amount 12,50]
 *     Runs a fake "Nova transakcija" through the same ingest path the listener uses, so it creates
 *     a real transaction row (delete it afterwards) and posts the categorize notification.
 *
 *   ... --el transaction_id 42
 *     Re-posts the categorize notification for an existing transaction; nothing is written.
 */
class FakeTransactionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        val existingId = intent.getLongExtra(EXTRA_TRANSACTION_ID, -1L)
        val amount = intent.getStringExtra(EXTRA_AMOUNT) ?: "4,10"
        val pending = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (existingId >= 0) {
                    CategorizeNotifier.notify(app, existingId)
                } else {
                    // Dedup is by text over a week, so the time of day keeps each broadcast distinct.
                    val stamp = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
                    val body = "Evidentiran je odliv u iznosu $amount BAM - Test transakcija $stamp. " +
                        "Raspoloživo stanje je: 620,70 BAM, raspoloživo rate: 1.075,86 BAM."
                    val id = NotificationRepository(AppDatabase.get(app)).ingest(
                        Constants.INTESA_PACKAGE_NAME,
                        Constants.NOTIFICATION_TITLE_TRANSACTION,
                        body,
                        System.currentTimeMillis(),
                    )
                    if (id != null) CategorizeNotifier.notify(app, id)
                }
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        const val EXTRA_TRANSACTION_ID = "transaction_id"
        const val EXTRA_AMOUNT = "amount"
    }
}
