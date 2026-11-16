package com.example.dofcalculator.data

import com.example.dofcalculator.model.CameraProfile
import kotlinx.coroutines.flow.Flow

data class PresetLibrary(
    val cameras: List<CameraProfile> = BuiltInCameraProfiles.cameras,
    val selectedCameraId: String = BuiltInCameraProfiles.DEFAULT_ID,
    val legacyCoCByCameraId: Map<String, Double> = emptyMap(),
    val lensKitsByCameraId: Map<String, com.example.dofcalculator.model.CameraLensKit> = emptyMap(),
)

interface PresetRepository {
    val library: Flow<PresetLibrary>
    suspend fun saveCamera(camera: CameraProfile)
    suspend fun deleteCamera(cameraId: String)
    suspend fun selectCamera(cameraId: String)
    suspend fun saveLens(cameraId: String, lens: com.example.dofcalculator.model.Lens)
    suspend fun deleteLens(cameraId: String, lensId: String)
    suspend fun selectLens(cameraId: String, lensId: String)
    suspend fun setShowAllPresets(cameraId: String, focalLengths: Boolean, enabled: Boolean)
}
