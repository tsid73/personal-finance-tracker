package com.example.finance.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.animateItemPlacement
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.finance.ui.components.FinanceTopAppBar
import com.example.finance.util.BackupManager
import com.example.finance.util.DateUtils
import com.example.finance.util.MoneyFormatter

private enum class ReportScope { BUDGET_VS_ACTUAL, COMPARISON }

@Composable
fun ReportsScreen(viewModel: ReportsViewModel) {
    val context = LocalContext.current
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val summary by viewModel.summary.collectAsState()
    val budgetVsActual by viewModel.budgetVsActual.collectAsState()
    val comparison by viewModel.comparison.collectAsState()
    var scope by remember { mutableStateOf(ReportScope.BUDGET_VS_ACTUAL) }
    var monthDetail by remember { mutableStateOf<String?>(null) }
    var categoryDetailKey by remember { mutableStateOf<Pair<String, Int>?>(null) }

    Scaffold(
        topBar = {
            FinanceTopAppBar(
                title = "Reports",
                actions = {
                    IconButton(
                        onClick = {
                            val exportRows = if (scope == ReportScope.BUDGET_VS_ACTUAL) {
                                viewModel.buildBudgetExportRows()
                            } else {
                                viewModel.buildComparisonExportRows()
                            }
                            if (exportRows.isNotEmpty()) {
                                val headers = exportRows.first().keys.toList()
                                val file = BackupManager.exportCsv(
                                    context = context,
                                    prefix = if (scope == ReportScope.BUDGET_VS_ACTUAL) "budget_report" else "comparison_report",
                                    headers = headers,
                                    rows = exportRows
                                )
                                val intent = BackupManager.createShareIntent(context, file, "text/csv", "Report export")
                                context.startActivity(Intent.createChooser(intent, "Share report"))
                            }
                        }
                    ) {
                        Icon(Icons.Default.Download, contentDescription = "Export report CSV")
                    }
                }
            )
        }
    ) { padding ->
        AnimatedContent(
            targetState = selectedMonth to scope,
            transitionSpec = { fadeIn(animationSpec = tween(180)) togetherWith fadeOut(animationSpec = tween(120)) },
            label = "reports_state"
        ) {
            LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
            item {
                Text("For ${DateUtils.formatDisplayMonth(selectedMonth)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ScopeButton("Budget vs actual", selected = scope == ReportScope.BUDGET_VS_ACTUAL) {
                        scope = ReportScope.BUDGET_VS_ACTUAL
                    }
                    ScopeButton("Multi-month comparison", selected = scope == ReportScope.COMPARISON) {
                        scope = ReportScope.COMPARISON
                    }
                }
            }
            item {
                SummaryMetricCard("Income", MoneyFormatter.format(summary.income))
            }
            item {
                SummaryMetricCard("Expense", MoneyFormatter.format(summary.expense))
            }
            item {
                SummaryMetricCard("Net cash flow", MoneyFormatter.format(summary.net))
            }
            item {
                SummaryMetricCard("Avg monthly expense", MoneyFormatter.format(summary.averageExpense))
            }
            item {
                SummaryMetricCard("Top expense category", summary.topCategory)
            }
            if (scope == ReportScope.BUDGET_VS_ACTUAL) {
                if (budgetVsActual.isEmpty()) {
                    item {
                        Text("No budget or spending data for this month.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    items(budgetVsActual, key = { "${it.categoryId}-${it.categoryName}" }) { row ->
                        BudgetActualCard(modifier = Modifier.animateItemPlacement(), row = row, onClick = {
                            categoryDetailKey = selectedMonth to row.categoryId
                        })
                    }
                }
            } else {
                items(comparison, key = { it.monthKey }) { row ->
                    ComparisonCard(modifier = Modifier.animateItemPlacement(), row = row, onClick = {
                        monthDetail = row.monthKey
                    })
                }
            }
        }
        }
    }

    monthDetail?.let { monthKey ->
        MonthBreakdownDialog(
            monthKey = monthKey,
            rows = viewModel.getMonthBreakdown(monthKey),
            onDismiss = { monthDetail = null },
            onExport = {
                val rows = viewModel.buildMonthBreakdownExportRows(monthKey)
                if (rows.isNotEmpty()) {
                    val file = BackupManager.exportCsv(
                        context = context,
                        prefix = "month_breakdown",
                        headers = rows.first().keys.toList(),
                        rows = rows
                    )
                    val intent = BackupManager.createShareIntent(context, file, "text/csv", "Month breakdown export")
                    context.startActivity(Intent.createChooser(intent, "Share month breakdown"))
                }
            },
            onCategoryClick = { categoryId ->
                monthDetail = null
                categoryDetailKey = monthKey to categoryId
            }
        )
    }

    categoryDetailKey?.let { (monthKey, categoryId) ->
        viewModel.getCategoryDetail(monthKey, categoryId)?.let { detail ->
            CategoryDetailDialog(
                detail = detail,
                onExport = {
                    val rows = viewModel.buildCategoryTransactionExportRows(detail)
                    if (rows.isNotEmpty()) {
                        val file = BackupManager.exportCsv(
                            context = context,
                            prefix = "category_transactions",
                            headers = rows.first().keys.toList(),
                            rows = rows
                        )
                        val intent = BackupManager.createShareIntent(context, file, "text/csv", "Category transactions export")
                        context.startActivity(Intent.createChooser(intent, "Share category transactions"))
                    }
                },
                onDismiss = { categoryDetailKey = null }
            )
        }
    }
}

