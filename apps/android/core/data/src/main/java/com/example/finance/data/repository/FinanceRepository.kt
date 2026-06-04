package com.example.finance.data.repository

import com.example.finance.data.database.AppDatabase
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
import com.example.finance.domain.model.TransactionKind
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

data class CategoryUsage(
    val transactionCount: Int,
    val budgetCount: Int,
    val recurringCount: Int
) {
    val isInUse: Boolean = transactionCount > 0 || budgetCount > 0 || recurringCount > 0
}

data class BackupSnapshot(
    val accounts: List<AccountEntity>,
    val categories: List<CategoryEntity>,
    val transactions: List<TransactionEntity>,
    val recurring: List<RecurringTransactionEntity>,
    val budgets: List<BudgetEntity>,
    val monthlyTargets: List<MonthlyBudgetTargetEntity>
)

class FinanceRepository(private val db: AppDatabase) {
    private val demoUserId = 1

    suspend fun ensureDemoUserAndSeeds() {
        if (db.userDao().getUserCount() > 0) {
            return
        }
        db.userDao().insertUser(UserEntity(id = demoUserId, fullName = "Demo User", email = "demo@example.com"))

        db.accountDao().insertAccounts(listOf(
            AccountEntity(id = 1, userId = demoUserId, name = "Bank", type = AccountType.BANK, balance = 4285000),
            AccountEntity(id = 2, userId = demoUserId, name = "Cash", type = AccountType.CASH, balance = 240000),
            AccountEntity(id = 3, userId = demoUserId, name = "Credit Card", type = AccountType.CREDIT, balance = -380000),
            AccountEntity(id = 4, userId = demoUserId, name = "UPI", type = AccountType.BANK, balance = 0),
            AccountEntity(id = 5, userId = demoUserId, name = "UPI-Lite", type = AccountType.BANK, balance = 0),
            AccountEntity(id = 6, userId = demoUserId, name = "NEFT", type = AccountType.BANK, balance = 0)
        ))

        db.categoryDao().insertCategories(listOf(
            CategoryEntity(id = 1, userId = null, name = "Groceries", type = TransactionKind.EXPENSE, color = "#0f766e", icon = "shopping-bag", isDefault = true),
            CategoryEntity(id = 2, userId = null, name = "Rent", type = TransactionKind.EXPENSE, color = "#b45309", icon = "home", isDefault = true),
            CategoryEntity(id = 3, userId = null, name = "Utilities", type = TransactionKind.EXPENSE, color = "#2563eb", icon = "bolt", isDefault = true),
            CategoryEntity(id = 4, userId = null, name = "Transport", type = TransactionKind.EXPENSE, color = "#7c3aed", icon = "car", isDefault = true),
            CategoryEntity(id = 5, userId = null, name = "Dining", type = TransactionKind.EXPENSE, color = "#dc2626", icon = "utensils", isDefault = true),
            CategoryEntity(id = 6, userId = null, name = "Healthcare", type = TransactionKind.EXPENSE, color = "#db2777", icon = "heart-pulse", isDefault = true),
            CategoryEntity(id = 7, userId = null, name = "Entertainment", type = TransactionKind.EXPENSE, color = "#0891b2", icon = "film", isDefault = true),
            CategoryEntity(id = 8, userId = null, name = "Shopping", type = TransactionKind.EXPENSE, color = "#ea580c", icon = "shirt", isDefault = true),
            CategoryEntity(id = 9, userId = null, name = "Salary", type = TransactionKind.INCOME, color = "#15803d", icon = "briefcase", isDefault = true),
            CategoryEntity(id = 10, userId = null, name = "Freelance", type = TransactionKind.INCOME, color = "#4338ca", icon = "laptop", isDefault = true),
            CategoryEntity(id = 11, userId = demoUserId, name = "Credit Card", type = TransactionKind.EXPENSE, color = "#475569", icon = "credit-card"),
            CategoryEntity(id = 12, userId = demoUserId, name = "EMI", type = TransactionKind.EXPENSE, color = "#7c2d12", icon = "bank"),
            CategoryEntity(id = 13, userId = demoUserId, name = "Food", type = TransactionKind.EXPENSE, color = "#991b1b", icon = "utensils"),
            CategoryEntity(id = 14, userId = demoUserId, name = "Investment", type = TransactionKind.EXPENSE, color = "#065f46", icon = "trending-up"),
            CategoryEntity(id = 15, userId = demoUserId, name = "Misc", type = TransactionKind.EXPENSE, color = "#52525b", icon = "layers"),
            CategoryEntity(id = 16, userId = demoUserId, name = "Hotel", type = TransactionKind.EXPENSE, color = "#7c2d12", icon = "bed")
        ))

        // Seed historical transactions (007 & 008)
        db.transactionDao().insertTransactions(listOf(
            // Jan 2026
            TransactionEntity(userId = demoUserId, accountId = 4, categoryId = 9, kind = TransactionKind.INCOME, title = "Salary", amount = 13300000, transactionDate = "2026-01-01"),
            TransactionEntity(userId = demoUserId, accountId = 4, categoryId = 13, kind = TransactionKind.EXPENSE, title = "Food", amount = 95000, transactionDate = "2026-01-01"),
            TransactionEntity(userId = demoUserId, accountId = 4, categoryId = 4, kind = TransactionKind.EXPENSE, title = "Petrol", amount = 25000, transactionDate = "2026-01-01"),
            TransactionEntity(userId = demoUserId, accountId = 4, categoryId = 11, kind = TransactionKind.EXPENSE, title = "CC", amount = 113000, transactionDate = "2026-01-02"),
            TransactionEntity(userId = demoUserId, accountId = 4, categoryId = 3, kind = TransactionKind.EXPENSE, title = "Home", amount = 300000, transactionDate = "2026-01-03"),
            TransactionEntity(userId = demoUserId, accountId = 4, categoryId = 15, kind = TransactionKind.EXPENSE, title = "Misc", amount = 170000, transactionDate = "2026-01-03"),
            TransactionEntity(userId = demoUserId, accountId = 4, categoryId = 12, kind = TransactionKind.EXPENSE, title = "EMI", amount = 3100000, transactionDate = "2026-01-05"),
            TransactionEntity(userId = demoUserId, accountId = 4, categoryId = 14, kind = TransactionKind.EXPENSE, title = "Investment", amount = 7000000, transactionDate = "2026-01-12"),
            TransactionEntity(userId = demoUserId, accountId = 4, categoryId = 16, kind = TransactionKind.EXPENSE, title = "Hotel", amount = 200000, transactionDate = "2026-01-20"),
            TransactionEntity(userId = demoUserId, accountId = 4, categoryId = 14, kind = TransactionKind.EXPENSE, title = "ULIP", amount = 249900, transactionDate = "2026-01-24"),
            
            // Feb 2026
            TransactionEntity(userId = demoUserId, accountId = 4, categoryId = 9, kind = TransactionKind.INCOME, title = "Salary", amount = 13300000, transactionDate = "2026-02-01"),
            TransactionEntity(userId = demoUserId, accountId = 4, categoryId = 12, kind = TransactionKind.EXPENSE, title = "EMI", amount = 3100000, transactionDate = "2026-02-05"),
            TransactionEntity(userId = demoUserId, accountId = 4, categoryId = 3, kind = TransactionKind.EXPENSE, title = "Ghar", amount = 2300000, transactionDate = "2026-02-08"),
            TransactionEntity(userId = demoUserId, accountId = 4, categoryId = 4, kind = TransactionKind.EXPENSE, title = "Car", amount = 560000, transactionDate = "2026-02-14"),
            TransactionEntity(userId = demoUserId, accountId = 4, categoryId = 14, kind = TransactionKind.EXPENSE, title = "Investment", amount = 4000000, transactionDate = "2026-02-27"),
            
            // Mar 2026
            TransactionEntity(userId = demoUserId, accountId = 4, categoryId = 9, kind = TransactionKind.INCOME, title = "Salary", amount = 13300000, transactionDate = "2026-03-01"),
            TransactionEntity(userId = demoUserId, accountId = 4, categoryId = 12, kind = TransactionKind.EXPENSE, title = "EMI", amount = 3100000, transactionDate = "2026-03-05"),
            TransactionEntity(userId = demoUserId, accountId = 4, categoryId = 14, kind = TransactionKind.EXPENSE, title = "Investment", amount = 8000000, transactionDate = "2026-03-19"),
            TransactionEntity(userId = demoUserId, accountId = 4, categoryId = 14, kind = TransactionKind.EXPENSE, title = "ULIP", amount = 250000, transactionDate = "2026-03-24"),
            TransactionEntity(userId = demoUserId, accountId = 4, categoryId = 3, kind = TransactionKind.EXPENSE, title = "Home", amount = 1200000, transactionDate = "2026-03-25"),
            
            // Apr 2026
            TransactionEntity(userId = demoUserId, accountId = 1, categoryId = 9, kind = TransactionKind.INCOME, title = "April salary", amount = 8500000, transactionDate = "2026-04-01"),
            TransactionEntity(userId = demoUserId, accountId = 1, categoryId = 2, kind = TransactionKind.EXPENSE, title = "House rent", amount = 2500000, transactionDate = "2026-04-03"),
            TransactionEntity(userId = demoUserId, accountId = 1, categoryId = 3, kind = TransactionKind.EXPENSE, title = "Apay", merchant = "amazon", amount = 150000, transactionDate = "2026-04-03"),
            TransactionEntity(userId = demoUserId, accountId = 1, categoryId = 15, kind = TransactionKind.EXPENSE, title = "Tank Clean", merchant = "UC", amount = 90000, transactionDate = "2026-04-04"),
            TransactionEntity(userId = demoUserId, accountId = 2, categoryId = 3, kind = TransactionKind.EXPENSE, title = "Taxes", merchant = "Nagar Nigam", amount = 130000, transactionDate = "2026-04-05"),
            TransactionEntity(userId = demoUserId, accountId = 1, categoryId = 11, kind = TransactionKind.EXPENSE, title = "CC Payment", amount = 390000, transactionDate = "2026-04-05"),
            TransactionEntity(userId = demoUserId, accountId = 1, categoryId = 12, kind = TransactionKind.EXPENSE, title = "EMI", amount = 3100000, transactionDate = "2026-04-05"),
            TransactionEntity(userId = demoUserId, accountId = 1, categoryId = 15, kind = TransactionKind.EXPENSE, title = "LinkedIn", amount = 50000, transactionDate = "2026-04-11"),
            TransactionEntity(userId = demoUserId, accountId = 2, categoryId = 13, kind = TransactionKind.EXPENSE, title = "Food", amount = 10500, transactionDate = "2026-04-11"),
            TransactionEntity(userId = demoUserId, accountId = 1, categoryId = 13, kind = TransactionKind.EXPENSE, title = "Food", amount = 14000, transactionDate = "2026-04-13"),
            TransactionEntity(userId = demoUserId, accountId = 1, categoryId = 11, kind = TransactionKind.EXPENSE, title = "CC", amount = 470000, transactionDate = "2026-04-13"),
            TransactionEntity(userId = demoUserId, accountId = 1, categoryId = 11, kind = TransactionKind.EXPENSE, title = "CC", amount = 23300, transactionDate = "2026-04-16"),
            TransactionEntity(userId = demoUserId, accountId = 1, categoryId = 3, kind = TransactionKind.EXPENSE, title = "Net + DTH", amount = 93200, transactionDate = "2026-04-18"),
            TransactionEntity(userId = demoUserId, accountId = 1, categoryId = 11, kind = TransactionKind.EXPENSE, title = "CC", amount = 120000, transactionDate = "2026-04-19"),
            TransactionEntity(userId = demoUserId, accountId = 1, categoryId = 14, kind = TransactionKind.EXPENSE, title = "ULIP", amount = 249900, transactionDate = "2026-04-24"),
            TransactionEntity(userId = demoUserId, accountId = 4, categoryId = 15, kind = TransactionKind.EXPENSE, title = "Haircut", amount = 15000, transactionDate = "2026-04-26"),
            TransactionEntity(userId = demoUserId, accountId = 1, categoryId = 15, kind = TransactionKind.EXPENSE, title = "Kite Fund", amount = 10000, transactionDate = "2026-04-28"),
            TransactionEntity(userId = demoUserId, accountId = 1, categoryId = 1, kind = TransactionKind.EXPENSE, title = "Home things", amount = 151300, transactionDate = "2026-04-28")
        ))

        // Seed monthly targets
        db.budgetDao().insertMonthlyTarget(MonthlyBudgetTargetEntity(userId = demoUserId, month = 4, year = 2026, totalBudget = 13000000))

        // Seed budgets
        db.budgetDao().insertBudgets(listOf(
            BudgetEntity(userId = demoUserId, categoryId = 11, month = 4, year = 2026, allocatedAmount = 1500000),
            BudgetEntity(userId = demoUserId, categoryId = 12, month = 4, year = 2026, allocatedAmount = 3100000),
            BudgetEntity(userId = demoUserId, categoryId = 13, month = 4, year = 2026, allocatedAmount = 300000),
            BudgetEntity(userId = demoUserId, categoryId = 14, month = 4, year = 2026, allocatedAmount = 4500000),
            BudgetEntity(userId = demoUserId, categoryId = 15, month = 4, year = 2026, allocatedAmount = 500000),
            BudgetEntity(userId = demoUserId, categoryId = 3, month = 4, year = 2026, allocatedAmount = 500000)
        ))
    }

