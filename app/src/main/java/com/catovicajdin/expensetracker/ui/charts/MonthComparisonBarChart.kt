package com.catovicajdin.expensetracker.ui.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.unit.dp

/**
 * Two direct-labeled bars (last month vs this month) - no legend needed for two series with
 * direct labels underneath, per the dataviz mark spec. Rounded 4dp data-ends, single value axis.
 */
@Composable
fun MonthComparisonBarChart(
    lastMonthLabel: String,
    lastMonthAmount: Double,
    thisMonthLabel: String,
    thisMonthAmount: Double,
    modifier: Modifier = Modifier,
) {
    val maxAmount = maxOf(lastMonthAmount, thisMonthAmount, 1.0)
    val mutedColor = MaterialTheme.colorScheme.surfaceVariant
    val accentColor = MaterialTheme.colorScheme.primary
    val chartHeight = 100.dp
    val cornerRadius = with(androidx.compose.ui.platform.LocalDensity.current) { 4.dp.toPx() }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        BarWithLabel(
            fraction = (lastMonthAmount / maxAmount).toFloat(),
            color = mutedColor,
            amount = lastMonthAmount,
            label = lastMonthLabel,
            chartHeight = chartHeight,
            cornerRadiusPx = cornerRadius,
        )
        BarWithLabel(
            fraction = (thisMonthAmount / maxAmount).toFloat(),
            color = accentColor,
            amount = thisMonthAmount,
            label = thisMonthLabel,
            chartHeight = chartHeight,
            cornerRadiusPx = cornerRadius,
        )
    }
}

@Composable
private fun BarWithLabel(
    fraction: Float,
    color: androidx.compose.ui.graphics.Color,
    amount: Double,
    label: String,
    chartHeight: androidx.compose.ui.unit.Dp,
    cornerRadiusPx: Float,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("%.2f".format(amount), style = MaterialTheme.typography.labelMedium)
        Canvas(
            modifier = Modifier
                .padding(top = 4.dp)
                .width(56.dp)
                .height(chartHeight),
        ) {
            val barHeight = size.height * fraction.coerceIn(0.02f, 1f)
            drawRoundRect(
                color = color,
                topLeft = androidx.compose.ui.geometry.Offset(0f, size.height - barHeight),
                size = Size(size.width, barHeight),
                cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
                style = Fill,
            )
        }
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}
