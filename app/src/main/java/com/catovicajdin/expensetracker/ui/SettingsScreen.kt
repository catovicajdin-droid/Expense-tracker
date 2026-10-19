package com.catovicajdin.expensetracker.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.catovicajdin.expensetracker.data.AppDatabase
import com.catovicajdin.expensetracker.data.backup.DataTransfer
import com.catovicajdin.expensetracker.notifications.BankPreferences
import com.catovicajdin.expensetracker.ui.components.ModernistCard
import com.catovicajdin.expensetracker.ui.components.SectionLabel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Everything that configures the app rather than a month's numbers: which banks are read, the
 * category list, getting statements in, and getting your data out. Budget amounts stay on Edit budgets.
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenCategories: () -> Unit,
    onOpenDuplicates: () -> Unit,
    onOpenImport: () -> Unit,
    onOpenImportHistory: () -> Unit,
) {
    val context = LocalContext.current
    val db = AppDatabase.get(context)

    val categories by db.categoryDao().all().collectAsState(initial = emptyList())
    val duplicateGroups by db.transactionDao().duplicateGroups().collectAsState(initial = emptyList())
    val statementImports by db.rawNotificationDao().statementImports().collectAsState(initial = emptyList())

    var enabledBanks by remember { mutableStateOf(BankPreferences.enabled(context)) }
    var showBankDialog by remember { mutableStateOf(false) }
    var pendingRestore by remember { mutableStateOf<DataTransfer.RestoreCheck.Ok?>(null) }
    val scope = rememberCoroutineScope()

    fun toast(text: String) = Toast.makeText(context, text, Toast.LENGTH_LONG).show()

    // Each runs once the user has picked where the file goes (or which file to read); a cancelled
    // picker comes back null and does nothing.
    val exportCsv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) scope.launch {
            runCatching { DataTransfer.exportCsv(context, uri) }
                .onSuccess { toast("Exported $it transactions.") }
                .onFailure { toast("Export failed: ${it.message}") }
        }
    }
    val backup = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri ->
        if (uri != null) scope.launch {
            runCatching { DataTransfer.backup(context, uri) }
                .onSuccess { toast("Backup saved.") }
                .onFailure { toast("Backup failed: ${it.message}") }
        }
    }
    val restore = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            when (val check = DataTransfer.checkBackup(context, uri)) {
                is DataTransfer.RestoreCheck.Ok -> pendingRestore = check
                is DataTransfer.RestoreCheck.Rejected -> toast(check.reason)
            }
        }
    }
    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date()) }

    Column(modifier = Modifier.fillMaxSize().padding(14.dp, 16.dp, 14.dp, 0.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp, 2.dp)) {
            TextButton(onClick = onBack, contentPadding = PaddingValues(0.dp)) {
                Text("← Home", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("Settings", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(start = 14.dp))
        }

        LazyColumn(modifier = Modifier.weight(1f).padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                ModernistCard(contentPadding = PaddingValues(0.dp)) {
                    SettingsGroupLabel("Banks")
                    SettingsRow(
                        label = "Banks to read",
                        value = enabledBanks.sortedBy { it.ordinal }.joinToString { it.displayName },
                        onClick = { showBankDialog = true },
                    )
                    Box(modifier = Modifier.height(4.dp))
                }
            }

            item {
                ModernistCard(contentPadding = PaddingValues(0.dp)) {
                    SettingsGroupLabel("Categories")
                    SettingsRow(
                        label = "Manage categories",
                        value = "${categories.size}",
                        onClick = onOpenCategories,
                    )
                    Box(modifier = Modifier.height(4.dp))
                }
            }

            item {
                ModernistCard(contentPadding = PaddingValues(0.dp)) {
                    SettingsGroupLabel("Statements")
                    SettingsRow(label = "Import a statement", value = "PDF", onClick = onOpenImport)
                    SettingsRow(
                        label = "Imports",
                        value = "${statementImports.size}",
                        onClick = onOpenImportHistory,
                    )
                    // Only offered when there is something to clean up - the duplication bugs are fixed,
                    // so for a database that never hit them this row would never mean anything.
                    if (duplicateGroups.isNotEmpty()) {
                        SettingsRow(
                            label = "Duplicate transactions",
                            value = "${duplicateGroups.size}",
                            valueColor = MaterialTheme.colorScheme.secondary,
                            onClick = onOpenDuplicates,
                        )
                    }
                    Box(modifier = Modifier.height(4.dp))
                }
            }
            item {
                ModernistCard(contentPadding = PaddingValues(0.dp)) {
                    SettingsGroupLabel("Your data")
                    SettingsRow(label = "Export transactions", value = "CSV", onClick = {
                        exportCsv.launch("transactions-$today.csv")
                    })
                    SettingsRow(label = "Back up everything", value = "File", onClick = {
                        backup.launch("expense-tracker-backup-$today.db")
                    })
                    SettingsRow(label = "Restore from a backup", value = "", onClick = {
                        restore.launch(arrayOf("*/*"))
                    })
                    Box(modifier = Modifier.height(4.dp))
                }
            }
            item { Box(modifier = Modifier.height(4.dp)) }
        }
    }

    pendingRestore?.let { check ->
        AlertDialog(
            onDismissRequest = {
                check.staged.delete()
                pendingRestore = null
            },
            title = { Text("Restore this backup?") },
            text = {
                Text(
                    "It holds ${check.transactionCount} transactions. Everything in the app now - " +
                        "transactions, categories, budgets and tags - is replaced by the backup, and " +
                        "the app restarts.",
                )
            },
            confirmButton = {
                TextButton(onClick = { DataTransfer.restore(context, check.staged) }) { Text("Replace and restart") }
            },
            dismissButton = {
                TextButton(onClick = {
                    check.staged.delete()
                    pendingRestore = null
                }) { Text("Cancel") }
            },
        )
    }

    if (showBankDialog) {
        BankSelectionDialog(
            enabled = enabledBanks,
            onDismiss = { showBankDialog = false },
            onSave = { banks ->
                BankPreferences.setEnabled(context, banks)
                enabledBanks = banks
                showBankDialog = false
            },
        )
    }
}

@Composable
private fun SettingsGroupLabel(text: String) {
    Box(modifier = Modifier.padding(20.dp, 18.dp, 20.dp, 4.dp)) {
        SectionLabel(text)
    }
}

@Composable
private fun SettingsRow(
    label: String,
    value: String,
    onClick: () -> Unit,
    valueColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(20.dp, 14.dp),
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(
            "$value →",
            style = MaterialTheme.typography.bodyMedium,
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 16.dp),
        )
    }
}
