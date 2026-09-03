package com.catovicajdin.expensetracker.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.catovicajdin.expensetracker.data.AppDatabase
import com.catovicajdin.expensetracker.data.entity.CategoryEntity
import com.catovicajdin.expensetracker.notifications.BudgetAlerts
import com.catovicajdin.expensetracker.ui.components.CategoryIconBadge
import com.catovicajdin.expensetracker.ui.components.Divider2
import com.catovicajdin.expensetracker.ui.components.SectionLabel
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
            BudgetAlerts.checkCategory(context, categoryId)
            onDone()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.padding(20.dp, 16.dp)) {
            Text("Categorize", style = MaterialTheme.typography.headlineMedium)
        }
        Divider2()

        suggested?.let { category ->
            SectionLabel("Suggested", modifier = Modifier.padding(20.dp, 14.dp, 20.dp, 6.dp))
            CategoryRow(category = category, onClick = { assign(category.id) })
            Divider2()
        }

        SectionLabel("All categories", modifier = Modifier.padding(20.dp, 14.dp, 20.dp, 6.dp))
        LazyColumn {
            items(categories) { category ->
                CategoryRow(category = category, onClick = { assign(category.id) })
                Divider2()
            }
        }
    }
}

@Composable
private fun CategoryRow(category: CategoryEntity, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(20.dp, 12.dp),
    ) {
        CategoryIconBadge(category, size = 28.dp)
        Text(
            category.name,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}
