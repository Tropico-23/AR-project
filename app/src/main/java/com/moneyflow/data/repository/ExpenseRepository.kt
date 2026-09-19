package com.tropico.moneyflow.data.repository

import com.tropico.moneyflow.data.dao.BudgetDao
import com.tropico.moneyflow.data.dao.ExpenseDao
import com.tropico.moneyflow.data.entity.CategoryBudgetEntity
import com.tropico.moneyflow.data.entity.ExpenseEntity
import com.tropico.moneyflow.data.entity.MonthlyBudgetEntity
import com.tropico.moneyflow.model.CategoryBudget
import com.tropico.moneyflow.model.Expense
import com.tropico.moneyflow.model.ExpenseCategory
import com.tropico.moneyflow.model.MonthlyBudget
import com.tropico.moneyflow.model.PaymentMethod
import com.tropico.moneyflow.util.DateUtils
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.math.roundToLong
import java.time.LocalTime
import java.time.ZoneId
import android.content.Context
import androidx.room.withTransaction
import org.json.JSONArray
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ExpenseRepository(
    private val expenseDao: ExpenseDao,
    private val budgetDao: BudgetDao
) {
    fun observeAllExpenses(): Flow<List<Expense>> = expenseDao.observeAll().map { it.map(ExpenseEntity::toModel) }

    suspend fun migrateLegacySharedPreferences(context: Context) {
        val db = expenseDao.javaClass // migration is idempotent through the marker below
        val prefs = context.getSharedPreferences("moneyflow", Context.MODE_PRIVATE)
        if (prefs.getBoolean("room_legacy_migration_done", false)) return

        val raw = prefs.getString("expenses", "[]").orEmpty()
        val array = runCatching { JSONArray(raw) }.getOrElse { JSONArray() }
        for (i in 0 until array.length()) {
            val item = array.getJSONObject(i)
            val id = item.optLong("id", 0L)
            val amount = item.optDouble("amount", 0.0)
            if (id == 0L || !amount.isFinite() || amount <= 0.0) continue
            val date = runCatching { LocalDate.parse(item.optString("date")) }.getOrElse { LocalDate.now() }
            val expense = Expense(
                id = id,
                amountMinor = (amount * 100.0).roundToLong(),
                title = item.optString("title"),
                category = ExpenseCategory.fromValue(item.optString("category", "Other")),
                date = date,
                time = null,
                note = item.optString("note").ifBlank { null },
                paymentMethod = PaymentMethod.fromValue(item.optString("payment", "Cash")),
                createdAt = LocalDateTime.now()
            )
            expenseDao.insert(expense.toEntity())
        }

        val budget = prefs.getFloat("budget", 500f).toDouble()
        budgetDao.upsertMonthlyBudget(
            MonthlyBudgetEntity(
                DateUtils.monthKey(LocalDate.now()),
                (budget * 100.0).roundToLong().coerceAtLeast(0L)
            )
        )
        prefs.edit().putBoolean("room_legacy_migration_done", true).apply()
    }

    fun observeRecentExpenses(limit: Int = 5): Flow<List<Expense>> =
        expenseDao.observeRecent(limit).map { entities -> entities.map(ExpenseEntity::toModel) }

    fun observeFilteredExpenses(
        query: String,
        category: ExpenseCategory?,
        paymentMethod: PaymentMethod?,
        startDate: LocalDate?,
        endDate: LocalDate?
    ): Flow<List<Expense>> = expenseDao.observeFiltered(
        query = query.trim(),
        category = category?.name,
        paymentMethod = paymentMethod?.name,
        startDate = startDate?.toEpochDay(),
        endDate = endDate?.toEpochDay()
    ).map { it.map(ExpenseEntity::toModel) }

    suspend fun addExpense(expense: Expense): Long = expenseDao.insert(expense.toEntity())

    suspend fun updateExpense(expense: Expense) = expenseDao.update(expense.toEntity())

    suspend fun deleteExpense(expense: Expense) = expenseDao.delete(expense.toEntity())

    suspend fun getExpense(id: Long): Expense? = expenseDao.getById(id)?.toModel()

    fun observeMonthlyBudget(monthKey: String): Flow<MonthlyBudget?> =
        budgetDao.observeMonthlyBudget(monthKey).map { it?.let(MonthlyBudgetEntity::toModel) }

    fun observeCategoryBudgets(monthKey: String): Flow<List<CategoryBudget>> =
        budgetDao.observeCategoryBudgets(monthKey).map { list -> list.map(CategoryBudgetEntity::toModel) }

    suspend fun upsertMonthlyBudget(monthlyBudget: MonthlyBudget) {
        budgetDao.upsertMonthlyBudget(monthlyBudget.toEntity())
    }

    suspend fun upsertCategoryBudget(categoryBudget: CategoryBudget) {
        budgetDao.upsertCategoryBudget(categoryBudget.toEntity())
    }

    suspend fun clearAllData() {
        expenseDao.clearAll()
        budgetDao.clearCategoryBudgets()
        budgetDao.clearMonthlyBudgets()
    }

    suspend fun insertDemoData() {
        val today = LocalDate.now()
        val now = LocalDateTime.now()
        val demo = listOf(
            Expense(amountMinor = 1250, title = "Lunch", category = ExpenseCategory.Food, date = today, time = LocalTime.NOON, note = null, paymentMethod = PaymentMethod.Cash, createdAt = now),
            Expense(amountMinor = 700, title = "Taxi", category = ExpenseCategory.Transport, date = today.minusDays(1), time = LocalTime.of(9, 15), note = null, paymentMethod = PaymentMethod.BankCard, createdAt = now),
            Expense(amountMinor = 450, title = "Notebook", category = ExpenseCategory.Education, date = today.minusDays(2), time = null, note = null, paymentMethod = PaymentMethod.Cash, createdAt = now),
            Expense(amountMinor = 1500, title = "Cinema", category = ExpenseCategory.Entertainment, date = today.minusDays(3), time = LocalTime.of(19, 30), note = null, paymentMethod = PaymentMethod.BankCard, createdAt = now),
            Expense(amountMinor = 4200, title = "Groceries", category = ExpenseCategory.Food, date = today.minusDays(4), time = null, note = "Weekly groceries", paymentMethod = PaymentMethod.BankTransfer, createdAt = now),
            Expense(amountMinor = 3500, title = "T-shirt", category = ExpenseCategory.Shopping, date = today.minusDays(6), time = null, note = null, paymentMethod = PaymentMethod.BankCard, createdAt = now)
        )
        demo.forEach { expenseDao.insert(it.toEntity()) }
    }

    suspend fun exportExpensesToCsv(expenses: List<Expense>, output: OutputStream) {
        output.bufferedWriter().use { writer ->
            writer.appendLine("Date,Time,Title,Category,Amount,Payment,Note")
            expenses.forEach { expense ->
                val date = expense.date.toString()
                val time = expense.time?.toString().orEmpty()
                val amount = "%.2f".format(expense.amountMinor / 100.0)
                val safeTitle = escapeCsv(expense.title)
                val safeNote = escapeCsv(expense.note.orEmpty())
                writer.appendLine("$date,$time,$safeTitle,${expense.category.label},$amount,${expense.paymentMethod.label},$safeNote")
            }
        }
    }

    suspend fun importExpensesFromCsv(input: InputStream): Result<Int> = runCatching {
        var inserted = 0
        BufferedReader(InputStreamReader(input)).useLines { lines ->
            lines.drop(1).forEach { line ->
                if (line.isBlank()) return@forEach
                val parts = parseCsvLine(line)
                require(parts.size >= 7) { "Malformed CSV row" }
                val date = LocalDate.parse(parts[0].trim())
                val time = parts[1].trim().takeIf { it.isNotBlank() }?.let { LocalTime.parse(it) }
                val title = parts[2].trim()
                require(title.isNotBlank()) { "Missing title" }
                val category = ExpenseCategory.entries.firstOrNull { it.label.equals(parts[3].trim(), true) }
                    ?: ExpenseCategory.Other
                val amountMinor = (parts[4].trim().toDouble() * 100).toLong()
                require(amountMinor > 0) { "Invalid amount" }
                val paymentMethod = PaymentMethod.entries.firstOrNull { it.label.equals(parts[5].trim(), true) }
                    ?: PaymentMethod.Other
                val note = parts.getOrNull(6)?.trim()?.ifBlank { null }
                expenseDao.insert(
                    Expense(
                        amountMinor = amountMinor,
                        title = title,
                        category = category,
                        date = date,
                        time = time,
                        note = note,
                        paymentMethod = paymentMethod,
                        createdAt = LocalDateTime.now()
                    ).toEntity()
                )
                inserted++
            }
        }
        inserted
    }

    private fun escapeCsv(value: String): String {
        val escaped = value.replace("\"", "\"\"")
        return if (escaped.contains(',') || escaped.contains('"')) "\"$escaped\"" else escaped
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val char = line[i]
            if (char == '"') {
                if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                    current.append('"')
                    i++
                } else {
                    inQuotes = !inQuotes
                }
            } else if (char == ',' && !inQuotes) {
                result += current.toString()
                current.clear()
            } else {
                current.append(char)
            }
            i++
        }
        result += current.toString()
        return result
    }
}

