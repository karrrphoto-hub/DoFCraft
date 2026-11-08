package com.example.dofcalculator.settings

import androidx.datastore.preferences.core.*
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test

class MacroWarningPersistenceTest {
    @Test fun `macro warning defaults on and its independent key survives DataStore restart`() = runBlocking {
        val dir = Files.createTempDirectory("macro-warning-preference").toFile()
        val file = File(dir, "settings.preferences_pb")
        var job = SupervisorJob()
        fun store() = PreferenceDataStoreFactory.create(
            scope = CoroutineScope(job + Dispatchers.IO), produceFile = { file })
        try {
            var data = store()
            var repo = DataStoreAppSettingsRepository(data)
            assertTrue(repo.settings.first().macroWarningsEnabled)
            val manual = DiffractionSettings(warningsEnabled = false,
                magnificationMode = com.example.dofcalculator.model.MagnificationMode.MANUAL,
                manualMagnification = 1.0, manualDistanceMeters = .3,
                manualFocalLengthMm = 100.0, manualConfirmed = true)
            repo.saveDiffractionSettings(manual)
            repo.saveCalculatorSettings(CalculatorSettings(focalLengthText = "100", focusDistanceText = "0.3"))
            data.edit { it[stringPreferencesKey("legacy_preserved")] = "keep" }
            val before = data.data.first().asMap()
            repo.setMacroWarningsEnabled(false)
            assertEquals(before, data.data.first().asMap().filterKeys { it.name != "macro_photography_warning" })
            job.cancelAndJoin()
            job = SupervisorJob()
            data = store()
            repo = DataStoreAppSettingsRepository(data)
            assertFalse(repo.settings.first().macroWarningsEnabled)
            assertEquals(manual, repo.settings.first().diffraction)
            assertEquals("0.3", repo.settings.first().calculator.focusDistanceText)
            assertEquals("keep", data.data.first()[stringPreferencesKey("legacy_preserved")])
            repo.setMacroWarningsEnabled(true)
            assertTrue(repo.settings.first().macroWarningsEnabled)
        } finally { job.cancelAndJoin(); dir.deleteRecursively() }
    }
}
