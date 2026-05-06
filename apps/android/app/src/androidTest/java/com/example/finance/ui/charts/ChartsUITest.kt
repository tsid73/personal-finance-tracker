package com.example.finance.ui.charts

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.finance.ui.components.CategoryPieChart
import com.example.finance.ui.components.PieChartData
import org.junit.Rule
import org.junit.Test

class ChartsUITest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun categoryPieChart_rendersWithoutCrashing() {
        val testData = listOf(
            PieChartData(label = "Food", value = 100f, color = Color.Red),
            PieChartData(label = "Rent", value = 300f, color = Color.Blue)
        )

        composeTestRule.setContent {
            CategoryPieChart(data = testData)
        }
        
        // At minimum, verify that setting the content with the canvas chart does not crash.
        // For deeper canvas assertions, snapshot testing is recommended.
    }
}
