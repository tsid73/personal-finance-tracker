package com.example.finance.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.finance.data.entity.BudgetEntity
import com.example.finance.data.entity.CategoryEntity
import com.example.finance.data.repository.FinanceRepository
import com.example.finance.data.repository.PreferenceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.example.finance.core.common.DateUtils

class BudgetsViewModel(private val repository: FinanceRepository, private val preferenceManager: PreferenceManager) : ViewModel() {
    private val _budgets = MutableStateFlow<List<BudgetEntity>>(emptyList())
    val budgets: StateFlow<List<BudgetEntity>> = _budgets.asStateFlow()

    private val _totalBudget = MutableStateFlow(0L)
    val totalBudget: StateFlow<Long> = _totalBudget.asStateFlow()

    private val _spendingByCategory = MutableStateFlow<Map<Int, Long>>(emptyMap())
    val spendingByCategory: StateFlow<Map<Int, Long>> = _spendingByCategory.asStateFlow()

    private val _selectedMonth = MutableStateFlow("")
    val selectedMonth: StateFlow<String> = _selectedMonth.asStateFlow()

    val categories: StateFlow<List<CategoryEntity>> = repository.getCategories(true)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            preferenceManager.selectedMonth.collectLatest { monthKey ->
                _selectedMonth.value = monthKey
                val parts = monthKey.split("-").map { it.toInt() }
                repository.getBudgetsByMonth(parts[1], parts[0]).collect {
                    _budgets.value = it
                }
            }
        }
        viewModelScope.launch {
            preferenceManager.selectedMonth.collectLatest { monthKey ->
                val parts = monthKey.split("-").map { it.toInt() }
                repository.getMonthlyTarget(parts[1], parts[0]).collect {
                    _totalBudget.value = it?.totalBudget ?: 0L
                }
            }
        }
        viewModelScope.launch {
            preferenceManager.selectedMonth.collectLatest { monthKey ->
                repository.getTransactionsByMonth(monthKey).collect { transactions ->
                    _spendingByCategory.value = transactions
                        .filter { it.kind == com.example.finance.domain.model.TransactionKind.EXPENSE }
                        .groupBy { it.categoryId }
                        .mapValues { (_, items) -> items.sumOf { it.amount } }
                }
            }
        }
    }

    fun setMonthlyTarget(total: Long) {
        viewModelScope.launch {
            val parts = _selectedMonth.value.split("-").map { it.toInt() }
            repository.setMonthlyTarget(parts[1], parts[0], total)
        }
    }

    fun setCategoryBudget(categoryId: Int, amount: Long) {
        viewModelScope.launch {
            val parts = _selectedMonth.value.split("-").map { it.toInt() }
            repository.setCategoryBudget(BudgetEntity(userId = 1, categoryId = categoryId, month = parts[1], year = parts[0], allocatedAmount = amount))
        }
    }

    fun updateCategoryBudget(id: Int, categoryId: Int, amount: Long) {
        viewModelScope.launch {
            val parts = _selectedMonth.value.split("-").map { it.toInt() }
            repository.updateBudget(BudgetEntity(id = id, userId = 1, categoryId = categoryId, month = parts[1], year = parts[0], allocatedAmount = amount))
        }
    }

    fun deleteBudget(budget: BudgetEntity) {
        viewModelScope.launch {
            repository.deleteBudget(budget)
        }
    }

    fun nextMonth() {
        viewModelScope.launch {
            preferenceManager.setMonth(com.example.finance.core.common.DateUtils.shiftMonth(_selectedMonth.value, 1))
        }
    }

    fun prevMonth() {
        viewModelScope.launch {
            preferenceManager.setMonth(com.example.finance.core.common.DateUtils.shiftMonth(_selectedMonth.value, -1))
        }
    }

    fun currentMonth() {
        viewModelScope.launch {
            preferenceManager.setMonth(DateUtils.getCurrentMonth())
        }
    }

    fun copyToNextMonth() {
        viewModelScope.launch {
            val nextMonthKey = DateUtils.shiftMonth(_selectedMonth.value, 1)
            val nextParts = nextMonthKey.split("-").map { it.toInt() }

            if (_totalBudget.value > 0) {
                repository.setMonthlyTarget(nextParts[1], nextParts[0], _totalBudget.value)
            }

            _budgets.value.forEach { budget ->
                repository.setCategoryBudget(
                    BudgetEntity(
                        userId = budget.userId,
                        categoryId = budget.categoryId,
                        month = nextParts[1],
                        year = nextParts[0],
                        allocatedAmount = budget.allocatedAmount
                    )
                )
            }

            preferenceManager.setMonth(nextMonthKey)
        }
    }
}
