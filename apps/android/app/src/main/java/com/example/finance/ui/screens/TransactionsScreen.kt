package com.example.finance.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.example.finance.data.entity.AccountEntity
import com.example.finance.data.entity.CategoryEntity
import com.example.finance.data.entity.TransactionEntity
import com.example.finance.domain.model.TransactionKind
import com.example.finance.ui.components.DatePickerField
import com.example.finance.ui.components.FinanceTopAppBar
import com.example.finance.util.BackupManager
import com.example.finance.core.common.DateUtils
import com.example.finance.core.common.MoneyFormatter
import com.example.finance.util.ReceiptScanner
import kotlinx.coroutines.launch

private enum class KindFilter { ALL, EXPENSE, INCOME }

@Composable
fun TransactionsScreen(viewModel: TransactionsViewModel) {
    val context = LocalContext.current
    val transactions by viewModel.transactions.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    var editingTransaction by remember { mutableStateOf<TransactionEntity?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var kindFilter by remember { mutableStateOf(KindFilter.ALL) }
    var selectedAccountId by remember { mutableStateOf<Int?>(null) }
    var selectedCategoryId by remember { mutableStateOf<Int?>(null) }
    var selectedIds by remember { mutableStateOf(setOf<Int>()) }
    var showBulkDialog by remember { mutableStateOf(false) }

    val filteredTransactions = remember(transactions, searchQuery, kindFilter, selectedAccountId, selectedCategoryId) {
        transactions.filter { transaction ->
            val matchesQuery = searchQuery.isBlank() ||
                transaction.title.contains(searchQuery, true) ||
                (transaction.notes?.contains(searchQuery, true) == true) ||
                (transaction.merchant?.contains(searchQuery, true) == true)
            val matchesKind = when (kindFilter) {
                KindFilter.ALL -> true
                KindFilter.EXPENSE -> transaction.kind == TransactionKind.EXPENSE
                KindFilter.INCOME -> transaction.kind == TransactionKind.INCOME
            }
            val matchesAccount = selectedAccountId == null || transaction.accountId == selectedAccountId
            val matchesCategory = selectedCategoryId == null || transaction.categoryId == selectedCategoryId
            matchesQuery && matchesKind && matchesAccount && matchesCategory
        }
    }

    Scaffold(
        topBar = {
            FinanceTopAppBar(
                title = if (selectedIds.isEmpty()) "Transactions" else "${selectedIds.size} selected",
                actions = {
                    if (selectedIds.isEmpty()) {
                        IconButton(
                            onClick = {
                                val rows = filteredTransactions.map { transaction ->
                                    mapOf(
                                        "Date" to transaction.transactionDate,
                                        "Title" to transaction.title,
                                        "Kind" to transaction.kind.name.lowercase(),
                                        "Amount" to MoneyFormatter.format(transaction.amount),
                                        "Category" to (categories.find { it.id == transaction.categoryId }?.name ?: ""),
                                        "Account" to (accounts.find { it.id == transaction.accountId }?.name ?: ""),
                                        "Merchant" to (transaction.merchant ?: ""),
                                        "Notes" to (transaction.notes ?: "")
                                    )
                                }
                                val file = BackupManager.exportTransactionsToCsv(context, rows)
                                val intent = BackupManager.createShareIntent(context, file, "text/csv", "Transactions export")
                                context.startActivity(Intent.createChooser(intent, "Share transactions"))
                            }
                        ) {
                            Icon(Icons.Default.Download, contentDescription = "Export transactions")
                        }
                    } else {
                        IconButton(onClick = { showBulkDialog = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Bulk recategorize")
                        }
                        IconButton(onClick = {
                            viewModel.bulkDeleteTransactions(selectedIds.toList(), "Deleted from Android bulk action")
                            selectedIds = emptySet()
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Bulk delete")
                        }
                        IconButton(onClick = { selectedIds = emptySet() }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear selection")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingTransaction = null
                    showEditor = true
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add transaction")
            }
        }
    ) { padding ->
        AnimatedContent(
            targetState = selectedMonth,
            transitionSpec = { fadeIn(animationSpec = tween(180)) togetherWith fadeOut(animationSpec = tween(120)) },
            label = "transactions_month"
        ) { animatedMonth ->
            LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
            item {
                Text(
                    text = DateUtils.formatDisplayMonth(animatedMonth),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            item {
                TransactionFilters(
                    searchQuery = searchQuery,
                    onSearchChange = { searchQuery = it },
                    kindFilter = kindFilter,
                    onKindFilterChange = { kindFilter = it },
                    selectedAccountId = selectedAccountId,
                    onAccountChange = { selectedAccountId = it },
                    selectedCategoryId = selectedCategoryId,
                    onCategoryChange = { selectedCategoryId = it },
                    accounts = accounts,
                    categories = categories
                )
            }
            if (filteredTransactions.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
                        Text("No transactions match the current filters.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(filteredTransactions, key = { it.id }) { transaction ->
                    val category = categories.find { it.id == transaction.categoryId }
                    val account = accounts.find { it.id == transaction.accountId }
                    TransactionItem(
                        transaction = transaction,
                        categoryName = category?.name ?: "Unknown",
                        accountName = account?.name ?: "Unknown",
                        selected = transaction.id in selectedIds,
                        onToggleSelect = {
                            selectedIds = selectedIds.toMutableSet().also { set ->
                                if (!set.add(transaction.id)) set.remove(transaction.id)
                            }
                        },
                        onEdit = {
                            editingTransaction = transaction
                            showEditor = true
                        },
                        onDelete = { viewModel.deleteTransaction(transaction) }
                    )
                }
            }
        }
        }
    }

    if (showEditor) {
        TransactionEditorDialog(
            initialTransaction = editingTransaction,
            accounts = accounts,
            categories = categories,
            selectedMonth = selectedMonth,
            onDismiss = {
                showEditor = false
                editingTransaction = null
            },
            onSave = { id, title, amount, kind, accountId, categoryId, date, notes, merchant ->
                viewModel.addTransaction(id, title, amount, kind, accountId, categoryId, date, notes, merchant)
                showEditor = false
                editingTransaction = null
            }
        )
    }

    if (showBulkDialog) {
        BulkRecategorizeDialog(
            categories = categories.filter { !it.isArchived },
            onDismiss = { showBulkDialog = false },
            onConfirm = { categoryId, note ->
                viewModel.bulkRecategorizeTransactions(selectedIds.toList(), categoryId, note)
                selectedIds = emptySet()
                showBulkDialog = false
            }
        )
    }
}

@Composable
private fun TransactionFilters(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    kindFilter: KindFilter,
    onKindFilterChange: (KindFilter) -> Unit,
    selectedAccountId: Int?,
    onAccountChange: (Int?) -> Unit,
    selectedCategoryId: Int?,
    onCategoryChange: (Int?) -> Unit,
    accounts: List<AccountEntity>,
    categories: List<CategoryEntity>
) {
    var showAdvancedFilters by remember { mutableStateOf(false) }
    val hasAdvancedFilter = kindFilter != KindFilter.ALL || selectedAccountId != null || selectedCategoryId != null

    Card {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    placeholder = { Text("Search transactions") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { showAdvancedFilters = !showAdvancedFilters }) {
                    Icon(Icons.Default.FilterList, contentDescription = "Toggle filters")
                }
                if (searchQuery.isNotBlank() || hasAdvancedFilter) {
                    IconButton(onClick = {
                        onSearchChange("")
                        onKindFilterChange(KindFilter.ALL)
                        onAccountChange(null)
                        onCategoryChange(null)
                    }) {
                        Icon(Icons.Default.RestartAlt, contentDescription = "Reset filters")
                    }
                }
            }
            AnimatedVisibility(visible = showAdvancedFilters || hasAdvancedFilter) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        FilterDropdown(
                            label = "Kind",
                            value = kindFilter.name.lowercase().replaceFirstChar(Char::titlecase),
                            options = listOf("All", "Expense", "Income"),
                            onSelect = {
                                onKindFilterChange(
                                    when (it) {
                                        "Expense" -> KindFilter.EXPENSE
                                        "Income" -> KindFilter.INCOME
                                        else -> KindFilter.ALL
                                    }
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                        FilterDropdown(
                            label = "Account",
                            value = accounts.find { it.id == selectedAccountId }?.name ?: "All",
                            options = listOf("All") + accounts.map { it.name },
                            onSelect = { label ->
                                onAccountChange(accounts.find { it.name == label }?.id)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    FilterDropdown(
                        label = "Category",
                        value = categories.find { it.id == selectedCategoryId }?.name ?: "All",
                        options = listOf("All") + categories.filter { !it.isArchived }.map { it.name },
                        onSelect = { label ->
                            onCategoryChange(categories.find { it.name == label }?.id)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterDropdown(
    label: String,
    value: String,
    options: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = modifier.menuAnchor()
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun TransactionItem(
    transaction: TransactionEntity,
    categoryName: String,
    accountName: String,
    selected: Boolean,
    onToggleSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onToggleSelect() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.Top) {
            Checkbox(checked = selected, onCheckedChange = { onToggleSelect() })
            Spacer(modifier = Modifier.size(8.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(transaction.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            "$categoryName • $accountName • ${DateUtils.formatDisplayDate(transaction.transactionDate)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        MoneyFormatter.format(transaction.amount),
                        color = if (transaction.kind == TransactionKind.INCOME) Color(0xFF15803D) else Color(0xFFDC2626),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                val merchant = transaction.merchant
                if (!merchant.isNullOrBlank()) {
                    Text(merchant, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
                val notes = transaction.notes
                if (!notes.isNullOrBlank()) {
                    Text(notes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row {
                    IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = "Edit transaction") }
                    IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Delete transaction") }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionEditorDialog(
    initialTransaction: TransactionEntity?,
    accounts: List<AccountEntity>,
    categories: List<CategoryEntity>,
    selectedMonth: String,
    onDismiss: () -> Unit,
    onSave: (Int, String, Long, TransactionKind, Int, Int, String, String?, String?) -> Unit
) {
    var title by remember(initialTransaction) { mutableStateOf(initialTransaction?.title ?: "") }
    var amount by remember(initialTransaction) { mutableStateOf(initialTransaction?.let { (it.amount / 100.0).toString() } ?: "") }
    var kind by remember(initialTransaction) { mutableStateOf(initialTransaction?.kind ?: TransactionKind.EXPENSE) }
    var notes by remember(initialTransaction) { mutableStateOf(initialTransaction?.notes ?: "") }
    var merchant by remember(initialTransaction) { mutableStateOf(initialTransaction?.merchant ?: "") }
    var selectedAccount by remember(initialTransaction, accounts) { mutableStateOf(accounts.find { it.id == initialTransaction?.accountId } ?: accounts.firstOrNull()) }
    var selectedCategory by remember(initialTransaction, categories, kind) { mutableStateOf(categories.find { it.id == initialTransaction?.categoryId } ?: categories.firstOrNull { it.type == kind && !it.isArchived }) }
    var date by remember(initialTransaction, selectedMonth) {
        mutableStateOf(
            DateUtils.getInitialTransactionDate(
                initialTransaction?.transactionDate,
                selectedMonth
            )
        )
    }
    val isDateInSelectedMonth = remember(date, selectedMonth) {
        date.startsWith(selectedMonth)
    }
    var accountExpanded by remember { mutableStateOf(false) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var isScanning by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let {
            isScanning = true
            scope.launch {
                val scanned = ReceiptScanner.scan(context, it)
                scanned?.let { result ->
                    result.merchant?.let { m -> title = m }
                    result.amount?.let { a -> amount = (a / 100.0).toString() }
                    result.date?.let { d -> date = DateUtils.formatDate(d) }
                }
                isScanning = false
            }
        }
    }

    val isDirty = remember(
        title,
        amount,
        kind,
        notes,
        merchant,
        selectedAccount,
        selectedCategory,
        date,
        initialTransaction
    ) {
        title != (initialTransaction?.title ?: "") ||
            amount != (initialTransaction?.let { (it.amount / 100.0).toString() } ?: "") ||
            kind != (initialTransaction?.kind ?: TransactionKind.EXPENSE) ||
            notes != (initialTransaction?.notes ?: "") ||
            merchant != (initialTransaction?.merchant ?: "") ||
            selectedAccount?.id != initialTransaction?.accountId ||
            selectedCategory?.id != initialTransaction?.categoryId ||
            date != DateUtils.getInitialTransactionDate(
                initialTransaction?.transactionDate,
                selectedMonth
            )
    }

    fun requestDismiss() {
        if (isDirty) showDiscardDialog = true else onDismiss()
    }

    Dialog(onDismissRequest = ::requestDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                topBar = {
                    FinanceTopAppBar(
                        title = if (initialTransaction == null) "Add transaction" else "Edit transaction",
                        actions = {
                            TextButton(onClick = ::requestDismiss) {
                                Text("Cancel")
                            }
                            TextButton(
                                onClick = {
                                    val accountId = selectedAccount?.id ?: return@TextButton
                                    val categoryId = selectedCategory?.id ?: return@TextButton
                                    onSave(
                                        initialTransaction?.id ?: 0,
                                        title.trim(),
                                        MoneyFormatter.parseToCents(amount),
                                        kind,
                                        accountId,
                                        categoryId,
                                        date,
                                        notes.trim().ifBlank { null },
                                        merchant.trim().ifBlank { null }
                                    )
                                },
                                enabled = title.isNotBlank() && amount.toDoubleOrNull()?.let { it > 0 } == true && selectedAccount != null && selectedCategory != null && isDateInSelectedMonth
                            ) {
                                Text("Save")
                            }
                        }
                    )
                }
            ) { padding ->
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (isScanning) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                    if (initialTransaction == null) {
                        Button(
                            onClick = {
                                launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isScanning
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = null)
                            Spacer(modifier = Modifier.size(8.dp))
                            Text("Scan Receipt")
                        }
                    }
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = amount, onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) amount = it }, label = { Text("Amount") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.material3.RadioButton(
                            selected = kind == TransactionKind.EXPENSE,
                            onClick = {
                                kind = TransactionKind.EXPENSE
                                selectedCategory = categories.firstOrNull { category -> category.type == kind && !category.isArchived }
                            },
                            modifier = Modifier.semantics { contentDescription = "Select expense type" }
                        )
                        Text("Expense")
                        Spacer(modifier = Modifier.size(12.dp))
                        androidx.compose.material3.RadioButton(
                            selected = kind == TransactionKind.INCOME,
                            onClick = {
                                kind = TransactionKind.INCOME
                                selectedCategory = categories.firstOrNull { category -> category.type == kind && !category.isArchived }
                            },
                            modifier = Modifier.semantics { contentDescription = "Select income type" }
                        )
                        Text("Income")
                    }
                    ExposedDropdownMenuBox(expanded = accountExpanded, onExpandedChange = { accountExpanded = !accountExpanded }) {
                        OutlinedTextField(value = selectedAccount?.name ?: "Select account", onValueChange = {}, readOnly = true, label = { Text("Account") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountExpanded) }, modifier = Modifier.menuAnchor().fillMaxWidth())
                        DropdownMenu(expanded = accountExpanded, onDismissRequest = { accountExpanded = false }) {
                            accounts.forEach { account ->
                                DropdownMenuItem(text = { Text(account.name) }, onClick = {
                                    selectedAccount = account
                                    accountExpanded = false
                                })
                            }
                        }
                    }
                    ExposedDropdownMenuBox(expanded = categoryExpanded, onExpandedChange = { categoryExpanded = !categoryExpanded }) {
                        OutlinedTextField(value = selectedCategory?.name ?: "Select category", onValueChange = {}, readOnly = true, label = { Text("Category") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) }, modifier = Modifier.menuAnchor().fillMaxWidth())
                        DropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }) {
                            categories.filter { it.type == kind && (!it.isArchived || it.id == initialTransaction?.categoryId) }.forEach { category ->
                                DropdownMenuItem(text = { Text(category.name) }, onClick = {
                                    selectedCategory = category
                                    categoryExpanded = false
                                })
                            }
                        }
                    }
                    DatePickerField(
                        value = date,
                        label = "Date",
                        onValueChange = { date = it },
                        monthKey = selectedMonth
                    )
                    if (!isDateInSelectedMonth) {
                        Text(
                            text = "Transaction date must fall within ${DateUtils.formatDisplayMonth(selectedMonth)}.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    OutlinedTextField(value = merchant, onValueChange = { merchant = it }, label = { Text("Merchant") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }

    if (showDiscardDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Discard changes?") },
            text = { Text("You have unsaved transaction changes.") },
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BulkRecategorizeDialog(
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onConfirm: (Int, String?) -> Unit
) {
    var selectedCategory by remember(categories) { mutableStateOf(categories.firstOrNull()) }
    var note by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Bulk recategorize") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                    OutlinedTextField(
                        value = selectedCategory?.name ?: "Select category",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("New category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        categories.forEach { category ->
                            DropdownMenuItem(text = { Text(category.name) }, onClick = {
                                selectedCategory = category
                                expanded = false
                            })
                        }
                    }
                }
                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Change note") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(onClick = { selectedCategory?.let { onConfirm(it.id, note.ifBlank { null }) } }, enabled = selectedCategory != null) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
