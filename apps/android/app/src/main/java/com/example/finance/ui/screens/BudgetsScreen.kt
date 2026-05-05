package com.example.finance.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.animateItemPlacement
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.finance.data.entity.BudgetEntity
import com.example.finance.data.entity.CategoryEntity
import com.example.finance.ui.components.FinanceTopAppBar
import com.example.finance.util.DateUtils
import com.example.finance.util.MoneyFormatter

@Composable
fun BudgetsScreen(viewModel: BudgetsViewModel) {
    val budgets by viewModel.budgets.collectAsState()
    val totalBudget by viewModel.totalBudget.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val spendingByCategory by viewModel.spendingByCategory.collectAsState()
    val allocatedTotal = budgets.sumOf { it.allocatedAmount }
    val remainingToAllocate = totalBudget - allocatedTotal
    val fixedAllocated = budgets.sumOf { budget ->
        if (categories.find { it.id == budget.categoryId }?.budgetMode == com.example.finance.domain.model.BudgetMode.FIXED) budget.allocatedAmount else 0L
    }
    val flexibleAllocated = budgets.sumOf { budget ->
        if (categories.find { it.id == budget.categoryId }?.budgetMode == com.example.finance.domain.model.BudgetMode.FLEXIBLE) budget.allocatedAmount else 0L
    }
    var showTargetDialog by remember { mutableStateOf(false) }
    var editingBudget by remember { mutableStateOf<BudgetEntity?>(null) }
    var showBudgetDialog by remember { mutableStateOf(false) }
    var showCopyDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            FinanceTopAppBar(
                title = "Budgets",
                actions = {
                    IconButton(onClick = { showCopyDialog = true }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy budgets to next month")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingBudget = null
                    showBudgetDialog = true
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add budget")
            }
        }
    ) { padding ->
        AnimatedContent(
            targetState = selectedMonth,
            transitionSpec = { fadeIn(animationSpec = tween(180)) togetherWith fadeOut(animationSpec = tween(120)) },
            label = "budgets_month"
        ) {
            LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { showTargetDialog = true }
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Monthly target", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(DateUtils.formatDisplayMonth(selectedMonth), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(MoneyFormatter.format(totalBudget), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                BudgetSummaryChip("Allocated", allocatedTotal)
                                BudgetSummaryChip("Remaining to allocate", remainingToAllocate, emphasizeNegative = true)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                BudgetSummaryChip("Fixed allocated", fixedAllocated)
                                BudgetSummaryChip("Flexible allocated", flexibleAllocated)
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Allocated: ${MoneyFormatter.format(allocatedTotal)}", style = MaterialTheme.typography.bodySmall)
                            Text("Left: ${MoneyFormatter.format(remainingToAllocate)}", style = MaterialTheme.typography.bodySmall)
                        }
                        OutlinedButton(onClick = { showTargetDialog = true }) {
                            Text("Edit monthly target")
                        }
                    }
                }
            }
            if (budgets.isEmpty()) {
                item {
                    Text("No category budgets for this month.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                items(budgets, key = { it.id }) { budget ->
                    val category = categories.find { it.id == budget.categoryId }
                    BudgetItem(
                        modifier = Modifier.animateItemPlacement(),
                        budget = budget,
                        category = category,
                        spentAmount = spendingByCategory[budget.categoryId] ?: 0L,
                        onEdit = {
                            editingBudget = budget
                            showBudgetDialog = true
                        },
                        onDelete = { viewModel.deleteBudget(budget) }
                    )
                }
            }
        }
        }
    }

    if (showTargetDialog) {
        SetTargetDialog(
            initialValue = totalBudget,
            onDismiss = { showTargetDialog = false },
            onConfirm = {
                viewModel.setMonthlyTarget(it)
                showTargetDialog = false
            }
        )
    }

    if (showBudgetDialog) {
        AddCategoryBudgetDialog(
            categories = categories.filter { category ->
                category.type == com.example.finance.domain.model.TransactionKind.EXPENSE &&
                    !category.isArchived &&
                    (editingBudget?.categoryId == category.id || budgets.none { it.categoryId == category.id })
            },
            editingBudget = editingBudget,
            onDismiss = {
                showBudgetDialog = false
                editingBudget = null
            },
            onConfirm = { budgetId, categoryId, amount ->
                if (budgetId == 0) {
                    viewModel.setCategoryBudget(categoryId, amount)
                } else {
                    viewModel.updateCategoryBudget(budgetId, categoryId, amount)
                }
                showBudgetDialog = false
                editingBudget = null
            }
        )
    }

    if (showCopyDialog) {
        AlertDialog(
            onDismissRequest = { showCopyDialog = false },
            title = { Text("Copy to next month") },
            text = { Text("This copies the monthly target and category budget baselines to the next month. Usage is not copied.") },
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
private fun BudgetItem(
    modifier: Modifier = Modifier,
    budget: BudgetEntity,
    category: CategoryEntity?,
    spentAmount: Long,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val remaining = budget.allocatedAmount - spentAmount
    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(category?.name ?: "Unknown", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    category?.budgetMode?.name?.lowercase()?.replaceFirstChar(Char::titlecase) ?: "Flexible",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Spent ${MoneyFormatter.format(spentAmount)} • Remaining ${MoneyFormatter.format(remaining)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (remaining < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(MoneyFormatter.format(budget.allocatedAmount), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit budget")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete budget")
                }
            }
        }
    }
}

@Composable
private fun BudgetSummaryChip(
    label: String,
    amount: Long,
    emphasizeNegative: Boolean = false
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                MoneyFormatter.format(amount),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (emphasizeNegative && amount < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun SetTargetDialog(initialValue: Long, onDismiss: () -> Unit, onConfirm: (Long) -> Unit) {
    var value by remember { mutableStateOf((initialValue / 100.0).toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set monthly target") },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text("Target amount") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
        },
        confirmButton = {
            Button(onClick = { onConfirm(MoneyFormatter.parseToCents(value)) }, enabled = value.toDoubleOrNull() != null) {
                Text("Save")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCategoryBudgetDialog(
    categories: List<CategoryEntity>,
    editingBudget: BudgetEntity?,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int, Long) -> Unit
) {
    var selectedCategory by remember(categories, editingBudget) {
        mutableStateOf(categories.find { it.id == editingBudget?.categoryId } ?: categories.firstOrNull())
    }
    var amount by remember(editingBudget) {
        mutableStateOf(editingBudget?.let { (it.allocatedAmount / 100.0).toString() } ?: "")
    }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (editingBudget == null) "Add category budget" else "Edit category budget") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                    OutlinedTextField(
                        value = selectedCategory?.name ?: "Select category",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        categories.forEach { category ->
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text(category.name) },
                                onClick = {
                                    selectedCategory = category
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Budget amount") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val categoryId = selectedCategory?.id ?: return@Button
                    onConfirm(editingBudget?.id ?: 0, categoryId, MoneyFormatter.parseToCents(amount))
                },
                enabled = selectedCategory != null && amount.toDoubleOrNull()?.let { it >= 0 } == true
            ) {
                Text(if (editingBudget == null) "Set" else "Save")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
