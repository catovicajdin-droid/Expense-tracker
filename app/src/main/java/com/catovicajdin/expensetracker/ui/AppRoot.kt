package com.catovicajdin.expensetracker.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.catovicajdin.expensetracker.notifications.TrackingMode
import com.catovicajdin.expensetracker.notifications.TrackingPreferences
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
            val context = LocalContext.current
            var welcomed by remember { mutableStateOf(TrackingPreferences.isDone(context)) }
            if (!welcomed) {
                WelcomeFlow(
                    isNotificationAccessGranted = isNotificationAccessGranted,
                    onRequestNotificationAccess = onRequestNotificationAccess,
                    onFinished = { welcomed = true },
                )
                return@Surface
            }

            // Re-read on every navigation, so switching to manual in Settings applies on the way out.
            var mode by remember(screen) { mutableStateOf(TrackingPreferences.mode(context)) }
            if (mode == TrackingMode.AUTOMATIC && !isNotificationAccessGranted) {
                OnboardingScreen(
                    onRequestNotificationAccess = onRequestNotificationAccess,
                    onTrackManually = {
                        TrackingPreferences.setMode(context, TrackingMode.MANUAL)
                        mode = TrackingMode.MANUAL
                    },
                )
                return@Surface
            }

            // Shared across Home's tag taps, Budget's category taps, and the Filters screen itself -
            // set explicitly by whichever action means "show me this slice of the ledger."
            var filter by remember { mutableStateOf(TransactionFilter()) }

            // Filters only live while the ledger (and the screens opened from it) is showing;
            // stepping anywhere else clears them so the next visit starts unfiltered. Keyed on the
            // destination, so a budget category tap - which sets a filter and then opens the
            // ledger - keeps its filter.
            LaunchedEffect(screen) {
                if (screen !is Screen.Ledger && screen !is Screen.Filters && screen !is Screen.Detail) {
                    filter = TransactionFilter()
                }
            }

            when (screen) {
                is Screen.Home -> HomeScreen(
                    onOpenLedger = { onNavigate(Screen.Ledger) },
                    onOpenBudget = { onNavigate(Screen.BudgetDashboard) },
                    onOpenAdd = { onNavigate(Screen.AddTransaction()) },
                    onOpenReview = { onNavigate(Screen.NeedsReview) },
                    onOpenDetail = { id -> onNavigate(Screen.Detail(id)) },
                    onOpenSettings = { onNavigate(Screen.Settings) },
                )
                is Screen.Ledger -> LedgerListScreen(
                    filter = filter,
                    onFilterChange = { filter = it },
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
                is Screen.Settings -> SettingsScreen(
                    onBack = { onNavigate(Screen.Home) },
                    onOpenCategories = { onNavigate(Screen.Categories) },
                    onOpenDuplicates = { onNavigate(Screen.Duplicates) },
                    onOpenImport = { onNavigate(Screen.ImportStatement) },
                    onOpenImportHistory = { onNavigate(Screen.ImportHistory) },
                )
                is Screen.Categories -> CategoriesScreen(
                    onBack = { onNavigate(Screen.Settings) },
                )
                is Screen.Duplicates -> DuplicatesScreen(
                    onBack = { onNavigate(Screen.Settings) },
                )
                is Screen.ImportHistory -> ImportHistoryScreen(
                    onBack = { onNavigate(Screen.Settings) },
                )
                is Screen.ImportStatement -> ImportStatementScreen(
                    onBack = { onNavigate(Screen.Settings) },
                    onImported = { onNavigate(Screen.BudgetDashboard) },
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