    // Transactions
    fun getAllTransactions() = db.transactionDao().getAllTransactions(demoUserId)
    fun getTransactionsByMonth(month: String) = db.transactionDao().getTransactionsByMonth(demoUserId, month)
    fun getRecentTransactions(month: String) = db.transactionDao().getRecentTransactions(demoUserId, month)
    suspend fun addTransaction(transaction: TransactionEntity) {
        val id = db.transactionDao().insertTransaction(transaction)
        logActivity("transaction", id.toInt(), "create", "Created transaction ${transaction.title}")
    }
    suspend fun updateTransaction(transaction: TransactionEntity) {
        db.transactionDao().updateTransaction(transaction)
        logActivity("transaction", transaction.id, "update", "Updated transaction ${transaction.title}")
    }
    suspend fun deleteTransaction(transaction: TransactionEntity) {
        db.transactionDao().deleteTransaction(transaction)
        logActivity("transaction", transaction.id, "delete", "Deleted transaction ${transaction.title}")
    }
    suspend fun bulkDeleteTransactions(ids: List<Int>, note: String? = null) {
        db.transactionDao().bulkDelete(ids, demoUserId)
        logActivity("transaction", 0, "bulk_delete", "Deleted ${ids.size} transactions", note)
    }
    suspend fun bulkRecategorizeTransactions(ids: List<Int>, categoryId: Int, note: String? = null) {
        db.transactionDao().bulkRecategorize(ids, categoryId, demoUserId)
        logActivity("transaction", 0, "bulk_recategorize", "Recategorized ${ids.size} transactions", note)
    }

