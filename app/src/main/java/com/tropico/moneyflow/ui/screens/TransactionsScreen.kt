package com.tropico.moneyflow.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tropico.moneyflow.model.ExpenseCategory
import com.tropico.moneyflow.model.PaymentMethod
import com.tropico.moneyflow.model.SortOrder
import com.tropico.moneyflow.ui.components.ExpenseRow
import com.tropico.moneyflow.ui.components.TransactionDetailDialog
import com.tropico.moneyflow.viewmodel.TransactionsUiState
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    state: TransactionsUiState,
    onQueryChange: (String) -> Unit,
    onCategoryChange: (ExpenseCategory?) -> Unit,
    onPaymentMethodChange: (PaymentMethod?) -> Unit,
    onSortChange: (SortOrder) -> Unit,
    onSelectExpense: (com.tropico.moneyflow.model.Expense?) -> Unit,
    onDeleteExpense: (com.tropico.moneyflow.model.Expense) -> Unit,
    onEditExpense: (com.tropico.moneyflow.model.Expense) -> Unit,
    onStartEdit: (com.tropico.moneyflow.model.Expense) -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = state.filters.query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            label = { Text("Search transactions") }
        )
        Row(modifier = Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip(onClick = { onCategoryChange(null) }, label = { Text(state.filters.category?.label ?: "All categories") })
            AssistChip(onClick = { onPaymentMethodChange(null) }, label = { Text(state.filters.paymentMethod?.label ?: "All payments") })
        }
        var sortExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(expanded = sortExpanded, onExpandedChange = { sortExpanded = !sortExpanded }) {
            OutlinedTextField(
                modifier = Modifier.menuAnchor().fillMaxWidth().padding(horizontal = 16.dp),
                value = state.filters.sortOrder.label,
                onValueChange = {},
                readOnly = true,
                label = { Text("Sort") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sortExpanded) }
            )
            DropdownMenu(expanded = sortExpanded, onDismissRequest = { sortExpanded = false }) {
                SortOrder.entries.forEach {
                    DropdownMenuItem(text = { Text(it.label) }, onClick = { onSortChange(it); sortExpanded = false })
                }
            }
        }

        val grouped = state.expenses.groupBy { it.date }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            grouped.forEach { (date, expenses) ->
                item { Text(date.format(DateTimeFormatter.ofPattern("EEE, dd MMM"))) }
                items(expenses, key = { it.id }) {
                    ExpenseRow(expense = it, currency = state.preferences.currency, onClick = { onSelectExpense(it) })
                }
            }
        }
    }

    state.selectedExpense?.let { selected ->
        TransactionDetailDialog(
            expense = selected,
            currency = state.preferences.currency,
            onDismiss = { onSelectExpense(null) },
            onEdit = { onStartEdit(selected) },
            onDelete = { onDeleteExpense(selected) }
        )
    }
}
