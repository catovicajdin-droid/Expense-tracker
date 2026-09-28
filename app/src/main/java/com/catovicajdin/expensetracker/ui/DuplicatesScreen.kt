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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.catovicajdin.expensetracker.data.AppDatabase
import com.catovicajdin.expensetracker.data.DuplicateGroup
import com.catovicajdin.expensetracker.ui.components.ModernistCard
import com.catovicajdin.expensetracker.ui.components.SectionLabel
import com.catovicajdin.expensetracker.ui.components.formatAmount
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Cleans up transactions that were recorded twice before the two duplication bugs were fixed. Both
 * fixes stop new duplicates; neither touched rows already written, which is what this is for.
 *
 * Nothing is deleted without being shown first: each group says what the notification was, how many
 * copies it produced, and when they landed. Removing a group keeps the earliest copy and deletes the
 * rest, so the transaction stays in the ledger - it just stops being counted more than once.
 */
@Composable
fun DuplicatesScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val db = AppDatabase.get(context)
    val scope = rememberCoroutineScope()

    val groups by db.transactionDao().duplicateGroups().collectAsState(initial = emptyList())
    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }
    var confirmRemoveAll by remember { mutableStateOf(false) }

    val extraCount = groups.sumOf { it.copies - 1 }
    val extraTotal = groups.sumOf { (it.copies - 1) * it.amount }

    Column(
        modifier = Modifier.fillMaxSize().padding(14.dp, 16.dp, 14.dp, 0.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp, 2.dp)) {
            TextButton(onClick = onBack, contentPadding = PaddingValues(0.dp)) {
                Text("← Edit budgets", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("Duplicates", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(start = 14.dp))
        }

        ModernistCard {
            if (groups.isEmpty()) {
                SectionLabel("Nothing to clean up")
                Text(
                    "No transaction was recorded more than once from the same notification.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 10.dp),
                )
            } else {
                SectionLabel("${groups.size} recorded more than once")
                Text(
                    "$extraCount extra ${if (extraCount == 1) "copy is" else "copies are"} inflating " +
                        "your spending by ${formatAmount(extraTotal)}. Removing keeps the first copy " +
                        "of each and deletes the rest.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
        }

        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(groups, key = { it.ids }) { group ->
                DuplicateGroupCard(
                    group = group,
                    firstSeen = dateFormat.format(group.firstPostedAt),
                    lastSeen = dateFormat.format(group.lastPostedAt),
                    onRemoveExtras = {
                        scope.launch { db.transactionDao().deleteAll(group.extraIds) }
                    },
                )
            }
        }

        if (groups.isNotEmpty()) {
            Button(
                onClick = { confirmRemoveAll = true },
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onBackground,
                    contentColor = MaterialTheme.colorScheme.surface,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Remove all $extraCount extra ${if (extraCount == 1) "copy" else "copies"}") }
        }
        Box(modifier = Modifier.height(12.dp))
    }

    if (confirmRemoveAll) {
        AlertDialog(
            onDismissRequest = { confirmRemoveAll = false },
            title = { Text("Remove every extra copy?") },
            text = { Text("The first copy of each transaction stays in the ledger. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { db.transactionDao().deleteAll(groups.flatMap { it.extraIds }) }
                    confirmRemoveAll = false
                }) { Text("Remove") }
            },
            dismissButton = { TextButton(onClick = { confirmRemoveAll = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun DuplicateGroupCard(
    group: DuplicateGroup,
    firstSeen: String,
    lastSeen: String,
    onRemoveExtras: () -> Unit,
) {
    ModernistCard(contentPadding = PaddingValues(20.dp, 16.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("${formatAmount(group.amount)} ${group.currency}", style = MaterialTheme.typography.titleMedium)
            Text(
                "×${group.copies}",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
        Text(
            group.body,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            if (firstSeen == lastSeen) firstSeen else "$firstSeen → $lastSeen",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp),
        )
        TextButton(onClick = onRemoveExtras, contentPadding = PaddingValues(0.dp), modifier = Modifier.padding(top = 4.dp)) {
            Text(
                "Keep the first, remove ${group.copies - 1}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}
