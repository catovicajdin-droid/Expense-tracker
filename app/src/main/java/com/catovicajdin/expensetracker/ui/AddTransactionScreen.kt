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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.catovicajdin.expensetracker.data.AppDatabase
import com.catovicajdin.expensetracker.data.NotificationRepository
import com.catovicajdin.expensetracker.data.entity.CategoryEntity
import com.catovicajdin.expensetracker.data.parseAmountInput
import com.catovicajdin.expensetracker.notifications.BudgetAlerts
import com.catovicajdin.expensetracker.ui.components.CategoryIconBadge
import com.catovicajdin.expensetracker.ui.components.ModernistCard
import com.catovicajdin.expensetracker.ui.components.SectionLabel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    prefillCategoryId: Long?,
    prefillPostedAt: Long?,
    resolvingRawId: Long?,
    onCancel: () -> Unit,
    onSaved: () -> Unit,
) {
    val context = LocalContext.current
    val db = AppDatabase.get(context)
    val scope = rememberCoroutineScope()

    val categories by db.categoryDao().all().collectAsState(initial = emptyList())
    val tags by db.tagDao().all().collectAsState(initial = emptyList())

    var amountText by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf(prefillCategoryId) }
    var postedAt by remember { mutableStateOf(prefillPostedAt ?: System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedTagIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var newTagText by remember { mutableStateOf("") }
    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()) }

    fun save() {
        val amount = parseAmountInput(amountText) ?: return
        scope.launch {
            val newTagIds = parseTagNames(newTagText).map { db.tagDao().getOrCreate(it) }
            val transactionId = if (resolvingRawId != null) {
                NotificationRepository(db).resolveReview(resolvingRawId, selectedCategoryId, amount, postedAt)
            } else {
                NotificationRepository(db).insertManual(amount, selectedCategoryId, postedAt)
            }
            db.tagDao().replaceTagsForTransaction(transactionId, selectedTagIds + newTagIds)
            selectedCategoryId?.let { BudgetAlerts.checkCategory(context, it) }
            BudgetAlerts.checkOverall(context)
            onSaved()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(14.dp, 16.dp, 14.dp, 0.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp, 2.dp)) {
            TextButton(onClick = onCancel, contentPadding = PaddingValues(0.dp)) {
                Text("← Cancel", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                if (resolvingRawId != null) "Resolve transaction" else "Add transaction",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(start = 14.dp),
            )
        }

        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            ModernistCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    SectionLabel("Amount · BAM")
                    Text(dateFormat.format(postedAt), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.clickable { showDatePicker = true })
                }
                TextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    placeholder = { Text("0.00") },
                    textStyle = MaterialTheme.typography.headlineLarge,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.outline,
                        focusedIndicatorColor = MaterialTheme.colorScheme.secondary,
                    ),
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                )
                Text(
                    "Cash and anything the bank never pushes",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }

            ModernistCard(contentPadding = PaddingValues(0.dp, 18.dp, 0.dp, 8.dp)) {
                SectionLabel("Category", modifier = Modifier.padding(start = 20.dp, bottom = 4.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxWidth().height((((categories.size + 1) / 2) * 60).dp),
                ) {
                    items(categories) { category ->
                        CategoryPickButton(
                            category = category,
                            selected = selectedCategoryId == category.id,
                            onClick = { selectedCategoryId = category.id },
                        )
                    }
                }
            }

            ModernistCard {
                TagSelector(
                    allTags = tags,
                    selectedTagIds = selectedTagIds,
                    onToggle = { tagId ->
                        selectedTagIds = if (selectedTagIds.contains(tagId)) selectedTagIds - tagId else selectedTagIds + tagId
                    },
                    onDeleteTag = { tag ->
                        selectedTagIds = selectedTagIds - tag.id
                        scope.launch { db.tagDao().delete(tag.id) }
                    },
                    newTagText = newTagText,
                    onNewTagTextChange = { newTagText = it },
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ModernistCard(contentPadding = PaddingValues(20.dp, 14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        categories.find { it.id == selectedCategoryId }?.name ?: "Pick a category",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        amountText.ifBlank { "0.00" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Button(
                onClick = { save() },
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onBackground, contentColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Save transaction") }
        }
        Box(modifier = Modifier.height(12.dp))
    }

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

@Composable
private fun CategoryPickButton(category: CategoryEntity, selected: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(if (selected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
            .padding(18.dp, 14.dp),
    ) {
        CategoryIconBadge(category, size = 26.dp)
        Text(
            category.name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.padding(start = 10.dp),
        )
    }
}

/** DatePicker's selectedDateMillis is UTC-midnight of the picked day; keep the current time-of-day when applying it. */
private fun combineDateWithCurrentTime(pickedUtcMillis: Long): Long {
    val pickedDate = Instant.ofEpochMilli(pickedUtcMillis).atZone(ZoneOffset.UTC).toLocalDate()
    return pickedDate.atTime(LocalTime.now()).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
}
