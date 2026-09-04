package com.catovicajdin.expensetracker.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.unit.dp
import com.catovicajdin.expensetracker.data.entity.TagEntity
import com.catovicajdin.expensetracker.ui.components.SectionLabel

/** Reusable tag picker used by both the Add and Edit transaction dialogs: existing tags as toggleable chips, plus a comma-separated field for new ones. Owns the delete-confirmation prompt so callers don't have to. */
@Composable
fun TagSelector(
    allTags: List<TagEntity>,
    selectedTagIds: Set<Long>,
    onToggle: (Long) -> Unit,
    onDeleteTag: (TagEntity) -> Unit,
    newTagText: String,
    onNewTagTextChange: (String) -> Unit,
) {
    var tagPendingDelete by remember { mutableStateOf<TagEntity?>(null) }

    SectionLabel("Tags", modifier = Modifier.padding(bottom = 12.dp))
    LazyRow {
        items(allTags) { tag ->
            TagChip(
                tag = tag,
                selected = selectedTagIds.contains(tag.id),
                onToggle = { onToggle(tag.id) },
                onDelete = { tagPendingDelete = tag },
                modifier = Modifier.padding(end = 8.dp),
            )
        }
    }
    TextField(
        value = newTagText,
        onValueChange = onNewTagTextChange,
        placeholder = { Text("New tag, comma separated") },
        colors = TextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedIndicatorColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
        ),
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
    )

    tagPendingDelete?.let { tag ->
        AlertDialog(
            onDismissRequest = { tagPendingDelete = null },
            title = { Text("Delete tag \"${tag.name}\"?") },
            text = { Text("This removes it from every transaction that has it. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteTag(tag)
                    tagPendingDelete = null
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { tagPendingDelete = null }) { Text("Cancel") } },
        )
    }
}

/** Splits "a, b, ,c" into ["a", "b", "c"] - blanks and surrounding whitespace dropped. Every tag is
 * displayed with a leading "#" already, so a stray one typed here would double up - strip it. */
fun parseTagNames(text: String): List<String> =
    text.split(",").map { it.trim().trimStart('#').trim() }.filter { it.isNotEmpty() }
