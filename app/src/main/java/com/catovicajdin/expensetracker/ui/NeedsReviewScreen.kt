package com.catovicajdin.expensetracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.catovicajdin.expensetracker.data.AppDatabase
import com.catovicajdin.expensetracker.data.NotificationRepository
import com.catovicajdin.expensetracker.data.entity.RawNotificationEntity
import com.catovicajdin.expensetracker.notifications.BudgetAlerts
import com.catovicajdin.expensetracker.ui.components.ModernistCard
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun NeedsReviewScreen(
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val db = AppDatabase.get(context)
    val scope = rememberCoroutineScope()
    val items by db.rawNotificationDao().needsReview().collectAsState(initial = emptyList())
    val categories by db.categoryDao().all().collectAsState(initial = emptyList())
    val tags by db.tagDao().all().collectAsState(initial = emptyList())
    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }
    var reviewingRaw by remember { mutableStateOf<RawNotificationEntity?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().padding(14.dp, 16.dp, 14.dp, 0.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(modifier = Modifier.padding(8.dp, 2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack, contentPadding = PaddingValues(0.dp)) {
                    Text("← Home", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("Needs review", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(start = 14.dp))
            }
            Text(
                "Notifications the parser could not read. Assign a category to keep the spend, or dismiss the row.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        if (items.isEmpty()) {
            ModernistCard {
                Text("Nothing to review.", style = MaterialTheme.typography.bodyLarge)
            }
        }

        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            items(items) { raw ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                ) {
                    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                        Box(modifier = Modifier.width(3.dp).fillMaxHeight().background(MaterialTheme.colorScheme.secondary))
                        Column(modifier = Modifier.weight(1f).padding(20.dp)) {
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
                                    .padding(top = 12.dp)
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.small)
                                    .padding(14.dp, 12.dp),
                            )
                            Text(
                                dateFormat.format(raw.postedAt),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 10.dp),
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Button(
                                    onClick = { reviewingRaw = raw },
                                    shape = MaterialTheme.shapes.medium,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.onBackground,
                                        contentColor = MaterialTheme.colorScheme.surface,
                                    ),
                                    modifier = Modifier.weight(1f),
                                ) { Text("Accept") }
                                Button(
                                    onClick = {
                                        scope.launch { NotificationRepository(db).dismissReview(raw.id) }
                                    },
                                    shape = MaterialTheme.shapes.medium,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = MaterialTheme.colorScheme.secondary,
                                    ),
                                    modifier = Modifier.weight(1f),
                                ) { Text("Deny") }
                            }
                        }
                    }
                }
            }
        }
        Box(modifier = Modifier.height(12.dp))
    }

    reviewingRaw?.let { raw ->
        ResolveReviewDialog(
            categories = categories,
            allTags = tags,
            onDismiss = { reviewingRaw = null },
            onDeleteTag = { tag -> scope.launch { db.tagDao().delete(tag.id) } },
            onAccept = { categoryId, amount, tagIds, newTagNames ->
                scope.launch {
                    val newTagIds = newTagNames.map { db.tagDao().getOrCreate(it) }
                    val transactionId = NotificationRepository(db).resolveReview(raw.id, categoryId, amount, raw.postedAt)
                    db.tagDao().replaceTagsForTransaction(transactionId, tagIds + newTagIds)
                    categoryId?.let { BudgetAlerts.checkCategory(context, it) }
                    BudgetAlerts.checkOverall(context)
                }
                reviewingRaw = null
            },
        )
    }
}
