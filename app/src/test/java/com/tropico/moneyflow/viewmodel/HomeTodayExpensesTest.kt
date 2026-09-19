package com.tropico.moneyflow.viewmodel

import com.tropico.moneyflow.model.Expense
import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeTodayExpensesTest {
    @Test
    fun onlyLocalTodayIsShownOnTodayExpenses() {
        val today = LocalDate.of(2026, 9, 19)
        val expenses = listOf(
            Expense(1, 500, "Coffee", com.tropico.moneyflow.model.ExpenseCategory.Coffee, today, null, null, com.tropico.moneyflow.model.PaymentMethod.Cash, LocalDateTime.now()),
            Expense(2, 1000, "Yesterday", com.tropico.moneyflow.model.ExpenseCategory.Food, today.minusDays(1), null, null, com.tropico.moneyflow.model.PaymentMethod.Cash, LocalDateTime.now())
        )
        assertEquals(listOf(1L), expenses.filter { it.date == today }.map { it.id })
    }
}
