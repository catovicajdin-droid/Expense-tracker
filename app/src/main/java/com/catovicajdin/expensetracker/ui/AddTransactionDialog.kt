package com.catovicajdin.expensetracker.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.catovicajdin.expensetracker.data.entity.CategoryEntity
import com.catovicajdin.expensetracker.data.entity.TagEntity
import com.catovicajdin.expensetracker.data.parseAmountInput
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionDialog(
    categories: List<CategoryEntity>,
    allTags: List<TagEntity>,
    onDismiss: () -> Unit,
    onDeleteTag: (TagEntity) -> Unit,
    onSave: (amount: Double, categoryId: Long?, postedAt: Long, tagIds: Set<Long>, newTagNames: List<String>) -> Unit,
) {
    var amountText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<CategoryEntity?>(null) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var postedAt by remember { mutableStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedTagIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var newTagText by remember { mutableStateOf("") }
    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add transaction") },
        text = {
            Column {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount (BAM)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                TextButton(onClick = { showDatePicker = true }, modifier = Modifier.padding(top = 8.dp)) {
                    Text("Date: ${dateFormat.format(postedAt)}")
                }
                Box(modifier = Modifier.padding(top = 4.dp)) {
                    TextButton(onClick = { categoryExpanded = true }) {
                        Text(selectedCategory?.name ?: "Choose category (optional)")
                    }
                    DropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text("Uncategorized") },
                            onClick = {
                                selectedCategory = null
                                categoryExpanded = false
                            },
                        )
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.name) },
                                onClick = {
                                    selectedCategory = category
                                    categoryExpanded = false
                                },
                            )
                        }
                    }
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
                parseAmountInput(amountText)?.let { amount ->
                    onSave(amount, selectedCategory?.id, postedAt, selectedTagIds, parseTagNames(newTagText))
                }
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
                    state.selectedDateMillis?.let { postedAt = combineDateWithCurrentTime(it) }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } },
        ) { DatePicker(state = state) }
    }
}

/** DatePicker's selectedDateMillis is UTC-midnight of the picked day; keep the current time-of-day when applying it. */
private fun combineDateWithCurrentTime(pickedUtcMillis: Long): Long {
    val pickedDate = Instant.ofEpochMilli(pickedUtcMillis).atZone(ZoneOffset.UTC).toLocalDate()
    return pickedDate.atTime(LocalTime.now()).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
}
