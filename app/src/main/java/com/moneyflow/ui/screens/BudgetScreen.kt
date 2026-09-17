package com.moneyflow.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moneyflow.model.ExpenseCategory
import com.moneyflow.ui.components.BudgetProgressCard
import com.moneyflow.util.MoneyFormat
import com.moneyflow.viewmodel.BudgetUiState

@Composable
fun BudgetScreen(
    state: BudgetUiState,
    onSetMonthlyBudget: (Long) -> Unit,
    onSetCategoryBudget: (ExpenseCategory, Long) -> Unit
) {
    var budgetDialog by remember { mutableStateOf(false) }
    var categoryDialog by remember { mutableStateOf<ExpenseCategory?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Budget", style = MaterialTheme.typography.headlineMedium)
        }
        item {
            BudgetProgressCard("Monthly budget", state.monthSpentMinor, state.monthBudgetMinor, state.preferences.currency)
        }
        item {
            Button(onClick = { budgetDialog = true }, modifier = Modifier.fillMaxWidth()) { Text("Edit monthly budget") }
        }
        item { Text("Category budgets", style = MaterialTheme.typography.titleMedium) }
        items(ExpenseCategory.entries) { category ->
            val categoryBudget = state.categoryBudgets.firstOrNull { it.category == category }?.amountMinor ?: 0
            val spent = state.expenses.filter { it.category == category }.sumOf { it.amountMinor }
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("${category.emoji} ${category.label}")
                    Text("${MoneyFormat.formatMinor(spent, state.preferences.currency)} / ${MoneyFormat.formatMinor(categoryBudget, state.preferences.currency)}")
                    val status = when {
                        categoryBudget <= 0 -> "No budget set"
                        spent >= categoryBudget -> "Over budget"
                        spent >= categoryBudget * 0.85 -> "Near limit"
                        else -> "Within budget"
                    }
                    Text("Status: $status")
                    Button(onClick = { categoryDialog = category }) { Text("Set budget") }
                }
            }
        }
    }

    if (budgetDialog) {
        BudgetInputDialog(onDismiss = { budgetDialog = false }) { value ->
            onSetMonthlyBudget(value)
            budgetDialog = false
        }
    }

    categoryDialog?.let { category ->
        BudgetInputDialog(onDismiss = { categoryDialog = null }) { value ->
            onSetCategoryBudget(category, value)
            categoryDialog = null
        }
    }
}

@Composable
private fun BudgetInputDialog(onDismiss: () -> Unit, onSave: (Long) -> Unit) {
    var value by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set budget") },
        text = {
            Column {
                OutlinedTextField(value = value, onValueChange = { value = it }, label = { Text("Amount") })
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val minor = com.moneyflow.util.MoneyFormat.parseMajorToMinor(value)
                if (minor == null || minor < 0) error = "Invalid amount" else onSave(minor)
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
