package com.catovicajdin.expensetracker.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    onDeleteTag: (TagEntity) -> Unit,
    onSave: (postedAt: Long, tagIds: Set<Long>, newTagNames: List<String>) -> Unit,
) {
    var postedAt by remember { mutableStateOf(transaction.postedAt) }
    var showDatePicker by remember { mutableStateOf(false) }
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
                TagSelector(
                    allTags = allTags,
                    selectedTagIds = selectedTagIds,
                    onToggle = { tagId ->
                        selectedTagIds = if (selectedTagIds.contains(tagId)) {
                            selectedTagIds - tagId
                        } else {
                            selectedTagIds + tagId
                        }
                    },
                    onDeleteTag = { tag ->
                        selectedTagIds = selectedTagIds - tag.id
                        onDeleteTag(tag)
                    },
                    newTagText = newTagText,
                    onNewTagTextChange = { newTagText = it },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(postedAt, selectedTagIds, parseTagNames(newTagText))
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
