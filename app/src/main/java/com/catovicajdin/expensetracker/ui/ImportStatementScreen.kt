package com.catovicajdin.expensetracker.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.catovicajdin.expensetracker.data.AppDatabase
import com.catovicajdin.expensetracker.data.entity.CategoryEntity
import com.catovicajdin.expensetracker.data.entity.TagEntity
import com.catovicajdin.expensetracker.data.statement.GroupChoice
import com.catovicajdin.expensetracker.data.statement.ImportPlan
import com.catovicajdin.expensetracker.data.statement.ImportRow
import com.catovicajdin.expensetracker.data.statement.MerchantGroup
import com.catovicajdin.expensetracker.data.statement.ParseResult
import com.catovicajdin.expensetracker.data.statement.PdfGlyphReader
import com.catovicajdin.expensetracker.data.statement.StatementImport
import com.catovicajdin.expensetracker.data.statement.StatementParser
import com.catovicajdin.expensetracker.ui.components.CategoryIconBadge
import com.catovicajdin.expensetracker.ui.components.Divider2
import com.catovicajdin.expensetracker.ui.components.ModernistCard
import com.catovicajdin.expensetracker.ui.components.SectionLabel
import com.catovicajdin.expensetracker.ui.components.formatAmount
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

/**
 * What the choice dialog is open for: a whole merchant, or particular rows of one. A single row is
 * just a selection of one, so nothing needs a second code path.
 */
private sealed interface Editing {
    val group: MerchantGroup

    data class Merchant(override val group: MerchantGroup) : Editing
    data class Rows(override val group: MerchantGroup, val rows: List<ImportRow>) : Editing
}

private sealed interface Stage {
    data object Idle : Stage
    data object Reading : Stage
    data class NeedsPassword(val uri: Uri) : Stage
    data class Failed(val detail: String) : Stage
    data class Review(val plan: ImportPlan) : Stage
    data class Done(val imported: Int, val sourceKey: String) : Stage
}

/**
 * Imports a bank statement PDF: pick the file, see what was read, decide a category per merchant,
 * import. Everything happens on the device.
 *
 * The statement is only offered for import once its figures add up - each row's balance following
 * from the one before it, all the way to the closing balance the bank printed. Until that holds, the
 * app cannot claim to have read the file correctly, and importing numbers it cannot vouch for is the
 * one thing an expense tracker must not do quietly.
 */
