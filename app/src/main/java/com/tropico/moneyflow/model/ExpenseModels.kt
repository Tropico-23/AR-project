package com.tropico.moneyflow.model

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

data class Expense(
    val id: Long = 0,
    val amountMinor: Long,
    val title: String,
    val category: ExpenseCategory,
    val date: LocalDate,
    val time: LocalTime?,
    val note: String?,
    val paymentMethod: PaymentMethod,
    val createdAt: LocalDateTime
)

data class MonthlyBudget(
    val monthKey: String,
    val amountMinor: Long
)

data class CategoryBudget(
    val monthKey: String,
    val category: ExpenseCategory,
    val amountMinor: Long
)

data class DashboardSummary(
    val todaySpent: Long = 0,
    val weekSpent: Long = 0,
    val monthSpent: Long = 0,
    val averageDaily: Long = 0,
    val budget: Long = 0,
    val remaining: Long = 0,
    val budgetPercent: Float = 0f
)

data class InsightStats(
    val totalSpent: Long = 0,
    val averageTransaction: Long = 0,
    val averageDaily: Long = 0,
    val largestTransaction: Long = 0,
    val topCategory: ExpenseCategory? = null,
    val transactionCount: Int = 0,
    val monthOverMonthChange: Float? = null
)
