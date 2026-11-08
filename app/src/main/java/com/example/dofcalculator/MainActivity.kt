package com.example.dofcalculator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.dofcalculator.settings.AppSettings
import com.example.dofcalculator.settings.DataStoreAppSettingsRepository
import com.example.dofcalculator.settings.appSettingsDataStore
import com.example.dofcalculator.ui.AppLocale
import com.example.dofcalculator.ui.calculator.DofCalculatorRoute
import com.example.dofcalculator.ui.theme.DOFCalculatorTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appSettingsRepository = DataStoreAppSettingsRepository(
            applicationContext.appSettingsDataStore,
        )
        setContent {
            val settings by appSettingsRepository.settings.collectAsState(
                initial = AppSettings(),
            )
            AppLocale(settings.language) {
                DOFCalculatorTheme(themeMode = settings.themeMode) {
                    DofCalculatorRoute(appSettingsRepository = appSettingsRepository)
                }
            }
        }
    }
}
