package com.tropico.moneyflow.viewmodel

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tropico.moneyflow.data.preferences.AppPreferences
import com.tropico.moneyflow.data.preferences.PreferencesRepository
import com.tropico.moneyflow.data.repository.ExpenseRepository
import com.tropico.moneyflow.model.AppCurrency
import com.tropico.moneyflow.model.PaymentMethod
import com.tropico.moneyflow.model.ThemeMode
import com.tropico.moneyflow.model.WeekStart
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val preferences: AppPreferences = AppPreferences(),
    val message: String? = null
)

data class AppearanceState(val themeMode: ThemeMode = ThemeMode.System)

class SettingsViewModel(
    private val repository: ExpenseRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {
    private val messageFlow = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)

    val uiState: StateFlow<SettingsUiState> = combine(
        preferencesRepository.preferences,
        messageFlow
    ) { preferences, message -> SettingsUiState(preferences, message) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    val appearanceState: StateFlow<AppearanceState> = preferencesRepository.preferences
        .map { AppearanceState(it.themeMode) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppearanceState())

    fun setTheme(mode: ThemeMode) = launchPref { preferencesRepository.setTheme(mode) }
    fun setCurrency(currency: AppCurrency) = launchPref { preferencesRepository.setCurrency(currency) }
    fun setDefaultPayment(method: PaymentMethod) = launchPref { preferencesRepository.setDefaultPaymentMethod(method) }
    fun setWeekStart(weekStart: WeekStart) = launchPref { preferencesRepository.setWeekStart(weekStart) }

    fun clearAllData() {
        viewModelScope.launch {
            runCatching { repository.clearAllData() }
                .onSuccess { messageFlow.value = "All data cleared." }
                .onFailure { messageFlow.value = "Could not clear data." }
        }
    }

    fun insertDemoData() {
        viewModelScope.launch {
            runCatching { repository.insertDemoData() }
                .onSuccess { messageFlow.value = "Demo data inserted." }
                .onFailure { messageFlow.value = "Could not insert demo data." }
        }
    }

    fun exportCsv(uri: Uri, resolver: ContentResolver, expenses: List<com.tropico.moneyflow.model.Expense>) {
        viewModelScope.launch {
            runCatching {
                resolver.openOutputStream(uri)?.use { repository.exportExpensesToCsv(expenses, it) }
                    ?: error("Unable to open file")
            }.onSuccess { messageFlow.value = "Transactions exported." }
                .onFailure { messageFlow.value = "Export failed. Please try again." }
        }
    }

    fun importCsv(uri: Uri, resolver: ContentResolver) {
        viewModelScope.launch {
            runCatching {
                resolver.openInputStream(uri)?.use { repository.importExpensesFromCsv(it).getOrThrow() }
                    ?: error("Unable to open file")
            }.onSuccess { count -> messageFlow.value = "Imported $count transactions." }
                .onFailure { messageFlow.value = "Import failed. Please verify CSV format." }
        }
    }

    private fun launchPref(block: suspend () -> Unit) {
        viewModelScope.launch {
            runCatching { block() }.onFailure { messageFlow.value = "Could not save preference." }
        }
    }
}
