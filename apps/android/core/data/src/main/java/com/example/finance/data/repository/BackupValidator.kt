package com.example.finance.data.repository

import com.example.finance.core.common.DateUtils
import java.time.YearMonth

object BackupValidator {
    const val currentFormatVersion = 1
    private const val demoUserId = 1

    fun validate(document: LocalBackupDocument): RestoreValidationResult {
        require(document.formatVersion in 1..currentFormatVersion) { "Unsupported backup format (version ${document.formatVersion})." }
        require(document.exportedAt > 0) { "Backup export timestamp is invalid." }
        
        val users = document.users ?: emptyList()
        require(users.size == 1) { "Backup must contain exactly one user." }

        val user = users.single()
        require(user.id == demoUserId) { "Backup user id is invalid (found ${user.id}, expected $demoUserId)." }
        require(document.appSettings != null) { "Backup app settings are missing." }
        val selectedMonth = document.appSettings.selectedMonth
        require(selectedMonth != null && runCatching { YearMonth.parse(selectedMonth) }.isSuccess) { "Selected month is invalid: $selectedMonth" }

        val accounts = document.accounts ?: emptyList()
        val categories = document.categories ?: emptyList()
        val transactions = document.transactions ?: emptyList()
        val recurring = document.recurring ?: emptyList()
        val budgets = document.budgets ?: emptyList()
        val monthlyTargets = document.monthlyTargets ?: emptyList()
        val activityLogs = document.activityLogs ?: emptyList()

        val accountIds = accounts.map { it.id }
        val categoryIds = categories.map { it.id }
        val recurringIds = recurring.map { it.id }

        require(accountIds.distinct().size == accountIds.size) { "Backup contains duplicate account ids." }
        require(categoryIds.distinct().size == categoryIds.size) { "Backup contains duplicate category ids." }
        require(recurringIds.distinct().size == recurringIds.size) { "Backup contains duplicate recurring ids." }
        require(transactions.map { it.id }.distinct().size == transactions.size) { "Backup contains duplicate transaction ids." }
        require(budgets.map { it.id }.distinct().size == budgets.size) { "Backup contains duplicate budget ids." }
        require(monthlyTargets.map { it.id }.distinct().size == monthlyTargets.size) { "Backup contains duplicate target ids." }
        require(activityLogs.map { it.id }.distinct().size == activityLogs.size) { "Backup contains duplicate activity ids." }
        
        require(accounts.map { it.name.trim().lowercase() }.distinct().size == accounts.size) {
            "Backup contains duplicate account names."
        }
        require(
            categories
                .map { Triple(it.userId ?: 0, it.type, it.name.trim().lowercase()) }
                .distinct()
                .size == categories.size
        ) {
            "Backup contains duplicate category names."
        }

        // Lenient check for createdAt to support older backups where it might be 0
        // require(user.createdAt > 0) { "Backup user timestamp is invalid." }

        accounts.forEach {
            require(it.userId == demoUserId) { "Backup account '${it.name}' has invalid user id." }
        }

        categories.forEach {
            require(it.userId == null || it.userId == demoUserId) { "Backup category '${it.name}' has invalid user id." }
        }

        transactions.forEach {
            require(it.userId == demoUserId) { "Backup transaction '${it.title}' has invalid user id." }
            require(it.accountId in accountIds) { "Backup transaction '${it.title}' references a missing account (id ${it.accountId})." }
            require(it.categoryId in categoryIds) { "Backup transaction '${it.title}' references a missing category (id ${it.categoryId})." }
            require(DateUtils.parseDateOrNull(it.transactionDate) != null) { "Backup transaction '${it.title}' date is invalid: ${it.transactionDate}" }
            require(it.recurringTransactionId == null || it.recurringTransactionId in recurringIds) {
                "Backup transaction '${it.title}' references a missing recurring schedule."
            }
        }

        recurring.forEach {
            require(it.userId == demoUserId) { "Backup recurring schedule '${it.title}' has invalid user id." }
            require(it.accountId in accountIds) { "Backup recurring schedule '${it.title}' references a missing account." }
            require(it.categoryId in categoryIds) { "Backup recurring schedule '${it.title}' references a missing category." }
            require(DateUtils.parseDateOrNull(it.startDate) != null) { "Backup recurring start date is invalid." }
            require(DateUtils.parseDateOrNull(it.nextDueDate) != null) { "Backup recurring next due date is invalid." }
            require(it.dayOfMonth in 1..31) { "Backup recurring day of month is invalid." }
        }

        budgets.forEach {
            require(it.userId == demoUserId) { "Backup budget has invalid user id." }
            require(it.categoryId in categoryIds) { "Backup budget references a missing category." }
            require(it.month in 1..12) { "Backup budget month is invalid." }
        }

        monthlyTargets.forEach {
            require(it.userId == demoUserId) { "Backup target has invalid user id." }
            require(it.month in 1..12) { "Backup target month is invalid." }
        }

        activityLogs.forEach {
            require(it.userId == demoUserId) { "Backup activity log has invalid user id." }
        }

        return RestoreValidationResult(
            transactionCount = transactions.size,
            recurringCount = recurring.size,
            budgetCount = budgets.size,
            accountCount = accounts.size,
            categoryCount = categories.size
        )
    }
}
