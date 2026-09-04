package com.catovicajdin.expensetracker.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.catovicajdin.expensetracker.data.AppDatabase
import com.catovicajdin.expensetracker.data.TransactionRow
import com.catovicajdin.expensetracker.ui.components.CategoryIconBadge
import com.catovicajdin.expensetracker.ui.components.Divider2
import com.catovicajdin.expensetracker.ui.components.ModernistCard
import com.catovicajdin.expensetracker.ui.components.formatAmount
import com.catovicajdin.expensetracker.ui.components.sourceLabel
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun LedgerListScreen(
    filter: TransactionFilter,
    onBack: () -> Unit,
    onOpenFilters: () -> Unit,
    onOpenDetail: (Long) -> Unit,
) {
    val context = LocalContext.current
    val db = AppDatabase.get(context)

    val categories by db.categoryDao().all().collectAsState(initial = emptyList())
    val tags by db.tagDao().all().collectAsState(initial = emptyList())
    val tagNamesByTransaction by db.tagDao().allTransactionTagNames().collectAsState(initial = emptyList())
    val rows by db.transactionDao().filteredWithSource(
        categoryIds = filter.categoryIds.toList(),
        categoryCount = filter.categoryIds.size,
        fromMillis = filter.fromMillis,
        toMillis = filter.toMillis,
        minAmount = filter.minAmount,
        maxAmount = filter.maxAmount,
        tagIds = filter.tagIds.toList(),
        matchAllTags = filter.tagMatchMode == TagMatchMode.ALL,
        tagCount = filter.tagIds.size,
    ).collectAsState(initial = emptyList())
    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }
    val tagsByTransaction = remember(tagNamesByTransaction) {
        tagNamesByTransaction.groupBy({ it.transactionId }, valueTransform = { it.tagName })
    }

    val filterSummaryText = remember(filter, categories, tags) { filterSummary(filter, categories, tags) }
    val sum = remember(rows) { rows.sumOf { it.transaction.amount } }

    Column(
        modifier = Modifier.fillMaxSize().padding(14.dp, 16.dp, 14.dp, 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp, 2.dp)) {
            TextButton(onClick = onBack, contentPadding = PaddingValues(0.dp)) {
                Text("← Back", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("Ledger", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(start = 14.dp))
        }

        ModernistCard(contentPadding = PaddingValues(20.dp, 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    onClick = onOpenFilters,
                    shape = MaterialTheme.shapes.small,
                    colors = ButtonDefaults.textButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                ) { Text("Filters", style = MaterialTheme.typography.labelLarge) }
                Text(
                    filterSummaryText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(start = 12.dp),
                )
                Text(formatAmount(sum), style = MaterialTheme.typography.titleSmall)
            }
        }

        ModernistCard(modifier = Modifier.weight(1f), contentPadding = PaddingValues(0.dp)) {
            LazyColumn {
                items(rows) { row ->
                    LedgerRow(
                        row = row,
                        category = categories.find { it.id == row.transaction.categoryId },
                        tagNames = tagsByTransaction[row.transaction.id].orEmpty(),
                        dateFormat = dateFormat,
                        onClick = { onOpenDetail(row.transaction.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun LedgerRow(
    row: TransactionRow,
    category: com.catovicajdin.expensetracker.data.entity.CategoryEntity?,
    tagNames: List<String>,
    dateFormat: SimpleDateFormat,
    onClick: () -> Unit,
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(20.dp, 14.dp),
        ) {
            CategoryIconBadge(category, size = 34.dp)
            Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
                Text(category?.name ?: "Uncategorized", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text(
                    "${dateFormat.format(row.transaction.postedAt)} · ${sourceLabel(row.source)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
                if (tagNames.isNotEmpty()) {
                    Text(
                        "#${tagNames.joinToString(" · ")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 3.dp),
                    )
                }
            }
            Text(formatAmount(row.transaction.amount), style = MaterialTheme.typography.titleMedium)
        }
        Divider2()
    }
}

private fun filterSummary(
    filter: TransactionFilter,
    categories: List<com.catovicajdin.expensetracker.data.entity.CategoryEntity>,
    tags: List<com.catovicajdin.expensetracker.data.entity.TagEntity>,
): String {
    val parts = mutableListOf<String>()
    parts += if (filter.categoryIds.isEmpty()) {
        "All categories"
    } else {
        categories.filter { filter.categoryIds.contains(it.id) }.joinToString(", ") { it.name }
    }
    if (filter.tagIds.isNotEmpty()) {
        val names = tags.filter { filter.tagIds.contains(it.id) }.joinToString(", ") { it.name }
        val mode = if (filter.tagMatchMode == TagMatchMode.ALL) "all" else "any"
        parts += "#$names ($mode)"
    }
    if (filter.fromMillis != null || filter.toMillis != null) parts += "date range"
    if (filter.minAmount != null || filter.maxAmount != null) parts += "amount range"
    return parts.joinToString(" · ")
}
