package com.example.finance.domain.usecase

import com.example.finance.data.repository.FinanceRepository
import com.example.finance.domain.model.DashboardSummary
import com.example.finance.domain.model.TransactionKind
import com.example.finance.core.common.DateUtils
import kotlinx.coroutines.flow.first

class CalculateDashboardSummaryUseCase(private val repository: FinanceRepository) {
    suspend operator fun invoke(monthKey: String): DashboardSummary {
        val income = repository.getMonthlyTotal(monthKey, TransactionKind.INCOME)
        val expense = repository.getMonthlyTotal(monthKey, TransactionKind.EXPENSE)
        
        val parts = monthKey.split("-").map { it.toInt() }
        val month = parts[1]
        val year = parts[0]
        
        val target = repository.getMonthlyTarget(month, year).first()?.totalBudget ?: 0L
        val allocated = repository.getTotalAllocated(month, year)
        val (budgetSpent, fixedBudget, flexibleBudget) = repository.calculateBudgetBreakdown(monthKey)
        
        val remainingDays = DateUtils.getRemainingDays(monthKey)
        val safeToSpend = if (remainingDays > 0) (target - expense) / remainingDays else 0L

        val transactions = repository.getAllTransactions().first()
            .filter { it.kind == TransactionKind.EXPENSE && it.transactionDate.startsWith("$monthKey-") }
            .sortedBy { it.transactionDate }

        val daysInMonth = DateUtils.getDaysInMonth(monthKey)
        val dailySpent = LongArray(daysInMonth) { 0L }
        transactions.forEach { txn ->
            val day = txn.transactionDate.takeLast(2).toInt()
            if (day in 1..daysInMonth) {
                dailySpent[day - 1] += txn.amount
            }
        }
        val dailySpentCumulative = mutableListOf<Long>()
        var runningTotal = 0L
        for (i in 0 until daysInMonth) {
            runningTotal += dailySpent[i]
            dailySpentCumulative.add(runningTotal)
        }
        
        return DashboardSummary(
            monthlyIncome = income,
            monthlyExpense = expense,
            totalBudget = target,
            budgetAllocated = allocated,
            budgetSpent = budgetSpent,
            remainingBudget = target - expense,
            availableToAllocate = target - allocated,
            savingsRate = if (income > 0) ((income - expense).toDouble() / income) * 100 else 0.0,
            safeToSpend = safeToSpend,
            remainingDays = remainingDays,
            fixedBudget = fixedBudget,
            flexibleBudget = flexibleBudget,
            dailySpentCumulative = dailySpentCumulative
        )
    }
}
