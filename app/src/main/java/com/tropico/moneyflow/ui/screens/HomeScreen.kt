package com.tropico.moneyflow.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tropico.moneyflow.domain.ExpenseAnalytics
import com.tropico.moneyflow.ui.components.BudgetProgressCard
import com.tropico.moneyflow.ui.components.ExpenseRow
import com.tropico.moneyflow.ui.components.Last7DaysBarChart
import com.tropico.moneyflow.viewmodel.HomeUiState
import java.time.LocalDate

@Composable
fun HomeScreen(
    state: HomeUiState,
    onSeeAll: () -> Unit
) {
    if (state.loading) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) { Text("Loading your dashboard…") }
        return
    }

    if (state.error != null) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) { Text(state.error) }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("MoneyFlow", style = MaterialTheme.typography.headlineMedium)
                Text("Good to see you 👋", style = MaterialTheme.typography.bodyLarge)
            }
        }
        item {
            BudgetProgressCard(
                title = "Spent this month",
                spent = state.summary.monthSpent,
                budget = state.summary.budget,
                currency = state.preferences.currency
            )
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Quick stats", style = MaterialTheme.typography.titleMedium)
                    Text("Today: ${com.tropico.moneyflow.util.MoneyFormat.formatMinor(state.summary.todaySpent, state.preferences.currency)}")
                    Text("This week: ${com.tropico.moneyflow.util.MoneyFormat.formatMinor(state.summary.weekSpent, state.preferences.currency)}")
                    Text("This month: ${com.tropico.moneyflow.util.MoneyFormat.formatMinor(state.summary.monthSpent, state.preferences.currency)}")
                    Text("Average daily: ${com.tropico.moneyflow.util.MoneyFormat.formatMinor(state.summary.averageDaily, state.preferences.currency)}")
                }
            }
        }
        item {
            Last7DaysBarChart(ExpenseAnalytics.trend(state.expenses, LocalDate.now(), com.tropico.moneyflow.model.TrendRange.Days7))
        }
        item {
            Text("Today’s Expenses", style = MaterialTheme.typography.titleLarge)
        }
        item { Text("Today: ${com.tropico.moneyflow.util.MoneyFormat.formatMinor(state.summary.todaySpent, state.preferences.currency)}", style = MaterialTheme.typography.headlineSmall) }
        if (state.todayExpenses.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Your spending story starts here 🌱", style = MaterialTheme.typography.titleMedium)
                        Text("Add your first expense and MoneyFlow will turn spending into useful insights.")
                    }
                }
            }
        } else {
            items(state.todayExpenses, key = { it.id }) { expense ->
                ExpenseRow(expense = expense, currency = state.preferences.currency)
            }
            item { TextButton(onClick = onSeeAll) { Text("View all expenses") } }
        }
    }
}
