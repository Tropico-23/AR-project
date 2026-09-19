package com.tropico.moneyflow.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tropico.moneyflow.data.preferences.AppPreferences
import com.tropico.moneyflow.data.preferences.PreferencesRepository
import com.tropico.moneyflow.data.repository.ExpenseRepository
import com.tropico.moneyflow.domain.ExpenseAnalytics
import com.tropico.moneyflow.model.Expense
import com.tropico.moneyflow.model.InsightStats
import com.tropico.moneyflow.model.TrendRange
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class InsightsUiState(
    val loading: Boolean = true,
    val expenses: List<Expense> = emptyList(),
    val stats: InsightStats = InsightStats(),
    val breakdown: List<Pair<com.tropico.moneyflow.model.ExpenseCategory, Long>> = emptyList(),
    val trend: List<Pair<LocalDate, Long>> = emptyList(),
    val selectedRange: TrendRange = TrendRange.Days30,
    val preferences: AppPreferences = AppPreferences(),
    val insights: List<String> = emptyList()
)

class InsightsViewModel(
    repository: ExpenseRepository,
    preferencesRepository: PreferencesRepository
) : ViewModel() {
    private val selectedRange = MutableStateFlow(TrendRange.Days30)

    val uiState: StateFlow<InsightsUiState> = combine(
        repository.observeAllExpenses(),
        preferencesRepository.preferences,
        selectedRange
    ) { expenses, prefs, range ->
        val now = LocalDate.now()
        InsightsUiState(
            loading = false,
            expenses = expenses,
            stats = ExpenseAnalytics.insightStats(expenses, now),
            breakdown = ExpenseAnalytics.categoryBreakdown(expenses),
            trend = ExpenseAnalytics.trend(expenses, now, range),
            selectedRange = range,
            preferences = prefs,
            insights = ExpenseAnalytics.factualInsights(expenses, now)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InsightsUiState())

    fun setRange(range: TrendRange) = selectedRange.update { range }
}
