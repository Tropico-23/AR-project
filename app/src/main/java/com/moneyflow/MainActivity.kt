package com.moneyflow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.moneyflow.ui.navigation.MoneyFlowApp
import com.moneyflow.ui.theme.MoneyFlowTheme
import com.moneyflow.viewmodel.SettingsViewModel
import com.moneyflow.viewmodel.ViewModelFactory

class MainActivity : ComponentActivity() {
    private val settingsViewModel: SettingsViewModel by viewModels {
        val app = application as MoneyFlowApplication
        ViewModelFactory(app.appContainer.repository, app.appContainer.preferencesRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MoneyFlowTheme(settingsViewModel.appearanceState.value.themeMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MoneyFlowApp()
                }
            }
        }
    }
}
