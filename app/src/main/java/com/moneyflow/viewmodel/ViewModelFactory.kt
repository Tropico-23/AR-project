package com.moneyflow.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.moneyflow.data.preferences.PreferencesRepository
import com.moneyflow.data.repository.ExpenseRepository

class ViewModelFactory(
    private val repository: ExpenseRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(HomeViewModel::class.java) -> HomeViewModel(repository, preferencesRepository) as T
            modelClass.isAssignableFrom(TransactionsViewModel::class.java) -> TransactionsViewModel(repository, preferencesRepository) as T
            modelClass.isAssignableFrom(InsightsViewModel::class.java) -> InsightsViewModel(repository, preferencesRepository) as T
            modelClass.isAssignableFrom(BudgetViewModel::class.java) -> BudgetViewModel(repository, preferencesRepository) as T
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> SettingsViewModel(repository, preferencesRepository) as T
            else -> error("Unknown ViewModel class")
        }
    }
}
