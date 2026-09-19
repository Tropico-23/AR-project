package com.tropico.moneyflow.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.tropico.moneyflow.data.dao.BudgetDao
import com.tropico.moneyflow.data.dao.ExpenseDao
import com.tropico.moneyflow.data.entity.CategoryBudgetEntity
import com.tropico.moneyflow.data.entity.ExpenseEntity
import com.tropico.moneyflow.data.entity.MonthlyBudgetEntity

@Database(
    entities = [ExpenseEntity::class, MonthlyBudgetEntity::class, CategoryBudgetEntity::class],
    version = 1,
    exportSchema = false
)
abstract class MoneyFlowDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao
    abstract fun budgetDao(): BudgetDao
}
