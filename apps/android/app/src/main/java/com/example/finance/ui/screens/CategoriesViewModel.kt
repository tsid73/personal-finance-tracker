package com.example.finance.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.finance.data.entity.BudgetEntity
import com.example.finance.data.entity.CategoryEntity
import com.example.finance.data.entity.RecurringTransactionEntity
import com.example.finance.data.entity.TransactionEntity
import com.example.finance.data.repository.CategoryUsage
import com.example.finance.data.repository.FinanceRepository
import com.example.finance.domain.model.BudgetMode
import com.example.finance.domain.model.TransactionKind
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CategoriesViewModel(private val repository: FinanceRepository) : ViewModel() {
    private val _categories = MutableStateFlow<List<CategoryEntity>>(emptyList())
    val categories: StateFlow<List<CategoryEntity>> = _categories.asStateFlow()

    val transactions: StateFlow<List<TransactionEntity>> = repository.getAllTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val budgets: StateFlow<List<BudgetEntity>> = repository.getAllBudgets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recurring: StateFlow<List<RecurringTransactionEntity>> = repository.getRecurringTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repository.getCategories(true).collect {
                _categories.value = it
            }
        }
    }

    fun addCategory(name: String, type: TransactionKind, color: String, icon: String, budgetMode: BudgetMode) {
        viewModelScope.launch {
            repository.addCategory(CategoryEntity(userId = 1, name = name, type = type, color = color, icon = icon, budgetMode = budgetMode))
        }
    }

    fun updateCategory(category: CategoryEntity) {
        viewModelScope.launch {
            repository.updateCategory(category)
        }
    }

    fun deleteCategory(category: CategoryEntity, reassignmentId: Int?) {
        viewModelScope.launch {
            repository.deleteCategory(category, reassignmentId)
        }
    }

    fun archiveCategory(category: CategoryEntity, archived: Boolean) {
        viewModelScope.launch {
            repository.archiveCategory(category, archived)
        }
    }

    suspend fun getCategoryUsage(categoryId: Int): CategoryUsage = repository.getCategoryUsage(categoryId)
}
