package com.example.finance.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.finance.data.entity.RecurringTransactionEntity
import com.example.finance.data.repository.FinanceRepository
import com.example.finance.domain.usecase.SyncRecurringTransactionsUseCase
import com.example.finance.core.common.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RecurringViewModel(private val repository: FinanceRepository, private val db: com.example.finance.data.database.AppDatabase) : ViewModel() {
    private val _recurring = MutableStateFlow<List<RecurringTransactionEntity>>(emptyList())
    val recurring: StateFlow<List<RecurringTransactionEntity>> = _recurring.asStateFlow()

    private val syncUseCase = SyncRecurringTransactionsUseCase(db)

    init {
        viewModelScope.launch {
            repository.getRecurringTransactions().collect {
                _recurring.value = it
            }
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            syncUseCase()
        }
    }

    fun addRecurring(
        id: Int = 0,
        title: String,
        amount: Long,
        kind: com.example.finance.domain.model.TransactionKind,
        accountId: Int,
        categoryId: Int,
        dayOfMonth: Int,
        startDate: String,
        autoCreate: Boolean,
        isActive: Boolean,
        notes: String?,
        merchant: String?
    ) {
        viewModelScope.launch {
            val recurring = com.example.finance.data.entity.RecurringTransactionEntity(
                id = id,
                userId = 1,
                accountId = accountId,
                categoryId = categoryId,
                kind = kind,
                title = title,
                amount = amount,
                dayOfMonth = dayOfMonth,
                startDate = startDate,
                nextDueDate = com.example.finance.core.common.DateUtils.getInitialNextDueDate(startDate, dayOfMonth),
                autoCreate = autoCreate,
                isActive = isActive,
                notes = notes,
                merchant = merchant
            )
            if (id == 0) {
                repository.addRecurring(recurring)
            } else {
                repository.updateRecurring(recurring)
            }
        }
    }

    fun deleteRecurring(recurring: RecurringTransactionEntity) {
        viewModelScope.launch {
            repository.deleteRecurring(recurring)
        }
    }

    fun copyToNextMonth() {
        viewModelScope.launch {
            val existing = _recurring.value
            val targetKeys = existing.mapTo(mutableSetOf()) { recurring ->
                listOf(
                    recurring.title,
                    recurring.amount.toString(),
                    recurring.kind.name,
                    recurring.accountId.toString(),
                    recurring.categoryId.toString(),
                    recurring.dayOfMonth.toString(),
                    recurring.startDate
                ).joinToString("|")
            }
            existing.forEach { recurring ->
                val shiftedStartDate = DateUtils.getNextMonthlyDueDate(recurring.startDate, recurring.dayOfMonth)
                val targetKey = listOf(
                    recurring.title,
                    recurring.amount.toString(),
                    recurring.kind.name,
                    recurring.accountId.toString(),
                    recurring.categoryId.toString(),
                    recurring.dayOfMonth.toString(),
                    shiftedStartDate
                ).joinToString("|")
                val duplicateExists = targetKey in targetKeys
                if (!duplicateExists) {
                    repository.addRecurring(
                        recurring.copy(
                            id = 0,
                            startDate = shiftedStartDate,
                            nextDueDate = DateUtils.getInitialNextDueDate(shiftedStartDate, recurring.dayOfMonth),
                            createdAt = System.currentTimeMillis()
                        )
                    )
                    targetKeys += targetKey
                }
            }
        }
    }
}
