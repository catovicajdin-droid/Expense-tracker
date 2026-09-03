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
fun TransactionListScreen(onOpenNeedsReview: () -> Unit) {
    val context = LocalContext.current
    val db = AppDatabase.get(context)
    val transactions by db.transactionDao().all().collectAsState(initial = emptyList())

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Button(onClick = onOpenNeedsReview) { Text("Needs review") }
        LazyColumn {
            items(transactions) { tx ->
                Text("${tx.amount} ${tx.currency} — balance ${tx.availableBalance}")
            }
        }
    }
}
