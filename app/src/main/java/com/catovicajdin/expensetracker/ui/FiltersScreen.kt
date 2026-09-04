package com.catovicajdin.expensetracker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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

    Column(
        modifier = Modifier.fillMaxSize().padding(14.dp, 16.dp, 14.dp, 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp, 2.dp)) {
            TextButton(onClick = onBack, contentPadding = PaddingValues(0.dp)) {
                Text("← Ledger", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("Filters", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(start = 14.dp))
        }

        FilterBar(
            categories = categories,
            tags = tags,
            filter = filter,
            onFilterChange = onFilterChange,
            modifier = Modifier.weight(1f),
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onShowResults,
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onBackground, contentColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Show results") }
            Button(
                onClick = { onFilterChange(TransactionFilter()) },
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Clear") }
        }
    }
}