@Composable
fun ImportStatementScreen(onBack: () -> Unit, onImported: () -> Unit) {
    val context = LocalContext.current
    val db = AppDatabase.get(context)
    val scope = rememberCoroutineScope()

    val categories by db.categoryDao().all().collectAsState(initial = emptyList())
    val tags by db.tagDao().all().collectAsState(initial = emptyList())

    var stage by remember { mutableStateOf<Stage>(Stage.Idle) }
    var choices by remember { mutableStateOf<Map<String, GroupChoice>>(emptyMap()) }
    var rowChoices by remember { mutableStateOf<Map<Int, GroupChoice>>(emptyMap()) }
    var dropped by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var selected by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var expanded by remember { mutableStateOf<Set<String>>(emptySet()) }
    var onlyUncategorized by remember { mutableStateOf(false) }
    var skipDuplicates by remember { mutableStateOf(true) }
    var editing by remember { mutableStateOf<Editing?>(null) }
    var confirmUnreconciled by remember { mutableStateOf(false) }

    fun load(uri: Uri, password: String?) {
        stage = Stage.Reading
        scope.launch {
            when (val read = PdfGlyphReader.read(context, uri, password)) {
                is PdfGlyphReader.Result.NeedsPassword -> stage = Stage.NeedsPassword(uri)
                is PdfGlyphReader.Result.Failure -> stage = Stage.Failed(read.detail)
                is PdfGlyphReader.Result.Success -> when (val parsed = StatementParser.parse(read.glyphs)) {
                    is ParseResult.Failure -> stage = Stage.Failed(parsed.detail)
                    is ParseResult.Success -> {
                        val plan = StatementImport.plan(context, parsed.statement)
                        choices = plan.groups
                            .filter { it.suggestedCategoryId != null }
                            .associate { it.key to GroupChoice(categoryId = it.suggestedCategoryId) }
                        rowChoices = emptyMap()
                        dropped = emptySet()
                        selected = emptySet()
                        expanded = emptySet()
                        onlyUncategorized = false
                        stage = Stage.Review(plan)
                    }
                }
            }
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) load(uri, null)
    }

    fun commit(plan: ImportPlan) {
        scope.launch {
            val imported = StatementImport.commit(context, plan, choices, rowChoices, dropped, skipDuplicates)
            stage = Stage.Done(imported, plan.sourceKey)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(14.dp, 16.dp, 14.dp, 0.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp, 2.dp)) {
            TextButton(onClick = onBack, contentPadding = PaddingValues(0.dp)) {
                Text("← Edit budgets", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("Import statement", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(start = 14.dp))
        }

        when (val current = stage) {
            is Stage.Idle -> ModernistCard {
                SectionLabel("From your bank")
                Text(
                    "Pick a statement PDF downloaded from your bank. It is read on this phone and " +
                        "nothing is uploaded. Transactions are checked against the statement's own " +
                        "balances before anything is imported.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 10.dp),
                )
                Button(
                    onClick = { picker.launch(arrayOf("application/pdf")) },
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onBackground,
                        contentColor = MaterialTheme.colorScheme.surface,
                    ),
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                ) { Text("Choose a PDF") }
            }

            is Stage.Reading -> ModernistCard {
                Text("Reading the statement…", style = MaterialTheme.typography.bodyLarge)
            }

            is Stage.Failed -> ModernistCard {
                SectionLabel("Could not import")
                Text(
                    current.detail,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 10.dp),
                )
                TextButton(
                    onClick = { stage = Stage.Idle },
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.padding(top = 8.dp),
                ) { Text("Try another file", color = MaterialTheme.colorScheme.secondary) }
            }

            is Stage.Done -> ModernistCard {
                SectionLabel("Imported")
                Text(
                    "${current.imported} transaction${if (current.imported == 1) "" else "s"} added.",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 10.dp),
                )
                Text(
                    "If it does not look right, undo it here, or later from Imports on the budget " +
                        "settings screen.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
                Button(
                    onClick = onImported,
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onBackground,
                        contentColor = MaterialTheme.colorScheme.surface,
                    ),
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                ) { Text("Done") }
                TextButton(
                    onClick = {
                        scope.launch {
                            db.rawNotificationDao().deleteImport(current.sourceKey)
                            stage = Stage.Idle
                        }
                    },
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.padding(top = 6.dp),
                ) { Text("Undo this import", color = MaterialTheme.colorScheme.secondary) }
            }

            is Stage.NeedsPassword -> PasswordPrompt(
                onCancel = { stage = Stage.Idle },
                onSubmit = { password -> load(current.uri, password) },
            )

            is Stage.Review -> ReviewContent(
                plan = current.plan,
                categories = categories,
                choices = choices,
                rowChoices = rowChoices,
                dropped = dropped,
                selected = selected,
                expanded = expanded,
                onlyUncategorized = onlyUncategorized,
                skipDuplicates = skipDuplicates,
                onToggleOnlyUncategorized = { onlyUncategorized = !onlyUncategorized },
                onToggleSkipDuplicates = { skipDuplicates = it },
                onToggleExpanded = { key -> expanded = if (key in expanded) expanded - key else expanded + key },
                onToggleSelected = { id -> selected = if (id in selected) selected - id else selected + id },
                onClearSelection = { keys -> selected = selected - keys },
                onEdit = { editing = it },
                onImport = {
                    if (current.plan.statement.reconciles) commit(current.plan) else confirmUnreconciled = true
                },
            )
        }
    }

    editing?.let { target ->
        val rows = (target as? Editing.Rows)?.rows
        val groupChoice = choices[target.group.key] ?: GroupChoice()
        val single = rows?.singleOrNull()
        val dateFormat = remember { DateTimeFormatter.ofPattern("dd.MM.yyyy") }
        ChoiceDialog(
            title = when {
                rows == null -> target.group.label
                single != null -> single.shortDescription
                else -> "${rows.size} transactions"
            },
            subtitle = when {
                rows == null -> "${target.group.rows.size} transactions · ${formatAmount(target.group.total)}"
                single != null -> "${single.postingDate.format(dateFormat)} · ${formatAmount(single.amount)}"
                else -> "${rows.size} selected · ${formatAmount(rows.sumOf { it.amount })}"
            },
            categories = categories,
            allTags = tags,
            current = rows?.firstNotNullOfOrNull { rowChoices[it.id] } ?: groupChoice,
            canFollowMerchant = rows != null && rows.any { rowChoices.containsKey(it.id) },
            // A whole merchant can be left out too, which is how you drop 25 conversion fees without
            // ticking them one by one.
            droppable = rows ?: target.group.rows,
            allDropped = (rows ?: target.group.rows).all { it.id in dropped },
            onToggleDropped = {
                val ids = (rows ?: target.group.rows).map { it.id }.toSet()
                dropped = if (ids.all { it in dropped }) dropped - ids else dropped + ids
                editing = null
            },
            onFollowMerchant = {
                if (rows != null) rowChoices = rowChoices - rows.map { it.id }.toSet()
                editing = null
            },
            onDismiss = { editing = null },
            onApply = { choice, pendingTagNames ->
                scope.launch {
                    // Tags typed here are created now rather than at import time: until the tag
                    // exists it cannot be offered to the other merchants in this same review, nor
                    // shown ticked where it was typed, which is merely confusing.
                    val created = pendingTagNames.map { db.tagDao().getOrCreate(it) }
                    val settled = choice.copy(tagIds = choice.tagIds + created)
                    if (rows == null) {
                        choices = choices + (target.group.key to settled)
                    } else {
                        rowChoices = rowChoices + rows.associate { it.id to settled }
                        selected = selected - rows.map { it.id }.toSet()
                    }
                }
                editing = null
            },
        )
    }

    if (confirmUnreconciled) {
        val plan = (stage as? Stage.Review)?.plan
        AlertDialog(
            onDismissRequest = { confirmUnreconciled = false },
            title = { Text("Import without a match?") },
            text = {
                Text(
                    "The statement's balances do not follow from the transactions read, so some " +
                        "rows are wrong or missing. Importing now will put figures in your ledger " +
                        "that the statement does not support.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmUnreconciled = false
                    plan?.let { commit(it) }
                }) { Text("Import anyway", color = MaterialTheme.colorScheme.secondary) }
            },
            dismissButton = { TextButton(onClick = { confirmUnreconciled = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun PasswordPrompt(onCancel: () -> Unit, onSubmit: (String) -> Unit) {
    var password by remember { mutableStateOf("") }
    ModernistCard {
        SectionLabel("Password needed")
        Text(
            "This statement is protected. The password is used here and not stored.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 10.dp),
        )
        TextField(
            value = password,
            onValueChange = { password = it },
            placeholder = { Text("Password") },
            colors = plainFieldColors(),
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        )
        Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("Cancel") }
            Button(
                onClick = { onSubmit(password) },
                enabled = password.isNotBlank(),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onBackground,
                    contentColor = MaterialTheme.colorScheme.surface,
                ),
                modifier = Modifier.weight(1f),
            ) { Text("Open") }
        }
    }
}

@Composable
private fun ColumnScope.ReviewContent(
    plan: ImportPlan,
    categories: List<CategoryEntity>,
    choices: Map<String, GroupChoice>,
    rowChoices: Map<Int, GroupChoice>,
    dropped: Set<Int>,
    selected: Set<Int>,
    expanded: Set<String>,
    onlyUncategorized: Boolean,
    skipDuplicates: Boolean,
    onToggleOnlyUncategorized: () -> Unit,
    onToggleSkipDuplicates: (Boolean) -> Unit,
    onToggleExpanded: (String) -> Unit,
    onToggleSelected: (Int) -> Unit,
    onClearSelection: (Set<Int>) -> Unit,
    onEdit: (Editing) -> Unit,
    onImport: () -> Unit,
) {
    val dateFormat = remember { DateTimeFormatter.ofPattern("dd.MM.yyyy") }
    val dayFormat = remember { DateTimeFormatter.ofPattern("dd.MM.") }
    val statement = plan.statement

    fun categoryIdFor(group: MerchantGroup, row: ImportRow): Long? =
        (rowChoices[row.id] ?: choices[group.key])?.categoryId

    val importable = plan.groups.sumOf { group ->
        group.rows.count { it.id !in dropped && !(skipDuplicates && it.looksAlreadyPresent) }
    }
    val uncategorizedGroups = plan.groups.filter { group ->
        group.rows.any { it.id !in dropped && categoryIdFor(group, it) == null }
    }
    val shownGroups = if (onlyUncategorized) uncategorizedGroups else plan.groups

    LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            ModernistCard {
                SectionLabel("What was read")
                Text(
                    "${statement.rows.size} rows · ${statement.postingFirst.format(dateFormat)} – " +
                        statement.postingLast.format(dateFormat),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 10.dp),
                )
                Text(
                    if (statement.reconciles) {
                        "Balances to the cent: ${formatAmount(statement.opening)} at the start, " +
                            "${formatAmount(statement.closing)} at the end."
                    } else {
                        "These do not add up - ${statement.mismatches} row(s) disagree with the " +
                            "statement's running balance, so something was misread."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (statement.reconciles) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(top = 6.dp),
                )
                if (plan.transactedEarlier > 0) {
                    Text(
                        "${plan.transactedEarlier} of these were paid for late in the previous month " +
                            "and only posted in this one. They are filed on the day the money left " +
                            "the account, so the statement's month stays whole.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }

        item {
            ModernistCard {
                SectionLabel("What will be imported")
                Text(
                    "${plan.expenseCount} expenses across ${plan.groups.size} merchants.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 10.dp),
                )
                Text(
                    "Tap a merchant to set it for all of them, or open it to tick off particular " +
                        "transactions and set those together.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
                if (plan.nonExpenseCount > 0) {
                    Text(
                        "${plan.nonExpenseCount} row(s) are money in or reversed charges, so they are left out.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
                if (dropped.isNotEmpty()) {
                    Text(
                        "${dropped.size} left out by you.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
                if (plan.alreadyImportedFromPeriod > 0) {
                    Text(
                        "You have already imported ${plan.alreadyImportedFromPeriod} transaction(s) " +
                            "from a statement covering this period.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
                if (plan.duplicateCount > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggleSkipDuplicates(!skipDuplicates) }
                            .padding(top = 10.dp),
                    ) {
                        Text(if (skipDuplicates) "☑" else "☐", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Skip ${plan.duplicateCount} that the app already has",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 12.dp),
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleOnlyUncategorized() }
                        .padding(top = 10.dp),
                ) {
                    Text(if (onlyUncategorized) "☑" else "☐", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Show only what still has no category (${uncategorizedGroups.size})",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 12.dp),
                    )
                }
            }
        }

        if (shownGroups.isEmpty()) {
            item {
                ModernistCard {
                    Text(
                        "Every merchant has a category.",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }

        items(shownGroups, key = { it.key }) { group ->
            val groupChoice = choices[group.key]
            val groupCategory = categories.find { it.id == groupChoice?.categoryId }
            val overridden = group.rows.count { rowChoices.containsKey(it.id) }
            val selectedHere = group.rows.filter { it.id in selected }
            val isOpen = group.key in expanded

            ModernistCard(contentPadding = PaddingValues(18.dp, 14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { onEdit(Editing.Merchant(group)) },
                ) {
                    CategoryIconBadge(groupCategory, size = 30.dp)
                    Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(
                            group.label,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            "${group.rows.size} × · ${groupCategory?.name ?: "tap to categorize"}" +
                                if (overridden > 0) " · $overridden set on their own" else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(formatAmount(group.total), style = MaterialTheme.typography.titleSmall)
                }

                TextButton(
                    onClick = { onToggleExpanded(group.key) },
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.padding(top = 2.dp),
                ) {
                    Text(
                        if (isOpen) "Hide transactions" else "Show ${group.rows.size} transaction${if (group.rows.size == 1) "" else "s"}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (isOpen) {
                    if (selectedHere.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        ) {
                            Text(
                                "${selectedHere.size} selected · ${formatAmount(selectedHere.sumOf { it.amount })}",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(
                                onClick = { onClearSelection(selectedHere.map { it.id }.toSet()) },
                                contentPadding = PaddingValues(6.dp, 0.dp),
                            ) { Text("Clear", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            TextButton(
                                onClick = { onEdit(Editing.Rows(group, selectedHere)) },
                                contentPadding = PaddingValues(6.dp, 0.dp),
                            ) { Text("Edit these", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary) }
                        }
                    }

                    group.rows.forEach { row ->
                        Divider2()
                        val own = rowChoices[row.id]
                        val rowCategory = categories.find { it.id == categoryIdFor(group, row) }
                        val isDropped = row.id in dropped
                        val isSelected = row.id in selected
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        ) {
                            Text(
                                if (isSelected) "☑" else "☐",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier
                                    .clickable { onToggleSelected(row.id) }
                                    .padding(vertical = 8.dp, horizontal = 2.dp),
                            )
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onEdit(Editing.Rows(group, listOf(row))) }
                                    .padding(start = 10.dp, top = 8.dp, bottom = 8.dp),
                            ) {
                                Text(
                                    "${row.postingDate.format(dateFormat)} · ${formatAmount(row.amount)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isDropped) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                                )
                                val note = when {
                                    isDropped -> "left out"
                                    own != null -> "${rowCategory?.name ?: "uncategorized"} · set on its own"
                                    else -> rowCategory?.name ?: "uncategorized"
                                }
                                val cardDate = if (row.transactionDate != row.postingDate) {
                                    " · card used ${row.transactionDate.format(dayFormat)}"
                                } else {
                                    ""
                                }
                                Text(
                                    note + cardDate,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isDropped || own != null) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            CategoryIconBadge(if (isDropped) null else rowCategory, size = 24.dp)
                        }
                    }
                }
            }
        }
    }

    Button(
        onClick = onImport,
        enabled = importable > 0,
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.onBackground,
            contentColor = MaterialTheme.colorScheme.surface,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) { Text("Import $importable transaction${if (importable == 1) "" else "s"}") }
    Box(modifier = Modifier.height(12.dp))
}

/**
 * Category and tags, for a whole merchant or for rows of it. A row set on its own can be handed back
 * to the merchant, so an override is never a one-way door, and rows can be left out of the import
 * entirely without touching the rest.
 */
@Composable
private fun ChoiceDialog(
    title: String,
    subtitle: String,
    categories: List<CategoryEntity>,
    allTags: List<TagEntity>,
    current: GroupChoice,
    canFollowMerchant: Boolean,
    droppable: List<ImportRow>,
    allDropped: Boolean,
    onToggleDropped: () -> Unit,
    onFollowMerchant: () -> Unit,
    onDismiss: () -> Unit,
    onApply: (GroupChoice, List<String>) -> Unit,
) {
    var categoryId by remember { mutableStateOf(current.categoryId) }
    var tagIds by remember { mutableStateOf(current.tagIds) }
    var newTagText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                SectionLabel("Category", modifier = Modifier.padding(top = 16.dp, bottom = 10.dp))
                LazyRow {
                    item {
                        CategoryOption(
                            name = "Uncategorized",
                            category = null,
                            selected = categoryId == null,
                            onClick = { categoryId = null },
                        )
                    }
                    items(categories) { option ->
                        CategoryOption(
                            name = option.name,
                            category = option,
                            selected = categoryId == option.id,
                            onClick = { categoryId = option.id },
                        )
                    }
                }
                SectionLabel("Tags", modifier = Modifier.padding(top = 20.dp, bottom = 6.dp))
                allTags.forEach { tag ->
                    val picked = tagIds.contains(tag.id)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { tagIds = if (picked) tagIds - tag.id else tagIds + tag.id }
                            .padding(vertical = 10.dp),
                    ) {
                        Text(if (picked) "☑" else "☐", style = MaterialTheme.typography.titleMedium)
                        Text("#${tag.name}", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 12.dp))
                    }
                }
                TextField(
                    value = newTagText,
                    onValueChange = { newTagText = it },
                    placeholder = { Text("New tag, comma separated") },
                    colors = plainFieldColors(),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                if (canFollowMerchant) {
                    TextButton(
                        onClick = onFollowMerchant,
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.padding(top = 10.dp),
                    ) {
                        Text("Follow the merchant again", color = MaterialTheme.colorScheme.secondary)
                    }
                }
                TextButton(onClick = onToggleDropped, contentPadding = PaddingValues(0.dp)) {
                    Text(
                        if (allDropped) {
                            "Import ${if (droppable.size == 1) "this" else "these"} after all"
                        } else {
                            "Do not import ${if (droppable.size == 1) "this one" else "these ${droppable.size}"}"
                        },
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onApply(GroupChoice(categoryId, tagIds), parseTagNames(newTagText))
            }) { Text("Apply") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun plainFieldColors() = TextFieldDefaults.colors(
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    unfocusedIndicatorColor = Color.Transparent,
    focusedIndicatorColor = Color.Transparent,
)
