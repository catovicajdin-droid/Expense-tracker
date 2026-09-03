package com.catovicajdin.expensetracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.catovicajdin.expensetracker.data.MonthRange
import com.catovicajdin.expensetracker.data.TransactionRow
import com.catovicajdin.expensetracker.ui.components.CategoryIconBadge
import com.catovicajdin.expensetracker.ui.components.Divider2
import com.catovicajdin.expensetracker.ui.components.SectionLabel
import com.catovicajdin.expensetracker.ui.components.formatAmount
import com.catovicajdin.expensetracker.ui.components.sourceLabel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun TransactionDetailScreen(
    transactionId: Long,
    onBack: () -> Unit,
    onDeleted: () -> Unit,
) {
    val context = LocalContext.current
    val db = AppDatabase.get(context)
    val scope = rememberCoroutineScope()

    var row by remember { mutableStateOf<TransactionRow?>(null) }
    var editingTagIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var showEdit by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    LaunchedEffect(transactionId) { row = db.transactionDao().byIdWithSource(transactionId) }

    val categories by db.categoryDao().all().collectAsState(initial = emptyList())
    val tags by db.tagDao().all().collectAsState(initial = emptyList())
    val tagNamesByTransaction by db.tagDao().allTransactionTagNames().collectAsState(initial = emptyList())
    val yearMonth = remember { MonthRange.current() }
    val range = remember { MonthRange.millisRange(yearMonth) }
    val categoryTotals by db.transactionDao().categoryTotals(range.first, range.second).collectAsState(initial = emptyList())
    val categoryBudgets by db.budgetDao().categoryBudgetsFlow(yearMonth).collectAsState(initial = emptyList())
    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }

    val transaction = row?.transaction
    val category = categories.find { it.id == transaction?.categoryId }
    val tagNames = tagNamesByTransaction.filter { it.transactionId == transactionId }.map { it.tagName }
    val categorySpent = categoryTotals.find { it.categoryId == transaction?.categoryId }?.total ?: 0.0
    val categoryBudget = categoryBudgets.find { it.categoryId == transaction?.categoryId }?.amount

    Column(modifier = Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(20.dp, 16.dp)) {
            TextButton(onClick = onBack, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                Text("← Ledger", style = MaterialTheme.typography.labelLarge)
            }
        }
        Divider2()

        if (transaction == null) {
            Text("Loading…", modifier = Modifier.padding(20.dp))
            return@Column
        }

        Column(modifier = Modifier.padding(20.dp, 24.dp)) {
            Text(
                "${sourceLabel(row?.source.orEmpty())} · ${dateFormat.format(transaction.postedAt)}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "${formatAmount(transaction.amount)} ${transaction.currency}",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(top = 8.dp),
            )
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 12.dp)) {
                CategoryIconBadge(category, size = 40.dp)
                Text(category?.name ?: "Uncategorized", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 10.dp))
            }
        }
        Divider2()

        Column(modifier = Modifier.padding(20.dp, 18.dp)) {
            SectionLabel("Category this month")
            if (categoryBudget != null && categoryBudget > 0.0) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                ) {
                    Text("${formatAmount(categorySpent)} of ${formatAmount(categoryBudget)}", style = MaterialTheme.typography.bodyMedium)
                    Text("${(categorySpent / categoryBudget * 100).toInt()}%", style = MaterialTheme.typography.labelLarge)
                }
                Box(modifier = Modifier.fillMaxWidth().height(10.dp).padding(top = 8.dp).background(MaterialTheme.colorScheme.surfaceVariant)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth((categorySpent / categoryBudget).coerceIn(0.0, 1.0).toFloat())
                            .height(10.dp)
                            .background(MaterialTheme.colorScheme.secondary),
                    )
                }
            } else {
                Text(
                    "Spent ${formatAmount(categorySpent)} so far, no budget set",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
        Divider2()

        Column(modifier = Modifier.padding(20.dp, 18.dp)) {
            SectionLabel("Tags")
            if (tagNames.isEmpty()) {
                Text(
                    "None",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            } else {
                LazyRow(modifier = Modifier.padding(top = 8.dp)) {
                    items(tagNames) { name ->
                        Box(
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .border(1.dp, MaterialTheme.colorScheme.outline)
                                .padding(9.dp, 4.dp),
                        ) {
                            Text("#$name", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }

        Box(modifier = Modifier.weight(1f))
        Divider2()
        Row(modifier = Modifier.fillMaxWidth()) {
            TextButton(
                onClick = {
                    scope.launch { editingTagIds = db.tagDao().tagIdsForTransaction(transactionId).toSet() }
                    showEdit = true
                },
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.weight(1f).padding(vertical = 8.dp),
            ) { Text("Edit") }
            TextButton(
                onClick = { showDeleteConfirm = true },
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.secondary),
                modifier = Modifier.weight(1f).padding(vertical = 8.dp),
            ) { Text("Delete") }
        }
    }

    if (showEdit) {
        val current = transaction
        if (current != null) {
            EditTransactionDialog(
                transaction = current,
                allTags = tags,
                currentTagIds = editingTagIds,
                onDismiss = { showEdit = false },
                onDeleteTag = { tag -> scope.launch { db.tagDao().delete(tag.id) } },
                onSave = { postedAt, tagIds, newTagNames ->
                    scope.launch {
                        val newTagIds = newTagNames.map { db.tagDao().getOrCreate(it) }
                        db.transactionDao().updatePostedAt(transactionId, postedAt)
                        db.tagDao().replaceTagsForTransaction(transactionId, tagIds + newTagIds)
                        row = db.transactionDao().byIdWithSource(transactionId)
                    }
                    showEdit = false
                },
            )
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete this transaction?") },
            text = { Text("This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        db.transactionDao().delete(transactionId)
                        onDeleted()
                    }
                    showDeleteConfirm = false
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } },
        )
    }
}
