package com.example.finance.data.repository

import com.example.finance.data.entity.AccountEntity
import com.example.finance.data.entity.ActivityLogEntity
import com.example.finance.data.entity.BudgetEntity
import com.example.finance.data.entity.CategoryEntity
import com.example.finance.data.entity.MonthlyBudgetTargetEntity
import com.example.finance.data.entity.RecurringTransactionEntity
import com.example.finance.data.entity.TransactionEntity
import com.example.finance.data.entity.UserEntity

data class AppSettingsSnapshot(
    val darkTheme: Boolean?,
    val selectedMonth: String,
    val syncEnabled: Boolean,
    val syncBaseUrl: String,
    val biometricEnabled: Boolean
)

data class LocalBackupDocument(
    val formatVersion: Int,
    val exportedAt: Long,
    val appSettings: AppSettingsSnapshot,
    val users: List<UserEntity>,
    val accounts: List<AccountEntity>,
    val categories: List<CategoryEntity>,
    val transactions: List<TransactionEntity>,
    val recurring: List<RecurringTransactionEntity>,
    val budgets: List<BudgetEntity>,
    val monthlyTargets: List<MonthlyBudgetTargetEntity>,
    val activityLogs: List<ActivityLogEntity>
)

data class RestoreValidationResult(
    val transactionCount: Int,
    val recurringCount: Int,
    val budgetCount: Int,
    val accountCount: Int,
    val categoryCount: Int
)
