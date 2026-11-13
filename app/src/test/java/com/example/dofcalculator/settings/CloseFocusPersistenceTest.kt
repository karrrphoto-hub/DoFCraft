package com.example.dofcalculator.settings

import androidx.datastore.preferences.core.*
import com.example.dofcalculator.model.SharpnessCriterionMode
import com.example.dofcalculator.model.SharpnessSettings
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test

class CloseFocusPersistenceTest {
    @Test fun `independent close focus flag survives disk restart without altering optics or legacy records`() = runBlocking {
        val dir = Files.createTempDirectory("close-focus-settings").toFile()
        val file = File(dir, "settings.preferences_pb")
        var job = SupervisorJob()
        fun store() = PreferenceDataStoreFactory.create(scope = CoroutineScope(job + Dispatchers.IO),
            produceFile = { file })
        try {
            var data = store()
            data.edit {
                it[stringPreferencesKey("macro_photography_warning")] = "false"
                it[stringPreferencesKey("manual_magnification")] = "2.3456789"
                it[stringPreferencesKey("manual_confirmed")] = "true"
                it[stringPreferencesKey("unrelated")] = "keep"
            }
            var repo = DataStoreAppSettingsRepository(data)
            assertTrue(repo.settings.first().closeFocusWarningsEnabled)
            val optical = CalculatorSettings(focalLengthText = "100", apertureText = "16",
                focusDistanceText = "0.6", focusAtInfinity = true)
            val sharpness = SharpnessSettings(mode = SharpnessCriterionMode.CUSTOM, customCoCmm = .012345)
            repo.saveCalculatorSettings(optical)
            repo.saveSharpnessSettings(sharpness)
            repo.setLanguage(AppLanguage.Russian)
            repo.setCloseFocusWarningsEnabled(false)
            assertTrue(repo.settings.first().diffraction.warningsEnabled)
            job.cancelAndJoin()
            job = SupervisorJob()
            data = store()
            repo = DataStoreAppSettingsRepository(data)
            val restored = repo.settings.first()
            assertFalse(restored.closeFocusWarningsEnabled)
            assertEquals(optical, restored.calculator)
            assertEquals(sharpness, restored.sharpness)
            assertEquals(AppLanguage.Russian, restored.language)
            assertTrue(restored.diffraction.warningsEnabled)
            repo.saveDiffractionSettings(DiffractionSettings(false))
            assertFalse(repo.settings.first().closeFocusWarningsEnabled)
            repo.setCloseFocusWarningsEnabled(true)
            assertFalse(repo.settings.first().diffraction.warningsEnabled)
            val raw = data.data.first()
            assertEquals("false", raw[stringPreferencesKey("macro_photography_warning")])
            assertEquals("2.3456789", raw[stringPreferencesKey("manual_magnification")])
            assertEquals("true", raw[stringPreferencesKey("manual_confirmed")])
            assertEquals("keep", raw[stringPreferencesKey("unrelated")])
            assertEquals(optical, repo.settings.first().calculator)
            assertEquals(sharpness, repo.settings.first().sharpness)
        } finally { job.cancelAndJoin(); dir.deleteRecursively() }
    }

    @Test fun `missing or corrupt new flag defaults on without reusing the old macro preference`() = runBlocking {
        val dir = Files.createTempDirectory("close-focus-defaults").toFile()
        val job = SupervisorJob()
        try {
            val data = PreferenceDataStoreFactory.create(scope = CoroutineScope(job + Dispatchers.IO),
                produceFile = { File(dir, "settings.preferences_pb") })
            val repo = DataStoreAppSettingsRepository(data)
            for (bad in listOf("", "broken", "0", "NaN")) {
                data.edit {
                    it[stringPreferencesKey("close_focus_warning")] = bad
                    it[stringPreferencesKey("macro_photography_warning")] = "false"
                }
                assertTrue(repo.settings.first().closeFocusWarningsEnabled)
            }
        } finally { job.cancelAndJoin(); dir.deleteRecursively() }
    }
}
