package com.catovicajdin.expensetracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.catovicajdin.expensetracker.data.entity.TagEntity

/** onDelete null hides the delete affordance (e.g. in a read-only filter context). */
@Composable
fun TagChip(
    tag: TagEntity,
    selected: Boolean,
    onToggle: () -> Unit,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val background = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val content = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .padding(end = 8.dp)
            .background(background)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(tag.name, color = content, modifier = Modifier.clickable { onToggle() })
        if (onDelete != null) {
            Text(" ×", color = content, modifier = Modifier.padding(start = 6.dp).clickable { onDelete() })
        }
    }
}
