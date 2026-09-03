package com.catovicajdin.expensetracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.catovicajdin.expensetracker.data.AppDatabase
import com.catovicajdin.expensetracker.data.MonthRange
import com.catovicajdin.expensetracker.data.entity.CategoryBudgetEntity
import com.catovicajdin.expensetracker.data.entity.CategoryEntity
import com.catovicajdin.expensetracker.data.entity.MonthlyBudgetEntity
import com.catovicajdin.expensetracker.notifications.BudgetAlerts
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private fun Double.formatAmount(): String = "%.2f".format(this)

@Composable
fun BudgetScreen(onBack: () -> Unit) {
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
    val grandTotal = remember(sortedTotals) { sortedTotals.sumOf { it.total } }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        item { TextButton(onClick = onBack) { Text("Back") } }

        item {
            MonthSelector(
                yearMonth = yearMonth,
                onPrevious = { yearMonth = MonthRange.previous(yearMonth) },
                onNext = { yearMonth = MonthRange.next(yearMonth) },
            )
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
            Text(
                "Category budgets",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
            )
        }

        items(categories) { category ->
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
            HorizontalDivider()
        }

        item {
            Text(
                "Where your money goes",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
            )
        }

        items(sortedTotals) { entry ->
            val category = categories.find { it.id == entry.categoryId }
            val percent = if (grandTotal > 0) (entry.total / grandTotal * 100) else 0.0
            SpendBreakdownRow(category = category, amount = entry.total, percent = percent)
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
        TextButton(onClick = onPrevious) { Text("< Prev") }
        Text(MonthRange.displayLabel(yearMonth), style = MaterialTheme.typography.titleMedium)
        TextButton(onClick = onNext) { Text("Next >") }
    }
}

@Composable
private fun OverallBudgetCard(budget: Double?, spent: Double, onSave: (Double) -> Unit) {
    var text by remember(budget) { mutableStateOf(budget?.toString() ?: "") }

    Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
        Text("Overall budget", style = MaterialTheme.typography.titleMedium)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Monthly budget") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = { text.toDoubleOrNull()?.let(onSave) }) { Text("Save") }
        }
        if (budget != null && budget > 0.0) {
            val percent = (spent / budget * 100).toInt()
            Text("Spent ${spent.formatAmount()} of ${budget.formatAmount()} ($percent%)", modifier = Modifier.padding(top = 8.dp))
            ProgressBar(fraction = (spent / budget).coerceIn(0.0, 1.0).toFloat())
        } else {
            Text("Spent so far: ${spent.formatAmount()}", modifier = Modifier.padding(top = 8.dp))
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

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CategoryAvatar(category)
            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                Text(category.name)
                if (budget != null && budget > 0.0) {
                    val percent = (spent / budget * 100).toInt()
                    Text("${spent.formatAmount()} / ${budget.formatAmount()} ($percent%)")
                    ProgressBar(fraction = (spent / budget).coerceIn(0.0, 1.0).toFloat())
                } else {
                    Text("Spent so far: ${spent.formatAmount()}")
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Budget") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = { text.toDoubleOrNull()?.let(onSave) }) { Text("Save") }
        }
        if (budget == null && suggestion != null) {
            TextButton(onClick = {
                text = suggestion.toString()
                onSave(suggestion)
            }) {
                Text("Use last month's: ${suggestion.formatAmount()}")
            }
        }
    }
}

@Composable
private fun SpendBreakdownRow(category: CategoryEntity?, amount: Double, percent: Double) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
    ) {
        CategoryAvatar(category)
        Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
            Text(category?.name ?: "Uncategorized")
            ProgressBar(fraction = (percent / 100.0).toFloat())
        }
        Text("${amount.formatAmount()} (${percent.toInt()}%)")
    }
}

@Composable
private fun ProgressBar(fraction: Float, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(8.dp)
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp)),
        )
    }
}
