package com.catovicajdin.expensetracker.ui.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.background
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

data class DonutEntry(val label: String, val amount: Double, val colorHex: String)

/**
 * A ring chart with a hero total in the center, plus a legend list below (name, swatch, amount,
 * percentage) - identity is never color-alone since every segment is also direct-labeled in the
 * legend, per the dataviz accessibility pass. 2px surface gaps between segments.
 */
@Composable
fun CategoryDonutChart(entries: List<DonutEntry>, modifier: Modifier = Modifier) {
    val total = entries.sumOf { it.amount }

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .size(160.dp)
                .padding(8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.size(160.dp)) {
                val strokeWidth = 26.dp.toPx()
                val gapDegrees = 3f
                var startAngle = -90f
                entries.forEach { entry ->
                    if (total <= 0.0) return@forEach
                    val rawSweep = (entry.amount / total * 360).toFloat()
                    val sweep = (rawSweep - gapDegrees).coerceAtLeast(0f)
                    drawArc(
                        color = parseCategoryColor(entry.colorHex),
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    )
                    startAngle += rawSweep
                }
            }
            Text("%.0f".format(total), style = MaterialTheme.typography.titleLarge)
        }

        entries.forEach { entry ->
            val percent = if (total > 0) (entry.amount / total * 100) else 0.0
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(parseCategoryColor(entry.colorHex)),
                )
                Text(
                    entry.label,
                    modifier = Modifier.padding(start = 8.dp).weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    "%.2f (%.0f%%)".format(entry.amount, percent),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun parseCategoryColor(colorHex: String): Color =
    runCatching { Color(android.graphics.Color.parseColor(colorHex)) }.getOrDefault(Color.Gray)
