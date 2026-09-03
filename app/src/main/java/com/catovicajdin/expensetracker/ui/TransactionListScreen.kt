package com.catovicajdin.expensetracker.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.catovicajdin.expensetracker.data.AppDatabase
import com.catovicajdin.expensetracker.data.NotificationRepository
import com.catovicajdin.expensetracker.data.entity.CategoryEntity
import com.catovicajdin.expensetracker.data.entity.TransactionEntity
import com.catovicajdin.expensetracker.notifications.BudgetAlerts
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun TransactionListScreen(onOpenNeedsReview: () -> Unit, onOpenBudget: () -> Unit) {
    val context = LocalContext.current
    val db = AppDatabase.get(context)
    val scope = rememberCoroutineScope()
    var showAddDialog by remember { mutableStateOf(false) }
    val categories by db.categoryDao().all().collectAsState(initial = emptyList())
    var filter by remember { mutableStateOf(TransactionFilter()) }
    val transactions by db.transactionDao().filtered(
        categoryId = filter.categoryId,
        fromMillis = filter.fromMillis,
        toMillis = filter.toMillis,
        minAmount = filter.minAmount,
        maxAmount = filter.maxAmount,
    ).collectAsState(initial = emptyList())
    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = onOpenNeedsReview) { Text("Needs review") }
            TextButton(onClick = onOpenBudget) { Text("Budget") }
            TextButton(onClick = { showAddDialog = true }) { Text("+ Add") }
        }
        FilterBar(categories = categories, filter = filter, onFilterChange = { filter = it })
        HorizontalDivider()
        LazyColumn {
            items(transactions) { transaction ->
                TransactionRow(
                    transaction = transaction,
                    category = categories.find { it.id == transaction.categoryId },
                    categories = categories,
                    dateFormat = dateFormat,
                    onReassign = { categoryId ->
                        scope.launch {
                            db.transactionDao().assignCategory(transaction.id, categoryId)
                            BudgetAlerts.checkCategory(context, categoryId)
                        }
                    },
                )
                HorizontalDivider()
            }
        }
    }

    if (showAddDialog) {
        AddTransactionDialog(
            categories = categories,
            onDismiss = { showAddDialog = false },
            onSave = { amount, categoryId ->
                scope.launch {
                    NotificationRepository(db).insertManual(amount, categoryId, System.currentTimeMillis())
                    if (categoryId != null) BudgetAlerts.checkCategory(context, categoryId)
                    BudgetAlerts.checkOverall(context)
                }
                showAddDialog = false
            },
        )
    }
}

@Composable
private fun TransactionRow(
    transaction: TransactionEntity,
    category: CategoryEntity?,
    categories: List<CategoryEntity>,
    dateFormat: SimpleDateFormat,
    onReassign: (Long) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box {
                Box(modifier = Modifier.clickable { expanded = true }) {
                    CategoryAvatar(category)
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    categories.forEach { candidate ->
                        DropdownMenuItem(
                            text = { Text(candidate.name) },
                            onClick = {
                                onReassign(candidate.id)
                                expanded = false
                            },
                        )
                    }
                }
            }
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(category?.name ?: "Uncategorized")
                Text(dateFormat.format(transaction.postedAt))
            }
        }
        Text("${transaction.amount} ${transaction.currency}")
    }
}
