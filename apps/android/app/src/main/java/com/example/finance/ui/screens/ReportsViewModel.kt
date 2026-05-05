package com.example.finance.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.finance.data.entity.BudgetEntity
import com.example.finance.data.entity.CategoryEntity
import com.example.finance.data.entity.TransactionEntity
import com.example.finance.data.repository.FinanceRepository
import com.example.finance.data.repository.PreferenceManager
import com.example.finance.domain.model.BudgetMode
import com.example.finance.domain.model.TransactionKind
import com.example.finance.util.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.time.format.DateTimeFormatter

data class ReportSummary(
    val income: Long = 0L,
    val expense: Long = 0L,
    val net: Long = 0L,
    val averageExpense: Long = 0L,
    val topCategory: String = "No data"
)

data class BudgetActualRow(
    val categoryId: Int,
    val categoryName: String,
    val color: String,
    val budgetMode: BudgetMode,
    val allocatedAmount: Long,
    val spentAmount: Long,
    val remainingAmount: Long
)

data class ComparisonRow(
    val monthKey: String,
    val monthLabel: String,
    val income: Long,
    val expense: Long,
    val net: Long,
    val targetBudget: Long,
    val allocatedBudget: Long
)

data class CategoryMonthDetail(
    val monthKey: String,
    val monthLabel: String,
    val categoryId: Int,
    val categoryName: String,
    val color: String,
    val budgetMode: BudgetMode,
    val allocatedAmount: Long,
    val spentAmount: Long,
    val remainingAmount: Long,
    val transactions: List<TransactionEntity>
)

