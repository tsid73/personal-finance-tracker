package com.example.finance.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.finance.data.repository.FinanceRepository
import com.example.finance.data.repository.PreferenceManager
import com.example.finance.data.repository.RemoteSyncManager
import com.example.finance.data.repository.SyncResult
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
    private val remoteSyncManager = RemoteSyncManager()

    private val _summary = MutableStateFlow<DashboardSummary?>(null)
    val summary: StateFlow<DashboardSummary?> = _summary.asStateFlow()

    private val _selectedMonth = MutableStateFlow("")
    val selectedMonth: StateFlow<String> = _selectedMonth.asStateFlow()

    private val _recentActivity = MutableStateFlow<List<com.example.finance.data.entity.ActivityLogEntity>>(emptyList())
    val recentActivity: StateFlow<List<com.example.finance.data.entity.ActivityLogEntity>> = _recentActivity.asStateFlow()

    private val _syncEnabled = MutableStateFlow(false)
    val syncEnabled: StateFlow<Boolean> = _syncEnabled.asStateFlow()

    private val _syncBaseUrl = MutableStateFlow("")
    val syncBaseUrl: StateFlow<String> = _syncBaseUrl.asStateFlow()

    private val _lastSyncResult = MutableStateFlow<SyncResult?>(null)
    val lastSyncResult: StateFlow<SyncResult?> = _lastSyncResult.asStateFlow()

    private val _syncInProgress = MutableStateFlow(false)
    val syncInProgress: StateFlow<Boolean> = _syncInProgress.asStateFlow()

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
        viewModelScope.launch {
            preferenceManager.syncEnabled.collectLatest { _syncEnabled.value = it }
        }
        viewModelScope.launch {
            preferenceManager.syncBaseUrl.collectLatest { _syncBaseUrl.value = it }
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
            val next = com.example.finance.util.DateUtils.shiftMonth(_selectedMonth.value, 1)
            preferenceManager.setMonth(next)
        }
    }

    fun prevMonth() {
        viewModelScope.launch {
            val prev = com.example.finance.util.DateUtils.shiftMonth(_selectedMonth.value, -1)
            preferenceManager.setMonth(prev)
        }
    }

    fun currentMonth() {
        viewModelScope.launch {
            preferenceManager.setMonth(com.example.finance.util.DateUtils.getCurrentMonth())
        }
    }

    fun updateSyncSettings(enabled: Boolean, baseUrl: String) {
        viewModelScope.launch {
            preferenceManager.setSyncEnabled(enabled)
            preferenceManager.setSyncBaseUrl(baseUrl)
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            if (_syncInProgress.value) return@launch
            _syncInProgress.value = true
            try {
                val snapshot = repository.getBackupSnapshot()
                _lastSyncResult.value = remoteSyncManager.sync(_syncBaseUrl.value, snapshot)
            } finally {
                _syncInProgress.value = false
            }
        }
    }
}
