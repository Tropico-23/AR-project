package com.tropico.moneyflow.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tropico.moneyflow.data.entity.CategoryBudgetEntity
import com.tropico.moneyflow.data.entity.MonthlyBudgetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Query("SELECT * FROM monthly_budgets WHERE monthKey = :monthKey LIMIT 1")
    fun observeMonthlyBudget(monthKey: String): Flow<MonthlyBudgetEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMonthlyBudget(budget: MonthlyBudgetEntity)

    @Query("SELECT * FROM category_budgets WHERE monthKey = :monthKey")
    fun observeCategoryBudgets(monthKey: String): Flow<List<CategoryBudgetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCategoryBudget(budget: CategoryBudgetEntity)

    @Query("DELETE FROM monthly_budgets")
    suspend fun clearMonthlyBudgets()

    @Query("DELETE FROM category_budgets")
    suspend fun clearCategoryBudgets()
}
