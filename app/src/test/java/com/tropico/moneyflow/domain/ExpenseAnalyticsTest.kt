package com.tropico.moneyflow.domain

import com.tropico.moneyflow.model.Expense
import com.tropico.moneyflow.model.ExpenseCategory
import com.tropico.moneyflow.model.PaymentMethod
import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpenseAnalyticsTest {
    private val now = LocalDate.of(2026, 9, 17)

    @Test
    fun dashboardCalculatesMonthAndWeekStats() {
        val expenses = listOf(
            expense(1000, now),
            expense(2500, now.minusDays(1)),
            expense(500, now.minusDays(10))
        )

        val summary = ExpenseAnalytics.dashboard(expenses, now, monthBudgetMinor = 10_000)

        assertEquals(1000, summary.todaySpent)
        assertEquals(3500, summary.weekSpent)
        assertEquals(4000, summary.monthSpent)
        assertTrue(summary.budgetPercent > 0.39f)
    }

    @Test
    fun factualInsightsRequireEnoughData() {
        val insights = ExpenseAnalytics.factualInsights(listOf(expense(500, now), expense(600, now)), now)
        assertTrue(insights.isEmpty())
    }

    private fun expense(amount: Long, date: LocalDate) = Expense(
        amountMinor = amount,
        title = "Test",
        category = ExpenseCategory.Food,
        date = date,
        time = null,
        note = null,
        paymentMethod = PaymentMethod.Cash,
        createdAt = LocalDateTime.now()
    )
}
