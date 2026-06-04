package com.example.finance.backup

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.finance.FinanceApplication
import com.example.finance.data.entity.AccountEntity
import com.example.finance.data.entity.ActivityLogEntity
import com.example.finance.data.entity.BudgetEntity
import com.example.finance.data.entity.CategoryEntity
import com.example.finance.data.entity.MonthlyBudgetTargetEntity
import com.example.finance.data.entity.RecurringTransactionEntity
import com.example.finance.data.entity.TransactionEntity
import com.example.finance.data.entity.UserEntity
import com.example.finance.data.repository.AppSettingsSnapshot
import com.example.finance.data.repository.LocalBackupDocument
import com.example.finance.domain.model.AccountType
import com.example.finance.domain.model.BudgetMode
import com.example.finance.domain.model.Frequency
import com.example.finance.domain.model.TransactionKind
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BackupRestoreFlowTest {
    @Test
    fun restoreReplacesDataAndSettingsEndToEnd() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val application = context.applicationContext as FinanceApplication
        val document = createDocument()

        val result = application.repository.restoreLocalBackup(document)
        application.preferenceManager.restoreSnapshot(document.appSettings)

        val restoredTransactions = application.repository.getAllTransactions().collectForTest()
        val restoredAccounts = application.repository.getAccounts().collectForTest()
        val restoredCategories = application.repository.getCategories(true).collectForTest()
        val restoredRecurring = application.repository.getRecurringTransactions().collectForTest()
        val restoredSettings = application.preferenceManager.getSnapshot()

        assertEquals(2, result.transactionCount)
        assertEquals(listOf("Second", "First"), restoredTransactions.map { it.title })
        assertEquals(listOf("Cash", "Main Bank"), restoredAccounts.map { it.name }.sorted())
        assertEquals(listOf("Food", "Salary"), restoredCategories.map { it.name }.sorted())
        assertEquals(1, restoredRecurring.size)
        assertTrue(restoredTransactions.all { transaction ->
            restoredAccounts.any { account -> account.id == transaction.accountId } &&
                restoredCategories.any { category -> category.id == transaction.categoryId }
        })
        assertEquals(document.appSettings.selectedMonth, restoredSettings.selectedMonth)
        assertEquals(document.appSettings.biometricEnabled, restoredSettings.biometricEnabled)
        assertEquals(document.appSettings.darkTheme, restoredSettings.darkTheme)
    }

    private suspend fun <T> kotlinx.coroutines.flow.Flow<List<T>>.collectForTest(): List<T> {
        return first()
    }

    private fun createDocument(): LocalBackupDocument {
        return LocalBackupDocument(
            formatVersion = 1,
            exportedAt = 10,
            appSettings = AppSettingsSnapshot(
                darkTheme = false,
                selectedMonth = "2026-06",
                syncEnabled = false,
                syncBaseUrl = "",
                biometricEnabled = true
            ),
            users = listOf(UserEntity(id = 1, fullName = "Demo User", email = "demo@example.com", createdAt = 1)),
            accounts = listOf(
                AccountEntity(id = 1, userId = 1, name = "Main Bank", type = AccountType.BANK, balance = 100_00),
                AccountEntity(id = 2, userId = 1, name = "Cash", type = AccountType.CASH, balance = 50_00)
            ),
            categories = listOf(
                CategoryEntity(id = 1, userId = 1, name = "Food", type = TransactionKind.EXPENSE, budgetMode = BudgetMode.FLEXIBLE, createdAt = 2),
                CategoryEntity(id = 2, userId = 1, name = "Salary", type = TransactionKind.INCOME, budgetMode = BudgetMode.FLEXIBLE, createdAt = 3)
            ),
            transactions = listOf(
                TransactionEntity(id = 1, userId = 1, accountId = 1, categoryId = 1, recurringTransactionId = 1, kind = TransactionKind.EXPENSE, title = "First", amount = 1000, transactionDate = "2026-06-01", createdAt = 4),
                TransactionEntity(id = 2, userId = 1, accountId = 2, categoryId = 2, kind = TransactionKind.INCOME, title = "Second", amount = 2000, transactionDate = "2026-06-10", createdAt = 5)
            ),
            recurring = listOf(
                RecurringTransactionEntity(
                    id = 1,
                    userId = 1,
                    accountId = 1,
                    categoryId = 1,
                    kind = TransactionKind.EXPENSE,
                    title = "Lunch",
                    amount = 1000,
                    frequency = Frequency.MONTHLY,
                    dayOfMonth = 1,
                    startDate = "2026-06-01",
                    nextDueDate = "2026-07-01",
                    createdAt = 6
                )
            ),
            budgets = listOf(
                BudgetEntity(id = 1, userId = 1, categoryId = 1, month = 6, year = 2026, allocatedAmount = 5000, createdAt = 7)
            ),
            monthlyTargets = listOf(
                MonthlyBudgetTargetEntity(id = 1, userId = 1, month = 6, year = 2026, totalBudget = 10000, createdAt = 8)
            ),
            activityLogs = listOf(
                ActivityLogEntity(id = 1, userId = 1, entityType = "backup", action = "create", title = "Created backup", source = "backup", createdAt = 9)
            )
        )
    }
}