    // Accounts
    fun getAccounts() = db.accountDao().getAccounts(demoUserId)

    // Categories
    fun getCategories(includeArchived: Boolean) = db.categoryDao().getCategories(demoUserId, includeArchived)
    suspend fun addCategory(category: CategoryEntity) {
        val id = db.categoryDao().insertCategory(category)
        logActivity("category", id.toInt(), "create", "Created category ${category.name}")
    }
    suspend fun updateCategory(category: CategoryEntity) {
        db.categoryDao().updateCategory(category)
        logActivity("category", category.id, "update", "Updated category ${category.name}")
    }
    suspend fun deleteCategory(category: CategoryEntity, reassignmentId: Int?) {
        if (reassignmentId != null) {
            db.transactionDao().reassignCategory(category.id, reassignmentId)
            db.budgetDao().reassignCategory(category.id, reassignmentId)
            db.recurringDao().reassignCategory(category.id, reassignmentId)
        }
        db.categoryDao().deleteCategory(category)
        logActivity("category", category.id, "delete", "Deleted category ${category.name}")
    }
    suspend fun archiveCategory(category: CategoryEntity, archived: Boolean) {
        db.categoryDao().updateCategory(category.copy(isArchived = archived))
        logActivity("category", category.id, if (archived) "archive" else "restore", if (archived) "Archived category ${category.name}" else "Restored category ${category.name}")
    }
    suspend fun getCategoryUsage(categoryId: Int): CategoryUsage {
        return CategoryUsage(
            transactionCount = db.transactionDao().getTransactionCountByCategory(categoryId),
            budgetCount = db.budgetDao().getBudgetCountByCategory(categoryId),
            recurringCount = db.recurringDao().getRecurringCountByCategory(categoryId)
        )
    }

