package com.example.finance.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.finance.FinanceApplication
import com.example.finance.ui.navigation.Screen
import com.example.finance.ui.screens.DashboardScreen
import com.example.finance.ui.screens.DashboardViewModel
import com.example.finance.util.DateUtils
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable
fun AppShell(
    application: FinanceApplication,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val selectedMonth by application.preferenceManager.selectedMonth.collectAsState(initial = DateUtils.getCurrentMonth())
    val scope = rememberCoroutineScope()
    var showMonthPicker by remember { androidx.compose.runtime.mutableStateOf(false) }
    val currentScreen = remember(currentDestination) {
        Screen.entries.firstOrNull { screen ->
            currentDestination?.hierarchy?.any { it.route == screen.route } == true
        } ?: Screen.Dashboard
    }

    val screens = Screen.entries.filter { it.showInBottomNav }

    Scaffold(
        topBar = {
            Column {
                if (currentScreen.supportsMonthControl) {
                    ShellMonthBar(
                        selectedMonth = selectedMonth,
                        isDarkTheme = isDarkTheme,
                        onPrev = {
                            scope.launch {
                                application.preferenceManager.setMonth(DateUtils.shiftMonth(selectedMonth, -1))
                            }
                        },
                        onNext = {
                            scope.launch {
                                application.preferenceManager.setMonth(DateUtils.shiftMonth(selectedMonth, 1))
                            }
                        },
                        onCurrent = {
                            showMonthPicker = true
                        },
                        onToggleTheme = onToggleTheme
                    )
                } else {
                    ShellThemeBar(
                        isDarkTheme = isDarkTheme,
                        onToggleTheme = onToggleTheme
                    )
                }
            }
        },
        bottomBar = {
            NavigationBar {
                screens.forEach { screen ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                when (screen) {
                                    Screen.Dashboard -> Icons.Default.Dashboard
                                    Screen.Transactions -> Icons.Default.List
                                    Screen.Recurring -> Icons.Default.Repeat
                                    Screen.Budgets -> Icons.Default.Wallet
                                    Screen.Categories -> Icons.Default.Settings
                                    Screen.Reports -> Icons.Default.BarChart
                                },
                                contentDescription = screen.title
                            )
                        },
                        label = null,
                        alwaysShowLabel = false,
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = {
                fadeIn(animationSpec = tween(220)) + slideInHorizontally(
                    initialOffsetX = { it / 8 },
                    animationSpec = tween(220)
                )
            },
            exitTransition = {
                fadeOut(animationSpec = tween(180)) + slideOutHorizontally(
                    targetOffsetX = { -it / 8 },
                    animationSpec = tween(180)
                )
            },
            popEnterTransition = {
                fadeIn(animationSpec = tween(220)) + slideInHorizontally(
                    initialOffsetX = { -it / 8 },
                    animationSpec = tween(220)
                )
            },
            popExitTransition = {
                fadeOut(animationSpec = tween(180)) + slideOutHorizontally(
                    targetOffsetX = { it / 8 },
                    animationSpec = tween(180)
                )
            }
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(viewModel = DashboardViewModel(application.repository, application.preferenceManager))
            }
            composable(Screen.Transactions.route) {
                com.example.finance.ui.screens.TransactionsScreen(viewModel = com.example.finance.ui.screens.TransactionsViewModel(application.repository, application.preferenceManager))
            }
            composable(Screen.Recurring.route) {
                com.example.finance.ui.screens.RecurringScreen(viewModel = com.example.finance.ui.screens.RecurringViewModel(application.repository, application.database))
            }
            composable(Screen.Budgets.route) {
                com.example.finance.ui.screens.BudgetsScreen(viewModel = com.example.finance.ui.screens.BudgetsViewModel(application.repository, application.preferenceManager))
            }
            composable(Screen.Categories.route) {
                com.example.finance.ui.screens.CategoriesScreen(viewModel = com.example.finance.ui.screens.CategoriesViewModel(application.repository))
            }
            composable(Screen.Reports.route) {
                com.example.finance.ui.screens.ReportsScreen(viewModel = com.example.finance.ui.screens.ReportsViewModel(application.repository, application.preferenceManager))
            }
        }
    }

    if (showMonthPicker) {
        MonthPickerDialog(
            selectedMonth = selectedMonth,
            onDismiss = { showMonthPicker = false },
            onCurrentMonth = {
                scope.launch {
                    application.preferenceManager.setMonth(DateUtils.getCurrentMonth())
                }
                showMonthPicker = false
            },
            onSelectMonth = { monthKey ->
                scope.launch {
                    application.preferenceManager.setMonth(monthKey)
                }
                showMonthPicker = false
            }
        )
    }
}

@Composable
private fun ShellMonthBar(
    selectedMonth: String,
    isDarkTheme: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onCurrent: () -> Unit,
    onToggleTheme: () -> Unit
) {
    Surface(
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPrev) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous month")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onCurrent) {
                    Text(
                        text = DateUtils.formatDisplayMonth(selectedMonth),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onToggleTheme) {
                    Icon(
                        if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = if (isDarkTheme) "Switch to light theme" else "Switch to dark theme"
                    )
                }
                IconButton(onClick = onNext) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next month")
                }
            }
        }
    }
}

@Composable
private fun MonthPickerDialog(
    selectedMonth: String,
    onDismiss: () -> Unit,
    onCurrentMonth: () -> Unit,
    onSelectMonth: (String) -> Unit
) {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM")
    var year by remember(selectedMonth) { androidx.compose.runtime.mutableIntStateOf(YearMonth.parse(selectedMonth, formatter).year) }
    val months = (1..12).map { month -> "%04d-%02d".format(year, month) }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Jump to month") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { year -= 1 }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous year")
                    }
                    Text(year.toString(), fontWeight = FontWeight.Bold)
                    IconButton(onClick = { year += 1 }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next year")
                    }
                }
                months.chunked(4).forEach { rowMonths ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        rowMonths.forEach { monthKey ->
                            OutlinedButton(
                                onClick = { onSelectMonth(monthKey) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(DateUtils.formatDisplayMonth(monthKey).take(3))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onCurrentMonth) {
                Text("Current month")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun ShellThemeBar(
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit
) {
    Surface(
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onToggleTheme) {
                Icon(
                    if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                    contentDescription = if (isDarkTheme) "Switch to light theme" else "Switch to dark theme"
                )
            }
        }
    }
}
