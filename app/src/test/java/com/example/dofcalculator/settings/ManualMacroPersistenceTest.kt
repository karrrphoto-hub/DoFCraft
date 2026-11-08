package com.example.dofcalculator.settings

import androidx.datastore.preferences.core.*
import com.example.dofcalculator.data.InMemoryPresetRepository
import com.example.dofcalculator.model.*
import com.example.dofcalculator.ui.calculator.DofCalculatorViewModel
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test

class ManualMacroPersistenceTest {
    @Test fun `confirmed pair and unrelated preferences survive real DataStore restart`() = runBlocking {
        val dir = Files.createTempDirectory("manual-pair").toFile()
        val file = File(dir, "settings.preferences_pb")
        var job = SupervisorJob()
        fun store() = PreferenceDataStoreFactory.create(
            scope = CoroutineScope(job + Dispatchers.IO), produceFile = { file })
        val pair = DiffractionSettings(false, MagnificationMode.MANUAL, .5, .6, 100.0, true)
        try {
            var data = store()
            data.edit { it[stringPreferencesKey("unrelated")] = "keep" }
            var repo = DataStoreAppSettingsRepository(data)
            repo.setLanguage(AppLanguage.Russian)
            repo.saveSharpnessSettings(SharpnessSettings(mode = SharpnessCriterionMode.VIDEO_BASED))
            repo.saveCalculatorSettings(CalculatorSettings(focalLengthText = "100", focusDistanceText = "0.6"))
            repo.saveDiffractionSettings(pair)
            job.cancelAndJoin()
            job = SupervisorJob()
            data = store()
            repo = DataStoreAppSettingsRepository(data)
            val restored = repo.settings.first()
            assertEquals(pair, restored.diffraction)
            assertEquals(AppLanguage.Russian, restored.language)
            assertEquals(SharpnessCriterionMode.VIDEO_BASED, restored.sharpness.mode)
            assertEquals("keep", data.data.first()[stringPreferencesKey("unrelated")])
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
            try {
                val vm = DofCalculatorViewModel(InMemoryPresetRepository(),
                    InMemoryAppSettingsRepository(restored), scope)
                assertEquals(ManualMagnificationStatus.VALID, vm.uiState.manualMagnificationAssessment.status)
                assertEquals(.5, vm.uiState.magnificationEstimate.value!!, 0.0)
            } finally { scope.cancel() }
            repo.saveCalculatorSettings(restored.calculator.copy(focusDistanceText = "2"))
            val changed = repo.settings.first()
            val scope2 = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
            try {
                val vm = DofCalculatorViewModel(InMemoryPresetRepository(),
                    InMemoryAppSettingsRepository(changed), scope2)
                assertTrue(vm.uiState.manualNeedsConfirmation)
                assertEquals(MagnificationMode.AUTO, vm.uiState.magnificationEstimate.source)
                assertEquals(pair.manualMagnification, vm.uiState.diffractionSettings.manualMagnification, 0.0)
                assertFalse(vm.uiState.diffractionSettings.manualConfirmed)
            } finally { scope2.cancel() }
        } finally { job.cancelAndJoin(); dir.deleteRecursively() }
    }

    @Test fun `legacy standalone and scale-driven magnifications are retained but unconfirmed`() = runBlocking {
        for (scaleDriven in listOf(false, true)) {
            val dir = Files.createTempDirectory("legacy-manual").toFile()
            val job = SupervisorJob()
            try {
                val data = PreferenceDataStoreFactory.create(scope = CoroutineScope(job + Dispatchers.IO),
                    produceFile = { File(dir, "settings.preferences_pb") })
                data.edit {
                    it[stringPreferencesKey("manual_magnification")] = "0.5"
                    it[stringPreferencesKey("magnification_mode")] = "MANUAL"
                    if (scaleDriven) it[stringPreferencesKey("focus_magnification")] = "2.0"
                    it[stringPreferencesKey("last_focus_distance")] = "0.45"
                    it[stringPreferencesKey("last_focal_length")] = "100"
                }
                val repo = DataStoreAppSettingsRepository(data)
                val settings = repo.settings.first()
                val expected = if (scaleDriven) 2.0 else .5
                assertEquals(expected, settings.diffraction.manualMagnification, 0.0)
                assertNull(settings.diffraction.manualDistanceMeters)
                assertFalse(settings.diffraction.manualConfirmed)
                val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
                try {
                    val vm = DofCalculatorViewModel(InMemoryPresetRepository(),
                        InMemoryAppSettingsRepository(settings), scope)
                    assertTrue(vm.uiState.manualNeedsConfirmation)
                    assertEquals(expected, vm.uiState.diffractionSettings.manualMagnification, 0.0)
                    assertEquals(MagnificationMode.AUTO, vm.uiState.magnificationEstimate.source)
                } finally { scope.cancel() }
                repo.saveDiffractionSettings(settings.diffraction.copy(magnificationMode = MagnificationMode.AUTO))
                repo.saveCalculatorSettings(settings.calculator.copy(focusMagnification = null))
                assertEquals(MagnificationMode.AUTO, repo.settings.first().diffraction.magnificationMode)
                assertEquals(expected, repo.settings.first().diffraction.manualMagnification, 0.0)
                assertEquals("0.5", data.data.first()[stringPreferencesKey("legacy_manual_magnification")])
                if (scaleDriven) assertEquals("2.0",
                    data.data.first()[stringPreferencesKey("focus_magnification")]) // archive retained
            } finally { job.cancelAndJoin(); dir.deleteRecursively() }
        }
    }

    @Test fun `corrupt new binding cannot activate stale manual scale`() = runBlocking {
        val dir = Files.createTempDirectory("bad-manual-pair").toFile()
        val job = SupervisorJob()
        try {
            val data = PreferenceDataStoreFactory.create(scope = CoroutineScope(job + Dispatchers.IO),
                produceFile = { File(dir, "settings.preferences_pb") })
            data.edit {
                it[stringPreferencesKey("manual_pair_version")] = "1"
                it[stringPreferencesKey("magnification_mode")] = "MANUAL"
                it[stringPreferencesKey("manual_magnification")] = "0.5"
                it[stringPreferencesKey("manual_distance_m")] = "Infinity"
                it[stringPreferencesKey("manual_focal_mm")] = "-1"
                it[stringPreferencesKey("manual_confirmed")] = "true"
            }
            val restored = DataStoreAppSettingsRepository(data).settings.first()
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
            try {
                val vm = DofCalculatorViewModel(InMemoryPresetRepository(),
                    InMemoryAppSettingsRepository(restored), scope)
                assertTrue(vm.uiState.manualNeedsConfirmation)
                assertFalse(vm.uiState.diffractionSettings.manualConfirmed)
                assertEquals(MagnificationMode.AUTO, vm.uiState.magnificationEstimate.source)
                assertEquals(.5, vm.uiState.diffractionSettings.manualMagnification, 0.0)
            } finally { scope.cancel() }
        } finally { job.cancelAndJoin(); dir.deleteRecursively() }
    }
}
