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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.catovicajdin.expensetracker.data.AppDatabase
import com.catovicajdin.expensetracker.data.MonthRange
import com.catovicajdin.expensetracker.data.entity.CategoryEntity
import com.catovicajdin.expensetracker.ui.components.CategoryIconBadge
import com.catovicajdin.expensetracker.ui.components.ModernistCard
import com.catovicajdin.expensetracker.ui.components.categoryColor
import com.catovicajdin.expensetracker.ui.components.formatAmount
import com.catovicajdin.expensetracker.ui.theme.Accent800

@Composable
fun BudgetDashboardScreen(
    onBack: () -> Unit,
    onOpenCategoryLedger: (Long) -> Unit,
    onOpenLedger: () -> Unit,
    onOpenAdd: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val context = LocalContext.current
    val db = AppDatabase.get(context)
    val yearMonth = remember { MonthRange.current() }
    val range = remember { MonthRange.millisRange(yearMonth) }

    val categories by db.categoryDao().all().collectAsState(initial = emptyList())
    val monthlyBudget by db.budgetDao().monthlyBudgetFlow(yearMonth).collectAsState(initial = null)
    val categoryBudgets by db.budgetDao().categoryBudgetsFlow(yearMonth).collectAsState(initial = emptyList())
    val categoryTotals by db.transactionDao().categoryTotals(range.first, range.second).collectAsState(initial = emptyList())
    val totalSpent by db.transactionDao().totalSpent(range.first, range.second).collectAsState(initial = 0.0)

    val budgetByCategory = remember(categoryBudgets) { categoryBudgets.associate { it.categoryId to it.amount } }
    val spentByCategory = remember(categoryTotals) { categoryTotals.associate { it.categoryId to it.total } }
    var sortOption by remember { mutableStateOf(BudgetSort.PERCENT_SPENT) }
    val cells = remember(categories, budgetByCategory, spentByCategory, sortOption) {
        val unsorted = categories.map { category ->
            BudgetCell(
                category = category,
                spent = spentByCategory[category.id] ?: 0.0,
                budget = budgetByCategory[category.id],
            )
        }
        when (sortOption) {
            BudgetSort.BUDGET_SIZE -> unsorted.sortedByDescending { it.budget ?: Double.NEGATIVE_INFINITY }
            BudgetSort.AMOUNT_SPENT -> unsorted.sortedByDescending { it.spent }
            BudgetSort.PERCENT_SPENT -> unsorted.sortedByDescending {
                val budget = it.budget
                if (budget != null && budget > 0.0) it.spent / budget else Double.NEGATIVE_INFINITY
            }
        }
    }
    val overCount = cells.count { it.budget != null && it.spent > it.budget }

    Column(
        modifier = Modifier.fillMaxSize().padding(14.dp, 16.dp, 14.dp, 0.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth().padding(8.dp, 2.dp),
        ) {
            TextButton(onClick = onBack, contentPadding = PaddingValues(0.dp)) {
                Text("← Home", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("Budget", style = MaterialTheme.typography.headlineMedium)
            TextButton(onClick = onOpenSettings, contentPadding = PaddingValues(0.dp)) {
                Text("Edit", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        val budget = monthlyBudget?.totalBudget
        ModernistCard(contentPadding = PaddingValues(20.dp, 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                val pctText = if (budget != null && budget > 0.0) "${(totalSpent / budget * 100).toInt()}% of ${formatAmount(budget)}" else "No overall budget set"
                Text(pctText, style = MaterialTheme.typography.bodyLarge)
                Text("$overCount over budget", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
            }
        }

        var sortMenuExpanded by remember { mutableStateOf(false) }
        Box(modifier = Modifier.padding(8.dp, 0.dp)) {
            TextButton(onClick = { sortMenuExpanded = true }, contentPadding = PaddingValues(0.dp)) {
                Text(
                    "Sort: ${sortOption.label} ▾",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            DropdownMenu(expanded = sortMenuExpanded, onDismissRequest = { sortMenuExpanded = false }) {
                BudgetSort.values().forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.label) },
                        onClick = {
                            sortOption = option
                            sortMenuExpanded = false
                        },
                    )
                }
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(cells) { cell ->
                BudgetCellView(cell = cell, onClick = { onOpenCategoryLedger(cell.category.id) })
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onOpenLedger,
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onBackground, contentColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.weight(1f),
            ) { Text("Ledger") }
            Button(
                onClick = onOpenAdd,
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp),
                modifier = Modifier.weight(1f),
            ) { Text("+ Add") }
        }
        Box(modifier = Modifier.height(12.dp))
    }
}

private enum class BudgetSort(val label: String) {
    PERCENT_SPENT("% of budget spent"),
    AMOUNT_SPENT("Amount spent"),
    BUDGET_SIZE("Budget size"),
}

private data class BudgetCell(val category: CategoryEntity, val spent: Double, val budget: Double?)

@Composable
private fun BudgetCellView(cell: BudgetCell, onClick: () -> Unit) {
    val budget = cell.budget
    val over = budget != null && cell.spent > budget
    val fraction = if (budget != null && budget > 0.0) (cell.spent / budget).coerceIn(0.0, 1.0).toFloat() else 0f
    val fillColor = if (over) {
        lerp(Color.White, Accent800, 0.32f)
    } else {
        lerp(Color.White, categoryColor(cell.category), 0.26f)
    }

    Card(
        modifier = Modifier.clickable(onClick = onClick).height(138.dp),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .height(138.dp * fraction)
                    .background(fillColor),
            )
            Column(modifier = Modifier.fillMaxSize().padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CategoryIconBadge(cell.category, size = 26.dp, modifier = Modifier.padding(end = 8.dp))
                    Text(cell.category.name, style = MaterialTheme.typography.bodyLarge)
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Bottom) {
                    Text(formatAmount(cell.spent), style = MaterialTheme.typography.titleLarge)
                    val statusText = when {
                        budget == null -> "no budget set"
                        over -> "${(fraction * 100).toInt()}% · over by ${formatAmount(cell.spent - budget)}"
                        else -> "${(fraction * 100).toInt()}% · ${formatAmount(budget - cell.spent)} left"
                    }
                    Text(
                        statusText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}
