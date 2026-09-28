package com.catovicajdin.expensetracker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
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
import com.catovicajdin.expensetracker.data.MonthRange
import com.catovicajdin.expensetracker.data.StatementImportSummary
import com.catovicajdin.expensetracker.ui.components.ModernistCard
import com.catovicajdin.expensetracker.ui.components.SectionLabel
import com.catovicajdin.expensetracker.ui.components.formatAmount
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Every statement import, and a way to take one back out.
 *
 * An import is a hundred-odd rows written in one go, so undoing it by hand is not an option - each
 * run is recorded under its own source, and removing one deletes exactly the transactions it
 * created. Imports done before this was added still appear: they were already stamped with the
 * statement's period, which is enough to identify them.
 */
@Composable
fun ImportHistoryScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val db = AppDatabase.get(context)
    val scope = rememberCoroutineScope()

    val imports by db.rawNotificationDao().statementImports().collectAsState(initial = emptyList())
    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()) }
    var pendingRemoval by remember { mutableStateOf<StatementImportSummary?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().padding(14.dp, 16.dp, 14.dp, 0.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp, 2.dp)) {
            TextButton(onClick = onBack, contentPadding = PaddingValues(0.dp)) {
                Text("← Edit budgets", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("Imports", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(start = 14.dp))
        }

        if (imports.isEmpty()) {
            ModernistCard {
                SectionLabel("Nothing imported yet")
                Text(
                    "Statements you import will be listed here, and can be removed again as a whole.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
        }

        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(imports, key = { it.sourceKey }) { import ->
                ModernistCard(contentPadding = PaddingValues(20.dp, 16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            runCatching { MonthRange.displayLabel(import.period) }.getOrDefault(import.period),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(formatAmount(import.total), style = MaterialTheme.typography.titleSmall)
                    }
                    Text(
                        "${import.transactionCount} transaction${if (import.transactionCount == 1) "" else "s"} · " +
                            "${dateFormat.format(import.firstPostedAt)} – ${dateFormat.format(import.lastPostedAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                    TextButton(
                        onClick = { pendingRemoval = import },
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.padding(top = 4.dp),
                    ) {
                        Text("Remove this import", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }
        }
        Box(modifier = Modifier.height(12.dp))
    }

    pendingRemoval?.let { import ->
        AlertDialog(
            onDismissRequest = { pendingRemoval = null },
            title = { Text("Remove this import?") },
            text = {
                Text(
                    "This deletes the ${import.transactionCount} transaction(s) it added, along with " +
                        "any categories and tags you gave them since. Transactions from your " +
                        "notifications are not touched. This can't be undone.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { db.rawNotificationDao().deleteImport(import.sourceKey) }
                    pendingRemoval = null
                }) { Text("Remove") }
            },
            dismissButton = { TextButton(onClick = { pendingRemoval = null }) { Text("Cancel") } },
        )
    }
}
