package com.catovicajdin.expensetracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.catovicajdin.expensetracker.data.AppDatabase
import com.catovicajdin.expensetracker.data.MonthRange
import com.catovicajdin.expensetracker.data.entity.TransactionEntity
import com.catovicajdin.expensetracker.ui.components.CategoryIconBadge
import com.catovicajdin.expensetracker.ui.components.ModernistCard
import com.catovicajdin.expensetracker.ui.components.SectionLabel
import com.catovicajdin.expensetracker.ui.components.formatAmount
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun HomeScreen(
    onOpenLedger: () -> Unit,
    onOpenBudget: () -> Unit,
    onOpenAdd: () -> Unit,
    onOpenReview: () -> Unit,
    onOpenDetail: (Long) -> Unit,
) {
    val context = LocalContext.current
    val db = AppDatabase.get(context)

    var yearMonth by remember { mutableStateOf(MonthRange.current()) }
    val lastMonth = remember(yearMonth) { MonthRange.previous(yearMonth) }
    val thisRange = remember(yearMonth) { MonthRange.millisRange(yearMonth) }
    val lastRange = remember(lastMonth) { MonthRange.millisRange(lastMonth) }

    val categories by db.categoryDao().all().collectAsState(initial = emptyList())
    val spent by db.transactionDao().totalSpent(thisRange.first, thisRange.second).collectAsState(initial = 0.0)
    val lastSpent by db.transactionDao().totalSpent(lastRange.first, lastRange.second).collectAsState(initial = 0.0)
    val monthlyBudget by db.budgetDao().monthlyBudgetFlow(yearMonth).collectAsState(initial = null)
    val needsReviewRows by db.rawNotificationDao().needsReview().collectAsState(initial = emptyList())
    val monthRows by db.transactionDao().filteredWithSource(
        categoryIds = emptyList(),
        categoryCount = 0,
        fromMillis = thisRange.first,
        toMillis = thisRange.second,
        minAmount = null,
        maxAmount = null,
        tagIds = emptyList(),
        matchAllTags = false,
        tagCount = 0,
    ).collectAsState(initial = emptyList())
    val recent = remember(monthRows) { monthRows.take(8).map { it.transaction } }
    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()) }

    Column(
        modifier = Modifier.fillMaxSize().padding(14.dp, 16.dp, 14.dp, 0.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ModernistCard(contentPadding = PaddingValues(22.dp, 24.dp, 22.dp, 22.dp)) {
            SectionLabel("Spent this month")
            Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 10.dp)) {
                Text(formatAmount(spent), style = MaterialTheme.typography.headlineLarge)
                Text(" BAM", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 6.dp, bottom = 4.dp))
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            ) {
                Text(
                    "${MonthRange.displayLabel(yearMonth)} · ${deltaText(spent, lastSpent)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Row {
                    TextButton(onClick = { yearMonth = MonthRange.previous(yearMonth) }, contentPadding = PaddingValues(6.dp, 0.dp)) {
                        Text("‹", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
                    }
                    TextButton(onClick = { yearMonth = MonthRange.next(yearMonth) }, contentPadding = PaddingValues(6.dp, 0.dp)) {
                        Text("›", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }

            val budget = monthlyBudget?.totalBudget
            if (budget != null && budget > 0.0) {
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).padding(vertical = 18.dp).background(MaterialTheme.colorScheme.outline))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                    SectionLabel("Budget ${formatAmount(budget)}")
                    Text(
                        "${formatAmount((budget - spent).coerceAtLeast(0.0))} left",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
                BudgetBar(fraction = (spent / budget).toFloat(), height = 10.dp, modifier = Modifier.padding(top = 12.dp))
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                ) {
                    Text(MonthRange.displayLabel(lastMonth), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatAmount(lastSpent), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                BudgetBar(fraction = (lastSpent / budget).toFloat(), height = 5.dp, muted = true, modifier = Modifier.padding(top = 7.dp))
            }
        }

        if (needsReviewRows.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onOpenReview() },
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            ) {
                Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                    Box(modifier = Modifier.width(3.dp).fillMaxHeight().background(MaterialTheme.colorScheme.secondary))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.weight(1f).padding(20.dp, 16.dp),
                    ) {
                        Text("Needs review · ${needsReviewRows.size}", style = MaterialTheme.typography.bodyLarge)
                        Text("Open →", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth().weight(1f),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth().padding(20.dp, 16.dp, 12.dp, 10.dp),
                ) {
                    SectionLabel("Recent")
                    TextButton(onClick = onOpenLedger) {
                        Text("All transactions →", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onOpenLedger,
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onBackground, contentColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.weight(1f),
            ) { Text("Ledger") }
            Button(
                onClick = onOpenBudget,
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp),
                modifier = Modifier.weight(1f),
            ) { Text("Budget") }
            Button(
                onClick = onOpenAdd,
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp),
                modifier = Modifier.weight(1f),
            ) { Text("+ Add") }
        }
        Box(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun BudgetBar(fraction: Float, height: Dp, modifier: Modifier = Modifier, muted: Boolean = false) {
    val fillColor = if (muted) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f) else MaterialTheme.colorScheme.onBackground
    val pill = RoundedCornerShape(percent = 50)
    Box(modifier = modifier.fillMaxWidth().height(height).background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.07f), pill)) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(height)
                .background(fillColor, pill),
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
        CategoryIconBadge(category, size = 34.dp)
        Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
            Text(category?.name ?: "Uncategorized", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(
                dateFormat.format(transaction.postedAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
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
