package com.catovicajdin.expensetracker.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import com.catovicajdin.expensetracker.data.AppDatabase
import com.catovicajdin.expensetracker.data.entity.CategoryEntity
import kotlinx.coroutines.launch

@Composable
fun CategorizeScreen(transactionId: Long, onDone: () -> Unit) {
    val context = LocalContext.current
    val db = AppDatabase.get(context)
    val categories by db.categoryDao().all().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var suggested by remember { mutableStateOf<CategoryEntity?>(null) }

    LaunchedEffect(transactionId) {
        val transaction = db.transactionDao().byId(transactionId)
        val suggestedId = transaction?.let { db.transactionDao().suggestedCategoryForAmount(it.amount) }
        suggested = suggestedId?.let { db.categoryDao().byId(it) }
    }

    fun assign(categoryId: Long) {
        scope.launch {
            db.transactionDao().assignCategory(transactionId, categoryId)
            onDone()
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Categorize transaction")

        suggested?.let { category ->
            Text("Suggested — same amount matched before:", modifier = Modifier.padding(top = 16.dp, bottom = 4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { assign(category.id) }
                    .padding(vertical = 8.dp),
            ) {
                CategoryAvatar(category)
                Text(category.name, modifier = Modifier.padding(start = 12.dp))
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
        }

        LazyColumn {
            items(categories) { category ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { assign(category.id) }
                        .padding(vertical = 8.dp),
                ) {
                    CategoryAvatar(category)
                    Text(category.name, modifier = Modifier.padding(start = 12.dp))
                }
            }
        }
    }
}
