package com.example.finance.data.repository

import com.example.finance.data.entity.AccountEntity
import com.example.finance.data.entity.ActivityLogEntity
import com.example.finance.data.entity.BudgetEntity
import com.example.finance.data.entity.CategoryEntity
import com.example.finance.data.entity.MonthlyBudgetTargetEntity
import com.example.finance.data.entity.RecurringTransactionEntity
import com.example.finance.data.entity.TransactionEntity
import com.example.finance.data.entity.UserEntity
import com.example.finance.domain.model.AccountType
import com.example.finance.domain.model.BudgetMode
import com.example.finance.domain.model.Frequency
import com.example.finance.domain.model.TransactionKind
import org.junit.Assert.assertEquals
import org.junit.Test

class BackupValidatorTest {
    @Test
    fun validatesWellFormedBackup() {
        val result = BackupValidator.validate(createDocument())
        assertEquals(1, result.accountCount)
        assertEquals(1, result.transactionCount)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsMissingAccountReference() {
        val document = createDocument(
            transactions = listOf(
                TransactionEntity(
                    id = 1,
                    userId = 1,
                    accountId = 999,
                    categoryId = 1,
                    kind = TransactionKind.EXPENSE,
                    title = "Groceries",
                    amount = 1200,
                    transactionDate = "2026-05-10"
                )
            )
        )
        BackupValidator.validate(document)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsDuplicateAccountNames() {
        val document = createDocument(
            accounts = listOf(
                AccountEntity(id = 1, userId = 1, name = "Bank", type = AccountType.BANK, balance = 1000),
                AccountEntity(id = 2, userId = 1, name = "bank", type = AccountType.BANK, balance = 2000)
            )
        )
        BackupValidator.validate(document)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsMalformedTimestamps() {
        val document = createDocument(
            activityLogs = listOf(
                ActivityLogEntity(
                    id = 1,
                    userId = 1,
                    entityType = "backup",
                    action = "create",
                    title = "Created backup",
                    source = "backup",
                    createdAt = 0
                )
            )
        )
        BackupValidator.validate(document)
    }

    private fun createDocument(
        accounts: List<AccountEntity> = listOf(AccountEntity(id = 1, userId = 1, name = "Bank", type = AccountType.BANK, balance = 1000)),
        transactions: List<TransactionEntity> = listOf(
            TransactionEntity(
                id = 1,
                userId = 1,
                accountId = 1,
                categoryId = 1,
                recurringTransactionId = 1,
                kind = TransactionKind.EXPENSE,
                title = "Groceries",
                amount = 1200,
                transactionDate = "2026-05-10",
                createdAt = 2
            )
        ),
        activityLogs: List<ActivityLogEntity> = listOf(
            ActivityLogEntity(
                id = 1,
                userId = 1,
                entityType = "backup",
                action = "create",
                title = "Created backup",
                source = "backup",
                createdAt = 8
            )
        )
    ): LocalBackupDocument {
        return LocalBackupDocument(
            formatVersion = 1,
            exportedAt = 1L,
            appSettings = AppSettingsSnapshot(
                darkTheme = true,
                selectedMonth = "2026-05",
                syncEnabled = false,
                syncBaseUrl = "",
                biometricEnabled = true
            ),
            users = listOf(UserEntity(id = 1, fullName = "Demo User", email = "demo@example.com", createdAt = 1)),
            accounts = accounts,
            categories = listOf(
                CategoryEntity(
                    id = 1,
                    userId = 1,
                    name = "Groceries",
                    type = TransactionKind.EXPENSE,
                    color = "#0f766e",
                    icon = "shopping-bag",
                    isDefault = false,
                    isArchived = false,
                    budgetMode = BudgetMode.FLEXIBLE,
                    createdAt = 3
                )
            ),
            transactions = transactions,
            recurring = listOf(
                RecurringTransactionEntity(
                    id = 1,
                    userId = 1,
                    accountId = 1,
                    categoryId = 1,
                    kind = TransactionKind.EXPENSE,
                    title = "Groceries",
                    amount = 1200,
                    frequency = Frequency.MONTHLY,
                    dayOfMonth = 10,
                    startDate = "2026-05-10",
                    nextDueDate = "2026-06-10",
                    createdAt = 4
                )
            ),
            budgets = listOf(BudgetEntity(id = 1, userId = 1, categoryId = 1, month = 5, year = 2026, allocatedAmount = 5000, createdAt = 5)),
            monthlyTargets = listOf(MonthlyBudgetTargetEntity(id = 1, userId = 1, month = 5, year = 2026, totalBudget = 10000, createdAt = 6)),
            activityLogs = activityLogs
        )
    }
}
