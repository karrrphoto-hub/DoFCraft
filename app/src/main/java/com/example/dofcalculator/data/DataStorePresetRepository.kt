package com.example.dofcalculator.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import com.example.dofcalculator.model.CameraProfile
import java.io.IOException
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class DataStorePresetRepository(private val dataStore: DataStore<Preferences>) : PresetRepository {
    override val library = dataStore.data.catch {
        if (it is IOException) emit(emptyPreferences()) else throw it
    }.map { p ->
        val cameras = BuiltInCameraProfiles.cameras + ids(p).mapNotNull { id ->
            (p[v3Key(id)] ?: p[key(id)] ?: p[legacyKey(id)])?.let(CameraPresetCodec::decode)
                ?.takeIf { it.id == id && !BuiltInCameraProfiles.isReservedId(id) }
        }
        PresetLibrary(cameras, p[SELECTED]?.takeIf { id -> cameras.any { it.id == id } }
            ?: BuiltInCameraProfiles.DEFAULT_ID,
            ids(p).mapNotNull { id ->
                (p[key(id)] ?: p[legacyKey(id)])?.let(CameraPresetCodec::legacyCoC)
                    ?.let { id to it }
            }.toMap(),
            cameras.mapNotNull { camera -> p[kitKey(camera.id)]?.let {
                camera.id to CameraLensKitCodec.decode(it)
            } }.toMap())
    }

    override suspend fun saveCamera(camera: CameraProfile) {
        require(!camera.isBuiltIn && !BuiltInCameraProfiles.isReservedId(camera.id))
        dataStore.edit { p ->
            val ids = ids(p).toMutableList()
            if (camera.id !in ids) ids += camera.id
            p[IDS] = ids.distinct().joinToString("\n")
            p[v3Key(camera.id)] = CameraPresetCodec.encode(camera)
        }
    }
    override suspend fun deleteCamera(cameraId: String) {
        require(!BuiltInCameraProfiles.isReservedId(cameraId))
        dataStore.edit { p ->
            p[IDS] = ids(p).filterNot { it == cameraId }.joinToString("\n")
            p.remove(v3Key(cameraId))
            p.remove(kitKey(cameraId))
            // Legacy payload is retained as an archive, but v2 ID list is authoritative.
            if (p[SELECTED] == cameraId) p[SELECTED] = BuiltInCameraProfiles.DEFAULT_ID
        }
    }
    override suspend fun selectCamera(cameraId: String) {
        dataStore.edit { p ->
            require(BuiltInCameraProfiles.cameras.any { it.id == cameraId } || cameraId in ids(p))
            p[SELECTED] = cameraId
        }
    }
    override suspend fun saveLens(cameraId: String, lens: com.example.dofcalculator.model.Lens) {
        updateKit(cameraId) { copy(lenses = lenses.filterNot { it.id == lens.id } + lens) }
    }
    override suspend fun deleteLens(cameraId: String, lensId: String) {
        updateKit(cameraId) { copy(lenses = lenses.filterNot { it.id == lensId }) }
    }
    override suspend fun setShowAllPresets(cameraId: String, focalLengths: Boolean, enabled: Boolean) {
        updateKit(cameraId) {
            if (focalLengths) copy(showAllFocalLengths = enabled) else copy(showAllApertures = enabled)
        }
    }
    private suspend fun updateKit(
        cameraId: String,
        update: com.example.dofcalculator.model.CameraLensKit.() -> com.example.dofcalculator.model.CameraLensKit,
    ) {
        dataStore.edit { p ->
            require(BuiltInCameraProfiles.cameras.any { it.id == cameraId } || cameraId in ids(p))
            p[kitKey(cameraId)] = CameraLensKitCodec.encode(CameraLensKitCodec.decode(p[kitKey(cameraId)]).update())
        }
    }
    private fun ids(p: Preferences) = (p[IDS] ?: p[LEGACY_IDS]).orEmpty()
        .lineSequence().filter { it.isNotBlank() }.distinct().toList()
    private companion object {
        val IDS = stringPreferencesKey("camera_ids_v2")
        val LEGACY_IDS = stringPreferencesKey("camera_ids")
        val SELECTED = stringPreferencesKey("selected_camera_id")
        fun kitKey(id: String) = stringPreferencesKey("camera_lens_kit_v1_$id")
        fun v3Key(id: String) = stringPreferencesKey("camera_v3_$id")
        fun key(id: String) = stringPreferencesKey("camera_v2_$id")
        fun legacyKey(id: String) = stringPreferencesKey("camera_$id")
    }
}
