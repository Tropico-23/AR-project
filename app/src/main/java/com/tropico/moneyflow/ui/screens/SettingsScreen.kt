package com.tropico.moneyflow.ui.screens

import android.content.ContentResolver
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.clickable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExposedDropdownMenu
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
import com.tropico.moneyflow.model.AppCurrency
import com.tropico.moneyflow.model.Expense
import com.tropico.moneyflow.model.PaymentMethod
import com.tropico.moneyflow.model.ThemeMode
import com.tropico.moneyflow.model.WeekStart
import com.tropico.moneyflow.viewmodel.SettingsUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    allExpenses: List<Expense>,
    contentResolver: ContentResolver,
    onThemeChanged: (ThemeMode) -> Unit,
    onCurrencyChanged: (AppCurrency) -> Unit,
    onDefaultPaymentChanged: (PaymentMethod) -> Unit,
    onWeekStartChanged: (WeekStart) -> Unit,
    onClearData: () -> Unit,
    onInsertDemoData: () -> Unit,
    onExportCsv: (android.net.Uri, ContentResolver, List<Expense>) -> Unit,
    onImportCsv: (android.net.Uri, ContentResolver) -> Unit
) {
    var clearDialog by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    val createCsvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) onExportCsv(uri, contentResolver, allExpenses)
    }
    val pickCsvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) onImportCsv(uri, contentResolver)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Text("Settings", style = MaterialTheme.typography.headlineMedium) }

        item {
            PreferencesDropdown("Appearance", state.preferences.themeMode, ThemeMode.entries, { it.name }, onThemeChanged)
        }
        item {
            PreferencesDropdown("Currency", state.preferences.currency, AppCurrency.entries, { it.code }, onCurrencyChanged)
        }
        item {
            PreferencesDropdown("Default payment", state.preferences.defaultPaymentMethod, PaymentMethod.entries, { it.label }, onDefaultPaymentChanged)
        }
        item {
            PreferencesDropdown("First day of week", state.preferences.weekStart, WeekStart.entries, { it.label }, onWeekStartChanged)
        }

        item {
            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Data", style = MaterialTheme.typography.titleMedium)
                    Button(onClick = { createCsvLauncher.launch("moneyflow-transactions.csv") }, modifier = Modifier.fillMaxWidth()) { Text("Export transactions to CSV") }
                    Button(onClick = { pickCsvLauncher.launch(arrayOf("text/*")) }, modifier = Modifier.fillMaxWidth()) { Text("Import/restore CSV") }
                    Button(onClick = onInsertDemoData, modifier = Modifier.fillMaxWidth()) { Text("Insert demo transactions") }
                    Button(onClick = { clearDialog = true }, modifier = Modifier.fillMaxWidth()) { Text("Clear all data") }
                }
            }
        }

        item {
            Card(modifier = Modifier.clickable { showAbout = true }) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("About", style = MaterialTheme.typography.titleMedium)
                    Text("MoneyFlow")
                    Text("Version 1.0")
                    Text("A simple and private way to understand your spending.")
                }
            }
        }

        state.message?.let { message ->
            item { Text(message) }
        }
    }

    if (showAbout) {
        AlertDialog(onDismissRequest = { showAbout = false }, title = { Text("About MoneyFlow") }, text = { Text("Made by tropico me") }, confirmButton = { TextButton(onClick = { showAbout = false }) { Text("Close") } })
    }

    if (clearDialog) {
        AlertDialog(
            onDismissRequest = { clearDialog = false },
            title = { Text("Clear all data?") },
            text = { Text("This will permanently remove transactions and budgets from your device.") },
            confirmButton = {
                TextButton(onClick = {
                    onClearData()
                    clearDialog = false
                }) { Text("Clear") }
            },
            dismissButton = { TextButton(onClick = { clearDialog = false }) { Text("Cancel") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> PreferencesDropdown(
    label: String,
    current: T,
    options: List<T>,
    itemLabel: (T) -> String,
    onSelect: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            value = itemLabel(current),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach {
                DropdownMenuItem(text = { Text(itemLabel(it)) }, onClick = { onSelect(it); expanded = false })
            }
        }
    }
}
