package com.catovicajdin.expensetracker.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.catovicajdin.expensetracker.data.AppDatabase

@Composable
fun NeedsReviewScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val db = AppDatabase.get(context)
    val items by db.rawNotificationDao().needsReview().collectAsState(initial = emptyList())

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Button(onClick = onBack) { Text("Back") }
        Text("These couldn't be auto-parsed — likely the bank changed its notification wording. Raw text kept below.")
        LazyColumn {
            items(items) { raw ->
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Text(raw.title)
                    Text(raw.body)
                    raw.failureReason?.let { Text("Reason: $it") }
                }
            }
        }
    }
}
