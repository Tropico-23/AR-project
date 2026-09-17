package com.moneyflow.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moneyflow.model.TrendRange
import com.moneyflow.ui.components.DonutChart
import com.moneyflow.ui.components.TrendLineChart
import com.moneyflow.util.MoneyFormat
import com.moneyflow.viewmodel.InsightsUiState

@Composable
fun InsightsScreen(state: InsightsUiState, onRangeChanged: (TrendRange) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Insights", style = MaterialTheme.typography.headlineMedium)
        }
        if (state.expenses.isEmpty()) {
            item { Card { Text("Add transactions to unlock insights.", modifier = Modifier.padding(16.dp)) } }
            return@LazyColumn
        }
        item {
            DonutChart(state.breakdown)
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Trend range")
                androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TrendRange.entries.forEach { range ->
                        AssistChip(onClick = { onRangeChanged(range) }, label = { Text(range.label) })
                    }
                }
            }
        }
        item {
            TrendLineChart(state.trend)
        }
        item {
            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Total spent: ${MoneyFormat.formatMinor(state.stats.totalSpent, state.preferences.currency)}")
                    Text("Average transaction: ${MoneyFormat.formatMinor(state.stats.averageTransaction, state.preferences.currency)}")
                    Text("Average daily: ${MoneyFormat.formatMinor(state.stats.averageDaily, state.preferences.currency)}")
                    Text("Largest transaction: ${MoneyFormat.formatMinor(state.stats.largestTransaction, state.preferences.currency)}")
                    Text("Most expensive category: ${state.stats.topCategory?.label ?: "N/A"}")
                    Text("Transactions count: ${state.stats.transactionCount}")
                    Text(
                        state.stats.monthOverMonthChange?.let { "Compared with previous month: ${"%.1f".format(it * 100)}%" }
                            ?: "Compared with previous month: not enough data"
                    )
                }
            }
        }
        items(state.insights) { insight ->
            Card { Text(insight, modifier = Modifier.padding(16.dp)) }
        }
    }
}
