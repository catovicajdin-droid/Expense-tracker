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

    /**
     * Held here rather than remembered inside setContent so a notification tap can redirect it:
     * with the app already running, singleTop delivers that tap to onNewIntent on this same
     * instance, and onCreate (where the cold-start case is read) never runs again.
     */
    private val screen = mutableStateOf<Screen>(Screen.Home)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        categorizeTarget(intent)?.let { screen.value = it }

        setContent {
            val currentScreen by screen
            val hasNotificationAccess by notificationAccessGranted
            AppRoot(
                screen = currentScreen,
                onNavigate = { screen.value = it },
                isNotificationAccessGranted = hasNotificationAccess,
                onRequestNotificationAccess = { openNotificationAccessSettings() },
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        categorizeTarget(intent)?.let { screen.value = it }
    }

    private fun categorizeTarget(intent: Intent?): Screen.Categorize? =
        intent?.takeIf { it.action == ACTION_CATEGORIZE }
            ?.getLongExtra(EXTRA_TRANSACTION_ID, -1L)
            ?.takeIf { it >= 0 }
            ?.let { Screen.Categorize(it) }

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
