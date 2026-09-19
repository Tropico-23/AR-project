package com.tropico.moneyflow.util

import com.tropico.moneyflow.model.AppCurrency
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat

object MoneyFormat {
    fun formatMinor(minor: Long, currency: AppCurrency): String {
        val major = BigDecimal(minor).divide(BigDecimal(100), 2, RoundingMode.HALF_UP)
        val formatter = DecimalFormat("#,##0.00")
        return "${formatter.format(major)} ${currency.code}"
    }

    fun parseMajorToMinor(value: String): Long? {
        val normalized = value.trim().replace(',', '.')
        if (normalized.isEmpty()) return null
        return runCatching {
            BigDecimal(normalized).multiply(BigDecimal(100)).setScale(0, RoundingMode.HALF_UP).longValueExact()
        }.getOrNull()
    }
}
