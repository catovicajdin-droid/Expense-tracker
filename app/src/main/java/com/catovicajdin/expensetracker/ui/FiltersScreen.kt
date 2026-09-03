package com.catovicajdin.expensetracker.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.catovicajdin.expensetracker.data.AppDatabase
import com.catovicajdin.expensetracker.ui.components.Divider2

@Composable
fun FiltersScreen(
    filter: TransactionFilter,
    onFilterChange: (TransactionFilter) -> Unit,
    onBack: () -> Unit,
    onShowResults: () -> Unit,
) {
    val context = LocalContext.current
    val db = AppDatabase.get(context)
    val categories by db.categoryDao().all().collectAsState(initial = emptyList())
    val tags by db.tagDao().all().collectAsState(initial = emptyList())

    Column(modifier = Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(20.dp, 16.dp)) {
            TextButton(onClick = onBack, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                Text(
                    "← Ledger",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            Text("Filters", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(start = 12.dp))
        }
        Divider2()
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            FilterBar(categories = categories, tags = tags, filter = filter, onFilterChange = onFilterChange)
        }
        Divider2()
        Row(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = { onFilterChange(TransactionFilter()) },
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ),
                modifier = Modifier.weight(1f),
            ) { Text("Clear") }
            Button(
                onClick = onShowResults,
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onBackground,
                    contentColor = MaterialTheme.colorScheme.background,
                ),
                modifier = Modifier.weight(1f),
            ) { Text("Show results") }
        }
    }
}
