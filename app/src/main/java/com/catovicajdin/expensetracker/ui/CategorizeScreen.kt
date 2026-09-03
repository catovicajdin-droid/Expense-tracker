package com.catovicajdin.expensetracker.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.catovicajdin.expensetracker.data.AppDatabase
import kotlinx.coroutines.launch

@Composable
fun CategorizeScreen(transactionId: Long, onDone: () -> Unit) {
    val context = LocalContext.current
    val db = AppDatabase.get(context)
    val categories by db.categoryDao().all().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Categorize transaction #$transactionId")
        LazyColumn {
            items(categories) { category ->
                TextButton(onClick = {
                    scope.launch {
                        db.transactionDao().assignCategory(transactionId, category.id)
                        onDone()
                    }
                }) {
                    Text(category.name)
                }
            }
        }
    }
}
