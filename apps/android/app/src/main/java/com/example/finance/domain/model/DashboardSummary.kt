package com.example.finance.domain.model

data class DashboardSummary(
    val monthlyIncome: Long,
    val monthlyExpense: Long,
    val totalBudget: Long,
    val budgetAllocated: Long,
    val budgetSpent: Long,
    val remainingBudget: Long,
    val availableToAllocate: Long,
    val savingsRate: Double,
    val safeToSpend: Long,
    val remainingDays: Int,
    val fixedBudget: Long,
    val flexibleBudget: Long
)
