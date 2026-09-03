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
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.catovicajdin.expensetracker.data.AppDatabase
import com.catovicajdin.expensetracker.data.NotificationRepository
import com.catovicajdin.expensetracker.data.entity.CategoryEntity
import com.catovicajdin.expensetracker.data.entity.TagEntity
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
    var editingTransaction by remember { mutableStateOf<TransactionEntity?>(null) }
    var editingTagIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    val categories by db.categoryDao().all().collectAsState(initial = emptyList())
    val tags by db.tagDao().all().collectAsState(initial = emptyList())
    val tagNamesByTransaction by db.tagDao().allTransactionTagNames().collectAsState(initial = emptyList())
    var filter by remember { mutableStateOf(TransactionFilter()) }
    val transactions by db.transactionDao().filtered(
        categoryId = filter.categoryId,
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

    fun deleteTag(tag: TagEntity) {
        scope.launch { db.tagDao().delete(tag.id) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = onOpenNeedsReview) { Text("Needs review") }
            TextButton(onClick = onOpenBudget) { Text("Budget") }
            TextButton(onClick = { showAddDialog = true }) { Text("+ Add") }
        }
        FilterBar(categories = categories, tags = tags, filter = filter, onFilterChange = { filter = it })
        HorizontalDivider()
        LazyColumn {
            items(transactions) { transaction ->
                TransactionRow(
                    transaction = transaction,
                    category = categories.find { it.id == transaction.categoryId },
                    categories = categories,
                    tagNames = tagsByTransaction[transaction.id].orEmpty(),
                    dateFormat = dateFormat,
                    onReassign = { categoryId ->
                        scope.launch {
                            db.transactionDao().assignCategory(transaction.id, categoryId)
                            BudgetAlerts.checkCategory(context, categoryId)
                        }
                    },
                    onDelete = {
                        scope.launch { db.transactionDao().delete(transaction.id) }
                    },
                    onEdit = {
                        scope.launch { editingTagIds = db.tagDao().tagIdsForTransaction(transaction.id).toSet() }
                        editingTransaction = transaction
                    },
                )
                HorizontalDivider()
            }
        }
    }

    if (showAddDialog) {
        AddTransactionDialog(
            categories = categories,
            allTags = tags,
            onDismiss = { showAddDialog = false },
            onDeleteTag = ::deleteTag,
            onSave = { amount, categoryId, postedAt, tagIds, newTagNames ->
                scope.launch {
                    val newTagIds = newTagNames.map { db.tagDao().getOrCreate(it) }
                    val transactionId = NotificationRepository(db).insertManual(amount, categoryId, postedAt)
                    db.tagDao().replaceTagsForTransaction(transactionId, tagIds + newTagIds)
                    if (categoryId != null) BudgetAlerts.checkCategory(context, categoryId)
                    BudgetAlerts.checkOverall(context)
                }
                showAddDialog = false
            },
        )
    }

    editingTransaction?.let { transaction ->
        EditTransactionDialog(
            transaction = transaction,
            allTags = tags,
            currentTagIds = editingTagIds,
            onDismiss = { editingTransaction = null },
            onDeleteTag = ::deleteTag,
            onSave = { postedAt, tagIds, newTagNames ->
                scope.launch {
                    val newTagIds = newTagNames.map { db.tagDao().getOrCreate(it) }
                    db.transactionDao().updatePostedAt(transaction.id, postedAt)
                    db.tagDao().replaceTagsForTransaction(transaction.id, tagIds + newTagIds)
                }
                editingTransaction = null
            },
        )
    }
}

@Composable
private fun TransactionRow(
    transaction: TransactionEntity,
    category: CategoryEntity?,
    categories: List<CategoryEntity>,
    tagNames: List<String>,
    dateFormat: SimpleDateFormat,
    onReassign: (Long) -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
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
                if (tagNames.isNotEmpty()) {
                    Text(tagNames.joinToString(", "), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("${transaction.amount} ${transaction.currency}")
            Row {
                TextButton(onClick = onEdit) { Text("Edit") }
                TextButton(onClick = { showDeleteConfirm = true }) { Text("Delete") }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete this transaction?") },
            text = { Text("${transaction.amount} ${transaction.currency} on ${dateFormat.format(transaction.postedAt)}. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    showDeleteConfirm = false
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } },
        )
    }
}
