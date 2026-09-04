package com.catovicajdin.expensetracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.catovicajdin.expensetracker.data.AppDatabase
import com.catovicajdin.expensetracker.data.MonthRange
import com.catovicajdin.expensetracker.data.entity.CategoryBudgetEntity
import com.catovicajdin.expensetracker.data.entity.CategoryEntity
import com.catovicajdin.expensetracker.data.entity.MonthlyBudgetEntity
import com.catovicajdin.expensetracker.data.parseAmountInput
import com.catovicajdin.expensetracker.notifications.BudgetAlerts
import com.catovicajdin.expensetracker.ui.charts.CategoryDonutChart
import com.catovicajdin.expensetracker.ui.charts.DonutEntry
import com.catovicajdin.expensetracker.ui.components.CategoryIconBadge
import com.catovicajdin.expensetracker.ui.components.ModernistCard
import com.catovicajdin.expensetracker.ui.components.SectionLabel
import com.catovicajdin.expensetracker.ui.components.formatAmount
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun BudgetSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val db = AppDatabase.get(context)
    val scope = rememberCoroutineScope()

    var yearMonth by remember { mutableStateOf(MonthRange.current()) }
    val range = remember(yearMonth) { MonthRange.millisRange(yearMonth) }

    val categories by db.categoryDao().all().collectAsState(initial = emptyList())
    val monthlyBudget by db.budgetDao().monthlyBudgetFlow(yearMonth).collectAsState(initial = null)
    val categoryBudgets by db.budgetDao().categoryBudgetsFlow(yearMonth).collectAsState(initial = emptyList())
    val categoryTotals by db.transactionDao().categoryTotals(range.first, range.second).collectAsState(initial = emptyList())
    val totalSpent by db.transactionDao().totalSpent(range.first, range.second).collectAsState(initial = 0.0)

    var suggestedCategoryBudgets by remember { mutableStateOf<Map<Long, Double>>(emptyMap()) }
    LaunchedEffect(yearMonth) {
        val priorMonth = db.budgetDao().mostRecentBudgetedMonth(yearMonth)
        suggestedCategoryBudgets = if (priorMonth != null) {
            db.budgetDao().categoryBudgetsFlow(priorMonth).first().associate { it.categoryId to it.amount }
        } else {
            emptyMap()
        }
    }

    val spentByCategory = remember(categoryTotals) { categoryTotals.associate { it.categoryId to it.total } }
    val budgetByCategory = remember(categoryBudgets) { categoryBudgets.associate { it.categoryId to it.amount } }
    val sortedTotals = remember(categoryTotals) { categoryTotals.sortedByDescending { it.total } }

    Column(modifier = Modifier.fillMaxSize().padding(14.dp, 16.dp, 14.dp, 0.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp, 2.dp)) {
            TextButton(onClick = onBack, contentPadding = PaddingValues(0.dp)) {
                Text("← Budget", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("Edit budgets", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(start = 14.dp))
        }

        LazyColumn(modifier = Modifier.weight(1f).padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                ModernistCard {
                    MonthSelector(
                        yearMonth = yearMonth,
                        onPrevious = { yearMonth = MonthRange.previous(yearMonth) },
                        onNext = { yearMonth = MonthRange.next(yearMonth) },
                    )
                }
            }

            item {
                OverallBudgetCard(
                    budget = monthlyBudget?.totalBudget,
                    spent = totalSpent,
                    onSave = { amount ->
                        scope.launch {
                            db.budgetDao().setMonthlyBudget(MonthlyBudgetEntity(yearMonth, amount))
                            BudgetAlerts.checkOverall(context)
                        }
                    },
                )
            }

            item {
                ModernistCard(contentPadding = PaddingValues(0.dp)) {
                    SectionLabel("Category budgets", modifier = Modifier.padding(20.dp, 18.dp, 20.dp, 4.dp))
                    categories.forEach { category ->
                        CategoryBudgetRow(
                            category = category,
                            budget = budgetByCategory[category.id],
                            spent = spentByCategory[category.id] ?: 0.0,
                            suggestion = suggestedCategoryBudgets[category.id],
                            onSave = { amount ->
                                scope.launch {
                                    db.budgetDao().setCategoryBudget(CategoryBudgetEntity(yearMonth, category.id, amount))
                                    BudgetAlerts.checkCategory(context, category.id)
                                }
                            },
                        )
                    }
                    Box(modifier = Modifier.height(8.dp))
                }
            }

            item {
                if (sortedTotals.isNotEmpty()) {
                    ModernistCard {
                        SectionLabel("Where your money goes")
                        val donutEntries = sortedTotals.map { entry ->
                            val category = categories.find { it.id == entry.categoryId }
                            DonutEntry(
                                label = category?.name ?: "Uncategorized",
                                amount = entry.total,
                                colorHex = category?.colorHex ?: "#9E9E9E",
                            )
                        }
                        CategoryDonutChart(entries = donutEntries, modifier = Modifier.padding(top = 14.dp))
                    }
                }
            }
            item { Box(modifier = Modifier.height(4.dp)) }
        }
    }
}

