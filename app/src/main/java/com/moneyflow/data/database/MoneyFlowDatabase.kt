package com.moneyflow.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.moneyflow.data.dao.BudgetDao
import com.moneyflow.data.dao.ExpenseDao
import com.moneyflow.data.entity.CategoryBudgetEntity
import com.moneyflow.data.entity.ExpenseEntity
import com.moneyflow.data.entity.MonthlyBudgetEntity

@Database(
    entities = [ExpenseEntity::class, MonthlyBudgetEntity::class, CategoryBudgetEntity::class],
    version = 1,
    exportSchema = false
)
abstract class MoneyFlowDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao
    abstract fun budgetDao(): BudgetDao
}
