package com.tropico.moneyflow.model

import java.time.DayOfWeek

enum class ExpenseCategory(val label: String, val emoji: String) {
    Food("Food", "🍔"),
    Coffee("Coffee", "☕"),
    Transport("Transport", "🚕"),
    Subscriptions("Subscriptions", "🔁"),
    Shopping("Shopping", "🛍️"),
    Bills("Bills", "🧾"),
    Education("Education", "📚"),
    Health("Health", "💊"),
    Entertainment("Entertainment", "🎮"),
    Other("Other", "✨");

    companion object {
        fun fromValue(value: String): ExpenseCategory = entries.firstOrNull { it.name == value } ?: Other
    }
}

enum class PaymentMethod(val label: String) {
    Cash("Cash"),
    BankCard("Bank card"),
    BankTransfer("Bank transfer"),
    Other("Other");

    companion object {
        fun fromValue(value: String): PaymentMethod = entries.firstOrNull { it.name == value } ?: Cash
    }
}

enum class AppCurrency(val code: String, val symbol: String) {
    TND("TND", "TND"),
    EUR("EUR", "€"),
    USD("USD", "$") ,
    GBP("GBP", "£");

    companion object {
        fun fromValue(value: String): AppCurrency = entries.firstOrNull { it.name == value } ?: TND
    }
}

enum class ThemeMode { System, Light, Dark }

enum class WeekStart(val day: DayOfWeek, val label: String) {
    Monday(DayOfWeek.MONDAY, "Monday"),
    Sunday(DayOfWeek.SUNDAY, "Sunday")
}

enum class SortOrder(val label: String) {
    Newest("Newest"),
    Oldest("Oldest"),
    Highest("Highest amount"),
    Lowest("Lowest amount")
}

enum class TrendRange(val label: String) {
    Days7("7 days"),
    Days30("30 days"),
    Months6("6 months"),
    Year1("1 year")
}
