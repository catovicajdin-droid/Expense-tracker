package com.catovicajdin.expensetracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.catovicajdin.expensetracker.data.AppDatabase
import com.catovicajdin.expensetracker.data.MonthRange
import com.catovicajdin.expensetracker.data.entity.CategoryEntity
import com.catovicajdin.expensetracker.ui.components.Divider2
import com.catovicajdin.expensetracker.ui.components.blendCategoryDot
import com.catovicajdin.expensetracker.ui.components.formatAmount

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
    val cells = remember(categories, budgetByCategory, spentByCategory) {
        categories.map { category ->
            BudgetCell(
                category = category,
                spent = spentByCategory[category.id] ?: 0.0,
                budget = budgetByCategory[category.id],
            )
        }.sortedByDescending { it.spent }
    }
    val overCount = cells.count { it.budget != null && it.spent > it.budget }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth().padding(20.dp, 16.dp),
        ) {
            TextButton(onClick = onBack, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                Text("← Home", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
            }
            Text("Budget", style = MaterialTheme.typography.headlineMedium)
            TextButton(onClick = onOpenSettings, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                Text("Edit", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
            }
        }
        Divider2()
        val budget = monthlyBudget?.totalBudget
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth().padding(20.dp, 14.dp),
        ) {
            val pctText = if (budget != null && budget > 0.0) "${(totalSpent / budget * 100).toInt()}% of ${formatAmount(budget)}" else "No overall budget set"
            Text(pctText, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                "$overCount over budget",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
        Divider2()
        LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.weight(1f)) {
            items(cells) { cell ->
                BudgetCellView(cell = cell, onClick = { onOpenCategoryLedger(cell.category.id) })
            }
        }
        Divider2()
        Row(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = onOpenLedger,
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onBackground,
                    contentColor = MaterialTheme.colorScheme.background,
                ),
                modifier = Modifier.weight(1f),
            ) { Text("Ledger") }
            Button(
                onClick = onOpenAdd,
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ),
                modifier = Modifier.weight(1f),
            ) { Text("+ Add") }
        }
    }
}

private data class BudgetCell(val category: CategoryEntity, val spent: Double, val budget: Double?)

@Composable
private fun BudgetCellView(cell: BudgetCell, onClick: () -> Unit) {
    val budget = cell.budget
    val over = budget != null && cell.spent > budget
    val fraction = if (budget != null && budget > 0.0) (cell.spent / budget).coerceIn(0.0, 1.0).toFloat() else 0f
    val fillAlpha = if (over) 0.3f else 0.16f

    Box(
        modifier = Modifier
            .clickable(onClick = onClick)
            .height(132.dp)
            .padding(12.dp),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .height((132.dp * fraction))
                .background(MaterialTheme.colorScheme.secondary.copy(alpha = fillAlpha)),
        )
        Column(modifier = Modifier.fillMaxSize()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .padding(end = 6.dp)
                        .height(8.dp)
                        .width(8.dp)
                        .background(blendCategoryDot(cell.category)),
                )
                Text(cell.category.name, style = MaterialTheme.typography.labelLarge)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = androidx.compose.foundation.layout.Arrangement.Bottom) {
                Text(formatAmount(cell.spent), style = MaterialTheme.typography.titleLarge)
                val statusText = when {
                    budget == null -> "no budget set"
                    over -> "${(fraction * 100).toInt()}% · over by ${formatAmount(cell.spent - budget)}"
                    else -> "${(fraction * 100).toInt()}% · ${formatAmount(budget - cell.spent)} left"
                }
                Text(
                    statusText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
