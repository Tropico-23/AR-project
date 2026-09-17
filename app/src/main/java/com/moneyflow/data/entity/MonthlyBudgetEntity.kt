package com.moneyflow.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "monthly_budgets")
data class MonthlyBudgetEntity(
    @PrimaryKey val monthKey: String,
    val amountMinor: Long
)
