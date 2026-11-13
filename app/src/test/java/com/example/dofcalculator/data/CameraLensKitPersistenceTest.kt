package com.example.dofcalculator.data

import androidx.datastore.preferences.core.*
import com.example.dofcalculator.model.*
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test

class CameraLensKitPersistenceTest {
    private val prime = Lens.Prime("lens-prime", "58mm f/1.7\t%\nРусский 🎞", 58.0, 1.7, 16.0)
    private val zoom = Lens.Zoom("lens-zoom", "", 48.0, 3.5, 217.0, 5.6, 45.0)

    @Test fun `versioned codec roundtrips escaped names type IDs flags and exact values`() {
        val expected = CameraLensKit(listOf(prime, zoom), true, false)
        assertEquals(expected, CameraLensKitCodec.decode(CameraLensKitCodec.encode(expected)))
        assertEquals(CameraLensKit(), CameraLensKitCodec.decode(null))
        assertEquals(CameraLensKit(), CameraLensKitCodec.decode("unknown-version"))
        val damaged = CameraLensKitCodec.encode(expected) +
            "\nprime\tbad\tbad\tNaN\t4\t\nzoom\tbad2\tbad\t55\t4\t18\t4\t"
        assertEquals(expected, CameraLensKitCodec.decode(damaged))
        val duplicate = CameraLensKitCodec.encode(expected) + "\n" +
            CameraLensKitCodec.encode(CameraLensKit(listOf(prime))).lineSequence().drop(1).single()
        assertEquals(expected, CameraLensKitCodec.decode(duplicate))
    }

    @Test fun `legacy built in and custom attachments independent flags and camera data survive DataStore restart`() = runBlocking {
        val dir = Files.createTempDirectory("camera-lenses").toFile()
        val file = File(dir, "cameras.preferences_pb")
        var job = SupervisorJob()
        fun store() = PreferenceDataStoreFactory.create(scope = CoroutineScope(job + Dispatchers.IO), produceFile = { file })
        val custom = CameraProfile("custom-id", "My camera", FrameFormat.Custom,
            customFormatName = "Half-frame", widthMm = 17.0, heightMm = 24.0)
        val builtId = BuiltInCameraProfiles.DEFAULT_ID
        val oldLens = "untouched obsolete lens payload"
        val oldCoC = "camera2\tcustom-id\tMy camera\tCustom\t0.015\tfalse"
        try {
            var data = store()
            data.edit {
                it[stringPreferencesKey("lens_old-id")] = oldLens
                it[stringPreferencesKey("camera_v2_custom-id")] = oldCoC
                it[stringPreferencesKey("unrelated")] = "keep"
                // Existing attachments from the previous app version are retained, not recreated.
                it[stringPreferencesKey("camera_lens_kit_v1_$builtId")] =
                    CameraLensKitCodec.encode(CameraLensKit(listOf(prime)))
            }
            var repo = DataStorePresetRepository(data)
            repo.saveCamera(custom)
            assertTrue(runCatching { repo.saveLens(builtId, zoom) }.isFailure)
            repo.saveLens(builtId, prime) // existing ID may still be edited
            repo.saveLens(custom.id, zoom)
            repo.saveLens(custom.id, zoom) // upsert, including repeated Save
            repo.setShowAllPresets(builtId, true, true)
            repo.setShowAllPresets(custom.id, false, true)
            repo.selectCamera(custom.id)
            val cameraRecord = data.data.first()[stringPreferencesKey("camera_v3_custom-id")]
            job.cancelAndJoin()
            job = SupervisorJob()
            data = store()
            repo = DataStorePresetRepository(data)
            val restored = repo.library.first()
            assertEquals(custom.id, restored.selectedCameraId)
            assertEquals(custom, restored.cameras.first { it.id == custom.id })
            assertEquals(BuiltInCameraProfiles.cameras, restored.cameras.filter { it.isBuiltIn })
            assertEquals(CameraLensKit(listOf(prime), true, false), restored.lensKitsByCameraId[builtId])
            assertEquals(CameraLensKit(listOf(zoom), false, true), restored.lensKitsByCameraId[custom.id])
            assertEquals(.015, restored.legacyCoCByCameraId[custom.id]!!, 0.0)
            assertEquals(cameraRecord, data.data.first()[stringPreferencesKey("camera_v3_custom-id")])
            assertEquals(oldLens, data.data.first()[stringPreferencesKey("lens_old-id")])
            assertEquals(oldCoC, data.data.first()[stringPreferencesKey("camera_v2_custom-id")])
            assertEquals("keep", data.data.first()[stringPreferencesKey("unrelated")])
            repo.saveCamera(custom.copy(name = "Renamed camera"))
            assertEquals(listOf(zoom), repo.library.first().lensKitsByCameraId[custom.id]!!.lenses)
            repo.saveLens(custom.id, zoom.copy(maxFocalLengthMm = 250.5))
            assertEquals(1, repo.library.first().lensKitsByCameraId[custom.id]!!.lenses.size)
            assertEquals(250.5, (repo.library.first().lensKitsByCameraId[custom.id]!!.lenses.single() as Lens.Zoom).maxFocalLengthMm, 0.0)
            repo.deleteLens(builtId, prime.id)
            assertEquals(emptyList<Lens>(), repo.library.first().lensKitsByCameraId[builtId]!!.lenses)
            assertTrue(repo.library.first().lensKitsByCameraId[builtId]!!.showAllFocalLengths)
            repo.deleteCamera(custom.id)
            assertFalse(repo.library.first().lensKitsByCameraId.containsKey(custom.id))
            assertNull(data.data.first()[stringPreferencesKey("camera_lens_kit_v1_custom-id")])
            assertEquals(oldCoC, data.data.first()[stringPreferencesKey("camera_v2_custom-id")])
        } finally { job.cancelAndJoin(); dir.deleteRecursively() }
    }

    @Test fun `existing profiles without attachments keep their original library`() = runBlocking<Unit> {
        val dir = Files.createTempDirectory("no-lenses-migration").toFile()
        val job = SupervisorJob()
        try {
            val data = PreferenceDataStoreFactory.create(scope = CoroutineScope(job + Dispatchers.IO),
                produceFile = { File(dir, "cameras.preferences_pb") })
            val repo = DataStorePresetRepository(data)
            assertEquals(PresetLibrary(), repo.library.first())
            assertThrows(IllegalArgumentException::class.java) {
                runBlocking { repo.saveLens("missing-camera", prime) }
            }
            for (camera in BuiltInCameraProfiles.cameras)
                assertTrue(runCatching { repo.saveLens(camera.id, prime) }.isFailure)
            assertEquals(PresetLibrary(), repo.library.first())
        } finally { job.cancelAndJoin(); dir.deleteRecursively() }
    }
}