    // Budgets
    fun getAllBudgets() = db.budgetDao().getAllBudgets(demoUserId)
    fun getBudgetsByMonth(month: Int, year: Int) = db.budgetDao().getBudgetsByMonth(demoUserId, month, year)
    fun getMonthlyTarget(month: Int, year: Int) = db.budgetDao().getMonthlyTarget(demoUserId, month, year)
    suspend fun setMonthlyTarget(month: Int, year: Int, total: Long) {
        db.budgetDao().insertMonthlyTarget(MonthlyBudgetTargetEntity(userId = demoUserId, month = month, year = year, totalBudget = total))
    }
    suspend fun setCategoryBudget(budget: BudgetEntity) {
        val id = db.budgetDao().insertBudget(budget)
        logActivity("budget", id.toInt(), "create", "Saved budget allocation")
    }
    suspend fun updateBudget(budget: BudgetEntity) {
        db.budgetDao().updateBudget(budget)
        logActivity("budget", budget.id, "update", "Updated budget allocation")
    }
    suspend fun deleteBudget(budget: BudgetEntity) {
        db.budgetDao().deleteBudget(budget)
        logActivity("budget", budget.id, "delete", "Deleted budget allocation")
    }

    // Recurring
    fun getRecurringTransactions() = db.recurringDao().getRecurringTransactions(demoUserId)
    suspend fun addRecurring(recurring: RecurringTransactionEntity) {
        val id = db.recurringDao().insertRecurring(recurring)
        logActivity("recurring_transaction", id.toInt(), "create", "Created recurring schedule ${recurring.title}")
    }
    suspend fun updateRecurring(recurring: RecurringTransactionEntity) {
        db.recurringDao().updateRecurring(recurring)
        logActivity("recurring_transaction", recurring.id, "update", "Updated recurring schedule ${recurring.title}")
    }
    suspend fun deleteRecurring(recurring: RecurringTransactionEntity) {
        db.recurringDao().deleteRecurring(recurring)
        logActivity("recurring_transaction", recurring.id, "delete", "Deleted recurring schedule ${recurring.title}")
    }
    suspend fun getRecurringById(id: Int) = db.recurringDao().getRecurringById(id)

