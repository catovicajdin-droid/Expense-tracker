package com.catovicajdin.expensetracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.catovicajdin.expensetracker.data.AppDatabase
import com.catovicajdin.expensetracker.data.NotificationRepository
import com.catovicajdin.expensetracker.ui.components.CategoryIconBadge
import com.catovicajdin.expensetracker.ui.components.Divider2
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun NeedsReviewScreen(
    onBack: () -> Unit,
    onResolve: (rawId: Long, prefillCategoryId: Long, prefillPostedAt: Long) -> Unit,
) {
    val context = LocalContext.current
    val db = AppDatabase.get(context)
    val scope = rememberCoroutineScope()
    val items by db.rawNotificationDao().needsReview().collectAsState(initial = emptyList())
    val categories by db.categoryDao().all().collectAsState(initial = emptyList())
    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(20.dp, 16.dp)) {
            TextButton(onClick = onBack, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                Text("← Home", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
            }
            Text("Needs review", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(start = 12.dp))
        }
        Row(modifier = Modifier.fillMaxWidth().padding(20.dp, 0.dp, 20.dp, 12.dp)) {
            Text(
                "Notifications the parser could not read. Assign a category to keep the spend, or dismiss the row.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Divider2()

        if (items.isEmpty()) {
            Text(
                "Nothing to review.",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(20.dp, 40.dp),
            )
        }

        LazyColumn {
            items(items) { raw ->
                Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                    Text(
                        (raw.failureReason ?: "Couldn't be parsed").uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                    Text(
                        raw.body,
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .padding(top = 10.dp)
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(12.dp, 10.dp),
                    )
                    Text(
                        dateFormat.format(raw.postedAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                    Row(modifier = Modifier.padding(top = 10.dp)) {
                        categories.take(4).forEach { category ->
                            OutlinedButton(
                                onClick = { onResolve(raw.id, category.id, raw.postedAt) },
                                shape = MaterialTheme.shapes.medium,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(10.dp, 6.dp),
                                modifier = Modifier.padding(end = 8.dp),
                            ) {
                                CategoryIconBadge(category, size = 18.dp)
                                Text(
                                    category.name,
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(start = 6.dp),
                                )
                            }
                        }
                        TextButton(onClick = {
                            scope.launch { NotificationRepository(db).dismissReview(raw.id) }
                        }) {
                            Text("Dismiss", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
                Divider2()
            }
        }
    }
}
