package com.catovicajdin.expensetracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.catovicajdin.expensetracker.data.entity.CategoryEntity
import com.catovicajdin.expensetracker.data.entity.TagEntity
import com.catovicajdin.expensetracker.data.parseAmountInput
import com.catovicajdin.expensetracker.ui.components.SectionLabel
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBar(
    categories: List<CategoryEntity>,
    tags: List<TagEntity>,
    filter: TransactionFilter,
    onFilterChange: (TransactionFilter) -> Unit,
) {
    var showFromPicker by remember { mutableStateOf(false) }
    var showToPicker by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()) }

    Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
        if (categories.isNotEmpty()) {
            SectionLabel("Categories", modifier = Modifier.padding(bottom = 6.dp))
            LazyRow {
                items(categories) { category ->
                    CategoryFilterChip(
                        category = category,
                        selected = filter.categoryIds.contains(category.id),
                        onToggle = {
                            val newIds = if (filter.categoryIds.contains(category.id)) {
                                filter.categoryIds - category.id
                            } else {
                                filter.categoryIds + category.id
                            }
                            onFilterChange(filter.copy(categoryIds = newIds))
                        },
                    )
                }
            }
        }

        if (tags.isNotEmpty()) {
            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                LazyRow(modifier = Modifier.weight(1f)) {
                    items(tags) { tag ->
                        TagChip(
                            tag = tag,
                            selected = filter.tagIds.contains(tag.id),
                            onToggle = {
                                val newIds = if (filter.tagIds.contains(tag.id)) {
                                    filter.tagIds - tag.id
                                } else {
                                    filter.tagIds + tag.id
                                }
                                onFilterChange(filter.copy(tagIds = newIds))
                            },
                        )
                    }
                }
                TextButton(onClick = {
                    val nextMode = if (filter.tagMatchMode == TagMatchMode.ANY) TagMatchMode.ALL else TagMatchMode.ANY
                    onFilterChange(filter.copy(tagMatchMode = nextMode))
                }) {
                    Text(if (filter.tagMatchMode == TagMatchMode.ANY) "Match: ANY" else "Match: ALL")
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            TextButton(onClick = { showFromPicker = true }) {
                Text(filter.fromMillis?.let { "From: ${dateFormat.format(it)}" } ?: "From date")
            }
            TextButton(onClick = { showToPicker = true }) {
                Text(filter.toMillis?.let { "To: ${dateFormat.format(it)}" } ?: "To date")
            }
        }

        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            OutlinedTextField(
                value = filter.minAmount?.toString() ?: "",
                onValueChange = { onFilterChange(filter.copy(minAmount = parseAmountInput(it))) },
                label = { Text("Min amount") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedTextField(
                value = filter.maxAmount?.toString() ?: "",
                onValueChange = { onFilterChange(filter.copy(maxAmount = parseAmountInput(it))) },
                label = { Text("Max amount") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
            )
        }

        if (filter != TransactionFilter()) {
            TextButton(onClick = { onFilterChange(TransactionFilter()) }) { Text("Clear filters") }
        }
    }

    if (showFromPicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = filter.fromMillis)
        DatePickerDialog(
            onDismissRequest = { showFromPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    onFilterChange(filter.copy(fromMillis = state.selectedDateMillis))
                    showFromPicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showFromPicker = false }) { Text("Cancel") } },
        ) { DatePicker(state = state) }
    }

    if (showToPicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = filter.toMillis)
        DatePickerDialog(
            onDismissRequest = { showToPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    onFilterChange(filter.copy(toMillis = state.selectedDateMillis))
                    showToPicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showToPicker = false }) { Text("Cancel") } },
        ) { DatePicker(state = state) }
    }
}

@Composable
private fun CategoryFilterChip(category: CategoryEntity, selected: Boolean, onToggle: () -> Unit) {
    val background = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val content = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        modifier = Modifier
            .padding(end = 8.dp)
            .clickable(onClick = onToggle)
            .background(background)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(category.name, color = content)
    }
}
