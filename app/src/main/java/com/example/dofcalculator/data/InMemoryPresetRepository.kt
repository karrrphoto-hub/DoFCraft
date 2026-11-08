package com.example.dofcalculator.data

import com.example.dofcalculator.model.CameraProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class InMemoryPresetRepository(
    initialLibrary: PresetLibrary = PresetLibrary(),
) : PresetRepository {
    private val state = MutableStateFlow(initialLibrary)
    override val library = state.asStateFlow()
    override suspend fun saveCamera(camera: CameraProfile) {
        require(!camera.isBuiltIn && !BuiltInCameraProfiles.isReservedId(camera.id))
        state.value = state.value.copy(cameras =
            state.value.cameras.filterNot { it.id == camera.id } + camera)
    }
    override suspend fun deleteCamera(cameraId: String) {
        require(!BuiltInCameraProfiles.isReservedId(cameraId))
        state.value = state.value.copy(
            cameras = state.value.cameras.filterNot { it.id == cameraId },
            lensKitsByCameraId = state.value.lensKitsByCameraId - cameraId,
            selectedCameraId = state.value.selectedCameraId.takeUnless { it == cameraId }
                ?: BuiltInCameraProfiles.DEFAULT_ID)
    }
    override suspend fun selectCamera(cameraId: String) {
        require(state.value.cameras.any { it.id == cameraId })
        state.value = state.value.copy(selectedCameraId = cameraId)
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
    private fun updateKit(
        cameraId: String,
        update: com.example.dofcalculator.model.CameraLensKit.() -> com.example.dofcalculator.model.CameraLensKit,
    ) {
        require(state.value.cameras.any { it.id == cameraId })
        val kit = state.value.lensKitsByCameraId[cameraId] ?: com.example.dofcalculator.model.CameraLensKit()
        state.value = state.value.copy(lensKitsByCameraId = state.value.lensKitsByCameraId + (cameraId to kit.update()))
    }
}
