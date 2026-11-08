package com.example.dofcalculator.ui.calculator

import com.example.dofcalculator.data.BuiltInCameraProfiles
import com.example.dofcalculator.model.CameraProfile
import com.example.dofcalculator.model.FrameFormat

data class PresetManagerUiState(
    val cameras: List<CameraProfile> = BuiltInCameraProfiles.cameras,
    val selectedCameraId: String? = BuiltInCameraProfiles.DEFAULT_ID,
    val isManagerVisible: Boolean = false,
    val cameraEditor: CameraEditorUiState? = null,
    val pendingDeleteId: String? = null,
    val storageError: Boolean = false,
    val lensKitsByCameraId: Map<String, com.example.dofcalculator.model.CameraLensKit> = emptyMap(),
)

data class CameraEditorUiState(
    val cameraId: String? = null,
    val nameText: String = "",
    val frameFormat: FrameFormat = FrameFormat.FullFrame35Mm,
    val customFormatName: String = "",
    val widthText: String = "",
    val heightText: String = "",
    val nameHasError: Boolean = false,
    val dimensionsHaveError: Boolean = false,
    val customFormatHasError: Boolean = false,
)
