package com.catovicajdin.expensetracker.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.catovicajdin.expensetracker.notifications.Bank

/**
 * Picks which banks' app notifications are read - any number of them at once. At least one has to
 * stay selected, since with none the app would silently track nothing.
 */
@Composable
fun BankSelectionDialog(
    enabled: Set<Bank>,
    onDismiss: () -> Unit,
    onSave: (Set<Bank>) -> Unit,
) {
    var selected by remember(enabled) { mutableStateOf(enabled) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Your banks") },
        text = {
            Column {
                Text(
                    "Transactions are read from the notifications of each bank app you pick.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                Bank.entries.forEach { bank ->
                    val checked = bank in selected
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selected = if (checked) selected - bank else selected + bank }
                            .padding(vertical = 4.dp),
                    ) {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = null,
                            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.onBackground),
                        )
                        Column(modifier = Modifier.padding(start = 12.dp)) {
                            Text(bank.displayName, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "${bank.appName} app",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(selected) }, enabled = selected.isNotEmpty()) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
