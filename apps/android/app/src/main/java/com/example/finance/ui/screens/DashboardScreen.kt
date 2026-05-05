package com.example.finance.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.example.finance.FinanceApplication
import com.example.finance.ui.components.FinanceTopAppBar
import com.example.finance.ui.components.SummaryCard
import com.example.finance.util.BackupManager
import com.example.finance.util.DateUtils
import kotlinx.coroutines.launch

@Composable
fun DashboardScreen(viewModel: DashboardViewModel) {
    val context = LocalContext.current
    val app = context.applicationContext as FinanceApplication
    val summary by viewModel.summary.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val recentActivity by viewModel.recentActivity.collectAsState()
    val accounts by app.repository.getAccounts().collectAsState(initial = emptyList())
    val categories by app.repository.getCategories(false).collectAsState(initial = emptyList())
    val syncEnabled by viewModel.syncEnabled.collectAsState()
    val syncBaseUrl by viewModel.syncBaseUrl.collectAsState()
    val syncInProgress by viewModel.syncInProgress.collectAsState()
    val lastSyncResult by viewModel.lastSyncResult.collectAsState()
    val scope = rememberCoroutineScope()
    var showSyncSettings by remember { mutableStateOf(false) }
    var showAddTransaction by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            FinanceTopAppBar(title = "Dashboard")
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddTransaction = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add transaction")
            }
        }
    ) { padding ->
        AnimatedContent(
            targetState = selectedMonth,
            transitionSpec = { fadeIn(animationSpec = tween(180)) togetherWith fadeOut(animationSpec = tween(120)) },
            label = "dashboard_month"
        ) {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
            summary?.let { s ->
                item {
                    Text(
                        "Monthly Summary - ${DateUtils.formatDisplayMonth(selectedMonth)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        SummaryCard(label = "Income", value = s.monthlyIncome, icon = Icons.Default.ArrowUpward, modifier = Modifier.weight(1f), color = Color(0xFFDCFCE7))
                        SummaryCard(label = "Expenses", value = s.monthlyExpense, icon = Icons.Default.ArrowDownward, modifier = Modifier.weight(1f), color = Color(0xFFFEE2E2))
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        SummaryCard(label = "Budget", value = s.totalBudget, icon = Icons.Default.Wallet, modifier = Modifier.weight(1f))
                        SummaryCard(
                            label = "Remaining",
                            value = s.remainingBudget,
                            icon = Icons.Default.Balance,
                            modifier = Modifier.weight(1f),
                            color = if (s.remainingBudget < 0) Color(0xFFFFE4E6) else MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        SummaryCard(label = "Daily Safe", value = s.safeToSpend, icon = Icons.Default.Savings, modifier = Modifier.weight(1f))
                        SummaryCard(label = "Days left", value = s.remainingDays.toLong(), icon = Icons.Default.CalendarToday, modifier = Modifier.weight(1f), isCurrency = false)
                    }
                }
                item {
                    Card {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Quick actions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                Button(onClick = { showAddTransaction = true }, modifier = Modifier.weight(1f)) {
                                    Text("Add txn")
                                }
                                Button(onClick = {
                                    scope.launch {
                                        val snapshot = app.repository.getBackupSnapshot()
                                        val file = BackupManager.exportSnapshotToJson(context, snapshot)
                                        val intent = BackupManager.createShareIntent(context, file, "application/json", "Finance backup")
                                        context.startActivity(Intent.createChooser(intent, "Share backup"))
                                    }
                                }, modifier = Modifier.weight(1f)) {
                                    Text("Share backup")
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                Button(onClick = { showSyncSettings = true }, modifier = Modifier.weight(1f)) {
                                    Text(if (syncEnabled) "Sync settings" else "Enable sync")
                                }
                                if (syncEnabled) {
                                    Button(
                                        onClick = viewModel::syncNow,
                                        enabled = !syncInProgress && syncBaseUrl.isNotBlank(),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(if (syncInProgress) "Syncing..." else "Sync now")
                                    }
                                }
                            }
                            if (syncEnabled) {
                                Text(
                                    text = syncBaseUrl,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            lastSyncResult?.let { result ->
                                val resultText = if (result.healthy) {
                                    "Synced ${result.pushedCategories} categories, ${result.pushedTransactions} transactions, ${result.pushedRecurring} recurring, ${result.pushedBudgets} budgets."
                                } else {
                                    result.errors.joinToString("\n").ifBlank { "Sync failed." }
                                }
                                Text(resultText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            } ?: item {
                CircularProgressIndicator()
            }

            if (recentActivity.isNotEmpty()) {
                item {
                    Text(
                        "Recent Activity",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                    )
                }
                items(recentActivity.take(5)) { log ->
                    ActivityItem(log)
                }
            }
        }
        }
    }

    if (showAddTransaction) {
        TransactionEditorDialog(
            initialTransaction = null,
            accounts = accounts,
            categories = categories,
            selectedMonth = selectedMonth,
            onDismiss = { showAddTransaction = false },
            onSave = { id, title, amount, kind, accountId, categoryId, date, notes, merchant ->
                app.repository.let { repository ->
                    scope.launch {
                        repository.addTransaction(
                            com.example.finance.data.entity.TransactionEntity(
                                id = id,
                                userId = 1,
                                accountId = accountId,
                                categoryId = categoryId,
                                kind = kind,
                                title = title,
                                amount = amount,
                                transactionDate = date,
                                notes = notes,
                                merchant = merchant
                            )
                        )
                        showAddTransaction = false
                    }
                }
            }
        )
    }

    if (showSyncSettings) {
        SyncSettingsDialog(
            initialEnabled = syncEnabled,
            initialBaseUrl = syncBaseUrl,
            onDismiss = { showSyncSettings = false },
            onSave = { enabled, baseUrl ->
                viewModel.updateSyncSettings(enabled, baseUrl)
                showSyncSettings = false
            }
        )
    }
}

@Composable
fun ActivityItem(log: com.example.finance.data.entity.ActivityLogEntity, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Icon(
                when (log.action) {
                    "create" -> Icons.Default.Add
                    "delete" -> Icons.Default.Delete
                    else -> Icons.Default.Edit
                },
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(12.dp))
            Column {
                Text(text = log.title, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = DateUtils.formatRelativeTime(log.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SyncSettingsDialog(
    initialEnabled: Boolean,
    initialBaseUrl: String,
    onDismiss: () -> Unit,
    onSave: (Boolean, String) -> Unit
) {
    var enabled by remember(initialEnabled) { mutableStateOf(initialEnabled) }
    var baseUrl by remember(initialBaseUrl) { mutableStateOf(initialBaseUrl) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sync settings") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text("Enable API sync")
                    Switch(checked = enabled, onCheckedChange = { enabled = it })
                }
                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = { baseUrl = it },
                    label = { Text("Base URL") },
                    placeholder = { Text("http://192.168.1.10:3001") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) }
                )
                Text("Sync is push-only. Android remains local-first and sends local data to the backend when you trigger sync.", style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            Button(onClick = { onSave(enabled, baseUrl) }, enabled = !enabled || baseUrl.isNotBlank()) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
