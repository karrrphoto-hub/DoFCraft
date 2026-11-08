package com.example.dofcalculator.settings

import androidx.datastore.preferences.core.*
import com.example.dofcalculator.data.*
import com.example.dofcalculator.model.*
import com.example.dofcalculator.calculation.StandardCoCResolver
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test

class StandardCoCPersistenceTest {
    @Test fun `old calculator CoC is archived before either migration write and survives restart`() = runBlocking {
        for (calculatorFirst in listOf(true, false)) {
            val dir = Files.createTempDirectory("standard-legacy").toFile()
            val file = File(dir, "settings.preferences_pb")
            var job = SupervisorJob()
            fun store() = PreferenceDataStoreFactory.create(
                scope = CoroutineScope(job + Dispatchers.IO), produceFile = { file })
            try {
                var data = store()
                data.edit {
                    it[stringPreferencesKey("last_circle_of_confusion")] = "0,017"
                    it[stringPreferencesKey("unrelated")] = "keep"
                }
                var repo = DataStoreAppSettingsRepository(data)
                val sharpness = SharpnessSettings(customCoCmm = .00876)
                val calculator = CalculatorSettings(circleOfConfusionText = ".020392777756239697")
                if (calculatorFirst) {
                    repo.saveCalculatorSettings(calculator)
                    repo.saveSharpnessSettings(sharpness)
                } else {
                    repo.saveSharpnessSettings(sharpness)
                    repo.saveCalculatorSettings(calculator)
                }
                job.cancelAndJoin()
                job = SupervisorJob()
                data = store()
                repo = DataStoreAppSettingsRepository(data)
                val restored = repo.settings.first()
                assertEquals(sharpness, restored.sharpness)
                assertTrue(restored.sharpnessMigrated)
                assertEquals("0,017", restored.legacyCoCForMigration)
                assertEquals("0,017", data.data.first()[stringPreferencesKey("legacy_calculator_coc")])
                assertEquals("keep", data.data.first()[stringPreferencesKey("unrelated")])
            } finally { job.cancelAndJoin(); dir.deleteRecursively() }
        }
    }

    @Test fun `existing custom geometry ID selection and camera legacy archive remain intact`() = runBlocking {
        val dir = Files.createTempDirectory("standard-camera").toFile()
        val file = File(dir, "settings.preferences_pb")
        var job = SupervisorJob()
        fun store() = PreferenceDataStoreFactory.create(
            scope = CoroutineScope(job + Dispatchers.IO), produceFile = { file })
        val camera = CameraProfile("half-old", "Half-frame film", FrameFormat.Custom,
            customFormatName = "Half-frame", widthMm = 17.0, heightMm = 24.0)
        val raw = CameraPresetCodec.encode(camera)
        val legacy = "camera2\thalf-old\tHalf-frame\tCustom\t0.015\tfalse"
        val chosen = SharpnessSettings(customCoCmm = .00876)
        try {
            var data = store()
            data.edit {
                it[stringPreferencesKey("camera_ids_v2")] = camera.id
                it[stringPreferencesKey("camera_v3_half-old")] = raw
                it[stringPreferencesKey("camera_v2_half-old")] = legacy
                it[stringPreferencesKey("selected_camera_id")] = camera.id
            }
            var settings = DataStoreAppSettingsRepository(data)
            settings.saveSharpnessSettings(chosen)
            settings.saveCalculatorSettings(CalculatorSettings(focusAtInfinity = true))
            job.cancelAndJoin()
            job = SupervisorJob()
            data = store()
            settings = DataStoreAppSettingsRepository(data)
            val library = DataStorePresetRepository(data).library.first()
            val restored = library.cameras.first { it.id == camera.id }
            assertEquals(camera, restored)
            assertEquals(camera.id, library.selectedCameraId)
            assertEquals(.015, library.legacyCoCByCameraId[camera.id]!!, 0.0)
            assertEquals(chosen, settings.settings.first().sharpness)
            assertTrue(settings.settings.first().calculator.focusAtInfinity)
            assertEquals(raw, data.data.first()[stringPreferencesKey("camera_v3_half-old")])
            assertEquals(legacy, data.data.first()[stringPreferencesKey("camera_v2_half-old")])
            val resolved = StandardCoCResolver.resolve(restored)
            assertEquals(.020392777756239697, resolved.coCmm, 1e-15)
            assertEquals(StandardCoCSource.DIAGONAL_FALLBACK, resolved.source)
            assertEquals(BuiltInCameraProfiles.cameras, library.cameras.filter { it.isBuiltIn })
        } finally { job.cancelAndJoin(); dir.deleteRecursively() }
    }
}
