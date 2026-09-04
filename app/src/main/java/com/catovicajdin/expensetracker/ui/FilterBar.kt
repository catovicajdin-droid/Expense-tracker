package com.catovicajdin.expensetracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.catovicajdin.expensetracker.data.entity.CategoryEntity
import com.catovicajdin.expensetracker.data.entity.TagEntity
import com.catovicajdin.expensetracker.data.parseAmountInput
import com.catovicajdin.expensetracker.ui.components.ModernistCard
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
    modifier: Modifier = Modifier,
) {
    var showFromPicker by remember { mutableStateOf(false) }
    var showToPicker by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()) }
    val fieldColors = TextFieldDefaults.colors(
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
        unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
        focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
        disabledIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
    )

    Column(modifier = modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (categories.isNotEmpty()) {
            ModernistCard {
                SectionLabel("Categories")
                LazyRow(modifier = Modifier.padding(top = 12.dp)) {
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
        }

        if (tags.isNotEmpty()) {
            ModernistCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    SectionLabel("Tags")
                    TextButton(onClick = {
                        val nextMode = if (filter.tagMatchMode == TagMatchMode.ANY) TagMatchMode.ALL else TagMatchMode.ANY
                        onFilterChange(filter.copy(tagMatchMode = nextMode))
                    }, contentPadding = PaddingValues(0.dp)) {
                        Text(
                            if (filter.tagMatchMode == TagMatchMode.ANY) "Match: ANY" else "Match: ALL",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                LazyRow(modifier = Modifier.padding(top = 12.dp)) {
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
                            modifier = Modifier.padding(end = 8.dp),
                        )
                    }
                }
            }
        }

        ModernistCard {
            SectionLabel("Date range")
            Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                InsetField(text = filter.fromMillis?.let { dateFormat.format(it) } ?: "From", onClick = { showFromPicker = true }, modifier = Modifier.weight(1f))
                InsetField(text = filter.toMillis?.let { dateFormat.format(it) } ?: "To", onClick = { showToPicker = true }, modifier = Modifier.weight(1f))
            }
            SectionLabel("Amount", modifier = Modifier.padding(top = 20.dp))
            Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TextField(
                    value = filter.minAmount?.toString() ?: "",
                    onValueChange = { onFilterChange(filter.copy(minAmount = parseAmountInput(it))) },
                    placeholder = { Text("Min") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = fieldColors,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.weight(1f),
                )
                TextField(
                    value = filter.maxAmount?.toString() ?: "",
                    onValueChange = { onFilterChange(filter.copy(maxAmount = parseAmountInput(it))) },
                    placeholder = { Text("Max") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = fieldColors,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.weight(1f),
                )
            }
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

/** A non-editable, tap-to-open field styled like the design's inset gray boxes (From/To, Min/Max). */
@Composable
private fun InsetField(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    )
}

@Composable
private fun CategoryFilterChip(category: CategoryEntity, selected: Boolean, onToggle: () -> Unit) {
    val background = if (selected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.surfaceVariant
    val content = if (selected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(end = 8.dp)
            .background(background, MaterialTheme.shapes.small)
            .clickable(onClick = onToggle)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(category.name, style = MaterialTheme.typography.labelLarge, color = content)
    }
}
