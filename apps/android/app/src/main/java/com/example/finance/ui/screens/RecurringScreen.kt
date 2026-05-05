package com.example.finance.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.finance.FinanceApplication
import com.example.finance.data.entity.AccountEntity
import com.example.finance.data.entity.CategoryEntity
import com.example.finance.data.entity.RecurringTransactionEntity
import com.example.finance.domain.model.TransactionKind
import com.example.finance.ui.components.DatePickerField
import com.example.finance.ui.components.FinanceTopAppBar
import com.example.finance.util.DateUtils
import com.example.finance.util.MoneyFormatter

@Composable
fun RecurringScreen(viewModel: RecurringViewModel) {
    val recurring by viewModel.recurring.collectAsState()
    var editingRecurring by remember { mutableStateOf<RecurringTransactionEntity?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var showCopyDialog by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val app = context.applicationContext as FinanceApplication
    val accounts by app.repository.getAccounts().collectAsState(initial = emptyList())
    val categories by app.repository.getCategories(false).collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            FinanceTopAppBar(
                title = "Recurring transactions",
                actions = {
                    IconButton(onClick = { showCopyDialog = true }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy recurring schedules to next month")
                    }
                    IconButton(onClick = viewModel::syncNow) {
                        Icon(Icons.Default.Sync, contentDescription = "Sync recurring transactions")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingRecurring = null
                    showEditor = true
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add recurring transaction")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (recurring.isEmpty()) {
                item {
                    Text("No recurring schedules.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                items(recurring, key = { it.id }) { item ->
                    RecurringItem(
                        item = item,
                        categoryName = categories.find { it.id == item.categoryId }?.name ?: "Unknown",
                        accountName = accounts.find { it.id == item.accountId }?.name ?: "Unknown",
                        onEdit = {
                            editingRecurring = item
                            showEditor = true
                        },
                        onToggleActive = {
                            viewModel.addRecurring(
                                id = item.id,
                                title = item.title,
                                amount = item.amount,
                                kind = item.kind,
                                accountId = item.accountId,
                                categoryId = item.categoryId,
                                dayOfMonth = item.dayOfMonth,
                                startDate = item.startDate,
                                autoCreate = item.autoCreate,
                                isActive = !item.isActive,
                                notes = item.notes,
                                merchant = item.merchant
                            )
                        },
                        onDelete = { viewModel.deleteRecurring(item) }
                    )
                }
            }
        }
    }

    if (showEditor) {
        AddRecurringDialog(
            accounts = accounts,
            categories = categories,
            initialRecurring = editingRecurring,
            onDismiss = {
                showEditor = false
                editingRecurring = null
            },
            onConfirm = { id, title, amount, kind, accountId, categoryId, day, start, autoCreate, isActive, notes, merchant ->
                viewModel.addRecurring(id, title, amount, kind, accountId, categoryId, day, start, autoCreate, isActive, notes, merchant)
                showEditor = false
                editingRecurring = null
            }
        )
    }

    if (showCopyDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showCopyDialog = false },
            title = { Text("Copy to next month") },
            text = { Text("This duplicates the current recurring schedules with next-month start dates. Existing matching copies are skipped.") },
            confirmButton = {
                Button(onClick = {
                    viewModel.copyToNextMonth()
                    showCopyDialog = false
                }) {
                    Text("Copy")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCopyDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun RecurringItem(
    item: RecurringTransactionEntity,
    categoryName: String,
    accountName: String,
    onEdit: () -> Unit,
    onToggleActive: () -> Unit,
    onDelete: () -> Unit
) {
    Card {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "$categoryName • $accountName • day ${item.dayOfMonth}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Next due ${DateUtils.formatDisplayDate(item.nextDueDate)}${if (item.autoCreate) " • auto-create" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(MoneyFormatter.format(item.amount), style = MaterialTheme.typography.titleMedium)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(if (item.isActive) "Active" else "Paused", color = if (item.isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                Row {
                    IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = "Edit recurring schedule") }
                    IconButton(onClick = onToggleActive) { Icon(Icons.Default.Sync, contentDescription = "Toggle recurring schedule") }
                    IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Delete recurring schedule") }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRecurringDialog(
    accounts: List<AccountEntity>,
    categories: List<CategoryEntity>,
    initialRecurring: RecurringTransactionEntity?,
    onDismiss: () -> Unit,
    onConfirm: (Int, String, Long, TransactionKind, Int, Int, Int, String, Boolean, Boolean, String?, String?) -> Unit
) {
    var title by remember(initialRecurring) { mutableStateOf(initialRecurring?.title ?: "") }
    var amount by remember(initialRecurring) { mutableStateOf(initialRecurring?.let { (it.amount / 100.0).toString() } ?: "") }
    var kind by remember(initialRecurring) { mutableStateOf(initialRecurring?.kind ?: TransactionKind.EXPENSE) }
    var selectedAccount by remember(initialRecurring, accounts) { mutableStateOf(accounts.find { it.id == initialRecurring?.accountId } ?: accounts.firstOrNull()) }
    var selectedCategory by remember(initialRecurring, categories, kind) {
        mutableStateOf(categories.find { it.id == initialRecurring?.categoryId } ?: categories.firstOrNull { it.type == kind })
    }
    var dayOfMonth by remember(initialRecurring) { mutableStateOf(initialRecurring?.dayOfMonth?.toString() ?: DateUtils.today().takeLast(2)) }
    var startDate by remember(initialRecurring) {
        mutableStateOf(DateUtils.coerceDate(initialRecurring?.startDate))
    }
    var autoCreate by remember(initialRecurring) { mutableStateOf(initialRecurring?.autoCreate ?: true) }
    var isActive by remember(initialRecurring) { mutableStateOf(initialRecurring?.isActive ?: true) }
    var merchant by remember(initialRecurring) { mutableStateOf(initialRecurring?.merchant ?: "") }
    var notes by remember(initialRecurring) { mutableStateOf(initialRecurring?.notes ?: "") }
    var accountExpanded by remember { mutableStateOf(false) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    val parsedStartDate = remember(startDate) { DateUtils.parseDateOrNull(startDate) }
    val validDayOfMonth = remember(dayOfMonth, startDate) {
        val day = dayOfMonth.toIntOrNull()
        val maxDay = parsedStartDate?.lengthOfMonth() ?: 31
        day != null && day in 1..maxDay
    }
    val isDirty = remember(
        title,
        amount,
        kind,
        selectedAccount,
        selectedCategory,
        dayOfMonth,
        startDate,
        autoCreate,
        isActive,
        merchant,
        notes,
        initialRecurring
    ) {
        title != (initialRecurring?.title ?: "") ||
            amount != (initialRecurring?.let { (it.amount / 100.0).toString() } ?: "") ||
            kind != (initialRecurring?.kind ?: TransactionKind.EXPENSE) ||
            selectedAccount?.id != initialRecurring?.accountId ||
            selectedCategory?.id != initialRecurring?.categoryId ||
            dayOfMonth != (initialRecurring?.dayOfMonth?.toString() ?: DateUtils.today().takeLast(2)) ||
            startDate != DateUtils.coerceDate(initialRecurring?.startDate) ||
            autoCreate != (initialRecurring?.autoCreate ?: true) ||
            isActive != (initialRecurring?.isActive ?: true) ||
            merchant != (initialRecurring?.merchant ?: "") ||
            notes != (initialRecurring?.notes ?: "")
    }

    fun requestDismiss() {
        if (isDirty) showDiscardDialog = true else onDismiss()
    }

    Dialog(onDismissRequest = ::requestDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                topBar = {
                    FinanceTopAppBar(
                        title = if (initialRecurring == null) "Add recurring schedule" else "Edit recurring schedule",
                        actions = {
                            TextButton(
                                onClick = {
                                    val accountId = selectedAccount?.id ?: return@TextButton
                                    val categoryId = selectedCategory?.id ?: return@TextButton
                                    val day = dayOfMonth.toIntOrNull() ?: return@TextButton
                                    val maxDay = parsedStartDate?.lengthOfMonth() ?: return@TextButton
                                    onConfirm(
                                        initialRecurring?.id ?: 0,
                                        title.trim(),
                                        MoneyFormatter.parseToCents(amount),
                                        kind,
                                        accountId,
                                        categoryId,
                                        day.coerceIn(1, maxDay),
                                        startDate,
                                        autoCreate,
                                        isActive,
                                        notes.trim().ifBlank { null },
                                        merchant.trim().ifBlank { null }
                                    )
                                },
                                enabled = title.isNotBlank() &&
                                    amount.toDoubleOrNull()?.let { it > 0 } == true &&
                                    selectedAccount != null &&
                                    selectedCategory != null &&
                                    parsedStartDate != null &&
                                    validDayOfMonth
                            ) {
                                Text("Save")
                            }
                        }
                    )
                }
            ) { padding ->
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .padding(padding)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.RadioButton(selected = kind == TransactionKind.EXPENSE, onClick = { kind = TransactionKind.EXPENSE })
                    Text("Expense")
                    androidx.compose.material3.RadioButton(selected = kind == TransactionKind.INCOME, onClick = { kind = TransactionKind.INCOME })
                    Text("Income")
                }
                ExposedDropdownMenuBox(expanded = accountExpanded, onExpandedChange = { accountExpanded = !accountExpanded }) {
                    OutlinedTextField(
                        value = selectedAccount?.name ?: "Select account",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Account") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    DropdownMenu(expanded = accountExpanded, onDismissRequest = { accountExpanded = false }) {
                        accounts.forEach {
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text(it.name) },
                                onClick = {
                                    selectedAccount = it
                                    accountExpanded = false
                                }
                            )
                        }
                    }
                }
                ExposedDropdownMenuBox(expanded = categoryExpanded, onExpandedChange = { categoryExpanded = !categoryExpanded }) {
                    OutlinedTextField(
                        value = selectedCategory?.name ?: "Select category",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    DropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }) {
                        categories.filter { it.type == kind && !it.isArchived }.forEach {
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text(it.name) },
                                onClick = {
                                    selectedCategory = it
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }
                OutlinedTextField(value = dayOfMonth, onValueChange = { dayOfMonth = it }, label = { Text("Day of month") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                DatePickerField(
                    value = startDate,
                    label = "Start date",
                    onValueChange = { startDate = it },
                    monthKey = startDate.take(7)
                )
                if (!validDayOfMonth) {
                    Text(
                        text = "Day must fit within the selected start month.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                OutlinedTextField(value = merchant, onValueChange = { merchant = it }, label = { Text("Merchant") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth())
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Auto-create due transactions")
                    Switch(checked = autoCreate, onCheckedChange = { autoCreate = it })
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Active")
                    Switch(checked = isActive, onCheckedChange = { isActive = it })
                }
            }
            }
        }
    }

    if (showDiscardDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Discard changes?") },
            text = { Text("You have unsaved recurring schedule changes.") },
            confirmButton = {
                Button(onClick = {
                    showDiscardDialog = false
                    onDismiss()
                }) {
                    Text("Discard")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text("Keep editing")
                }
            }
        )
    }
}