@Composable
private fun MonthSelector(yearMonth: String, onPrevious: () -> Unit, onNext: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth(),
    ) {
        TextButton(onClick = onPrevious, contentPadding = PaddingValues(0.dp)) {
            Text("‹ Prev", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(MonthRange.displayLabel(yearMonth), style = MaterialTheme.typography.titleMedium)
        TextButton(onClick = onNext, contentPadding = PaddingValues(0.dp)) {
            Text("Next ›", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private val fieldShape = RoundedCornerShape(10.dp)

@Composable
private fun amountFieldColors() = TextFieldDefaults.colors(
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    unfocusedIndicatorColor = Color.Transparent,
    focusedIndicatorColor = Color.Transparent,
)

@Composable
private fun OverallBudgetCard(budget: Double?, spent: Double, onSave: (Double) -> Unit) {
    var text by remember(budget) { mutableStateOf(budget?.toString() ?: "") }

    ModernistCard {
        SectionLabel("Overall budget")
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 12.dp)) {
            TextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("Monthly budget") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = amountFieldColors(),
                shape = fieldShape,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = { parseAmountInput(text)?.let(onSave) }, modifier = Modifier.padding(start = 8.dp)) { Text("Save") }
        }
        if (budget != null && budget > 0.0) {
            val percent = (spent / budget * 100).toInt()
            Text(
                "Spent ${formatAmount(spent)} of ${formatAmount(budget)} ($percent%)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp),
            )
            ProgressBar(fraction = (spent / budget).coerceIn(0.0, 1.0).toFloat(), modifier = Modifier.padding(top = 8.dp))
        } else {
            Text(
                "Spent so far: ${formatAmount(spent)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}

@Composable
private fun CategoryBudgetRow(
    category: CategoryEntity,
    budget: Double?,
    spent: Double,
    suggestion: Double?,
    onSave: (Double) -> Unit,
) {
    var text by remember(budget) { mutableStateOf(budget?.toString() ?: "") }

    Column(modifier = Modifier.fillMaxWidth().padding(20.dp, 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CategoryIconBadge(category, size = 30.dp)
            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                Text(category.name, style = MaterialTheme.typography.bodyLarge)
                val statusText = if (budget != null && budget > 0.0) {
                    "${formatAmount(spent)} / ${formatAmount(budget)} (${(spent / budget * 100).toInt()}%)"
                } else {
                    "Spent so far: ${formatAmount(spent)}"
                }
                Text(statusText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (budget != null && budget > 0.0) {
            ProgressBar(fraction = (spent / budget).coerceIn(0.0, 1.0).toFloat(), modifier = Modifier.padding(top = 8.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
            TextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("Budget") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = amountFieldColors(),
                shape = fieldShape,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = { parseAmountInput(text)?.let(onSave) }, modifier = Modifier.padding(start = 8.dp)) { Text("Save") }
        }
        if (budget == null && suggestion != null) {
            TextButton(onClick = { text = suggestion.toString(); onSave(suggestion) }, contentPadding = PaddingValues(0.dp), modifier = Modifier.padding(top = 4.dp)) {
                Text("Use last month's: ${formatAmount(suggestion)}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}

@Composable
private fun ProgressBar(fraction: Float, modifier: Modifier = Modifier) {
    val pill = RoundedCornerShape(percent = 50)
    Box(modifier = modifier.fillMaxWidth().height(8.dp).background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.07f), pill)) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(8.dp)
                .background(MaterialTheme.colorScheme.onBackground, pill),
        )
    }
}