class ReportsViewModel(
    private val repository: FinanceRepository,
    private val preferenceManager: PreferenceManager
) : ViewModel() {
    private val monthFormatter = DateTimeFormatter.ofPattern("yyyy-MM")

    private val _summary = MutableStateFlow(ReportSummary())
    val summary: StateFlow<ReportSummary> = _summary.asStateFlow()

    private val _budgetVsActual = MutableStateFlow<List<BudgetActualRow>>(emptyList())
    val budgetVsActual: StateFlow<List<BudgetActualRow>> = _budgetVsActual.asStateFlow()

    private val _comparison = MutableStateFlow<List<ComparisonRow>>(emptyList())
    val comparison: StateFlow<List<ComparisonRow>> = _comparison.asStateFlow()

    private val _selectedMonth = MutableStateFlow("")
    val selectedMonth: StateFlow<String> = _selectedMonth.asStateFlow()

    private var transactionsCache: List<TransactionEntity> = emptyList()
    private var categoriesCache: List<CategoryEntity> = emptyList()
    private var budgetsCache: List<BudgetEntity> = emptyList()
    private var targetsCache: Map<String, Long> = emptyMap()

    init {
        viewModelScope.launch {
            preferenceManager.selectedMonth.collectLatest { month ->
                _selectedMonth.value = month
                refreshReport(month)
            }
        }
    }

    private fun refreshReport(month: String) {
        viewModelScope.launch {
            transactionsCache = repository.getAllTransactions().first()
            categoriesCache = repository.getCategories(false).first()
            budgetsCache = repository.getAllBudgets().first()
            val comparisonMonths = buildTrailingMonthKeys(month)
            targetsCache = (budgetsCache.map { it.year to it.month } + comparisonMonths.map {
                val parsed = YearMonth.parse(it, monthFormatter)
                parsed.year to parsed.monthValue
            })
                .distinct()
                .associate { (year, monthValue) ->
                    val key = formatMonthKey(year, monthValue)
                    key to (repository.getMonthlyTarget(monthValue, year).first()?.totalBudget ?: 0L)
                }

            _budgetVsActual.value = buildBudgetVsActual(month)
            _comparison.value = buildComparison(month)

            val monthTransactions = transactionsCache.filter { it.transactionDate.startsWith("$month-") }
            val expenseTransactions = monthTransactions.filter { it.kind == TransactionKind.EXPENSE }
            val monthlyIncome = monthTransactions.filter { it.kind == TransactionKind.INCOME }.sumOf { it.amount }
            val monthlyExpense = expenseTransactions.sumOf { it.amount }
            val comparisonRows = _comparison.value

            _summary.value = ReportSummary(
                income = monthlyIncome,
                expense = monthlyExpense,
                net = monthlyIncome - monthlyExpense,
                averageExpense = if (comparisonRows.isEmpty()) 0L else comparisonRows.sumOf { it.expense } / comparisonRows.size,
                topCategory = expenseTransactions
                    .groupBy { it.categoryId }
                    .maxByOrNull { (_, items) -> items.sumOf { it.amount } }
                    ?.key
                    ?.let { categoryId -> categoriesCache.find { it.id == categoryId }?.name }
                    ?: "No data"
            )
        }
    }

    fun getMonthBreakdown(monthKey: String): List<BudgetActualRow> = buildBudgetVsActual(monthKey)

    fun getCategoryDetail(monthKey: String, categoryId: Int): CategoryMonthDetail? {
        val category = categoriesCache.find { it.id == categoryId } ?: return null
        val row = buildBudgetVsActual(monthKey).find { it.categoryId == categoryId }
        val transactions = transactionsCache
            .filter {
                it.kind == TransactionKind.EXPENSE &&
                    it.categoryId == categoryId &&
                    it.transactionDate.startsWith("$monthKey-")
            }
            .sortedByDescending { it.transactionDate }

        return CategoryMonthDetail(
            monthKey = monthKey,
            monthLabel = DateUtils.formatDisplayMonth(monthKey),
            categoryId = categoryId,
            categoryName = category.name,
            color = category.color,
            budgetMode = category.budgetMode,
            allocatedAmount = row?.allocatedAmount ?: 0L,
            spentAmount = row?.spentAmount ?: 0L,
            remainingAmount = row?.remainingAmount ?: 0L,
            transactions = transactions
        )
    }

    fun buildBudgetExportRows(): List<Map<String, String>> = _budgetVsActual.value.map { row ->
        mapOf(
            "Month" to DateUtils.formatDisplayMonth(_selectedMonth.value),
            "Category" to row.categoryName,
            "Mode" to row.budgetMode.name.lowercase(),
            "Allocated" to formatExportAmount(row.allocatedAmount),
            "Spent" to formatExportAmount(row.spentAmount),
            "Remaining" to formatExportAmount(row.remainingAmount)
        )
    }

    fun buildComparisonExportRows(): List<Map<String, String>> = _comparison.value.map { row ->
        mapOf(
            "Month" to row.monthLabel,
            "Income" to formatExportAmount(row.income),
            "Expense" to formatExportAmount(row.expense),
            "Net" to formatExportAmount(row.net),
            "TargetBudget" to formatExportAmount(row.targetBudget),
            "AllocatedBudget" to formatExportAmount(row.allocatedBudget)
        )
    }

    fun buildMonthBreakdownExportRows(monthKey: String): List<Map<String, String>> = getMonthBreakdown(monthKey).map { row ->
        mapOf(
            "Month" to DateUtils.formatDisplayMonth(monthKey),
            "Category" to row.categoryName,
            "Mode" to row.budgetMode.name.lowercase(),
            "Allocated" to formatExportAmount(row.allocatedAmount),
            "Spent" to formatExportAmount(row.spentAmount),
            "Remaining" to formatExportAmount(row.remainingAmount)
        )
    }

    fun buildCategoryTransactionExportRows(detail: CategoryMonthDetail): List<Map<String, String>> = detail.transactions.map { transaction ->
        mapOf(
            "Month" to detail.monthLabel,
            "Category" to detail.categoryName,
            "Date" to transaction.transactionDate,
            "Title" to transaction.title,
            "Amount" to formatExportAmount(transaction.amount),
            "Merchant" to (transaction.merchant ?: ""),
            "Notes" to (transaction.notes ?: "")
        )
    }

    private fun buildBudgetVsActual(monthKey: String): List<BudgetActualRow> {
        val budgetsByCategory = budgetsCache
            .filter { formatMonthKey(it.year, it.month) == monthKey }
            .associateBy { it.categoryId }
        val spentByCategory = transactionsCache
            .filter { it.kind == TransactionKind.EXPENSE && it.transactionDate.startsWith("$monthKey-") }
            .groupBy { it.categoryId }
            .mapValues { (_, items) -> items.sumOf { it.amount } }

        return categoriesCache
            .asSequence()
            .filter { it.type == TransactionKind.EXPENSE }
            .mapNotNull { category ->
                val allocated = budgetsByCategory[category.id]?.allocatedAmount ?: 0L
                val spent = spentByCategory[category.id] ?: 0L
                if (allocated == 0L && spent == 0L) {
                    null
                } else {
                    BudgetActualRow(
                        categoryId = category.id,
                        categoryName = category.name,
                        color = category.color,
                        budgetMode = category.budgetMode,
                        allocatedAmount = allocated,
                        spentAmount = spent,
                        remainingAmount = allocated - spent
                    )
                }
            }
            .sortedWith(compareByDescending<BudgetActualRow> { it.spentAmount }.thenByDescending { it.allocatedAmount }.thenBy { it.categoryName })
            .toList()
    }

    private fun buildComparison(selectedMonth: String): List<ComparisonRow> {
        return buildTrailingMonthKeys(selectedMonth).map { monthKey ->
            val monthTransactions = transactionsCache.filter { it.transactionDate.startsWith("$monthKey-") }
            val budgets = budgetsCache.filter { formatMonthKey(it.year, it.month) == monthKey }
            val income = monthTransactions.filter { it.kind == TransactionKind.INCOME }.sumOf { it.amount }
            val expense = monthTransactions.filter { it.kind == TransactionKind.EXPENSE }.sumOf { it.amount }
            ComparisonRow(
                monthKey = monthKey,
                monthLabel = DateUtils.formatDisplayMonth(monthKey),
                income = income,
                expense = expense,
                net = income - expense,
                targetBudget = targetsCache[monthKey] ?: 0L,
                allocatedBudget = budgets.sumOf { it.allocatedAmount }
            )
        }
    }

    private fun buildTrailingMonthKeys(selectedMonth: String): List<String> {
        val endMonth = YearMonth.parse(selectedMonth, monthFormatter)
        return (11 downTo 0).map { offset ->
            endMonth.minusMonths(offset.toLong()).format(monthFormatter)
        }
    }

    private fun formatMonthKey(year: Int, month: Int): String = "%04d-%02d".format(year, month)

    private fun formatExportAmount(amountCents: Long): String = "%.2f".format(amountCents / 100.0)
}
