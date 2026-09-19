package com.tropico.moneyflow.data.entity

import androidx.room.Entity

@Entity(tableName = "category_budgets", primaryKeys = ["monthKey", "category"])
data class CategoryBudgetEntity(
    val monthKey: String,
    val category: String,
    val amountMinor: Long
)
