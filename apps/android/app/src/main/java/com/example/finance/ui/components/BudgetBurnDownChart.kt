package com.example.finance.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

data class BurnDownData(
    val dailySpent: List<Long>, // cumulative spent per day
    val totalBudget: Long,
    val daysInMonth: Int
)

@Composable
fun BudgetBurnDownChart(
    data: BurnDownData,
    modifier: Modifier = Modifier
) {
    val totalBudget = data.totalBudget.toFloat()
    if (totalBudget <= 0f) return

    val colorScheme = MaterialTheme.colorScheme

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(16.dp)
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val width = size.width
            val height = size.height
            val days = data.daysInMonth.toFloat()

            // Draw Target Line (Ideal spending)
            drawLine(
                color = Color.Gray.copy(alpha = 0.5f),
                start = Offset(0f, height),
                end = Offset(width, 0f),
                strokeWidth = 2.dp.toPx()
            )

            // Draw Actual Spending Path
            if (data.dailySpent.isNotEmpty()) {
                val path = Path()
                data.dailySpent.forEachIndexed { index, spent ->
                    val x = (index / (days - 1)) * width
                    val y = height - (spent.toFloat() / totalBudget) * height
                    
                    if (index == 0) {
                        path.moveTo(x, height)
                        path.lineTo(x, y)
                    } else {
                        path.lineTo(x, y)
                    }
                }
                
                drawPath(
                    path = path,
                    color = if (data.dailySpent.last() > data.totalBudget) Color.Red else colorScheme.primary,
                    style = Stroke(width = 3.dp.toPx())
                )
            }
        }
    }
}
