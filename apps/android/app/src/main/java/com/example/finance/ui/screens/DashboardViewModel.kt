package com.example.finance.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.finance.data.repository.FinanceRepository
import com.example.finance.data.repository.PreferenceManager
import com.example.finance.domain.model.DashboardSummary
import com.example.finance.domain.usecase.CalculateDashboardSummaryUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val repository: FinanceRepository,
    private val preferenceManager: PreferenceManager
) : ViewModel() {
    private val calculateDashboardSummaryUseCase = CalculateDashboardSummaryUseCase(repository)

    private val _summary = MutableStateFlow<DashboardSummary?>(null)
    val summary: StateFlow<DashboardSummary?> = _summary.asStateFlow()

    private val _selectedMonth = MutableStateFlow("")
    val selectedMonth: StateFlow<String> = _selectedMonth.asStateFlow()

    private val _recentActivity = MutableStateFlow<List<com.example.finance.data.entity.ActivityLogEntity>>(emptyList())
    val recentActivity: StateFlow<List<com.example.finance.data.entity.ActivityLogEntity>> = _recentActivity.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureDemoUserAndSeeds()
            preferenceManager.selectedMonth.collectLatest { month ->
                _selectedMonth.value = month
                refreshSummary(month)
            }
        }
        viewModelScope.launch {
            repository.getRecentActivity().collect {
                _recentActivity.value = it
            }
        }
    }
    
    fun toggleTheme(isDark: Boolean) {
        viewModelScope.launch {
            preferenceManager.setTheme(isDark)
        }
    }

    private fun refreshSummary(month: String) {
        viewModelScope.launch {
            _summary.value = calculateDashboardSummaryUseCase(month)
        }
    }

    fun nextMonth() {
        viewModelScope.launch {
            val next = com.example.finance.core.common.DateUtils.shiftMonth(_selectedMonth.value, 1)
            preferenceManager.setMonth(next)
        }
    }

    fun prevMonth() {
        viewModelScope.launch {
            val prev = com.example.finance.core.common.DateUtils.shiftMonth(_selectedMonth.value, -1)
            preferenceManager.setMonth(prev)
        }
    }

    fun currentMonth() {
        viewModelScope.launch {
            preferenceManager.setMonth(com.example.finance.core.common.DateUtils.getCurrentMonth())
        }
    }
}
