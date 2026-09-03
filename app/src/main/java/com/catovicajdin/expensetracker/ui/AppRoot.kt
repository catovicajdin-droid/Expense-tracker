package com.catovicajdin.expensetracker.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.catovicajdin.expensetracker.ui.theme.ExpenseTrackerTheme

@Composable
fun AppRoot(
    screen: Screen,
    onNavigate: (Screen) -> Unit,
    isNotificationAccessGranted: Boolean,
    onRequestNotificationAccess: () -> Unit,
) {
    ExpenseTrackerTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
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