@Composable
private fun ScopeButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Button(onClick = onClick, enabled = !selected) {
        Text(label)
    }
}

@Composable
private fun BudgetActualCard(
    modifier: Modifier = Modifier,
    row: BudgetActualRow,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(row.categoryName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        row.budgetMode.name.lowercase().replaceFirstChar(Char::titlecase),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    modifier = Modifier.size(12.dp),
                    shape = MaterialTheme.shapes.small,
                    color = parseColor(row.color)
                ) {}
            }
            ReportAmountRow("Budget", row.allocatedAmount)
            ReportAmountRow("Spent", row.spentAmount)
            ReportAmountRow("Remaining", row.remainingAmount, emphasizeNegative = true)
        }
    }
}

@Composable
private fun ComparisonCard(
    modifier: Modifier = Modifier,
    row: ComparisonRow,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(row.monthLabel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            ReportAmountRow("Income", row.income)
            ReportAmountRow("Expense", row.expense)
            ReportAmountRow("Net", row.net, emphasizeNegative = true)
            ReportAmountRow("Target budget", row.targetBudget)
            ReportAmountRow("Allocated budget", row.allocatedBudget)
        }
    }
}

@Composable
private fun ReportAmountRow(
    label: String,
    amount: Long,
    emphasizeNegative: Boolean = false
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            MoneyFormatter.format(amount),
            fontWeight = FontWeight.SemiBold,
            color = if (emphasizeNegative && amount < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun MonthBreakdownDialog(
    monthKey: String,
    rows: List<BudgetActualRow>,
    onDismiss: () -> Unit,
    onExport: () -> Unit,
    onCategoryClick: (Int) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.large) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .heightIn(max = 600.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(DateUtils.formatDisplayMonth(monthKey), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Category breakdown", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row {
                        IconButton(onClick = onExport) {
                            Icon(Icons.Default.Download, contentDescription = "Export month breakdown")
                        }
                        TextButton(onClick = onDismiss) {
                            Text("Close")
                        }
                    }
                }
                if (rows.isEmpty()) {
                    Text("No categories recorded for this month.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rows.forEach { row ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onCategoryClick(row.categoryId) },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(row.categoryName, fontWeight = FontWeight.SemiBold)
                                    ReportAmountRow("Budget", row.allocatedAmount)
                                    ReportAmountRow("Spent", row.spentAmount)
                                    ReportAmountRow("Remaining", row.remainingAmount, emphasizeNegative = true)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryDetailDialog(
    detail: CategoryMonthDetail,
    onExport: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.large) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .heightIn(max = 600.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(detail.categoryName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(detail.monthLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row {
                        IconButton(onClick = onExport, enabled = detail.transactions.isNotEmpty()) {
                            Icon(Icons.Default.Download, contentDescription = "Export category transactions")
                        }
                        TextButton(onClick = onDismiss) {
                            Text("Close")
                        }
                    }
                }
                ReportAmountRow("Budget", detail.allocatedAmount)
                ReportAmountRow("Spent", detail.spentAmount)
                ReportAmountRow("Remaining", detail.remainingAmount, emphasizeNegative = true)
                HorizontalDivider()
                if (detail.transactions.isEmpty()) {
                    Text("No expense transactions for this category in this month.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        detail.transactions.forEach { transaction ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(transaction.title, fontWeight = FontWeight.SemiBold)
                                        Text(MoneyFormatter.format(transaction.amount), fontWeight = FontWeight.SemiBold)
                                    }
                                    Text(DateUtils.formatDisplayDate(transaction.transactionDate), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    transaction.merchant?.takeIf { it.isNotBlank() }?.let {
                                        Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    transaction.notes?.takeIf { it.isNotBlank() }?.let {
                                        Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryMetricCard(label: String, value: String) {
    Card {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}

private fun parseColor(value: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(value))
    } catch (_: IllegalArgumentException) {
        Color.Gray
    }
}
