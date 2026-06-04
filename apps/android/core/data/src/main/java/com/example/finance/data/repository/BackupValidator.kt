package com.example.finance.data.repository

import com.example.finance.core.common.DateUtils
import java.time.YearMonth

object BackupValidator {
    const val currentFormatVersion = 1
    private const val demoUserId = 1

    fun validate(document: LocalBackupDocument): RestoreValidationResult {
        require(document.formatVersion in 1..currentFormatVersion) { "Unsupported backup format." }
        require(document.exportedAt > 0) { "Backup export timestamp is invalid." }
        require(document.users.size == 1) { "Backup must contain exactly one user." }

        val user = document.users.single()
        require(user.id == demoUserId) { "Backup user id is invalid." }
        require(runCatching { YearMonth.parse(document.appSettings.selectedMonth) }.isSuccess) { "Selected month is invalid." }

        val accountIds = document.accounts.map { it.id }
        val categoryIds = document.categories.map { it.id }
        val recurringIds = document.recurring.map { it.id }

        require(accountIds.distinct().size == accountIds.size) { "Backup contains duplicate account ids." }
        require(categoryIds.distinct().size == categoryIds.size) { "Backup contains duplicate category ids." }
        require(recurringIds.distinct().size == recurringIds.size) { "Backup contains duplicate recurring ids." }
        require(document.transactions.map { it.id }.distinct().size == document.transactions.size) { "Backup contains duplicate transaction ids." }
        require(document.budgets.map { it.id }.distinct().size == document.budgets.size) { "Backup contains duplicate budget ids." }
        require(document.monthlyTargets.map { it.id }.distinct().size == document.monthlyTargets.size) { "Backup contains duplicate target ids." }
        require(document.activityLogs.map { it.id }.distinct().size == document.activityLogs.size) { "Backup contains duplicate activity ids." }
        require(document.accounts.map { it.name.trim().lowercase() }.distinct().size == document.accounts.size) {
            "Backup contains duplicate account names."
        }
        require(
            document.categories
                .map { Triple(it.userId ?: 0, it.type, it.name.trim().lowercase()) }
                .distinct()
                .size == document.categories.size
        ) {
            "Backup contains duplicate category names."
        }

        require(user.createdAt > 0) { "Backup user timestamp is invalid." }

        document.accounts.forEach {
            require(it.userId == demoUserId) { "Backup account has invalid user id." }
        }

        document.categories.forEach {
            require(it.userId == null || it.userId == demoUserId) { "Backup category has invalid user id." }
            require(it.createdAt > 0) { "Backup category timestamp is invalid." }
        }

        document.transactions.forEach {
            require(it.userId == demoUserId) { "Backup transaction has invalid user id." }
            require(it.accountId in accountIds) { "Backup transaction references a missing account." }
            require(it.categoryId in categoryIds) { "Backup transaction references a missing category." }
            require(DateUtils.parseDateOrNull(it.transactionDate) != null) { "Backup transaction date is invalid." }
            require(it.createdAt > 0) { "Backup transaction timestamp is invalid." }
            require(it.recurringTransactionId == null || it.recurringTransactionId in recurringIds) {
                "Backup transaction references a missing recurring schedule."
            }
        }

        document.recurring.forEach {
            require(it.userId == demoUserId) { "Backup recurring schedule has invalid user id." }
            require(it.accountId in accountIds) { "Backup recurring schedule references a missing account." }
            require(it.categoryId in categoryIds) { "Backup recurring schedule references a missing category." }
            require(DateUtils.parseDateOrNull(it.startDate) != null) { "Backup recurring start date is invalid." }
            require(DateUtils.parseDateOrNull(it.nextDueDate) != null) { "Backup recurring next due date is invalid." }
            require(it.dayOfMonth in 1..31) { "Backup recurring day of month is invalid." }
            require(it.createdAt > 0) { "Backup recurring timestamp is invalid." }
        }

        document.budgets.forEach {
            require(it.userId == demoUserId) { "Backup budget has invalid user id." }
            require(it.categoryId in categoryIds) { "Backup budget references a missing category." }
            require(it.month in 1..12) { "Backup budget month is invalid." }
            require(it.createdAt > 0) { "Backup budget timestamp is invalid." }
        }

        document.monthlyTargets.forEach {
            require(it.userId == demoUserId) { "Backup target has invalid user id." }
            require(it.month in 1..12) { "Backup target month is invalid." }
            require(it.createdAt > 0) { "Backup target timestamp is invalid." }
        }

        document.activityLogs.forEach {
            require(it.userId == demoUserId) { "Backup activity log has invalid user id." }
            require(it.createdAt > 0) { "Backup activity timestamp is invalid." }
            require(it.source.isNotBlank()) { "Backup activity source is invalid." }
        }

        return RestoreValidationResult(
            transactionCount = document.transactions.size,
            recurringCount = document.recurring.size,
            budgetCount = document.budgets.size,
            accountCount = document.accounts.size,
            categoryCount = document.categories.size
        )
    }
}
