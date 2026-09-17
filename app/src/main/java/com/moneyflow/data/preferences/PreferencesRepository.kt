package com.moneyflow.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.moneyflow.model.AppCurrency
import com.moneyflow.model.PaymentMethod
import com.moneyflow.model.ThemeMode
import com.moneyflow.model.WeekStart
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("moneyflow_prefs")

data class AppPreferences(
    val themeMode: ThemeMode = ThemeMode.System,
    val currency: AppCurrency = AppCurrency.TND,
    val defaultPaymentMethod: PaymentMethod = PaymentMethod.Cash,
    val weekStart: WeekStart = WeekStart.Monday
)

class PreferencesRepository(private val context: Context) {
    private object Keys {
        val themeMode = stringPreferencesKey("theme_mode")
        val currency = stringPreferencesKey("currency")
        val defaultPaymentMethod = stringPreferencesKey("default_payment_method")
        val weekStart = stringPreferencesKey("week_start")
    }

    val preferences: Flow<AppPreferences> = context.dataStore.data.map { pref ->
        AppPreferences(
            themeMode = pref.enumValue(Keys.themeMode, ThemeMode.System),
            currency = pref.enumValue(Keys.currency, AppCurrency.TND),
            defaultPaymentMethod = pref.enumValue(Keys.defaultPaymentMethod, PaymentMethod.Cash),
            weekStart = pref.enumValue(Keys.weekStart, WeekStart.Monday)
        )
    }

    suspend fun setTheme(themeMode: ThemeMode) = set(Keys.themeMode, themeMode.name)
    suspend fun setCurrency(currency: AppCurrency) = set(Keys.currency, currency.name)
    suspend fun setDefaultPaymentMethod(method: PaymentMethod) = set(Keys.defaultPaymentMethod, method.name)
    suspend fun setWeekStart(weekStart: WeekStart) = set(Keys.weekStart, weekStart.name)

    private suspend fun set(key: Preferences.Key<String>, value: String) {
        context.dataStore.edit { it[key] = value }
    }

    private inline fun <reified T : Enum<T>> Preferences.enumValue(
        key: Preferences.Key<String>,
        fallback: T
    ): T = this[key]?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: fallback
}
