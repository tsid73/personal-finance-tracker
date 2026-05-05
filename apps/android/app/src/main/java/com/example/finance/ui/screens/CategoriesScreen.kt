package com.example.finance.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.finance.data.entity.CategoryEntity
import com.example.finance.domain.model.BudgetMode
import com.example.finance.domain.model.TransactionKind
import com.example.finance.ui.components.FinanceTopAppBar

@Composable
fun CategoriesScreen(viewModel: CategoriesViewModel) {
    val categories by viewModel.categories.collectAsState()
    var editingCategory by remember { mutableStateOf<CategoryEntity?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<CategoryEntity?>(null) }
    val activeCategories = categories.filter { !it.isArchived }
    val archivedCategories = categories.filter { it.isArchived }

    Scaffold(
        topBar = { FinanceTopAppBar(title = "Categories") },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingCategory = null
                    showEditor = true
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add category")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("Active", style = MaterialTheme.typography.titleMedium)
            }
            items(activeCategories, key = { it.id }) { category ->
                CategoryItem(
                    category = category,
                    onEdit = {
                        editingCategory = category
                        showEditor = true
                    },
                    onArchive = {
                        if (!category.isDefault) {
                            viewModel.archiveCategory(category, true)
                        }
                    },
                    onRestore = {},
                    onDelete = {
                        if (!category.isDefault) {
                            deleteTarget = category
                        }
                    }
                )
            }
            if (archivedCategories.isNotEmpty()) {
                item {
                    Text("Archived", style = MaterialTheme.typography.titleMedium)
                }
                items(archivedCategories, key = { it.id }) { category ->
                    CategoryItem(
                        category = category,
                        onEdit = {},
                        onArchive = {},
                        onRestore = { viewModel.archiveCategory(category, false) },
                        onDelete = { deleteTarget = category }
                    )
                }
            }
        }
    }

    if (showEditor) {
        AddCategoryDialog(
            initialCategory = editingCategory,
            onDismiss = {
                showEditor = false
                editingCategory = null
            },
            onConfirm = { name, type, color, icon, budgetMode ->
                if (editingCategory == null) {
                    viewModel.addCategory(name, type, color, icon, budgetMode)
                } else {
                    viewModel.updateCategory(
                        editingCategory!!.copy(
                            name = name,
                            type = type,
                            color = color,
                            icon = icon,
                            budgetMode = budgetMode
                        )
                    )
                }
                showEditor = false
                editingCategory = null
            }
        )
    }

    deleteTarget?.let { category ->
        DeleteCategoryDialog(
            category = category,
            replacementOptions = activeCategories.filter { it.id != category.id && it.type == category.type },
            usageProvider = { viewModel.getCategoryUsage(category.id) },
            onDismiss = { deleteTarget = null },
            onConfirm = { replacementId ->
                viewModel.deleteCategory(category, replacementId)
                deleteTarget = null
            }
        )
    }
}

@Composable
private fun CategoryItem(
    category: CategoryEntity,
    onEdit: () -> Unit,
    onArchive: () -> Unit,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    Card {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(14.dp).background(Color(android.graphics.Color.parseColor(category.color)), CircleShape)
            )
            Spacer(modifier = Modifier.size(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(category.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${category.type.name.lowercase().replaceFirstChar(Char::titlecase)} • ${category.budgetMode.name.lowercase().replaceFirstChar(Char::titlecase)}${if (category.isDefault) " • Default" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!category.isDefault && !category.isArchived) {
                IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = "Edit category") }
                IconButton(onClick = onArchive) { Icon(Icons.Default.Archive, contentDescription = "Archive category") }
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Delete category") }
            } else if (!category.isDefault) {
                IconButton(onClick = onRestore) { Icon(Icons.Default.Unarchive, contentDescription = "Restore category") }
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Delete category") }
            }
        }
    }
}

@Composable
fun AddCategoryDialog(
    initialCategory: CategoryEntity?,
    onDismiss: () -> Unit,
    onConfirm: (String, TransactionKind, String, String, BudgetMode) -> Unit
) {
    var name by remember(initialCategory) { mutableStateOf(initialCategory?.name ?: "") }
    var type by remember(initialCategory) { mutableStateOf(initialCategory?.type ?: TransactionKind.EXPENSE) }
    var budgetMode by remember(initialCategory) { mutableStateOf(initialCategory?.budgetMode ?: BudgetMode.FLEXIBLE) }
    var color by remember(initialCategory) { mutableStateOf(initialCategory?.color ?: "#0f766e") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialCategory == null) "Add category" else "Edit category") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = color, onValueChange = { color = it }, label = { Text("Color (#RRGGBB)") }, modifier = Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.RadioButton(selected = type == TransactionKind.EXPENSE, onClick = { type = TransactionKind.EXPENSE })
                    Text("Expense")
                    Spacer(modifier = Modifier.size(8.dp))
                    androidx.compose.material3.RadioButton(selected = type == TransactionKind.INCOME, onClick = { type = TransactionKind.INCOME })
                    Text("Income")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.RadioButton(selected = budgetMode == BudgetMode.FLEXIBLE, onClick = { budgetMode = BudgetMode.FLEXIBLE })
                    Text("Flexible")
                    Spacer(modifier = Modifier.size(8.dp))
                    androidx.compose.material3.RadioButton(selected = budgetMode == BudgetMode.FIXED, onClick = { budgetMode = BudgetMode.FIXED })
                    Text("Fixed")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name.trim(), type, color.trim(), "wallet", budgetMode) },
                enabled = name.trim().length >= 2 && color.matches(Regex("^#[0-9a-fA-F]{6}$"))
            ) {
                Text(if (initialCategory == null) "Add" else "Save")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeleteCategoryDialog(
    category: CategoryEntity,
    replacementOptions: List<CategoryEntity>,
    usageProvider: suspend () -> com.example.finance.data.repository.CategoryUsage,
    onDismiss: () -> Unit,
    onConfirm: (Int?) -> Unit
) {
    var selectedReplacementId by remember { mutableStateOf<Int?>(null) }
    var expanded by remember { mutableStateOf(false) }
    var usageText by remember { mutableStateOf("Checking usage...") }
    var requiresReplacement by remember { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(category.id) {
        val usage = usageProvider()
        requiresReplacement = usage.isInUse
        usageText = if (!usage.isInUse) {
            "This category is not referenced by any transaction, budget, or recurring schedule."
        } else {
            "This category is used by ${usage.transactionCount} transactions, ${usage.budgetCount} budgets, and ${usage.recurringCount} recurring schedules."
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete ${category.name}?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(usageText, style = MaterialTheme.typography.bodySmall)
                if (requiresReplacement) {
                    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                        OutlinedTextField(
                            value = replacementOptions.find { it.id == selectedReplacementId }?.name ?: "Select replacement",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Replacement category") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            replacementOptions.forEach { option ->
                                androidx.compose.material3.DropdownMenuItem(
                                    text = { Text(option.name) },
                                    onClick = {
                                        selectedReplacementId = option.id
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(selectedReplacementId) }, enabled = !requiresReplacement || selectedReplacementId != null) {
                Text("Delete")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
