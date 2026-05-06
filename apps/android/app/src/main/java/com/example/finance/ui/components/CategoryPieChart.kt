package com.example.finance.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.unit.dp

data class PieChartData(
    val label: String,
    val value: Float,
    val color: Color
)

@Composable
fun CategoryPieChart(
    data: List<PieChartData>,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) return

    val total = data.sumOf { it.value.toDouble() }.toFloat()
    if (total <= 0f) return

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp)
            .padding(16.dp)
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            var startAngle = -90f
            data.forEach { slice ->
                val sweepAngle = (slice.value / total) * 360f
                drawArc(
                    color = slice.color,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = true,
                    size = Size(size.minDimension, size.minDimension),
                    style = Fill
                )
                startAngle += sweepAngle
            }
        }
    }
}
