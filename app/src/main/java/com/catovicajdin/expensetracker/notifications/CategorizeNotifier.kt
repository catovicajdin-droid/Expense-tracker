package com.catovicajdin.expensetracker.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.catovicajdin.expensetracker.Constants
import com.catovicajdin.expensetracker.MainActivity
import com.catovicajdin.expensetracker.R
import com.catovicajdin.expensetracker.data.AppDatabase

/** Builds the per-transaction notification: one-tap quick-category buttons, plus a "More" fallback. */
object CategorizeNotifier {

    private fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                Constants.CHANNEL_ID_CATEGORIZE,
                "Categorize transactions",
                NotificationManager.IMPORTANCE_HIGH,
            )
        )
    }

    suspend fun notify(context: Context, transactionId: Long) {
        ensureChannel(context)

        val quickPicks = AppDatabase.get(context).categoryDao().quickPicks(limit = 3)

        val builder = NotificationCompat.Builder(context, Constants.CHANNEL_ID_CATEGORIZE)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.categorize_notification_title))
            .setContentText(context.getString(R.string.categorize_notification_text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        quickPicks.forEach { category ->
            builder.addAction(0, category.name, categoryActionIntent(context, transactionId, category.id))
        }
        builder.addAction(0, context.getString(R.string.categorize_more), morePendingIntent(context, transactionId))

        context.getSystemService(NotificationManager::class.java).notify(transactionId.toInt(), builder.build())
    }

    private fun categoryActionIntent(context: Context, transactionId: Long, categoryId: Long): PendingIntent {
        val intent = Intent(context, CategoryActionReceiver::class.java).apply {
            putExtra(CategoryActionReceiver.EXTRA_TRANSACTION_ID, transactionId)
            putExtra(CategoryActionReceiver.EXTRA_CATEGORY_ID, categoryId)
        }
        return PendingIntent.getBroadcast(
            context,
            "${transactionId}_$categoryId".hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun morePendingIntent(context: Context, transactionId: Long): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = MainActivity.ACTION_CATEGORIZE
            putExtra(MainActivity.EXTRA_TRANSACTION_ID, transactionId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            transactionId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
