package com.tropico.moneyflow.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amountMinor: Long,
    val title: String,
    val category: String,
    val dateEpochDay: Long,
    val timeMinutes: Int?,
    val note: String?,
    val paymentMethod: String,
    val createdAtEpochMillis: Long
)