private fun Expense.toEntity(): ExpenseEntity = ExpenseEntity(
    id = id,
    amountMinor = amountMinor,
    title = title,
    category = category.name,
    dateEpochDay = date.toEpochDay(),
    timeMinutes = time?.let { it.hour * 60 + it.minute },
    note = note,
    paymentMethod = paymentMethod.name,
    createdAtEpochMillis = createdAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
)

private fun ExpenseEntity.toModel(): Expense = Expense(
    id = id,
    amountMinor = amountMinor,
    title = title,
    category = ExpenseCategory.fromValue(category),
    date = LocalDate.ofEpochDay(dateEpochDay),
    time = timeMinutes?.let { LocalTime.of(it / 60, it % 60) },
    note = note,
    paymentMethod = PaymentMethod.fromValue(paymentMethod),
    createdAt = LocalDateTime.ofInstant(Instant.ofEpochMilli(createdAtEpochMillis), ZoneId.systemDefault())
)

private fun MonthlyBudgetEntity.toModel(): MonthlyBudget = MonthlyBudget(monthKey, amountMinor)
private fun MonthlyBudget.toEntity(): MonthlyBudgetEntity = MonthlyBudgetEntity(monthKey, amountMinor)
private fun CategoryBudgetEntity.toModel(): CategoryBudget = CategoryBudget(monthKey, ExpenseCategory.fromValue(category), amountMinor)
private fun CategoryBudget.toEntity(): CategoryBudgetEntity = CategoryBudgetEntity(monthKey, category.name, amountMinor)
