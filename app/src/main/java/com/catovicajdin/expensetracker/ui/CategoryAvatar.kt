package com.catovicajdin.expensetracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.catovicajdin.expensetracker.data.entity.CategoryEntity

@Composable
fun CategoryAvatar(category: CategoryEntity?, modifier: Modifier = Modifier) {
    val color = category?.colorHex
        ?.let { runCatching { Color(android.graphics.Color.parseColor(it)) }.getOrNull() }
        ?: Color(0xFF9E9E9E)
    val initial = category?.name?.firstOrNull()?.uppercaseChar()?.toString() ?: "?"

    Box(
        modifier = modifier.size(40.dp).background(color, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = initial, color = Color.White)
    }
}
