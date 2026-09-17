package com.catovicajdin.expensetracker.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.catovicajdin.expensetracker.data.AppDatabase
import com.catovicajdin.expensetracker.data.TransactionRow
import com.catovicajdin.expensetracker.notifications.BudgetAlerts
import com.catovicajdin.expensetracker.ui.components.CategoryIconBadge
import com.catovicajdin.expensetracker.ui.components.Divider2
import com.catovicajdin.expensetracker.ui.components.ModernistCard
import com.catovicajdin.expensetracker.ui.components.formatAmount
import com.catovicajdin.expensetracker.ui.components.sourceLabel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun LedgerListScreen(
    filter: TransactionFilter,
    onFilterChange: (TransactionFilter) -> Unit,
    onBack: () -> Unit,
    onOpenFilters: () -> Unit,
    onOpenDetail: (Long) -> Unit,
) {
    val context = LocalContext.current
    val db = AppDatabase.get(context)
    val scope = rememberCoroutineScope()

    val categories by db.categoryDao().all().collectAsState(initial = emptyList())
    val tags by db.tagDao().all().collectAsState(initial = emptyList())

    // A filter can outlive what it points at - open a category's ledger from the budget grid, then
    // delete that category, and the id lingers here, matching nothing and explaining nothing. Drop
    // ids that no longer exist so the filter heals itself. Guarded on a loaded list, since both
    // flows start empty and would otherwise wipe the filter on the first frame.
    LaunchedEffect(categories, tags, filter) {
        if (categories.isEmpty() && tags.isEmpty()) return@LaunchedEffect
        val liveCategoryIds = categories.mapTo(mutableSetOf()) { it.id }
        val liveTagIds = tags.mapTo(mutableSetOf()) { it.id }
        val prunedCategories = if (categories.isEmpty()) filter.categoryIds else filter.categoryIds intersect liveCategoryIds
        val prunedTags = if (tags.isEmpty()) filter.tagIds else filter.tagIds intersect liveTagIds
        if (prunedCategories != filter.categoryIds || prunedTags != filter.tagIds) {
            onFilterChange(filter.copy(categoryIds = prunedCategories, tagIds = prunedTags))
        }
    }
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

    // Long-press a row to start selecting; an empty set means selection mode is off.
    var selectedIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var showBulkCategory by remember { mutableStateOf(false) }
    var showBulkTags by remember { mutableStateOf(false) }

    // Selecting rows and then narrowing the filter would otherwise leave invisible rows selected and
    // silently included in the next bulk edit. Keep the selection to what's actually on screen.
    LaunchedEffect(rows) {
        val visible = rows.mapTo(mutableSetOf()) { it.transaction.id }
        if (selectedIds.any { it !in visible }) selectedIds = selectedIds intersect visible
    }

    fun applyBulk(work: suspend (List<Long>) -> Unit) {
        val target = selectedIds.toList()
        scope.launch { work(target) }
        selectedIds = emptySet()
    }

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
            if (selectedIds.isEmpty()) {
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
            } else {
                val selectedTotal = rows.filter { selectedIds.contains(it.transaction.id) }
                    .sumOf { it.transaction.amount }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = { selectedIds = emptySet() }, contentPadding = PaddingValues(0.dp)) {
                        Text("✕", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(
                        "${selectedIds.size} selected · ${formatAmount(selectedTotal)}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f).padding(start = 12.dp),
                    )
                    TextButton(
                        onClick = { selectedIds = rows.mapTo(mutableSetOf()) { it.transaction.id } },
                        contentPadding = PaddingValues(6.dp, 0.dp),
                    ) {
                        Text("All", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = { showBulkCategory = true },
                        shape = MaterialTheme.shapes.small,
                        colors = ButtonDefaults.textButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                        modifier = Modifier.weight(1f),
                    ) { Text("Category", style = MaterialTheme.typography.labelLarge) }
                    TextButton(
                        onClick = { showBulkTags = true },
                        shape = MaterialTheme.shapes.small,
                        colors = ButtonDefaults.textButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                        modifier = Modifier.weight(1f),
                    ) { Text("Tags", style = MaterialTheme.typography.labelLarge) }
                }
            }
        }

        ModernistCard(modifier = Modifier.weight(1f), contentPadding = PaddingValues(0.dp)) {
            if (rows.isEmpty()) {
                val filtered = filter != TransactionFilter()
                Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                    Text(
                        if (filtered) "No transactions match these filters." else "No transactions yet.",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    if (filtered) {
                        TextButton(
                            onClick = { onFilterChange(TransactionFilter()) },
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.padding(top = 8.dp),
                        ) {
                            Text("Clear filters", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
            }
            LazyColumn {
                items(rows) { row ->
                    val id = row.transaction.id
                    fun toggle() {
                        selectedIds = if (selectedIds.contains(id)) selectedIds - id else selectedIds + id
                    }
                    LedgerRow(
                        row = row,
                        category = categories.find { it.id == row.transaction.categoryId },
                        categories = categories,
                        tagNames = tagsByTransaction[id].orEmpty(),
                        dateFormat = dateFormat,
                        selected = selectedIds.contains(id),
                        selecting = selectedIds.isNotEmpty(),
                        // While selecting, a tap picks rather than navigates - otherwise it's far too
                        // easy to lose a selection by opening a transaction by accident.
                        onClick = { if (selectedIds.isEmpty()) onOpenDetail(id) else toggle() },
                        onLongClick = ::toggle,
                        onReassign = { categoryId ->
                            scope.launch {
                                db.transactionDao().assignCategory(id, categoryId)
                                categoryId?.let { BudgetAlerts.checkCategory(context, it) }
                            }
                        },
                    )
                }
            }
        }
    }

    if (showBulkCategory) {
        val count = selectedIds.size
        AlertDialog(
            onDismissRequest = { showBulkCategory = false },
            title = { Text("Category for $count transaction${if (count == 1) "" else "s"}") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    CategoryOption(
                        name = "Uncategorized",
                        category = null,
                        selected = false,
                        onClick = {
                            applyBulk { ids -> db.transactionDao().assignCategoryToAll(ids, null) }
                            showBulkCategory = false
                        },
                    )
                    categories.forEach { category ->
                        CategoryOption(
                            name = category.name,
                            category = category,
                            selected = false,
                            onClick = {
                                applyBulk { ids ->
                                    db.transactionDao().assignCategoryToAll(ids, category.id)
                                    BudgetAlerts.checkCategory(context, category.id)
                                }
                                showBulkCategory = false
                            },
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showBulkCategory = false }) { Text("Cancel") } },
        )
    }

    if (showBulkTags) {
        BulkTagDialog(
            count = selectedIds.size,
            allTags = tags,
            onDismiss = { showBulkTags = false },
            onApply = { tagIds, add ->
                applyBulk { ids ->
                    tagIds.forEach { tagId ->
                        if (add) db.tagDao().addTagToAll(ids, tagId) else db.tagDao().removeTagFromAll(ids, tagId)
                    }
                }
                showBulkTags = false
            },
        )
    }
}

/**
 * Add or remove, never replace: each transaction in the selection has its own tags, so applying one
 * list to all of them would silently drop the others.
 */
@Composable
private fun BulkTagDialog(
    count: Int,
    allTags: List<com.catovicajdin.expensetracker.data.entity.TagEntity>,
    onDismiss: () -> Unit,
    onApply: (tagIds: Set<Long>, add: Boolean) -> Unit,
) {
    var picked by remember { mutableStateOf<Set<Long>>(emptySet()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tags for $count transaction${if (count == 1) "" else "s"}") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                if (allTags.isEmpty()) {
                    Text("No tags yet - add one from a transaction first.", style = MaterialTheme.typography.bodyMedium)
                } else {
                    allTags.forEach { tag ->
                        val isPicked = picked.contains(tag.id)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { picked = if (isPicked) picked - tag.id else picked + tag.id }
                                .padding(vertical = 10.dp),
                        ) {
                            Text(if (isPicked) "☑" else "☐", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "#${tag.name}",
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(start = 12.dp),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = picked.isNotEmpty(), onClick = { onApply(picked, true) }) { Text("Add") }
        },
        dismissButton = {
            TextButton(enabled = picked.isNotEmpty(), onClick = { onApply(picked, false) }) {
                Text("Remove", color = MaterialTheme.colorScheme.secondary)
            }
        },
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LedgerRow(
    row: TransactionRow,
    category: com.catovicajdin.expensetracker.data.entity.CategoryEntity?,
    categories: List<com.catovicajdin.expensetracker.data.entity.CategoryEntity>,
    tagNames: List<String>,
    dateFormat: SimpleDateFormat,
    selected: Boolean,
    selecting: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onReassign: (Long?) -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                .background(if (selected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
                .padding(20.dp, 14.dp),
        ) {
            // Tapping the badge reassigns the category in place; the badge's own clickable consumes
            // the tap so it never falls through to the row's open-detail click. Suppressed while
            // selecting, where every tap on the row should mean "pick this one".
            Box {
                Box(modifier = Modifier.clickable(enabled = !selecting) { menuExpanded = true }) {
                    // Keeps the category badge underneath so the row stays recognisable, with a
                    // check laid over it rather than replacing it (an empty badge would read as
                    // "uncategorized", which is a thing a row can actually be).
                    Box(contentAlignment = Alignment.Center) {
                        CategoryIconBadge(category, size = 34.dp)
                        if (selected) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("✓", color = MaterialTheme.colorScheme.surface, style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text("Uncategorized") },
                        leadingIcon = { CategoryIconBadge(null, size = 22.dp) },
                        onClick = {
                            onReassign(null)
                            menuExpanded = false
                        },
                    )
                    categories.forEach { candidate ->
                        DropdownMenuItem(
                            text = { Text(candidate.name) },
                            leadingIcon = { CategoryIconBadge(candidate, size = 22.dp) },
                            onClick = {
                                onReassign(candidate.id)
                                menuExpanded = false
                            },
                        )
                    }
                }
            }
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
    // Names are resolved against the live lists, so a filter naming something since deleted would
    // otherwise join to an empty string and leave the bar blank - never show nothing.
    val categoryNames = categories.filter { filter.categoryIds.contains(it.id) }.map { it.name }
    parts += when {
        filter.categoryIds.isEmpty() -> "All categories"
        categoryNames.isEmpty() -> "Deleted category"
        else -> categoryNames.joinToString(", ")
    }
    if (filter.tagIds.isNotEmpty()) {
        val names = tags.filter { filter.tagIds.contains(it.id) }.map { it.name }
        val mode = if (filter.tagMatchMode == TagMatchMode.ALL) "all" else "any"
        parts += if (names.isEmpty()) "deleted tag" else "#${names.joinToString(", ")} ($mode)"
    }
    if (filter.fromMillis != null || filter.toMillis != null) parts += "date range"
    if (filter.minAmount != null || filter.maxAmount != null) parts += "amount range"
    return parts.joinToString(" · ")
}
