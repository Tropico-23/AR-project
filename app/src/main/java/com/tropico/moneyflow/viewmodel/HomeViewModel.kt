package com.tropico.moneyflow.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tropico.moneyflow.data.preferences.AppPreferences
import com.tropico.moneyflow.data.preferences.PreferencesRepository
import com.tropico.moneyflow.data.repository.ExpenseRepository
import com.tropico.moneyflow.domain.ExpenseAnalytics
import com.tropico.moneyflow.model.DashboardSummary
import com.tropico.moneyflow.model.Expense
import com.tropico.moneyflow.model.MonthlyBudget
import com.tropico.moneyflow.util.DateUtils
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val expenses: List<Expense> = emptyList(),
    val recentExpenses: List<Expense> = emptyList(),
    val todayExpenses: List<Expense> = emptyList(),
    val summary: DashboardSummary = DashboardSummary(),
    val preferences: AppPreferences = AppPreferences()
)

class HomeViewModel(
    private val repository: ExpenseRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {
    private val monthKey = DateUtils.monthKey(LocalDate.now())
    private val manualError = MutableStateFlow<String?>(null)

    val uiState: StateFlow<HomeUiState> = combine(
        repository.observeAllExpenses(),
        repository.observeRecentExpenses(),
        repository.observeMonthlyBudget(monthKey),
        preferencesRepository.preferences,
        manualError
    ) { expenses, recent, budget, prefs, error ->
        val summary = ExpenseAnalytics.dashboard(expenses, LocalDate.now(), budget?.amountMinor ?: 0)
        HomeUiState(
            loading = false,
            error = error,
            expenses = expenses,
            recentExpenses = recent,
            todayExpenses = expenses.filter { it.date == LocalDate.now() },
            summary = summary,
            preferences = prefs
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun addExpense(expense: Expense, onDone: () -> Unit) {
        viewModelScope.launch {
            runCatching { repository.addExpense(expense) }
                .onSuccess {
                    manualError.value = null
                    onDone()
                }
                .onFailure { manualError.value = "Could not save expense. Please try again." }
        }
    }

    fun setMonthlyBudget(amountMinor: Long) {
        viewModelScope.launch {
            runCatching {
                repository.upsertMonthlyBudget(MonthlyBudget(monthKey = monthKey, amountMinor = amountMinor))
            }.onFailure { manualError.update { "Could not save budget." } }
        }
    }
}
