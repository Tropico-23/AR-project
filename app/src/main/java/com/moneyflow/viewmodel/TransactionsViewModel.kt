package com.moneyflow.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneyflow.data.preferences.AppPreferences
import com.moneyflow.data.preferences.PreferencesRepository
import com.moneyflow.data.repository.ExpenseRepository
import com.moneyflow.model.Expense
import com.moneyflow.model.ExpenseCategory
import com.moneyflow.model.PaymentMethod
import com.moneyflow.model.SortOrder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TransactionFilters(
    val query: String = "",
    val category: ExpenseCategory? = null,
    val paymentMethod: PaymentMethod? = null,
    val sortOrder: SortOrder = SortOrder.Newest
)

data class TransactionsUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val expenses: List<Expense> = emptyList(),
    val filters: TransactionFilters = TransactionFilters(),
    val preferences: AppPreferences = AppPreferences(),
    val selectedExpense: Expense? = null
)

class TransactionsViewModel(
    private val repository: ExpenseRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {
    private val filters = MutableStateFlow(TransactionFilters())
    private val manualError = MutableStateFlow<String?>(null)
    private val selectedExpense = MutableStateFlow<Expense?>(null)

    val uiState: StateFlow<TransactionsUiState> = combine(
        filters.flatMapLatest { filter ->
            repository.observeFilteredExpenses(
                query = filter.query,
                category = filter.category,
                paymentMethod = filter.paymentMethod,
                startDate = null,
                endDate = null
            ).map { expenses ->
                when (filter.sortOrder) {
                    SortOrder.Newest -> expenses.sortedWith(compareByDescending<Expense> { it.date }.thenByDescending { it.time })
                    SortOrder.Oldest -> expenses.sortedWith(compareBy<Expense> { it.date }.thenBy { it.time })
                    SortOrder.Highest -> expenses.sortedByDescending(Expense::amountMinor)
                    SortOrder.Lowest -> expenses.sortedBy(Expense::amountMinor)
                }
            }
        },
        preferencesRepository.preferences,
        filters,
        manualError,
        selectedExpense
    ) { expenses, prefs, filter, error, selected ->
        TransactionsUiState(
            loading = false,
            error = error,
            expenses = expenses,
            filters = filter,
            preferences = prefs,
            selectedExpense = selected
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TransactionsUiState())

    fun updateQuery(query: String) = filters.update { it.copy(query = query) }
    fun setCategory(category: ExpenseCategory?) = filters.update { it.copy(category = category) }
    fun setPaymentMethod(method: PaymentMethod?) = filters.update { it.copy(paymentMethod = method) }
    fun setSortOrder(sortOrder: SortOrder) = filters.update { it.copy(sortOrder = sortOrder) }

    fun selectExpense(expense: Expense?) {
        selectedExpense.value = expense
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            runCatching { repository.deleteExpense(expense) }
                .onSuccess { selectedExpense.value = null }
                .onFailure { manualError.value = "Could not delete expense." }
        }
    }

    fun updateExpense(expense: Expense) {
        viewModelScope.launch {
            runCatching { repository.updateExpense(expense) }
                .onSuccess { selectedExpense.value = null }
                .onFailure { manualError.value = "Could not update expense." }
        }
    }
}
