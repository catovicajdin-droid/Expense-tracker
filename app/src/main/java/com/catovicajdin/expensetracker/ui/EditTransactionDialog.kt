package com.catovicajdin.expensetracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.catovicajdin.expensetracker.data.entity.TagEntity
import com.catovicajdin.expensetracker.data.entity.TransactionEntity
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTransactionDialog(
    transaction: TransactionEntity,
    allTags: List<TagEntity>,
    currentTagIds: Set<Long>,
    onDismiss: () -> Unit,
    onSave: (postedAt: Long, notes: String?, tagIds: Set<Long>, newTagName: String?) -> Unit,
) {
    var postedAt by remember { mutableStateOf(transaction.postedAt) }
    var showDatePicker by remember { mutableStateOf(false) }
    var notesText by remember { mutableStateOf(transaction.notes ?: "") }
    var selectedTagIds by remember { mutableStateOf(currentTagIds) }
    var newTagText by remember { mutableStateOf("") }
    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit transaction") },
        text = {
            Column {
                TextButton(onClick = { showDatePicker = true }) {
                    Text("Date: ${dateFormat.format(postedAt)}")
                }
                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("Notes") },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                Text("Tags", modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
                LazyRow {
                    items(allTags) { tag ->
                        TagChip(
                            tag = tag,
                            selected = selectedTagIds.contains(tag.id),
                            onToggle = {
                                selectedTagIds = if (selectedTagIds.contains(tag.id)) {
                                    selectedTagIds - tag.id
                                } else {
                                    selectedTagIds + tag.id
                                }
                            },
                        )
                    }
                }
                OutlinedTextField(
                    value = newTagText,
                    onValueChange = { newTagText = it },
                    label = { Text("New tag") },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(postedAt, notesText.ifBlank { null }, selectedTagIds, newTagText.ifBlank { null })
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )

    if (showDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = postedAt)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { postedAt = combineDateKeepingTimeOfDay(it, postedAt) }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } },
        ) { DatePicker(state = state) }
    }
}

/** DatePicker's selectedDateMillis is UTC-midnight of the picked day; keep the entry's existing time-of-day. */
private fun combineDateKeepingTimeOfDay(pickedUtcMillis: Long, previousMillis: Long): Long {
    val pickedDate = Instant.ofEpochMilli(pickedUtcMillis).atZone(ZoneOffset.UTC).toLocalDate()
    val previousTime = Instant.ofEpochMilli(previousMillis).atZone(ZoneId.systemDefault()).toLocalTime()
    return pickedDate.atTime(previousTime).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
}

@Composable
private fun TagChip(tag: TagEntity, selected: Boolean, onToggle: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(end = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onToggle() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            tag.name,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
