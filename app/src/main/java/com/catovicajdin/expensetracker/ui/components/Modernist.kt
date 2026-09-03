package com.catovicajdin.expensetracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
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
import com.catovicajdin.expensetracker.ui.theme.blendWithInk

/** A 2px hairline rule, matching the design's `border-bottom:2px solid var(--color-divider)`. */
@Composable
fun Divider2() {
    HorizontalDivider(thickness = 2.dp, color = MaterialTheme.colorScheme.outline)
}

/** Small uppercase bold label, e.g. section headers ("TAGS", "CATEGORY"). */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Text(text.uppercase(), style = MaterialTheme.typography.labelMedium, color = color, modifier = modifier)
}

/** Mirrors the design's `color-mix(in srgb, {{ color }} 55%, #201e1d)` used on every category dot/fill. */
fun blendCategoryDot(category: CategoryEntity?): Color {
    val base = category?.colorHex
        ?.let { runCatching { Color(android.graphics.Color.parseColor(it)) }.getOrNull() }
        ?: Color(0xFF9E9E9E)
    return blendWithInk(base)
}

/** The small colored square used everywhere a category is referenced in a compact row. */
@Composable
fun CategoryDot(category: CategoryEntity?, modifier: Modifier = Modifier, size: Dp = 8.dp) {
    Box(modifier = modifier.size(size).background(blendCategoryDot(category)))
}

/** A tinted square carrying the category's emoji glyph - used where there's room for more than a dot (grid cells, pickers, the detail hero row). Always paired with the category name nearby, never standing in for it. */
@Composable
fun CategoryIconBadge(category: CategoryEntity?, modifier: Modifier = Modifier, size: Dp = 32.dp) {
    val base = category?.colorHex
        ?.let { runCatching { Color(android.graphics.Color.parseColor(it)) }.getOrNull() }
        ?: Color(0xFF9E9E9E)
    Box(
        modifier = modifier.size(size).background(base.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(CategoryIcons.iconFor(category?.name ?: ""), fontSize = (size.value * 0.5f).sp)
    }
}

fun formatAmount(amount: Double): String = "%.2f".format(amount)

/** Maps a raw_notifications.packageName to the short label the design shows next to a transaction. */
fun sourceLabel(packageName: String): String = when (packageName) {
    "manual" -> "Manual"
    com.catovicajdin.expensetracker.Constants.INTESA_PACKAGE_NAME -> "Intesa"
    else -> packageName
}
