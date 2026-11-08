package com.example.dofcalculator.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.dofcalculator.model.FrameFormat
import java.io.IOException
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class DataStoreAppSettingsRepository(
    private val dataStore: DataStore<Preferences>,
) : AppSettingsRepository {
    override val settings = dataStore.data
        .catch { error ->
            if (error is IOException) {
                emit(emptyPreferences())
            } else {
                throw error
            }
        }
        .map { preferences ->
            val defaults = AppSettings()
            AppSettings(
                themeMode = preferences[THEME_MODE]
                    .toEnumOrNull<ThemeMode>()
                    ?: defaults.themeMode,
                language = preferences[LANGUAGE]
                    .toEnumOrNull<AppLanguage>()
                    ?: defaults.language,
                apertureIncrements = preferences[APERTURE_INCREMENTS].toEnumOrNull<ApertureIncrements>()
                    ?: defaults.apertureIncrements,
                distanceUnit = preferences[DISTANCE_UNIT].toEnumOrNull<DistanceUnit>() ?: defaults.distanceUnit,
                distanceRounding = preferences[DISTANCE_ROUNDING].toEnumOrNull<DistanceRounding>() ?: defaults.distanceRounding,
                diffraction = DiffractionSettings(
                    warningsEnabled = preferences[DIFFRACTION_WARNINGS]?.toBooleanStrictOrNull() ?: true,
                ),
                sharpness = SharpnessSettingsCodec.decode(preferences[SHARPNESS_V2] ?: preferences[SHARPNESS]),
                sharpnessMigrated = preferences[SHARPNESS_V2] != null || preferences[SHARPNESS] != null,
                legacyCoCForMigration = preferences[LEGACY_CALCULATOR_COC]
                    ?: preferences[CIRCLE_OF_CONFUSION]
                        .takeIf { preferences[SHARPNESS] == null && preferences[SHARPNESS_V2] == null },
                calculator = CalculatorSettings(
                    mode = preferences[CALCULATOR_MODE]
                        .toEnumOrNull<CalculatorMode>()
                        ?: defaults.calculator.mode,
                    frameFormat = preferences[FRAME_FORMAT]
                        .toEnumOrNull<FrameFormat>()
                        ?: defaults.calculator.frameFormat,
                    circleOfConfusionText =
                        preferences[CIRCLE_OF_CONFUSION]
                            ?: defaults.calculator.circleOfConfusionText,
                    focalLengthText = preferences[FOCAL_LENGTH]
                        ?: defaults.calculator.focalLengthText,
                    apertureText = preferences[APERTURE]
                        ?: defaults.calculator.apertureText,
                    focusAtInfinity = preferences[FOCUS_INFINITY] == "true",
                    focusDistanceText = preferences[FOCUS_DISTANCE]
                        ?: defaults.calculator.focusDistanceText,
                ),
            )
        }

    override suspend fun saveDiffractionSettings(settings: DiffractionSettings) {
        dataStore.edit {
            it[DIFFRACTION_WARNINGS] = settings.warningsEnabled.toString()
            // Do not migrate, remove or overwrite any legacy magnification preferences.
            // Preferences.edit changes only the ordinary warning flag above.
        }
    }

    override suspend fun setThemeMode(themeMode: ThemeMode) {
        dataStore.edit { preferences ->
            preferences[THEME_MODE] = themeMode.name
        }
    }

    override suspend fun setLanguage(language: AppLanguage) {
        dataStore.edit { preferences ->
            preferences[LANGUAGE] = language.name
        }
    }

    override suspend fun setApertureIncrements(increments: ApertureIncrements) {
        dataStore.edit { it[APERTURE_INCREMENTS] = increments.name }
    }

    override suspend fun setDistanceDisplay(unit: DistanceUnit, rounding: DistanceRounding) {
        dataStore.edit {
            it[DISTANCE_UNIT] = unit.name
            it[DISTANCE_ROUNDING] = rounding.name
        }
    }

    override suspend fun saveSharpnessSettings(settings: com.example.dofcalculator.model.SharpnessSettings) {
        dataStore.edit {
            archiveLegacyCalculatorCoC(it)
            it[SHARPNESS_V2] = SharpnessSettingsCodec.encode(settings)
        }
    }

    override suspend fun saveCalculatorSettings(
        settings: CalculatorSettings,
    ) {
        dataStore.edit { preferences ->
            archiveLegacyCalculatorCoC(preferences)
            preferences[CALCULATOR_MODE] = settings.mode.name
            preferences[FRAME_FORMAT] = settings.frameFormat.name
            preferences[CIRCLE_OF_CONFUSION] =
                settings.circleOfConfusionText
            preferences[FOCAL_LENGTH] = settings.focalLengthText
            preferences[APERTURE] = settings.apertureText
            preferences[FOCUS_DISTANCE] = settings.focusDistanceText
            preferences[FOCUS_INFINITY] = settings.focusAtInfinity.toString()
            // Old scale-driven distance is a read-only migration archive.
        }
    }

    private fun archiveLegacyCalculatorCoC(preferences: MutablePreferences) {
        if (preferences[SHARPNESS] == null && preferences[SHARPNESS_V2] == null &&
            preferences[LEGACY_CALCULATOR_COC] == null) {
            preferences[CIRCLE_OF_CONFUSION]?.let { preferences[LEGACY_CALCULATOR_COC] = it }
        }
    }

    private inline fun <reified T : Enum<T>> String?.toEnumOrNull(): T? =
        this?.let { storedName ->
            enumValues<T>().firstOrNull { it.name == storedName }
        }

    // Legacy key declarations remain for documentation and data compatibility.
    // The old values are deliberately unused and survive every ordinary preferences edit.
    @Suppress("unused")
    private companion object {
        val DIFFRACTION_WARNINGS = stringPreferencesKey("diffraction_warnings")
        val MACRO_WARNINGS = stringPreferencesKey("macro_photography_warning")
        val MAGNIFICATION_MODE = stringPreferencesKey("magnification_mode")
        val MANUAL_MAGNIFICATION = stringPreferencesKey("manual_magnification")
        val LEGACY_MANUAL_ARCHIVE = stringPreferencesKey("legacy_manual_magnification")
        val MANUAL_PAIR_VERSION = stringPreferencesKey("manual_pair_version")
        val MANUAL_DISTANCE = stringPreferencesKey("manual_distance_m")
        val MANUAL_FOCAL = stringPreferencesKey("manual_focal_mm")
        val MANUAL_CONFIRMED = stringPreferencesKey("manual_confirmed")
        val SHARPNESS_V2 = stringPreferencesKey("sharpness_settings_v2")
        val SHARPNESS = stringPreferencesKey("sharpness_settings_v1")
        val APERTURE_INCREMENTS = stringPreferencesKey("aperture_increments")
        val DISTANCE_UNIT = stringPreferencesKey("distance_unit")
        val DISTANCE_ROUNDING = stringPreferencesKey("distance_rounding")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val LANGUAGE = stringPreferencesKey("language")
        val CALCULATOR_MODE = stringPreferencesKey("calculator_mode")
        val FRAME_FORMAT = stringPreferencesKey("last_frame_format")
        val CIRCLE_OF_CONFUSION =
            stringPreferencesKey("last_circle_of_confusion")
        val LEGACY_CALCULATOR_COC = stringPreferencesKey("legacy_calculator_coc")
        val FOCAL_LENGTH = stringPreferencesKey("last_focal_length")
        val APERTURE = stringPreferencesKey("last_aperture")
        val FOCUS_MAGNIFICATION = stringPreferencesKey("focus_magnification")
        val FOCUS_INFINITY = stringPreferencesKey("focus_at_infinity")
        val FOCUS_DISTANCE = stringPreferencesKey("last_focus_distance")
    }
}
