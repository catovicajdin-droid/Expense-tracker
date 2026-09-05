package com.catovicajdin.expensetracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.catovicajdin.expensetracker.data.entity.CategoryEntity
import com.catovicajdin.expensetracker.data.entity.TagEntity
import com.catovicajdin.expensetracker.data.entity.TransactionEntity
import com.catovicajdin.expensetracker.ui.components.CategoryIconBadge
import com.catovicajdin.expensetracker.ui.components.SectionLabel
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTransactionDialog(
    transaction: TransactionEntity,
    categories: List<CategoryEntity>,
    allTags: List<TagEntity>,
    currentTagIds: Set<Long>,
    onDismiss: () -> Unit,
    onDeleteTag: (TagEntity) -> Unit,
    onSave: (categoryId: Long?, postedAt: Long, tagIds: Set<Long>, newTagNames: List<String>) -> Unit,
) {
    var postedAt by remember { mutableStateOf(transaction.postedAt) }
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedCategoryId by remember { mutableStateOf(transaction.categoryId) }
    var selectedTagIds by remember { mutableStateOf(currentTagIds) }
    var newTagText by remember { mutableStateOf("") }
    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit transaction") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                TextButton(onClick = { showDatePicker = true }) {
                    Text("Date: ${dateFormat.format(postedAt)}")
                }
                SectionLabel("Category", modifier = Modifier.padding(top = 8.dp, bottom = 8.dp))
                LazyRow {
                    item {
                        CategoryOption(
                            name = "Uncategorized",
                            category = null,
                            selected = selectedCategoryId == null,
                            onClick = { selectedCategoryId = null },
                        )
                    }
                    items(categories) { category ->
                        CategoryOption(
                            name = category.name,
                            category = category,
                            selected = selectedCategoryId == category.id,
                            onClick = { selectedCategoryId = category.id },
                        )
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
                onSave(selectedCategoryId, postedAt, selectedTagIds, parseTagNames(newTagText))
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

/** Shared with ResolveReviewDialog - the Needs Review "accept" step uses the same category picker. */
@Composable
fun CategoryOption(name: String, category: CategoryEntity?, selected: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(end = 8.dp)
            .background(
                if (selected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.surfaceVariant,
                MaterialTheme.shapes.small,
            )
            .clickable(onClick = onClick)
            .padding(10.dp, 8.dp),
    ) {
        CategoryIconBadge(category, size = 22.dp)
        Text(
            name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

/** DatePicker's selectedDateMillis is UTC-midnight of the picked day; keep the entry's existing time-of-day. */
private fun combineDateKeepingTimeOfDay(pickedUtcMillis: Long, previousMillis: Long): Long {
    val pickedDate = Instant.ofEpochMilli(pickedUtcMillis).atZone(ZoneOffset.UTC).toLocalDate()
    val previousTime = Instant.ofEpochMilli(previousMillis).atZone(ZoneId.systemDefault()).toLocalTime()
    return pickedDate.atTime(previousTime).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
}
