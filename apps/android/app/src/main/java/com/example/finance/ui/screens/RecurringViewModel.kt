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
            val existing = if (id == 0) null else repository.getRecurringById(id)
            val nextDueDate = when {
                existing == null -> DateUtils.getInitialNextDueDate(startDate, dayOfMonth)
                existing.dayOfMonth == dayOfMonth && existing.startDate == startDate -> existing.nextDueDate
                else -> DateUtils.getFirstMonthlyDueOnOrAfter(
                    referenceDate = maxOf(DateUtils.today(), existing.nextDueDate, startDate),
                    dayOfMonth = dayOfMonth,
                    startDate = startDate
                )
            }
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
                nextDueDate = nextDueDate,
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
                recurringCopyKey(
                    title = recurring.title,
                    amount = recurring.amount,
                    kind = recurring.kind.name,
                    accountId = recurring.accountId,
                    categoryId = recurring.categoryId,
                    dayOfMonth = recurring.dayOfMonth,
                    startDate = recurring.startDate
                )
            }

            existing.forEach { recurring ->
                val shiftedStartDate = DateUtils.getNextMonthlyDueDate(recurring.startDate, recurring.dayOfMonth)
                val targetKey = recurringCopyKey(
                    title = recurring.title,
                    amount = recurring.amount,
                    kind = recurring.kind.name,
                    accountId = recurring.accountId,
                    categoryId = recurring.categoryId,
                    dayOfMonth = recurring.dayOfMonth,
                    startDate = shiftedStartDate
                )

                if (targetKey !in targetKeys) {
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

    private fun recurringCopyKey(
        title: String,
        amount: Long,
        kind: String,
        accountId: Int,
        categoryId: Int,
        dayOfMonth: Int,
        startDate: String
    ): String {
        return listOf(
            title,
            amount.toString(),
            kind,
            accountId.toString(),
            categoryId.toString(),
            dayOfMonth.toString(),
            startDate
        ).joinToString("|")
    }
}
