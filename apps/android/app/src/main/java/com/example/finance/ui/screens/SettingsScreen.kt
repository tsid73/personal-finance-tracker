package com.example.finance.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.finance.data.repository.PreferenceManager
import com.example.finance.ui.components.FinanceTopAppBar
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    preferenceManager: PreferenceManager
) {
    val context = LocalContext.current
    val biometricEnabled by preferenceManager.biometricEnabled.collectAsState(initial = false)
    val syncEnabled by viewModel.syncEnabled.collectAsState()
    val syncBaseUrl by viewModel.syncBaseUrl.collectAsState()
    val lastSyncResult by viewModel.lastSyncResult.collectAsState()
    val activeOperation by viewModel.activeOperation.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val scope = rememberCoroutineScope()
    var syncEnabledDraft by remember(syncEnabled) { mutableStateOf(syncEnabled) }
    var syncBaseUrlDraft by remember(syncBaseUrl) { mutableStateOf(syncBaseUrl) }
    var pendingRestore by remember { mutableStateOf<RestorePreview?>(null) }

    val backupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            viewModel.backupToUri(context, uri)
        }
    }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    pendingRestore = viewModel.prepareRestore(context, uri)
                } catch (error: Exception) {
                    viewModel.showStatus(error.localizedMessage ?: "Unable to read backup file.")
                }
            }
        }
    }

    Scaffold(
        topBar = { FinanceTopAppBar(title = "Settings") }
    ) { padding ->
        val statusText = statusMessage
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (!statusText.isNullOrBlank()) {
                item {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                statusText,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            TextButton(onClick = viewModel::clearStatus) {
                                Text("Dismiss")
                            }
                        }
                    }
                }
            }

            item {
                Text("Security", style = MaterialTheme.typography.titleMedium)
            }
            item {
                SettingsCard(
                    title = "Biometric lock",
                    description = "Lock the app with fingerprint or face ID on startup."
                ) {
                    Switch(
                        checked = biometricEnabled,
                        enabled = activeOperation == null,
                        onCheckedChange = {
                            scope.launch {
                                preferenceManager.setBiometricEnabled(it)
                            }
                        }
                    )
                }
            }

            item {
                Text("Backup and Restore", style = MaterialTheme.typography.titleMedium)
            }
            item {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Local backup", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        Text(
                            "Back up the full Android database and app settings to a JSON file. Restore replaces all current local data only after the backup file passes validation.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = {
                                    viewModel.clearStatus()
                                    backupLauncher.launch("personal-finance-backup-${System.currentTimeMillis()}.json")
                                },
                                enabled = activeOperation == null,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(if (activeOperation == SettingsOperation.BACKUP) "Backing up..." else "Create backup")
                            }
                            Button(
                                onClick = {
                                    viewModel.clearStatus()
                                    restoreLauncher.launch(arrayOf("application/json", "text/plain"))
                                },
                                enabled = activeOperation == null,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(if (activeOperation == SettingsOperation.RESTORE) "Restoring..." else "Restore backup")
                            }
                        }
                    }
                }
            }

            item {
                Text("Sync", style = MaterialTheme.typography.titleMedium)
            }
            item {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Enable API sync", style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    "Sync remains push-only and manual.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = syncEnabledDraft,
                                enabled = activeOperation == null,
                                onCheckedChange = { syncEnabledDraft = it }
                            )
                        }
                        OutlinedTextField(
                            value = syncBaseUrlDraft,
                            onValueChange = { syncBaseUrlDraft = it },
                            label = { Text("Base URL") },
                            placeholder = { Text("http://192.168.1.10:3001") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = activeOperation == null
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = { viewModel.updateSyncSettings(syncEnabledDraft, syncBaseUrlDraft) },
                                enabled = activeOperation == null && (!syncEnabledDraft || syncBaseUrlDraft.isNotBlank()),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Save sync")
                            }
                            Button(
                                onClick = viewModel::syncNow,
                                enabled = activeOperation == null && syncEnabled && syncBaseUrl.isNotBlank(),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(if (activeOperation == SettingsOperation.SYNC) "Syncing..." else "Sync now")
                            }
                        }
                        lastSyncResult?.let { result ->
                            val text = if (result.healthy) {
                                "Synced ${result.pushedCategories} categories, ${result.pushedTransactions} transactions, ${result.pushedRecurring} recurring, ${result.pushedBudgets} budgets."
                            } else {
                                result.errors.joinToString("\n").ifBlank { "Sync failed." }
                            }
                            Text(
                                text,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    pendingRestore?.let { preview ->
        val formatter = remember { DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm") }
        val exportedAt = remember(preview.document.exportedAt) {
            Instant.ofEpochMilli(preview.document.exportedAt)
                .atZone(ZoneId.systemDefault())
                .format(formatter)
        }
        AlertDialog(
            onDismissRequest = { pendingRestore = null },
            title = { Text("Restore backup?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("This replaces all local Android data with the selected backup.")
                    Text("Exported: $exportedAt", style = MaterialTheme.typography.bodySmall)
                    Text("Accounts: ${preview.validation.accountCount}", style = MaterialTheme.typography.bodySmall)
                    Text("Categories: ${preview.validation.categoryCount}", style = MaterialTheme.typography.bodySmall)
                    Text("Transactions: ${preview.validation.transactionCount}", style = MaterialTheme.typography.bodySmall)
                    Text("Recurring: ${preview.validation.recurringCount}", style = MaterialTheme.typography.bodySmall)
                    Text("Budgets: ${preview.validation.budgetCount}", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.restoreDocument(preview.document)
                        pendingRestore = null
                    },
                    enabled = activeOperation == null
                ) {
                    Text("Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRestore = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SettingsCard(
    title: String,
    description: String,
    control: @Composable () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            control()
        }
    }
}
