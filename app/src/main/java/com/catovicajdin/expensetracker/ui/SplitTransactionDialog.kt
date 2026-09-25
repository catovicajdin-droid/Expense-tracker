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
import com.catovicajdin.expensetracker.data.parseAmountInput
import com.catovicajdin.expensetracker.ui.components.SectionLabel
import com.catovicajdin.expensetracker.ui.components.formatAmount

/**
 * Splits one transaction in two. The case it exists for: a cash withdrawal where only part of the
 * money went on the thing you were actually buying, and the rest is still cash in hand.
 *
 * The two amounts always add up to the original - you name what the known expense was, and the
 * remainder is computed rather than typed, so a split can't quietly invent or lose money.
 */
@Composable
fun SplitTransactionDialog(
    total: Double,
    currency: String,
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onSplit: (splitAmount: Double, splitCategoryId: Long?, remainderCategoryId: Long?) -> Unit,
) {
    var amountText by remember { mutableStateOf("") }
    var splitCategoryId by remember { mutableStateOf<Long?>(null) }
    var remainderCategoryId by remember { mutableStateOf<Long?>(null) }

    val splitAmount = parseAmountInput(amountText)
    val remainder = splitAmount?.let { total - it }
    val valid = splitAmount != null && splitAmount > 0.0 && splitAmount < total

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Split ${formatAmount(total)} $currency") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                SectionLabel("This part", modifier = Modifier.padding(bottom = 8.dp))
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
                if (amountText.isNotBlank() && !valid) {
                    Text(
                        "Enter an amount between 0 and ${formatAmount(total)}.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
                CategoryChoiceRow(
                    categories = categories,
                    selectedId = splitCategoryId,
                    onSelect = { splitCategoryId = it },
                )

                SectionLabel(
                    if (remainder != null && valid) "Rest · ${formatAmount(remainder)} $currency" else "Rest",
                    modifier = Modifier.padding(top = 20.dp, bottom = 8.dp),
                )
                Text(
                    "Stays on this transaction - give it the category the leftover cash belongs to.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                CategoryChoiceRow(
                    categories = categories,
                    selectedId = remainderCategoryId,
                    onSelect = { remainderCategoryId = it },
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = { onSplit(splitAmount!!, splitCategoryId, remainderCategoryId) },
            ) { Text("Split") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun CategoryChoiceRow(
    categories: List<CategoryEntity>,
    selectedId: Long?,
    onSelect: (Long?) -> Unit,
) {
    LazyRow(modifier = Modifier.padding(top = 8.dp)) {
        item {
            CategoryOption(
                name = "Uncategorized",
                category = null,
                selected = selectedId == null,
                onClick = { onSelect(null) },
            )
        }
        items(categories) { category ->
            CategoryOption(
                name = category.name,
                category = category,
                selected = selectedId == category.id,
                onClick = { onSelect(category.id) },
            )
        }
    }
}
