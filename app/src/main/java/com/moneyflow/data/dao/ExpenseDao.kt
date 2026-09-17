package com.moneyflow.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.moneyflow.data.entity.ExpenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY dateEpochDay DESC, COALESCE(timeMinutes, 0) DESC, id DESC")
    fun observeAll(): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses ORDER BY dateEpochDay DESC, COALESCE(timeMinutes, 0) DESC, id DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<ExpenseEntity>>

    @Query(
        """
        SELECT * FROM expenses
        WHERE (:query = '' OR title LIKE '%' || :query || '%' OR note LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%')
          AND (:category IS NULL OR category = :category)
          AND (:paymentMethod IS NULL OR paymentMethod = :paymentMethod)
          AND (:startDate IS NULL OR dateEpochDay >= :startDate)
          AND (:endDate IS NULL OR dateEpochDay <= :endDate)
        """
    )
    fun observeFiltered(
        query: String,
        category: String?,
        paymentMethod: String?,
        startDate: Long?,
        endDate: Long?
    ): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): ExpenseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(expense: ExpenseEntity): Long

    @Update
    suspend fun update(expense: ExpenseEntity)

    @Delete
    suspend fun delete(expense: ExpenseEntity)

    @Query("DELETE FROM expenses")
    suspend fun clearAll()
}
