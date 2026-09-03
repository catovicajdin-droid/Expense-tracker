package com.catovicajdin.expensetracker.notifications

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.catovicajdin.expensetracker.Constants
import com.catovicajdin.expensetracker.data.AppDatabase
import com.catovicajdin.expensetracker.data.NotificationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class IntesaNotificationListenerService : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var repository: NotificationRepository

    override fun onCreate() {
        super.onCreate()
        repository = NotificationRepository(AppDatabase.get(applicationContext))
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName != Constants.INTESA_PACKAGE_NAME) return

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
            }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
