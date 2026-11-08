package com.example.dofcalculator.settings

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.example.dofcalculator.model.FrameFormat
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class DataStoreAppSettingsRepositoryTest {
    @Test
    fun `theme calculator mode and last inputs survive recreation`() =
        runBlocking {
            val directory = Files.createTempDirectory("dof-settings").toFile()
            val dataFile = File(directory, "app-settings.preferences_pb")
            val firstJob = SupervisorJob()
            val firstStore = PreferenceDataStoreFactory.create(
                scope = CoroutineScope(firstJob + Dispatchers.IO),
                produceFile = { dataFile },
            )
            firstStore.edit {
                it[stringPreferencesKey("distance_control_minimum")] = "12"
            }
            val firstRepository = DataStoreAppSettingsRepository(firstStore)
            val expectedCalculator = CalculatorSettings(
                mode = CalculatorMode.Hyperfocal,
                frameFormat = FrameFormat.MediumFormat6x6,
                circleOfConfusionText = "0,052",
                focalLengthText = "80",
                apertureText = "3,5",
                focusDistanceText = "6,25",
                focusAtInfinity = true,
            )

            try {
                firstRepository.setDistanceDisplay(DistanceUnit.Feet, DistanceRounding.None)
                firstRepository.setApertureIncrements(ApertureIncrements.ThirdStop)
                firstRepository.setThemeMode(ThemeMode.Dark)
                firstRepository.setLanguage(AppLanguage.Russian)
                firstRepository.saveCalculatorSettings(expectedCalculator)
                assertEquals(
                    AppSettings(
                        themeMode = ThemeMode.Dark,
                        language = AppLanguage.Russian,
                        calculator = expectedCalculator,
                        legacyCoCForMigration = expectedCalculator.circleOfConfusionText,
                        distanceUnit = DistanceUnit.Feet,
                        distanceRounding = DistanceRounding.None,
                        apertureIncrements = ApertureIncrements.ThirdStop,
                    ),
                    firstRepository.settings.first {
                        it.themeMode == ThemeMode.Dark &&
                            it.language == AppLanguage.Russian &&
                            it.calculator == expectedCalculator
                    },
                )

                firstJob.cancelAndJoin()

                val secondJob = SupervisorJob()
                val secondRepository = DataStoreAppSettingsRepository(
                    PreferenceDataStoreFactory.create(
                        scope = CoroutineScope(secondJob + Dispatchers.IO),
                        produceFile = { dataFile },
                    ),
                )
                try {
                    assertEquals(
                        AppSettings(
                        themeMode = ThemeMode.Dark,
                        language = AppLanguage.Russian,
                        calculator = expectedCalculator,
                        legacyCoCForMigration = expectedCalculator.circleOfConfusionText,
                        distanceUnit = DistanceUnit.Feet,
                        distanceRounding = DistanceRounding.None,
                        apertureIncrements = ApertureIncrements.ThirdStop,
                    ),
                        secondRepository.settings.first(),
                    )

                    secondRepository.setThemeMode(ThemeMode.Light)
                    secondRepository.setLanguage(AppLanguage.English)
                    secondRepository.saveCalculatorSettings(
                        expectedCalculator.copy(
                            mode = CalculatorMode.DepthOfField,
                            focusDistanceText = "7",
                        ),
                    )
                    val edited = secondRepository.settings.first {
                        it.themeMode == ThemeMode.Light &&
                            it.calculator.mode ==
                            CalculatorMode.DepthOfField
                    }
                    assertEquals(AppLanguage.English, edited.language)
                    assertEquals("7", edited.calculator.focusDistanceText)
                } finally {
                    secondJob.cancelAndJoin()
                }
            } finally {
                if (firstJob.isActive) {
                    firstJob.cancelAndJoin()
                }
                directory.deleteRecursively()
            }
        }
}
