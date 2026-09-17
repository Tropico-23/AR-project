package com.moneyflow.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneyflow.data.preferences.AppPreferences
import com.moneyflow.data.preferences.PreferencesRepository
import com.moneyflow.data.repository.ExpenseRepository
import com.moneyflow.domain.ExpenseAnalytics
import com.moneyflow.model.CategoryBudget
import com.moneyflow.model.Expense
import com.moneyflow.model.ExpenseCategory
import com.moneyflow.model.MonthlyBudget
import com.moneyflow.util.DateUtils
import java.time.LocalDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BudgetUiState(
    val loading: Boolean = true,
    val monthBudgetMinor: Long = 0,
    val monthSpentMinor: Long = 0,
    val monthRemainingMinor: Long = 0,
    val monthPercent: Float = 0f,
    val categoryBudgets: List<CategoryBudget> = emptyList(),
    val expenses: List<Expense> = emptyList(),
    val preferences: AppPreferences = AppPreferences(),
    val error: String? = null
)

class BudgetViewModel(
    private val repository: ExpenseRepository,
    preferencesRepository: PreferencesRepository
) : ViewModel() {
    private val monthKey = DateUtils.monthKey(LocalDate.now())

    val uiState: StateFlow<BudgetUiState> = combine(
        repository.observeAllExpenses(),
        repository.observeMonthlyBudget(monthKey),
        repository.observeCategoryBudgets(monthKey),
        preferencesRepository.preferences
    ) { expenses, monthBudget, categoryBudgets, preferences ->
        val summary = ExpenseAnalytics.dashboard(expenses, LocalDate.now(), monthBudget?.amountMinor ?: 0)
        BudgetUiState(
            loading = false,
            monthBudgetMinor = monthBudget?.amountMinor ?: 0,
            monthSpentMinor = summary.monthSpent,
            monthRemainingMinor = summary.remaining,
            monthPercent = summary.budgetPercent,
            categoryBudgets = categoryBudgets,
            expenses = expenses,
            preferences = preferences
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BudgetUiState())

    fun setMonthlyBudget(amountMinor: Long) {
        viewModelScope.launch {
            repository.upsertMonthlyBudget(MonthlyBudget(monthKey = monthKey, amountMinor = amountMinor))
        }
    }

    fun setCategoryBudget(category: ExpenseCategory, amountMinor: Long) {
        viewModelScope.launch {
            repository.upsertCategoryBudget(CategoryBudget(monthKey = monthKey, category = category, amountMinor = amountMinor))
        }
    }
}
