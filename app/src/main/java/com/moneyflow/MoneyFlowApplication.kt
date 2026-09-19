package com.tropico.moneyflow

import android.app.Application
import androidx.room.Room
import com.tropico.moneyflow.data.database.MoneyFlowDatabase
import com.tropico.moneyflow.data.preferences.PreferencesRepository
import com.tropico.moneyflow.data.repository.ExpenseRepository

class MoneyFlowApplication : Application() {
    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            appContainer.repository.migrateLegacySharedPreferences(this@MoneyFlowApplication)
        }
    }
}

class AppContainer(application: Application) {
    private val db: MoneyFlowDatabase = Room.databaseBuilder(
        application,
        MoneyFlowDatabase::class.java,
        "moneyflow.db"
    ).build()

    val repository: ExpenseRepository = ExpenseRepository(db.expenseDao(), db.budgetDao())
    val preferencesRepository: PreferencesRepository = PreferencesRepository(application)
}
