package com.example.dofcalculator.settings

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class InMemoryAppSettingsRepository(
    initialSettings: AppSettings = AppSettings(),
) : AppSettingsRepository {
    private val state = MutableStateFlow(initialSettings)

    override val settings = state.asStateFlow()

    override suspend fun saveDiffractionSettings(settings: DiffractionSettings) {
        state.value = state.value.copy(diffraction = settings)
    }

    override suspend fun setMacroWarningsEnabled(enabled: Boolean) {
        state.value = state.value.copy(macroWarningsEnabled = enabled)
    }

    override suspend fun setThemeMode(themeMode: ThemeMode) {
        state.value = state.value.copy(themeMode = themeMode)
    }

    override suspend fun setLanguage(language: AppLanguage) {
        state.value = state.value.copy(language = language)
    }

    override suspend fun setApertureIncrements(increments: ApertureIncrements) {
        state.value = state.value.copy(apertureIncrements = increments)
    }

    override suspend fun setDistanceDisplay(unit: DistanceUnit, rounding: DistanceRounding) {
        state.value = state.value.copy(distanceUnit = unit, distanceRounding = rounding)
    }

    override suspend fun saveSharpnessSettings(settings: com.example.dofcalculator.model.SharpnessSettings) {
        state.value = state.value.copy(sharpness = settings, sharpnessMigrated = true)
    }

    override suspend fun saveCalculatorSettings(
        settings: CalculatorSettings,
    ) {
        state.value = state.value.copy(calculator = settings)
    }
}