    // Activity
    fun getRecentActivity() = db.activityDao().getRecentActivity(demoUserId)

    private suspend fun logActivity(type: String, id: Int, action: String, title: String, note: String? = null) {
        db.activityDao().insertLog(
            ActivityLogEntity(
                userId = demoUserId,
                entityType = type,
                entityId = id,
                action = action,
                title = title,
                note = note,
                source = "local"
            )
        )
    }

    suspend fun getMonthlyTotal(month: String, kind: TransactionKind): Long = db.transactionDao().getMonthlyTotal(demoUserId, month, kind) ?: 0L
    suspend fun getTotalAllocated(month: Int, year: Int): Long = db.budgetDao().getTotalAllocated(demoUserId, month, year) ?: 0L
    suspend fun getBackupSnapshot(): BackupSnapshot {
        return BackupSnapshot(
            accounts = db.accountDao().getAccounts(demoUserId).firstOrNull() ?: emptyList(),
            categories = db.categoryDao().getCategories(demoUserId, true).firstOrNull() ?: emptyList(),
            transactions = db.transactionDao().getAllTransactions(demoUserId).firstOrNull() ?: emptyList(),
            recurring = db.recurringDao().getRecurringTransactions(demoUserId).firstOrNull() ?: emptyList(),
            budgets = db.budgetDao().getAllBudgets(demoUserId).firstOrNull() ?: emptyList(),
            monthlyTargets = getAllMonthlyTargets()
        )
    }

