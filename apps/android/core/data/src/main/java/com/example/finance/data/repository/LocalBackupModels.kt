package com.example.finance.data.repository

import com.example.finance.data.entity.AccountEntity
import com.example.finance.data.entity.ActivityLogEntity
import com.example.finance.data.entity.BudgetEntity
import com.example.finance.data.entity.CategoryEntity
import com.example.finance.data.entity.MonthlyBudgetTargetEntity
import com.example.finance.data.entity.RecurringTransactionEntity
import com.example.finance.data.entity.TransactionEntity
import com.example.finance.data.entity.UserEntity
import com.google.gson.annotations.SerializedName
import androidx.annotation.Keep

@Keep
data class AppSettingsSnapshot(
    @SerializedName("darkTheme") val darkTheme: Boolean?,
    @SerializedName("selectedMonth") val selectedMonth: String?,
    @SerializedName("syncEnabled") val syncEnabled: Boolean?,
    @SerializedName("syncBaseUrl") val syncBaseUrl: String?,
    @SerializedName("biometricEnabled") val biometricEnabled: Boolean?
)

@Keep
data class LocalBackupDocument(
    @SerializedName("formatVersion") val formatVersion: Int,
    @SerializedName("exportedAt") val exportedAt: Long,
    @SerializedName("appSettings") val appSettings: AppSettingsSnapshot?,
    @SerializedName("users") val users: List<UserEntity>?,
    @SerializedName("accounts") val accounts: List<AccountEntity>?,
    @SerializedName("categories") val categories: List<CategoryEntity>?,
    @SerializedName("transactions") val transactions: List<TransactionEntity>?,
    @SerializedName("recurring") val recurring: List<RecurringTransactionEntity>?,
    @SerializedName("budgets") val budgets: List<BudgetEntity>?,
    @SerializedName("monthlyTargets") val monthlyTargets: List<MonthlyBudgetTargetEntity>?,
    @SerializedName("activityLogs") val activityLogs: List<ActivityLogEntity>?
)

@Keep
data class RestoreValidationResult(
    val transactionCount: Int,
    val recurringCount: Int,
    val budgetCount: Int,
    val accountCount: Int,
    val categoryCount: Int
)
