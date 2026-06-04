package com.example.finance.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.finance.data.entity.AccountEntity
import com.example.finance.data.entity.CategoryEntity
import com.example.finance.data.entity.TransactionEntity
import com.example.finance.data.repository.FinanceRepository
import com.example.finance.data.repository.PreferenceManager
import com.example.finance.domain.model.TransactionKind
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TransactionsViewModel(
    private val repository: FinanceRepository,
    private val preferenceManager: PreferenceManager
) : ViewModel() {
    private val _transactions = MutableStateFlow<List<TransactionEntity>>(emptyList())
    val transactions: StateFlow<List<TransactionEntity>> = _transactions.asStateFlow()

    private val _selectedMonth = MutableStateFlow("")
    val selectedMonth: StateFlow<String> = _selectedMonth.asStateFlow()

    val accounts: StateFlow<List<AccountEntity>> = repository.getAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<CategoryEntity>> = repository.getCategories(true)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            preferenceManager.selectedMonth.collectLatest { month ->
                _selectedMonth.value = month
                repository.getTransactionsByMonth(month).collect {
                    _transactions.value = it
                }
            }
        }
    }

    fun addTransaction(
        id: Int = 0,
        title: String,
        amount: Long,
        kind: TransactionKind,
        accountId: Int,
        categoryId: Int,
        date: String,
        notes: String?,
        merchant: String?
    ) {
        viewModelScope.launch {
            val transaction = TransactionEntity(
                id = id,
                userId = 1,
                accountId = accountId,
                categoryId = categoryId,
                kind = kind,
                title = title,
                amount = amount,
                transactionDate = date,
                notes = notes,
                merchant = merchant
            )
            if (id == 0) {
                repository.addTransaction(transaction)
            } else {
                repository.updateTransaction(transaction)
            }
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }

    fun bulkDeleteTransactions(ids: List<Int>, note: String? = null) {
        viewModelScope.launch {
            repository.bulkDeleteTransactions(ids, note)
        }
    }

    fun bulkRecategorizeTransactions(ids: List<Int>, categoryId: Int, note: String? = null) {
        viewModelScope.launch {
            repository.bulkRecategorizeTransactions(ids, categoryId, note)
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
            preferenceManager.setMonth(com.example.finance.core.common.DateUtils.getCurrentMonth())
        }
    }
}
