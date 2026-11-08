package com.example.dofcalculator.settings

import androidx.datastore.preferences.core.*
import com.example.dofcalculator.model.*
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test

class SharpnessPersistenceTest {
    @Test fun `all sharpness parameters and other preferences survive a real DataStore restart`() = runBlocking {
        val dir = Files.createTempDirectory("sharpness-restart").toFile()
        val file = File(dir, "settings.preferences_pb")
        var job = SupervisorJob()
        fun store() = PreferenceDataStoreFactory.create(
            scope = CoroutineScope(job + Dispatchers.IO), produceFile = { file })
        val expected = SharpnessSettings(
            mode = SharpnessCriterionMode.PIXEL_BASED, megapixels = 45.7, pixelBlur = 2.0,
            printWidthCm = 45.5, printHeightCm = 30.2, printPpi = 240.0,
            printDpi = 1200.0, printViewingCm = 75.5, printAngleMinutes = 1.5,
            printSize = PrintSizePreset.CUSTOM,
            printPlacement = PrintPlacement.FIT, customCoCmm = .0087654321098765,
            screenDevice = ScreenDevice.TABLET, screenDiagonalInches = 11.25,
            screenWidthPx = 2560.0, screenHeightPx = 1600.0, screenViewingCm = 55.5,
            screenAngleMinutes = 1.25, screenCriterion = ScreenCriterion.DISPLAY_PIXELS,
            screenBlurPx = 2.5, screenPlacement = PrintPlacement.FILL)
        try {
            var repo = DataStoreAppSettingsRepository(store())
            repo.setThemeMode(ThemeMode.Dark)
            repo.setLanguage(AppLanguage.Russian)
            repo.setDistanceDisplay(DistanceUnit.Feet, DistanceRounding.None)
            repo.saveCalculatorSettings(CalculatorSettings(focusDistanceText = "12.75"))
            repo.saveSharpnessSettings(expected)
            job.cancelAndJoin()
            job = SupervisorJob()
            repo = DataStoreAppSettingsRepository(store())
            val restored = repo.settings.first()
            assertEquals(expected, restored.sharpness)
            assertTrue(restored.sharpnessMigrated)
            assertEquals(ThemeMode.Dark, restored.themeMode)
            assertEquals(AppLanguage.Russian, restored.language)
            assertEquals(DistanceUnit.Feet, restored.distanceUnit)
            assertEquals("12.75", restored.calculator.focusDistanceText)
            for (mode in SharpnessCriterionMode.entries) {
                repo.saveSharpnessSettings(expected.copy(mode = mode))
                assertEquals(expected.copy(mode = mode), repo.settings.first().sharpness)
            }
        } finally {
            job.cancelAndJoin()
            dir.deleteRecursively()
        }
    }

    @Test fun `legacy print settings remain archived while the new model and notice survive restart`() = runBlocking {
        val dir = Files.createTempDirectory("print-model-migration").toFile()
        val file = File(dir, "settings.preferences_pb")
        var job = SupervisorJob()
        fun store() = PreferenceDataStoreFactory.create(
            scope = CoroutineScope(job + Dispatchers.IO), produceFile = { file })
        val legacy = "PRINT_BASED\t45.7\t2\t45\t30\t240\t9.5\tFILL\t0.00876"
        try {
            var data = store()
            data.edit {
                it[stringPreferencesKey("sharpness_settings_v1")] = legacy
                it[stringPreferencesKey("language")] = "Russian"
            }
            var repo = DataStoreAppSettingsRepository(data)
            val migrated = repo.settings.first().sharpness
            assertEquals(SharpnessCriterionMode.PRINT_BASED, migrated.mode)
            assertEquals(45.0, migrated.printWidthCm, 0.0)
            assertEquals(30.0, migrated.printHeightCm, 0.0)
            assertEquals(PrintPlacement.FILL, migrated.printPlacement)
            assertEquals(240.0, migrated.printPpi!!, 0.0)
            assertEquals(2.0, migrated.printAngleMinutes, 0.0)
            assertEquals(50.0, migrated.printViewingCm, 0.0)
            assertTrue(migrated.printModelNoticePending)
            assertEquals(45.7, migrated.megapixels, 0.0)
            assertEquals(.00876, migrated.customCoCmm, 0.0)
            repo.saveSharpnessSettings(migrated.copy(printModelNoticePending = false,
                screenCriterion = ScreenCriterion.DISPLAY_PIXELS, screenBlurPx = 2.75))
            job.cancelAndJoin()
            job = SupervisorJob()
            data = store()
            repo = DataStoreAppSettingsRepository(data)
            val restored = repo.settings.first()
            assertFalse(restored.sharpness.printModelNoticePending)
            assertEquals(2.75, restored.sharpness.screenBlurPx, 0.0)
            assertEquals(legacy, data.data.first()[stringPreferencesKey("sharpness_settings_v1")])
            assertEquals(AppLanguage.Russian, restored.language)
        } finally { job.cancelAndJoin(); dir.deleteRecursively() }
    }

    @Test fun `settings codec safely handles corrupted records`() {
        for (payload in listOf(null, "", "bad", "CUSTOM\tNaN", "STANDARD\tInfinity")) {
            assertEquals(SharpnessSettings(), SharpnessSettingsCodec.decode(payload))
        }
    }
}
