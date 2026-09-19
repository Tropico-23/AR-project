package com.tropico.moneyflow.domain

import com.tropico.moneyflow.model.DashboardSummary
import com.tropico.moneyflow.model.Expense
import com.tropico.moneyflow.model.ExpenseCategory
import com.tropico.moneyflow.model.InsightStats
import com.tropico.moneyflow.model.TrendRange
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.roundToLong

object ExpenseAnalytics {
    fun dashboard(expenses: List<Expense>, now: LocalDate, monthBudgetMinor: Long): DashboardSummary {
        val todaySpent = expenses.filter { it.date == now }.sumOf { it.amountMinor }
        val weekStart = now.minusDays(6)
        val weekSpent = expenses.filter { it.date in weekStart..now }.sumOf { it.amountMinor }
        val month = YearMonth.from(now)
        val monthExpenses = expenses.filter { YearMonth.from(it.date) == month }
        val monthSpent = monthExpenses.sumOf { it.amountMinor }
        val averageDaily = if (monthExpenses.isEmpty()) 0 else (monthSpent.toDouble() / now.dayOfMonth).roundToLong()
        val remaining = (monthBudgetMinor - monthSpent).coerceAtLeast(0)
        val budgetPercent = if (monthBudgetMinor <= 0) 0f else (monthSpent.toFloat() / monthBudgetMinor.toFloat()).coerceIn(0f, 1f)
        return DashboardSummary(todaySpent, weekSpent, monthSpent, averageDaily, monthBudgetMinor, remaining, budgetPercent)
    }

    fun categoryBreakdown(expenses: List<Expense>): List<Pair<ExpenseCategory, Long>> =
        expenses.groupBy { it.category }
            .mapValues { it.value.sumOf(Expense::amountMinor) }
            .toList()
            .sortedByDescending { it.second }

    fun trend(expenses: List<Expense>, now: LocalDate, range: TrendRange): List<Pair<LocalDate, Long>> {
        val days = when (range) {
            TrendRange.Days7 -> 7
            TrendRange.Days30 -> 30
            TrendRange.Months6 -> 180
            TrendRange.Year1 -> 365
        }
        val start = now.minusDays((days - 1).toLong())
        return (0 until days).map { offset ->
            val day = start.plusDays(offset.toLong())
            day to expenses.filter { it.date == day }.sumOf(Expense::amountMinor)
        }
    }

    fun insightStats(expenses: List<Expense>, now: LocalDate): InsightStats {
        if (expenses.isEmpty()) return InsightStats()
        val total = expenses.sumOf(Expense::amountMinor)
        val averageTransaction = total / expenses.size
        val firstDate = expenses.minOf(Expense::date)
        val spanDays = (now.toEpochDay() - firstDate.toEpochDay() + 1).coerceAtLeast(1)
        val averageDaily = total / spanDays
        val largest = expenses.maxOf(Expense::amountMinor)
        val grouped = categoryBreakdown(expenses)
        val topCategory = grouped.firstOrNull()?.first
        val currentMonth = YearMonth.from(now)
        val previousMonth = currentMonth.minusMonths(1)
        val currentTotal = expenses.filter { YearMonth.from(it.date) == currentMonth }.sumOf(Expense::amountMinor)
        val previousTotal = expenses.filter { YearMonth.from(it.date) == previousMonth }.sumOf(Expense::amountMinor)
        val change = if (previousTotal > 0) ((currentTotal - previousTotal) / previousTotal.toFloat()) else null
        return InsightStats(total, averageTransaction, averageDaily, largest, topCategory, expenses.size, change)
    }

    fun factualInsights(expenses: List<Expense>, now: LocalDate): List<String> {
        if (expenses.size < 3) return emptyList()
        val insights = mutableListOf<String>()
        val topCategory = categoryBreakdown(expenses).firstOrNull()
        if (topCategory != null) {
            insights += "You spent the most on ${topCategory.first.label}."
        }
        val stats = insightStats(expenses, now)
        stats.monthOverMonthChange?.let { delta ->
            val direction = if (delta >= 0) "higher" else "lower"
            insights += "This month spending is ${"%.0f".format(kotlin.math.abs(delta) * 100)}% $direction than last month."
        }
        val highestDay = expenses.groupBy { it.date }.maxByOrNull { it.value.sumOf(Expense::amountMinor) }
        highestDay?.let { insights += "Highest spending day: ${it.key.dayOfWeek.name.lowercase().replaceFirstChar(Char::titlecase)}." }
        return insights.take(3)
    }
}
