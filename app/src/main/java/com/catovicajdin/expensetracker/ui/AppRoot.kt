package com.catovicajdin.expensetracker.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
        Surface(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
            if (!isNotificationAccessGranted) {
                OnboardingScreen(onRequestNotificationAccess = onRequestNotificationAccess)
                return@Surface
            }

            // Shared across Home's tag taps, Budget's category taps, and the Filters screen itself -
            // set explicitly by whichever action means "show me this slice of the ledger."
            var filter by remember { mutableStateOf(TransactionFilter()) }

            when (screen) {
                is Screen.Home -> HomeScreen(
                    onOpenLedger = { onNavigate(Screen.Ledger) },
                    onOpenBudget = { onNavigate(Screen.BudgetDashboard) },
                    onOpenAdd = { onNavigate(Screen.AddTransaction()) },
                    onOpenReview = { onNavigate(Screen.NeedsReview) },
                    onOpenDetail = { id -> onNavigate(Screen.Detail(id)) },
                )
                is Screen.Ledger -> LedgerListScreen(
                    filter = filter,
                    onBack = { onNavigate(Screen.Home) },
                    onOpenFilters = { onNavigate(Screen.Filters) },
                    onOpenDetail = { id -> onNavigate(Screen.Detail(id)) },
                )
                is Screen.Filters -> FiltersScreen(
                    filter = filter,
                    onFilterChange = { filter = it },
                    onBack = { onNavigate(Screen.Ledger) },
                    onShowResults = { onNavigate(Screen.Ledger) },
                )
                is Screen.BudgetDashboard -> BudgetDashboardScreen(
                    onBack = { onNavigate(Screen.Home) },
                    onOpenCategoryLedger = { categoryId ->
                        filter = TransactionFilter(categoryIds = setOf(categoryId))
                        onNavigate(Screen.Ledger)
                    },
                    onOpenLedger = { onNavigate(Screen.Ledger) },
                    onOpenAdd = { onNavigate(Screen.AddTransaction()) },
                    onOpenSettings = { onNavigate(Screen.BudgetSettings) },
                )
                is Screen.BudgetSettings -> BudgetSettingsScreen(
                    onBack = { onNavigate(Screen.BudgetDashboard) },
                )
                is Screen.NeedsReview -> NeedsReviewScreen(
                    onBack = { onNavigate(Screen.Home) },
                )
                is Screen.AddTransaction -> AddTransactionScreen(
                    prefillCategoryId = screen.prefillCategoryId,
                    prefillPostedAt = screen.prefillPostedAt,
                    resolvingRawId = screen.resolvingRawId,
                    onCancel = { onNavigate(if (screen.resolvingRawId != null) Screen.NeedsReview else Screen.Home) },
                    onSaved = { onNavigate(if (screen.resolvingRawId != null) Screen.NeedsReview else Screen.Home) },
                )
                is Screen.Detail -> TransactionDetailScreen(
                    transactionId = screen.transactionId,
                    onBack = { onNavigate(Screen.Ledger) },
                    onDeleted = { onNavigate(Screen.Ledger) },
                )
                is Screen.Categorize -> CategorizeScreen(
                    transactionId = screen.transactionId,
                    onDone = { onNavigate(Screen.Home) },
                )
            }
        }
    }
}
