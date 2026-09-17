package com.moneyflow.util

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter

object DateUtils {
    val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
    val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    fun monthKey(date: LocalDate): String = YearMonth.from(date).toString()

    fun nowDateTime(): LocalDateTime = LocalDateTime.now()

    fun parseTime(text: String): LocalTime? = runCatching { LocalTime.parse(text, timeFormatter) }.getOrNull()
}
