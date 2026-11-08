package com.example.dofcalculator.settings

import com.example.dofcalculator.model.FrameFormat
import com.example.dofcalculator.model.SharpnessSettings
import kotlinx.coroutines.flow.Flow

enum class ThemeMode {
    System,
    Light,
    Dark,
}

enum class AppLanguage {
    System,
    English,
    Russian,
}

enum class CalculatorMode {
    DepthOfField,
    Hyperfocal,
}

data class CalculatorSettings(
    val mode: CalculatorMode = CalculatorMode.DepthOfField,
    val frameFormat: FrameFormat = FrameFormat.FullFrame35Mm,
    val circleOfConfusionText: String = "0,030",
    val focalLengthText: String = "50",
    val apertureText: String = "8",
    val focusDistanceText: String = "5",
    val focusAtInfinity: Boolean = false,
)

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.System,
    val language: AppLanguage = AppLanguage.System,
    val calculator: CalculatorSettings = CalculatorSettings(),
    val distanceUnit: DistanceUnit = DistanceUnit.Meters,
    val distanceRounding: DistanceRounding = DistanceRounding.Three,
    val apertureIncrements: ApertureIncrements = ApertureIncrements.FullStop,
    val sharpness: SharpnessSettings = SharpnessSettings(),
    val diffraction: DiffractionSettings = DiffractionSettings(),
    val sharpnessMigrated: Boolean = false,
    // Archived raw calculator CoC, kept for explicit import without replacing an active criterion.
    val legacyCoCForMigration: String? = null,
)

interface AppSettingsRepository {
    val settings: Flow<AppSettings>

    suspend fun setThemeMode(themeMode: ThemeMode)

    suspend fun setLanguage(language: AppLanguage)

    suspend fun setDistanceDisplay(unit: DistanceUnit, rounding: DistanceRounding)

    suspend fun saveCalculatorSettings(settings: CalculatorSettings)

    suspend fun saveSharpnessSettings(settings: SharpnessSettings)

    suspend fun setApertureIncrements(increments: ApertureIncrements)

    suspend fun saveDiffractionSettings(settings: DiffractionSettings)
}