    suspend fun getLocalBackupDocument(appSettings: AppSettingsSnapshot): LocalBackupDocument {
        val users = listOfNotNull(db.userDao().getUserById(demoUserId))
        return LocalBackupDocument(
            formatVersion = BackupValidator.currentFormatVersion,
            exportedAt = System.currentTimeMillis(),
            appSettings = appSettings,
            users = users,
            accounts = db.accountDao().getAccounts(demoUserId).firstOrNull() ?: emptyList(),
            categories = db.categoryDao().getCategories(demoUserId, true).firstOrNull() ?: emptyList(),
            transactions = db.transactionDao().getAllTransactions(demoUserId).firstOrNull() ?: emptyList(),
            recurring = db.recurringDao().getRecurringTransactions(demoUserId).firstOrNull() ?: emptyList(),
            budgets = db.budgetDao().getAllBudgets(demoUserId).firstOrNull() ?: emptyList(),
            monthlyTargets = getAllMonthlyTargets(),
            activityLogs = db.activityDao().getAllActivity(demoUserId)
        )
    }

    suspend fun restoreLocalBackup(document: LocalBackupDocument): RestoreValidationResult {
        val validation = BackupValidator.validate(document)

        db.withTransaction {
            db.activityDao().deleteAll()
            db.transactionDao().deleteAll()
            db.recurringDao().deleteAll()
            db.budgetDao().deleteAllBudgets()
            db.budgetDao().deleteAllMonthlyTargets()
            db.categoryDao().deleteAll()
            db.accountDao().deleteAll()
            db.userDao().deleteAll()

            db.userDao().insertUsers(document.users)
            db.accountDao().insertAccounts(document.accounts)
            db.categoryDao().insertCategories(document.categories)
            db.recurringDao().insertRecurringList(document.recurring)
            db.transactionDao().insertTransactions(document.transactions)
            db.budgetDao().insertBudgets(document.budgets)
            db.budgetDao().insertMonthlyTargets(document.monthlyTargets)
            db.activityDao().insertLogs(
                document.activityLogs + ActivityLogEntity(
                    userId = demoUserId,
                    entityType = "backup",
                    entityId = null,
                    action = "restore",
                    title = "Restored app data from backup",
                    source = "backup"
                )
            )
        }

        return validation
    }

    suspend fun calculateBudgetBreakdown(monthKey: String): Triple<Long, Long, Long> {
        val parts = monthKey.split("-").map { it.toInt() }
        val month = parts[1]
        val year = parts[0]
        val budgets = db.budgetDao().getBudgetsByMonth(demoUserId, month, year).firstOrNull() ?: emptyList()
        val categories = db.categoryDao().getCategories(demoUserId, true).firstOrNull() ?: emptyList()
        val budgetCategoryIds = budgets.map { it.categoryId }.toSet()
        val transactions = db.transactionDao().getTransactionsByMonth(demoUserId, monthKey).firstOrNull() ?: emptyList()

        val spent = transactions
            .filter { it.kind == TransactionKind.EXPENSE && it.categoryId in budgetCategoryIds }
            .sumOf { it.amount }

        val fixed = budgets.sumOf { budget ->
            if (categories.find { it.id == budget.categoryId }?.budgetMode == BudgetMode.FIXED) budget.allocatedAmount else 0L
        }
        val flexible = budgets.sumOf { budget ->
            if (categories.find { it.id == budget.categoryId }?.budgetMode == BudgetMode.FLEXIBLE) budget.allocatedAmount else 0L
        }

        return Triple(spent, fixed, flexible)
    }

    private suspend fun getAllMonthlyTargets(): List<MonthlyBudgetTargetEntity> {
        return db.budgetDao().getAllMonthlyTargets(demoUserId)
    }
}
