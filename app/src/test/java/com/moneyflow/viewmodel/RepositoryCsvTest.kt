package com.moneyflow.viewmodel

import com.moneyflow.data.dao.BudgetDao
import com.moneyflow.data.dao.ExpenseDao
import com.moneyflow.data.entity.CategoryBudgetEntity
import com.moneyflow.data.entity.ExpenseEntity
import com.moneyflow.data.entity.MonthlyBudgetEntity
import com.moneyflow.data.repository.ExpenseRepository
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class RepositoryCsvTest {
    @Test
    fun importCsvInsertsRows() = runTest {
        val expenseDao = FakeExpenseDao()
        val repository = ExpenseRepository(expenseDao, FakeBudgetDao())
        val csv = """
            Date,Time,Title,Category,Amount,Payment,Note
            2026-09-17,12:30,Lunch,Food,12.50,Cash,University lunch
        """.trimIndent()

        val result = repository.importExpensesFromCsv(ByteArrayInputStream(csv.toByteArray())).getOrThrow()

        assertEquals(1, result)
        assertEquals(1, expenseDao.inserted.size)
        assertEquals("Lunch", expenseDao.inserted.first().title)
    }

    @Test
    fun exportCsvWritesHeader() = runTest {
        val expenseDao = FakeExpenseDao()
        val repository = ExpenseRepository(expenseDao, FakeBudgetDao())
        val output = ByteArrayOutputStream()

        repository.exportExpensesToCsv(emptyList(), output)

        val text = output.toString()
        assert(text.contains("Date,Time,Title,Category,Amount,Payment,Note"))
    }
}

private class FakeExpenseDao : ExpenseDao {
    val inserted = mutableListOf<ExpenseEntity>()
    override fun observeAll(): Flow<List<ExpenseEntity>> = MutableStateFlow(emptyList())
    override fun observeRecent(limit: Int): Flow<List<ExpenseEntity>> = MutableStateFlow(emptyList())
    override fun observeFiltered(query: String, category: String?, paymentMethod: String?, startDate: Long?, endDate: Long?): Flow<List<ExpenseEntity>> = MutableStateFlow(emptyList())
    override suspend fun getById(id: Long): ExpenseEntity? = null
    override suspend fun insert(expense: ExpenseEntity): Long { inserted += expense; return inserted.size.toLong() }
    override suspend fun update(expense: ExpenseEntity) = Unit
    override suspend fun delete(expense: ExpenseEntity) = Unit
    override suspend fun clearAll() = Unit
}

private class FakeBudgetDao : BudgetDao {
    override fun observeMonthlyBudget(monthKey: String): Flow<MonthlyBudgetEntity?> = MutableStateFlow(null)
    override suspend fun upsertMonthlyBudget(budget: MonthlyBudgetEntity) = Unit
    override fun observeCategoryBudgets(monthKey: String): Flow<List<CategoryBudgetEntity>> = MutableStateFlow(emptyList())
    override suspend fun upsertCategoryBudget(budget: CategoryBudgetEntity) = Unit
    override suspend fun clearMonthlyBudgets() = Unit
    override suspend fun clearCategoryBudgets() = Unit
}
