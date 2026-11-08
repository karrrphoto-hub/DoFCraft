package com.example.dofcalculator.ui.calculator

import com.example.dofcalculator.model.CalculatedDistance
import com.example.dofcalculator.calculation.FocusDistanceScale
import com.example.dofcalculator.model.DepthOfFieldResult
import com.example.dofcalculator.model.FrameFormat
import com.example.dofcalculator.settings.DistanceUnit
import com.example.dofcalculator.settings.DistanceRounding
import com.example.dofcalculator.settings.AppLanguage
import com.example.dofcalculator.settings.CalculatorMode
import com.example.dofcalculator.settings.ThemeMode

data class DofCalculatorUiState(
    val focalLengthText: String = "50",
    val apertureText: String = "8",
    val apertureIncrements: com.example.dofcalculator.settings.ApertureIncrements =
        com.example.dofcalculator.settings.ApertureIncrements.FullStop,
    val focusDistanceText: String = "5",
    val focusInputDraft: String? = null,
    val isFocusDistanceVisible: Boolean = false,
    val isMacroFocusVisible: Boolean = false,
    val macroForm: MacroFormUiState = MacroFormUiState(),
    val manualMagnificationAssessment: com.example.dofcalculator.model.ManualMagnificationAssessment =
        com.example.dofcalculator.model.ManualMagnificationAssessment(),
    val distanceUnit: DistanceUnit = DistanceUnit.Meters,
    val distanceRounding: DistanceRounding = DistanceRounding.Three,
    val focusDistanceMeters: Double? = 5.0,
    val focusAtInfinity: Boolean = false,
    val focusDistance: CalculatedDistance? = CalculatedDistance.Finite(5.0),
    val distanceScale: FocusDistanceScale? = null,
    val distanceSliderPosition: Double = 0.0,
    val selectedFrameFormat: FrameFormat = FrameFormat.FullFrame35Mm,
    val circleOfConfusionText: String = "0,030",
    val effectiveCoCMm: Double? = 0.030,
    val outputSharpness: com.example.dofcalculator.calculation.OutputSharpnessResult? = null,
    val standardCoC: com.example.dofcalculator.model.StandardCoCResult? = null,
    val sharpnessCalculationFailed: Boolean = false,
    val sharpness: com.example.dofcalculator.model.SharpnessSettings = com.example.dofcalculator.model.SharpnessSettings(),
    val sharpnessDrafts: Map<com.example.dofcalculator.model.SharpnessField, String> = emptyMap(),
    val isSharpnessVisible: Boolean = false,
    val videoDrafts: Map<com.example.dofcalculator.model.VideoField, String> = emptyMap(),
    val videoResult: com.example.dofcalculator.model.VideoCoCResult? = null,
    val videoError: com.example.dofcalculator.model.VideoError? = null,
    val videoInvalidFields: Set<com.example.dofcalculator.model.VideoField> = emptySet(),
    val diffractionSettings: com.example.dofcalculator.settings.DiffractionSettings =
        com.example.dofcalculator.settings.DiffractionSettings(),
    val diffractionResult: com.example.dofcalculator.model.DiffractionResult? = null,
    val magnificationEstimate: com.example.dofcalculator.model.MagnificationEstimate =
        com.example.dofcalculator.model.MagnificationEstimate(null),
    val macroDetection: com.example.dofcalculator.model.MacroDetectionState =
        com.example.dofcalculator.model.MacroDetectionState.None,
    val macroWarningsEnabled: Boolean = true,
    val macroDetectionInput: com.example.dofcalculator.model.MacroDetectionInput? = null,
    val isDiffractionInfoVisible: Boolean = false,
    val isMagnificationSettingsVisible: Boolean = false,
    val legacyCoCByCameraId: Map<String, Double> = emptyMap(),
    val legacyCalculatorCoCmm: Double? = null,
    val calculatorMode: CalculatorMode = CalculatorMode.DepthOfField,
    val themeMode: ThemeMode = ThemeMode.System,
    val language: AppLanguage = AppLanguage.System,
    val isSettingsVisible: Boolean = false,
    val isAboutVisible: Boolean = false,
    val settingsStorageError: Boolean = false,
    val result: DepthOfFieldResult? = null,
    val focalLengthHasError: Boolean = false,
    val apertureHasError: Boolean = false,
    val focusDistanceHasError: Boolean = false,
    val circleOfConfusionHasError: Boolean = false,
    val selectedFocalLengthPresetMm: Double? = 50.0,
    val focalLengthPresets: List<Double> = CalculatorPresets.typicalFocalLengthsMm.map(Int::toDouble),
    val aperturePresets: List<Double> = apertureIncrements.apertures,
    val lensManager: LensManagerUiState = LensManagerUiState(),
    val canDecreaseFocalLength: Boolean = true,
    val canIncreaseFocalLength: Boolean = true,
    val selectedStandardAperture: Double? = 8.0,
    val canSelectPreviousAperture: Boolean = true,
    val canSelectNextAperture: Boolean = true,
    val presets: PresetManagerUiState = PresetManagerUiState(),
    val inputError: DofInputError? = null,
) {
    val isMacroModeActive: Boolean get() = calculatorMode == CalculatorMode.DepthOfField &&
        diffractionSettings.magnificationMode == com.example.dofcalculator.model.MagnificationMode.MANUAL
    val isFocusDistanceEditable: Boolean get() = calculatorMode == CalculatorMode.DepthOfField
    val focusMagnification: Double? get() = diffractionSettings.manualMagnification.takeIf {
        diffractionSettings.magnificationMode == com.example.dofcalculator.model.MagnificationMode.MANUAL &&
            manualMagnificationAssessment.status == com.example.dofcalculator.model.ManualMagnificationStatus.VALID
    }
    val manualNeedsConfirmation: Boolean get() =
        diffractionSettings.magnificationMode == com.example.dofcalculator.model.MagnificationMode.MANUAL &&
            manualMagnificationAssessment.status in listOf(
                com.example.dofcalculator.model.ManualMagnificationStatus.NEEDS_CONFIRMATION,
                com.example.dofcalculator.model.ManualMagnificationStatus.INVALID)
    val showManualMagnificationNotice: Boolean get() = isMacroModeActive && manualNeedsConfirmation
    val cameraLensKit: com.example.dofcalculator.model.CameraLensKit
        get() = presets.lensKitsByCameraId[presets.selectedCameraId]
            ?: com.example.dofcalculator.model.CameraLensKit()
    val showDiffractionWarning: Boolean get() = diffractionSettings.warningsEnabled &&
        diffractionResult?.level?.let { it != com.example.dofcalculator.model.DiffractionLevel.NONE } == true
    val showMacroWarning: Boolean get() = macroWarningsEnabled &&
        calculatorMode == CalculatorMode.DepthOfField && !isMacroModeActive && !focusAtInfinity &&
        (macroDetection is com.example.dofcalculator.model.MacroDetectionState.MacroEstimated ||
            macroDetection == com.example.dofcalculator.model.MacroDetectionState.MagnificationUnavailableAtCloseDistance)
    val showMacroIndicator: Boolean get() = calculatorMode == CalculatorMode.DepthOfField &&
        (magnificationEstimate.suspension != null ||
            magnificationEstimate.source == com.example.dofcalculator.model.MagnificationMode.MANUAL ||
            (magnificationEstimate.value ?: 0.0) >=
                com.example.dofcalculator.calculation.MagnificationEstimator.MACRO_INDICATOR_THRESHOLD)

    /** Compare canonical metres, never the rounded text shown in the input. */
    val isFocusedAtHyperfocal: Boolean
        get() {
            if (focusAtInfinity || focusDistanceHasError) return false
            val distance = focusDistance as? CalculatedDistance.Finite ?: return false
            val hyperfocal = distanceScale?.hyperfocalMeters ?: return false
            return kotlin.math.abs(distance.meters - hyperfocal) <= hyperfocal * 1e-12
        }
}

enum class DofInputError {
    InvalidNumber,
    OutOfRange,
}
