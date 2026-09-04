package com.catovicajdin.expensetracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.catovicajdin.expensetracker.data.CategoryIcons
import com.catovicajdin.expensetracker.data.entity.CategoryEntity

/** A 1px hairline rule, for row separators inside a card - matches the design's soft `rgba(32,30,29,.09)` divider. */
@Composable
fun Divider2() {
    HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline)
}

/** Small uppercase bold label, e.g. section headers ("TAGS", "CATEGORY"). */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Text(text.uppercase(), style = MaterialTheme.typography.labelMedium, color = color, modifier = modifier)
}

/** A white, rounded, softly-shadowed section container - the base unit of every screen's layout now. */
@Composable
fun ModernistCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Column(modifier = Modifier.padding(contentPadding), content = content)
    }
}

fun categoryColor(category: CategoryEntity?): Color =
    category?.colorHex
        ?.let { runCatching { Color(android.graphics.Color.parseColor(it)) }.getOrNull() }
        ?: Color(0xFF9E9E9E)

/** A solid-color rounded square carrying the category's emoji glyph - used everywhere a category is referenced, from compact list rows to grid cells and pickers. Always paired with the category name nearby, never standing in for it. */
@Composable
fun CategoryIconBadge(category: CategoryEntity?, modifier: Modifier = Modifier, size: Dp = 32.dp) {
    Box(
        modifier = modifier.size(size).background(categoryColor(category), RoundedCornerShape(size * 0.3f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(CategoryIcons.iconFor(category?.name ?: ""), fontSize = (size.value * 0.46f).sp)
    }
}

fun formatAmount(amount: Double): String = "%.2f".format(amount)

/** Maps a raw_notifications.packageName to the short label the design shows next to a transaction. */
fun sourceLabel(packageName: String): String = when (packageName) {
    "manual" -> "Manual"
    com.catovicajdin.expensetracker.Constants.INTESA_PACKAGE_NAME -> "Intesa"
    else -> packageName
}
