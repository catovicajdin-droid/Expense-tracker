package com.catovicajdin.expensetracker

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.catovicajdin.expensetracker.ui.AppRoot
import com.catovicajdin.expensetracker.ui.Screen

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* nothing to do either way */ }

    /**
     * Notification-access is granted from a system Settings screen outside the app, so it can only
     * be observed by re-checking on resume - a one-time check in onCreate would never notice the
     * user coming back with it granted.
     */
    private val notificationAccessGranted = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        val startTransactionId = intent.takeIf { it.action == ACTION_CATEGORIZE }
            ?.getLongExtra(EXTRA_TRANSACTION_ID, -1L)
            ?.takeIf { it >= 0 }

        setContent {
            var screen by remember {
                mutableStateOf(
                    if (startTransactionId != null) Screen.Categorize(startTransactionId)
                    else Screen.TransactionList
                )
            }
            val hasNotificationAccess by notificationAccessGranted
            AppRoot(
                screen = screen,
                onNavigate = { screen = it },
                isNotificationAccessGranted = hasNotificationAccess,
                onRequestNotificationAccess = { openNotificationAccessSettings() },
            )
        }
    }

    override fun onResume() {
        super.onResume()
        notificationAccessGranted.value = isNotificationAccessGranted()
    }

    private fun isNotificationAccessGranted(): Boolean {
        val enabled = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        return enabled?.contains(packageName) == true
    }

    private fun openNotificationAccessSettings() {
        startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
    }

    companion object {
        const val ACTION_CATEGORIZE = "com.catovicajdin.expensetracker.action.CATEGORIZE"
        const val EXTRA_TRANSACTION_ID = "extra_transaction_id"
    }
}
