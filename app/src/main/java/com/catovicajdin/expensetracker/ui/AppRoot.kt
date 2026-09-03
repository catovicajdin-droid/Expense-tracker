package com.catovicajdin.expensetracker.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable

@Composable
fun AppRoot(
    screen: Screen,
    onNavigate: (Screen) -> Unit,
    isNotificationAccessGranted: Boolean,
    onRequestNotificationAccess: () -> Unit,
) {
    MaterialTheme {
        Surface {
            if (!isNotificationAccessGranted) {
                OnboardingScreen(onRequestNotificationAccess = onRequestNotificationAccess)
                return@Surface
            }
            when (screen) {
                is Screen.TransactionList -> TransactionListScreen(
                    onOpenNeedsReview = { onNavigate(Screen.NeedsReview) },
                )
                is Screen.NeedsReview -> NeedsReviewScreen(
                    onBack = { onNavigate(Screen.TransactionList) },
                )
                is Screen.Categorize -> CategorizeScreen(
                    transactionId = screen.transactionId,
                    onDone = { onNavigate(Screen.TransactionList) },
                )
            }
        }
    }
}
