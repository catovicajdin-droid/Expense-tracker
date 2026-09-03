package com.catovicajdin.expensetracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.catovicajdin.expensetracker.data.AppDatabase
import com.catovicajdin.expensetracker.data.MonthRange
import com.catovicajdin.expensetracker.data.entity.TransactionEntity
import com.catovicajdin.expensetracker.ui.components.CategoryDot
import com.catovicajdin.expensetracker.ui.components.Divider2
import com.catovicajdin.expensetracker.ui.components.SectionLabel
import com.catovicajdin.expensetracker.ui.components.formatAmount
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun HomeScreen(
    onOpenLedger: () -> Unit,
    onOpenTagLedger: (Long) -> Unit,
    onOpenBudget: () -> Unit,
    onOpenAdd: () -> Unit,
    onOpenReview: () -> Unit,
    onOpenDetail: (Long) -> Unit,
) {
    val context = LocalContext.current
    val db = AppDatabase.get(context)

    val thisMonth = remember { MonthRange.current() }
    val lastMonth = remember { MonthRange.previous(thisMonth) }
    val thisRange = remember { MonthRange.millisRange(thisMonth) }
    val lastRange = remember { MonthRange.millisRange(lastMonth) }

    val categories by db.categoryDao().all().collectAsState(initial = emptyList())
    val spent by db.transactionDao().totalSpent(thisRange.first, thisRange.second).collectAsState(initial = 0.0)
    val lastSpent by db.transactionDao().totalSpent(lastRange.first, lastRange.second).collectAsState(initial = 0.0)
    val monthlyBudget by db.budgetDao().monthlyBudgetFlow(thisMonth).collectAsState(initial = null)
    val needsReviewRows by db.rawNotificationDao().needsReview().collectAsState(initial = emptyList())
    val tagTotals by db.tagDao().tagTotals(thisRange.first, thisRange.second).collectAsState(initial = emptyList())
    val recent by db.transactionDao().recent(8).collectAsState(initial = emptyList())
    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()) }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(20.dp, 20.dp, 20.dp, 16.dp)) {
            SectionLabel("Spent this month")
            Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 6.dp)) {
                Text(formatAmount(spent), style = MaterialTheme.typography.headlineLarge)
                Text(" BAM", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 4.dp))
            }
            Text(
                "${MonthRange.displayLabel(thisMonth)} · ${deltaText(spent, lastSpent)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Divider2()

        val budget = monthlyBudget?.totalBudget
        if (budget != null && budget > 0.0) {
            Column(modifier = Modifier.padding(20.dp, 16.dp, 20.dp, 16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween) {
                    SectionLabel("Budget ${formatAmount(budget)}")
                    Text(
                        "${formatAmount((budget - spent).coerceAtLeast(0.0))} left",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
                BudgetBar(fraction = (spent / budget).toFloat(), height = 14.dp, modifier = Modifier.padding(top = 10.dp))
                Text(
                    "${MonthRange.displayLabel(lastMonth)} ${formatAmount(lastSpent)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 10.dp),
                )
                BudgetBar(fraction = (lastSpent / budget).toFloat(), height = 8.dp, modifier = Modifier.padding(top = 6.dp))
            }
            Divider2()
        }

        if (needsReviewRows.isNotEmpty()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenReview() }
                    .background(MaterialTheme.colorScheme.secondary)
                    .padding(20.dp, 12.dp),
            ) {
                Text(
                    "NEEDS REVIEW · ${needsReviewRows.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondary,
                )
                Text("Open →", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSecondary)
            }
            Divider2()
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp, 14.dp, 20.dp, 8.dp),
        ) {
            SectionLabel("Tags")
        }
        if (tagTotals.isNotEmpty()) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.height((((tagTotals.size + 1) / 2) * 76).dp),
            ) {
                items(tagTotals) { tag ->
                    Column(
                        modifier = Modifier
                            .clickable { onOpenTagLedger(tag.tagId) }
                            .background(MaterialTheme.colorScheme.background)
                            .padding(20.dp, 11.dp),
                    ) {
                        Text("#${tag.tagName}", style = MaterialTheme.typography.labelLarge)
                        Text(formatAmount(tag.total), style = MaterialTheme.typography.titleLarge)
                        Text(
                            "${tag.count} tx",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Divider2()
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth().padding(20.dp, 14.dp, 20.dp, 8.dp),
        ) {
            SectionLabel("Recent")
            TextButton(onClick = onOpenLedger) {
                Text("All transactions", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
            }
        }
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(recent) { transaction ->
                RecentRow(
                    transaction = transaction,
                    category = categories.find { it.id == transaction.categoryId },
                    dateFormat = dateFormat,
                    onClick = { onOpenDetail(transaction.id) },
                )
            }
        }

        Divider2()
        Row(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = onOpenLedger,
                shape = MaterialTheme.shapes.medium,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onBackground,
                    contentColor = MaterialTheme.colorScheme.background,
                ),
                modifier = Modifier.weight(1f),
            ) { Text("Ledger") }
            Button(
                onClick = onOpenBudget,
                shape = MaterialTheme.shapes.medium,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ),
                modifier = Modifier.weight(1f),
            ) { Text("Budget") }
            Button(
                onClick = onOpenAdd,
                shape = MaterialTheme.shapes.medium,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ),
                modifier = Modifier.weight(1f),
            ) { Text("+ Add") }
        }
    }
}

@Composable
private fun BudgetBar(fraction: Float, height: androidx.compose.ui.unit.Dp, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth().height(height).background(MaterialTheme.colorScheme.surfaceVariant)) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(height)
                .background(MaterialTheme.colorScheme.secondary),
        )
    }
}

@Composable
private fun RecentRow(
    transaction: TransactionEntity,
    category: com.catovicajdin.expensetracker.data.entity.CategoryEntity?,
    dateFormat: SimpleDateFormat,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(20.dp, 12.dp),
    ) {
        CategoryDot(category)
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Text(category?.name ?: "Uncategorized", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            Text(
                dateFormat.format(transaction.postedAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(formatAmount(transaction.amount), style = MaterialTheme.typography.titleMedium)
    }
}

private fun deltaText(spent: Double, lastSpent: Double): String {
    val diff = lastSpent - spent
    val sign = if (diff >= 0) "−" else "+"
    return "$sign${formatAmount(kotlin.math.abs(diff))} vs last month"
}
