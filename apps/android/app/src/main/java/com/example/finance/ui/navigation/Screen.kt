package com.example.finance.ui.navigation

sealed class Screen(
    val route: String,
    val title: String,
    val supportsMonthControl: Boolean = false,
    val showInBottomNav: Boolean = true
) {
    object Dashboard : Screen("dashboard", "Dashboard", supportsMonthControl = true)
    object Transactions : Screen("transactions", "Transactions", supportsMonthControl = true)
    object Recurring : Screen("recurring", "Recurring")
    object Budgets : Screen("budgets", "Budgets", supportsMonthControl = true)
    object Categories : Screen("categories", "Categories")
    object Reports : Screen("reports", "Reports", supportsMonthControl = true)
    object Settings : Screen("settings", "Settings")

    companion object {
        val entries = listOf(Dashboard, Transactions, Recurring, Budgets, Categories, Reports, Settings)
    }
}
