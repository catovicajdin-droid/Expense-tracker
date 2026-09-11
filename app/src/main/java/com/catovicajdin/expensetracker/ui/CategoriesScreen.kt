package com.catovicajdin.expensetracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.catovicajdin.expensetracker.data.AppDatabase
import com.catovicajdin.expensetracker.data.CategoryColors
import com.catovicajdin.expensetracker.data.entity.CategoryEntity
import com.catovicajdin.expensetracker.ui.components.CategoryIconBadge
import com.catovicajdin.expensetracker.ui.components.Divider2
import com.catovicajdin.expensetracker.ui.components.ModernistCard
import com.catovicajdin.expensetracker.ui.components.SectionLabel
import kotlinx.coroutines.launch

@Composable
fun CategoriesScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val db = AppDatabase.get(context)
    val scope = rememberCoroutineScope()

    val categories by db.categoryDao().all().collectAsState(initial = emptyList())
    // Non-null while the editor is open; NewCategory (id 0) means "adding" rather than editing.
    var editingCategory by remember { mutableStateOf<CategoryEntity?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().padding(14.dp, 16.dp, 14.dp, 0.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth().padding(8.dp, 2.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack, contentPadding = PaddingValues(0.dp)) {
                    Text("← Budget", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("Categories", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(start = 14.dp))
            }
            TextButton(onClick = { editingCategory = NewCategory }, contentPadding = PaddingValues(0.dp)) {
                Text("+ Add", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
            }
        }

        ModernistCard(modifier = Modifier.weight(1f), contentPadding = PaddingValues(0.dp)) {
            LazyColumn {
                itemsIndexed(categories) { index, category ->
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { editingCategory = category }
                                .padding(20.dp, 14.dp),
                        ) {
                            CategoryIconBadge(category, size = 32.dp)
                            Text(
                                category.name,
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f).padding(start = 12.dp),
                            )
                            Text(
                                "Edit",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (index < categories.lastIndex) Divider2()
                    }
                }
            }
        }
        Box(modifier = Modifier.height(12.dp))
    }

    editingCategory?.let { target ->
        val isNew = target.id == 0L
        CategoryEditorDialog(
            category = target,
            existingNames = categories.filter { it.id != target.id }.map { it.name },
            canDelete = !isNew,
            onDismiss = { editingCategory = null },
            onSave = { name, icon, colorHex ->
                scope.launch {
                    if (isNew) {
                        db.categoryDao().create(name, icon, colorHex)
                    } else {
                        db.categoryDao().updateDetails(target.id, name, icon, colorHex)
                    }
                }
                editingCategory = null
            },
            onDelete = {
                scope.launch { db.categoryDao().deleteAndDetach(target.id) }
                editingCategory = null
            },
        )
    }
}

/** Sentinel passed to the editor for "add a category" - id 0 is what Room treats as unassigned. */
private val NewCategory = CategoryEntity(name = "")

private val fieldShape = RoundedCornerShape(10.dp)

@Composable
private fun editorFieldColors() = TextFieldDefaults.colors(
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    unfocusedIndicatorColor = Color.Transparent,
    focusedIndicatorColor = Color.Transparent,
)

@Composable
private fun CategoryEditorDialog(
    category: CategoryEntity,
    existingNames: List<String>,
    canDelete: Boolean,
    onDismiss: () -> Unit,
    onSave: (name: String, icon: String, colorHex: String) -> Unit,
    onDelete: () -> Unit,
) {
    var name by remember { mutableStateOf(category.name) }
    var icon by remember { mutableStateOf(category.icon) }
    var colorHex by remember { mutableStateOf(category.colorHex) }
    var confirmingDelete by remember { mutableStateOf(false) }

    val trimmedName = name.trim()
    val duplicate = existingNames.any { it.equals(trimmedName, ignoreCase = true) }
    val canSave = trimmedName.isNotEmpty() && !duplicate

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (canDelete) "Edit category" else "New category") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Preview of exactly what the badge will look like everywhere else in the app.
                    CategoryIconBadge(
                        category.copy(icon = icon.ifBlank { CategoryEntity.DEFAULT_ICON }, colorHex = colorHex),
                        size = 44.dp,
                    )
                    TextField(
                        value = name,
                        onValueChange = { name = it },
                        placeholder = { Text("Name") },
                        singleLine = true,
                        colors = editorFieldColors(),
                        shape = fieldShape,
                        modifier = Modifier.weight(1f).padding(start = 12.dp),
                    )
                }
                if (duplicate) {
                    Text(
                        "A category called \"$trimmedName\" already exists.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }

                SectionLabel("Icon", modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
                TextField(
                    value = icon,
                    onValueChange = { input ->
                        // One glyph only - an emoji can be several chars, so cap by code points and
                        // keep the last one typed rather than rejecting the edit outright.
                        icon = input.takeIf { it.codePointCount(0, it.length) <= 1 }
                            ?: input.substring(input.offsetByCodePoints(0, input.codePointCount(0, input.length) - 1))
                    },
                    placeholder = { Text(CategoryEntity.DEFAULT_ICON) },
                    singleLine = true,
                    colors = editorFieldColors(),
                    shape = fieldShape,
                    modifier = Modifier.fillMaxWidth(),
                )

                SectionLabel("Color", modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
                LazyRow {
                    items(CategoryColors.byName.values.distinct()) { swatch ->
                        val selected = swatch.equals(colorHex, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(36.dp)
                                .background(
                                    runCatching { Color(android.graphics.Color.parseColor(swatch)) }.getOrDefault(Color.Gray),
                                    RoundedCornerShape(10.dp),
                                )
                                .clickable { colorHex = swatch },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (selected) {
                                Text("✓", color = Color.White, style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }

                if (canDelete) {
                    TextButton(
                        onClick = { confirmingDelete = true },
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.padding(top = 16.dp),
                    ) {
                        Text("Delete category", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = canSave,
                onClick = { onSave(trimmedName, icon.ifBlank { CategoryEntity.DEFAULT_ICON }, colorHex) },
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )

    if (confirmingDelete) {
        AlertDialog(
            onDismissRequest = { confirmingDelete = false },
            title = { Text("Delete \"${category.name}\"?") },
            text = {
                Text(
                    "Transactions in this category are kept, but become uncategorized. Its budgets " +
                        "are removed. This can't be undone.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmingDelete = false
                    onDelete()
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmingDelete = false }) { Text("Cancel") } },
        )
    }
}
