package com.catovicajdin.expensetracker.notifications

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.catovicajdin.expensetracker.data.AppDatabase
import com.catovicajdin.expensetracker.data.NotificationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Reads every supported bank's pushes, not just Intesa's. The name stays because notification access
 * is granted to this exact component - renaming it would silently revoke access on existing installs.
 */
class IntesaNotificationListenerService : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var repository: NotificationRepository

    override fun onCreate() {
        super.onCreate()
        repository = NotificationRepository(AppDatabase.get(applicationContext))
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val bank = Bank.fromPackage(sbn.packageName) ?: return
        if (TrackingPreferences.mode(applicationContext) == TrackingMode.MANUAL) return
        if (bank !in BankPreferences.enabled(applicationContext)) return

        // An app posting several notifications also posts a summary standing for the group, and it
        // usually repeats one of their bodies - which would read here as a second transaction
        // arriving milliseconds after the real one.
        if (sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return

        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
        val body = (bigText ?: text).orEmpty()
        if (title.isEmpty() && body.isEmpty()) return

        val postedAt = sbn.postTime

        scope.launch {
            val transactionId = repository.ingest(sbn.packageName, title, body, postedAt)
            if (transactionId != null) {
                CategorizeNotifier.notify(applicationContext, transactionId)
                BudgetAlerts.checkOverall(applicationContext)
            }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
