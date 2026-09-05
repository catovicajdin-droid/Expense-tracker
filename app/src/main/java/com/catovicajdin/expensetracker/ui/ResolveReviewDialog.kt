package com.catovicajdin.expensetracker.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.catovicajdin.expensetracker.data.entity.CategoryEntity
import com.catovicajdin.expensetracker.data.entity.TagEntity
import com.catovicajdin.expensetracker.data.parseAmountInput
import com.catovicajdin.expensetracker.ui.components.SectionLabel

/**
 * Accept step for a Needs Review row. The raw notification failed to parse, so unlike
 * EditTransactionDialog this also collects the amount - everything else (category, tags) reuses
 * the same pickers so accepting doesn't require leaving the Needs Review screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResolveReviewDialog(
    categories: List<CategoryEntity>,
    allTags: List<TagEntity>,
    onDismiss: () -> Unit,
    onDeleteTag: (TagEntity) -> Unit,
    onAccept: (categoryId: Long?, amount: Double, tagIds: Set<Long>, newTagNames: List<String>) -> Unit,
) {
    var amountText by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }
    var selectedTagIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var newTagText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Accept transaction") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                SectionLabel("Amount · BAM", modifier = Modifier.padding(bottom = 8.dp))
                TextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    placeholder = { Text("0.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                    ),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth(),
                )
                SectionLabel("Category", modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
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
            TextButton(
                onClick = {
                    val amount = parseAmountInput(amountText) ?: return@TextButton
                    onAccept(selectedCategoryId, amount, selectedTagIds, parseTagNames(newTagText))
                },
            ) { Text("Accept") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
