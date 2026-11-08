package com.example.dofcalculator.data

import androidx.datastore.preferences.core.*
import com.example.dofcalculator.model.*
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test

class DataStorePresetRepositoryTest {
    @Test fun `digital built ins merge once preserve users and retain selection after restart`() = runBlocking {
        val dir = Files.createTempDirectory("digital-camera-catalog").toFile()
        val file = File(dir, "cameras.preferences_pb")
        var job = SupervisorJob()
        fun store() = PreferenceDataStoreFactory.create(
            scope = CoroutineScope(job + Dispatchers.IO), produceFile = { file })
        val user = CameraProfile("user-camera", "My camera", FrameFormat.MediumFormat6x6)
        val digitalIds = listOf("fujifilm_gfx", "hasselblad_x", "hasselblad_h6d_100c")
        try {
            var data = store()
            // Simulate existing user data and an obsolete duplicate catalogue record.
            val archived = CameraPresetCodec.encode(user.copy(id = "fujifilm_gfx"))
            data.edit {
                it[stringPreferencesKey("camera_ids_v2")] = "user-camera\nfujifilm_gfx\nfujifilm_gfx"
                it[stringPreferencesKey("camera_v3_user-camera")] = CameraPresetCodec.encode(user)
                it[stringPreferencesKey("camera_v3_fujifilm_gfx")] = archived
                it[stringPreferencesKey("selected_camera_id")] = user.id
                it[stringPreferencesKey("unrelated")] = "keep"
            }
            repeat(3) {
                val library = DataStorePresetRepository(data).library.first()
                assertEquals(20, library.cameras.size)
                assertEquals(library.cameras.size, library.cameras.map { it.id }.distinct().size)
                assertEquals(user, library.cameras.single { it.id == user.id })
                assertEquals(user.id, library.selectedCameraId)
                digitalIds.forEach { id -> assertTrue(library.cameras.single { it.id == id }.isBuiltIn) }
            }
            val repo = DataStorePresetRepository(data)
            for (id in digitalIds) {
                val camera = BuiltInCameraProfiles.cameras.single { it.id == id }
                assertTrue(runCatching { repo.deleteCamera(id) }.isFailure)
                assertTrue(runCatching { repo.saveCamera(camera.copy(isBuiltIn = false)) }.isFailure)
            }
            repo.selectCamera("hasselblad_h6d_100c")
            job.cancelAndJoin()
            job = SupervisorJob()
            data = store()
            val restored = DataStorePresetRepository(data).library.first()
            assertEquals("hasselblad_h6d_100c", restored.selectedCameraId)
            assertEquals(user, restored.cameras.single { it.id == user.id })
            assertEquals(20, restored.cameras.size)
            assertEquals(archived, data.data.first()[stringPreferencesKey("camera_v3_fujifilm_gfx")])
            assertEquals("keep", data.data.first()[stringPreferencesKey("unrelated")])
        } finally {
            job.cancelAndJoin()
            dir.deleteRecursively()
        }
    }

    @Test fun `legacy data selection custom CoC edits and deletion survive restart`() = runBlocking {
        val dir = Files.createTempDirectory("camera-migration").toFile()
        val file = File(dir, "cameras.preferences_pb")
        var job = SupervisorJob()
        fun store() = PreferenceDataStoreFactory.create(
            scope = CoroutineScope(job + Dispatchers.IO), produceFile = { file })
        try {
            var data = store()
            val legacy = "camera\told\tYashica+Mat+124G\tMediumFormat6x6\t0.040\nlens\tlens\tYashinon\t80\t3.5"
            data.edit {
                it[stringPreferencesKey("camera_ids")] = "old\nold"
                it[stringPreferencesKey("camera_old")] = legacy
                it[stringPreferencesKey("selected_camera_id")] = "old"
                it[stringPreferencesKey("selected_lens_id")] = "lens"
                it[stringPreferencesKey("unrelated")] = "keep"
            }
            var repo = DataStorePresetRepository(data)
            var library = repo.library.first()
            assertEquals(BuiltInCameraProfiles.cameras.size + 1, library.cameras.size)
            assertEquals("old", library.selectedCameraId)
            val camera = library.cameras.single { it.id == "old" }
            assertEquals(.040, library.legacyCoCByCameraId["old"]!!, 0.0)
            repo.saveCamera(camera.copy(name = "Fine Art"))
            job.cancelAndJoin()
            job = SupervisorJob()
            data = store()
            repo = DataStorePresetRepository(data)
            library = repo.library.first()
            assertEquals("old", library.selectedCameraId)
            assertEquals("Fine Art", library.cameras.single { it.id == "old" }.name)
            assertEquals(.040, library.legacyCoCByCameraId["old"]!!, 0.0)
            assertEquals(56.0, library.cameras.single { it.id == "old" }.widthMm!!, 0.0)
            assertEquals(legacy, data.data.first()[stringPreferencesKey("camera_old")])
            assertEquals("keep", data.data.first()[stringPreferencesKey("unrelated")])
            assertTrue(runCatching { repo.deleteCamera(BuiltInCameraProfiles.DEFAULT_ID) }.isFailure)
            assertTrue(runCatching { repo.saveCamera(BuiltInCameraProfiles.cameras.first()) }.isFailure)
            repo.deleteCamera("old")
            assertEquals(BuiltInCameraProfiles.cameras.size, repo.library.first().cameras.size)
            assertEquals(BuiltInCameraProfiles.DEFAULT_ID, repo.library.first().selectedCameraId)
            repo.selectCamera("builtin.8x10")
            job.cancelAndJoin()
            job = SupervisorJob()
            repo = DataStorePresetRepository(store())
            assertEquals("builtin.8x10", repo.library.first().selectedCameraId)
            assertEquals(BuiltInCameraProfiles.cameras.size, repo.library.first().cameras.size)
        } finally {
            job.cancelAndJoin()
            dir.deleteRecursively()
        }
    }
}
