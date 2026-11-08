package com.example.dofcalculator.ui.calculator

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.dofcalculator.calculation.DiffractionCalculator
import com.example.dofcalculator.calculation.MagnificationEstimator
import com.example.dofcalculator.model.MagnificationMode
import com.example.dofcalculator.model.*
import com.example.dofcalculator.calculation.ThinLensEquivalentCalculator
import com.example.dofcalculator.calculation.ManualMagnificationValidator
import com.example.dofcalculator.model.MagnificationEstimate
import com.example.dofcalculator.settings.DiffractionSettings
import com.example.dofcalculator.calculation.DepthOfFieldCalculator
import com.example.dofcalculator.calculation.FocusDistanceScale
import com.example.dofcalculator.model.CalculatedDistance
import com.example.dofcalculator.data.InMemoryPresetRepository
import com.example.dofcalculator.data.PresetLibrary
import com.example.dofcalculator.data.PresetRepository
import com.example.dofcalculator.model.CameraProfile
import com.example.dofcalculator.model.DepthOfFieldInput
import com.example.dofcalculator.model.FrameFormat
import com.example.dofcalculator.model.SharpnessSettings
import com.example.dofcalculator.model.SharpnessCriterionMode
import com.example.dofcalculator.model.SharpnessField
import com.example.dofcalculator.model.PrintPlacement
import com.example.dofcalculator.calculation.SharpnessCriterionCalculator
import com.example.dofcalculator.calculation.StandardCoCResolver
import com.example.dofcalculator.settings.DistanceUnit
import com.example.dofcalculator.settings.DistanceRounding
import com.example.dofcalculator.settings.ApertureIncrements
import com.example.dofcalculator.settings.AppLanguage
import com.example.dofcalculator.settings.AppSettings
import com.example.dofcalculator.settings.AppSettingsRepository
import com.example.dofcalculator.settings.CalculatorMode
import com.example.dofcalculator.settings.CalculatorSettings
import com.example.dofcalculator.settings.InMemoryAppSettingsRepository
import com.example.dofcalculator.settings.ThemeMode
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class DofCalculatorViewModel internal constructor(
    private val presetRepository: PresetRepository,
    private val appSettingsRepository: AppSettingsRepository,
    private val coroutineScope: CoroutineScope,
) : ViewModel() {
    constructor(
        presetRepository: PresetRepository,
        appSettingsRepository: AppSettingsRepository,
    ) : this(
        presetRepository = presetRepository,
        appSettingsRepository = appSettingsRepository,
        coroutineScope = CoroutineScope(
            SupervisorJob() + Dispatchers.Main.immediate,
        ),
    )

    internal constructor() : this(
        presetRepository = InMemoryPresetRepository(),
        appSettingsRepository = InMemoryAppSettingsRepository(),
        coroutineScope = CoroutineScope(
            SupervisorJob() + Dispatchers.Unconfined,
        ),
    )

    var uiState by mutableStateOf(DofCalculatorUiState())
        private set

    private var settingsRestored = false
    private var libraryRestored = false
    private var sharpnessMigrationComplete = false
    private var lastValidFocusDistanceText = uiState.focusDistanceText

    init {
        uiState = recalculate(uiState)
        coroutineScope.launch {
            presetRepository.library.collect(::onPresetLibraryChanged)
        }
        coroutineScope.launch {
            appSettingsRepository.settings.collect(::onSettingsChanged)
        }
    }

    fun onOpenCameraLenses(cameraId: String) {
        if (uiState.presets.cameras.none { it.id == cameraId }) return
        uiState = uiState.copy(lensManager = LensManagerUiState(cameraId))
    }

    fun onCloseCameraLenses() {
        if (uiState.lensManager.editor?.isSaving == true) return
        uiState = uiState.copy(lensManager = LensManagerUiState())
    }

    fun onAddLens() {
        if (uiState.lensManager.cameraId == null) return
        uiState = uiState.copy(lensManager = uiState.lensManager.copy(
            editor = LensEditorUiState(LensDraft(UUID.randomUUID().toString()))))
    }

    fun onEditLens(lensId: String) {
        val cameraId = uiState.lensManager.cameraId ?: return
        val lens = uiState.presets.lensKitsByCameraId[cameraId]?.lenses
            ?.firstOrNull { it.id == lensId } ?: return
        val draft = LensDraft.from(lens)
        uiState = uiState.copy(lensManager = uiState.lensManager.copy(editor =
            LensEditorUiState(draft, com.example.dofcalculator.calculation.LensInputValidator.validate(draft),
                isNew = false)))
    }

    private fun updateLensDraft(update: LensDraft.() -> LensDraft) {
        val editor = uiState.lensManager.editor?.takeUnless { it.isSaving } ?: return
        val draft = editor.draft.update()
        uiState = uiState.copy(lensManager = uiState.lensManager.copy(editor = editor.copy(
            draft = draft, validation = com.example.dofcalculator.calculation.LensInputValidator.validate(draft))))
    }
    fun onLensNameChanged(value: String) = updateLensDraft { copy(name = value) }
    fun onLensZoomChanged(zoom: Boolean) = updateLensDraft {
        copy(type = if (zoom) LensType.ZOOM else LensType.PRIME)
    }
    fun onLensFieldChanged(field: LensField, value: String) = updateLensDraft {
        copy(fields = fields + (field to value))
    }
    fun onCancelLensEditor() {
        if (uiState.lensManager.editor?.isSaving == true) return
        uiState = uiState.copy(lensManager = uiState.lensManager.copy(editor = null))
    }
    fun onSaveLens() {
        val cameraId = uiState.lensManager.cameraId ?: return
        val editor = uiState.lensManager.editor?.takeUnless { it.isSaving } ?: return
        val validation = com.example.dofcalculator.calculation.LensInputValidator.validate(editor.draft)
        uiState = uiState.copy(lensManager = uiState.lensManager.copy(editor =
            editor.copy(validation = validation, showErrors = true, isSaving = validation.lens != null)))
        val lens = validation.lens ?: return
        coroutineScope.launch {
            try {
                presetRepository.saveLens(cameraId, lens)
                if (uiState.lensManager.editor?.draft?.id == lens.id)
                    uiState = uiState.copy(lensManager = uiState.lensManager.copy(editor = null))
            } catch (error: kotlinx.coroutines.CancellationException) {
                throw error
            } catch (_: Exception) {
                uiState = uiState.copy(presets = uiState.presets.copy(storageError = true),
                    lensManager = uiState.lensManager.copy(editor = editor.copy(isSaving = false)))
            }
        }
    }
    fun onDeleteLens(lensId: String) {
        uiState = uiState.copy(lensManager = uiState.lensManager.copy(pendingDeleteId = lensId))
    }
    fun onCancelDeleteLens() {
        uiState = uiState.copy(lensManager = uiState.lensManager.copy(pendingDeleteId = null))
    }
    fun onConfirmDeleteLens() {
        val cameraId = uiState.lensManager.cameraId ?: return
        val id = uiState.lensManager.pendingDeleteId ?: return
        onCancelDeleteLens()
        launchRepositoryOperation { presetRepository.deleteLens(cameraId, id) }
    }
    fun onShowAllFocalLengths(enabled: Boolean) = savePresetVisibility(true, enabled)
    fun onShowAllApertures(enabled: Boolean) = savePresetVisibility(false, enabled)
    private fun savePresetVisibility(focalLengths: Boolean, enabled: Boolean) {
        val cameraId = uiState.presets.selectedCameraId ?: return
        launchRepositoryOperation { presetRepository.setShowAllPresets(cameraId, focalLengths, enabled) }
    }

    fun onFrameFormatSelected(frameFormat: FrameFormat) {
        uiState = recalculate(uiState.copy(selectedFrameFormat = frameFormat))
    }

    /** Compatibility entry point: manual CoC always belongs to Custom, never a camera. */
    fun onCircleOfConfusionChanged(value: String) {
        uiState = uiState.copy(sharpness = uiState.sharpness.copy(mode = SharpnessCriterionMode.CUSTOM))
        onSharpnessFieldChanged(SharpnessField.CUSTOM_COC, value)
    }

    fun onOpenSharpness() { uiState = uiState.copy(isSharpnessVisible = true) }
    fun onCloseSharpness() { uiState = uiState.copy(isSharpnessVisible = false) }

    fun onSharpnessModeSelected(mode: SharpnessCriterionMode) {
        updateSharpness(uiState.sharpness.copy(mode = mode))
    }

    fun onVideoResolutionSelected(resolution: com.example.dofcalculator.model.VideoResolution) {
        updateSharpness(uiState.sharpness.copy(video = uiState.sharpness.video.copy(resolution = resolution)))
    }

    fun onVideoAreaModeSelected(mode: com.example.dofcalculator.model.VideoAreaMode) {
        updateSharpness(uiState.sharpness.copy(video = uiState.sharpness.video.copy(areaMode = mode)))
    }

    fun onVideoFieldChanged(field: com.example.dofcalculator.model.VideoField, text: String) {
        val value = com.example.dofcalculator.calculation.VideoCoCCalculator.parseField(field, text)
        uiState = uiState.copy(videoDrafts = uiState.videoDrafts + (field to text))
        updateSharpness(uiState.sharpness.copy(video = if (value == null) uiState.sharpness.video
            else field.update(uiState.sharpness.video, value)))
    }

    fun onPrintPlacementSelected(placement: PrintPlacement) {
        updateSharpness(uiState.sharpness.copy(printPlacement = placement))
    }

    fun onSharpnessFieldChanged(field: SharpnessField, text: String) {
        val value = SharpnessCriterionCalculator.positiveNumber(text)?.takeIf(field::isValid)
        uiState = uiState.copy(sharpnessDrafts = uiState.sharpnessDrafts + (field to text))
        updateSharpness(when {
            field.optional && text.isBlank() -> field.update(uiState.sharpness, null)
            value != null -> field.update(uiState.sharpness, value)
            else -> uiState.sharpness
        })
    }

    fun onPrintSizeSelected(size: com.example.dofcalculator.model.PrintSizePreset) {
        uiState = uiState.copy(sharpnessDrafts = uiState.sharpnessDrafts -
            setOf(SharpnessField.PRINT_WIDTH, SharpnessField.PRINT_HEIGHT))
        updateSharpness(uiState.sharpness.copy(printSize = size,
            printWidthCm = size.widthCm ?: uiState.sharpness.printWidthCm,
            printHeightCm = size.heightCm ?: uiState.sharpness.printHeightCm))
    }

    fun onScreenDeviceSelected(device: com.example.dofcalculator.model.ScreenDevice) {
        if (device == com.example.dofcalculator.model.ScreenDevice.CUSTOM) {
            updateSharpness(uiState.sharpness.copy(screenDevice = device))
            return
        }
        uiState = uiState.copy(sharpnessDrafts = uiState.sharpnessDrafts -
            setOf(SharpnessField.SCREEN_DIAGONAL, SharpnessField.SCREEN_WIDTH,
                SharpnessField.SCREEN_HEIGHT, SharpnessField.SCREEN_VIEWING))
        updateSharpness(uiState.sharpness.copy(screenDevice = device,
            screenDiagonalInches = device.diagonal, screenWidthPx = device.widthPx,
            screenHeightPx = device.heightPx, screenViewingCm = device.viewingCm))
    }

    fun onScreenCriterionSelected(criterion: com.example.dofcalculator.model.ScreenCriterion) {
        updateSharpness(uiState.sharpness.copy(screenCriterion = criterion))
    }

    fun onScreenPlacementSelected(placement: PrintPlacement) {
        updateSharpness(uiState.sharpness.copy(screenPlacement = placement))
    }

    fun onAcknowledgePrintModel() {
        updateSharpness(uiState.sharpness.copy(printModelNoticePending = false))
    }

    fun onImportLegacyCoC(cameraId: String?) {
        val value = (if (cameraId == null) uiState.legacyCalculatorCoCmm
            else uiState.legacyCoCByCameraId[cameraId]) ?: return
        uiState = uiState.copy(sharpnessDrafts = uiState.sharpnessDrafts - SharpnessField.CUSTOM_COC)
        updateSharpness(uiState.sharpness.copy(mode = SharpnessCriterionMode.CUSTOM, customCoCmm = value))
    }

    private fun updateSharpness(settings: SharpnessSettings) {
        uiState = recalculate(uiState.copy(sharpness = settings))
        if (sharpnessMigrationComplete) {
            launchSettingsOperation { appSettingsRepository.saveSharpnessSettings(settings) }
        }
    }

    fun onCameraProfileSelected(cameraId: String) {
        if (uiState.presets.cameras.none { it.id == cameraId }) return
        uiState = uiState.copy(presets = uiState.presets.copy(selectedCameraId = cameraId))
        applySelectedCamera()
        launchRepositoryOperation { presetRepository.selectCamera(cameraId) }
    }

    fun onFocalLengthChanged(value: String) {
        uiState = recalculate(uiState.copy(focalLengthText = value))
    }

    fun onStepFocalLength(increase: Boolean) {
        val current = uiState.focalLengthText.toPositiveDecimalOrNull() ?: return
        val stepped = java.math.BigDecimal(
            uiState.focalLengthText.trim().replace(',', '.'),
        ).add(if (increase) java.math.BigDecimal.ONE else java.math.BigDecimal.ONE.negate())
        val value = stepped.toDouble()
        if (!value.isFinite() || value <= 0.0 || value == current) return
        onFocalLengthChanged(stepped.stripTrailingZeros().toPlainString())
    }

    fun onFocalLengthPresetSelected(focalLengthMm: Int) =
        onFocalLengthPresetSelected(focalLengthMm.toDouble())

    fun onFocalLengthPresetSelected(focalLengthMm: Double) {
        onFocalLengthChanged(focalLengthMm.toPresetInput())
    }

    fun onApertureIncrementsSelected(increments: ApertureIncrements) {
        uiState = recalculate(uiState.copy(apertureIncrements = increments))
        launchSettingsOperation { appSettingsRepository.setApertureIncrements(increments) }
    }

    fun onApertureChanged(value: String) {
        uiState = recalculate(uiState.copy(apertureText = value))
    }

    fun onAperturePresetSelected(aperture: Double) {
        onApertureChanged(aperture.toPresetInput())
    }

    fun onPreviousAperture() {
        stepAperture(forward = false)
    }

    fun onNextAperture() {
        stepAperture(forward = true)
    }

    fun onFocusDistanceChanged(value: String) {
        if (!uiState.isFocusDistanceEditable) return
        uiState = recalculate(uiState.copy(focusDistanceText = value, focusAtInfinity = false, focusInputDraft = null))
    }

    fun onFocusDistanceInputChanged(value: String) {
        if (!uiState.isFocusDistanceEditable) return
        val meters = value.toPositiveDecimalOrNull()?.let(uiState.distanceUnit::toMeters)
            ?.takeIf { it.isFinite() && it > 0.0 }
        uiState = recalculate(uiState.copy(
            focusInputDraft = value,
            focusDistanceText = meters?.let { java.math.BigDecimal.valueOf(it).toPlainString() }.orEmpty(),
            focusAtInfinity = false,
        ))
    }

    fun onDistanceUnitSelected(unit: DistanceUnit) {
        uiState = uiState.copy(distanceUnit = unit, focusInputDraft = null)
        launchSettingsOperation { appSettingsRepository.setDistanceDisplay(unit, uiState.distanceRounding) }
    }

    fun onDistanceRoundingSelected(rounding: DistanceRounding) {
        uiState = uiState.copy(distanceRounding = rounding, focusInputDraft = null)
        launchSettingsOperation { appSettingsRepository.setDistanceDisplay(uiState.distanceUnit, rounding) }
    }

    fun onDistanceSliderChanged(position: Double) {
        if (!uiState.isFocusDistanceEditable) return
        val scale = uiState.distanceScale ?: return
        if (!position.isFinite()) return
        setFocusDistance(scale.distanceAt(position))
    }

    fun onFocusAtHyperfocal() {
        uiState.distanceScale?.let {
            setFocusDistance(CalculatedDistance.Finite(it.hyperfocalMeters))
        }
    }

    fun onFocusAtInfinity() {
        setFocusDistance(CalculatedDistance.Infinity)
    }

    private fun setFocusDistance(distance: CalculatedDistance) {
        if (!uiState.isFocusDistanceEditable) return
        uiState = recalculate(when (distance) {
            CalculatedDistance.Infinity -> uiState.copy(focusAtInfinity = true, focusInputDraft = null)
            is CalculatedDistance.Finite -> uiState.copy(
                focusAtInfinity = false,
                focusInputDraft = null,
                focusDistanceText = java.math.BigDecimal.valueOf(distance.meters)
                    .stripTrailingZeros().toPlainString(),
            )
        })
    }

    fun onStepFocusDistance(increase: Boolean) {
        if (uiState.focusAtInfinity || !uiState.isFocusDistanceEditable) return
        val value = uiState.focusDistanceText.toPositiveDecimalOrNull() ?: return
        val stepped = java.math.BigDecimal.valueOf(value).add(
            java.math.BigDecimal(if (increase) "0.01" else "-0.01"))
        val meters = stepped.toDouble()
        if (meters.isFinite() && meters > 0.0) setFocusDistance(CalculatedDistance.Finite(meters))
    }

    fun onCalculatorModeSelected(mode: CalculatorMode) {
        uiState = recalculate(uiState.copy(calculatorMode = mode))
    }

    fun onThemeModeSelected(themeMode: ThemeMode) {
        uiState = uiState.copy(themeMode = themeMode)
        launchSettingsOperation {
            appSettingsRepository.setThemeMode(themeMode)
        }
    }

    fun onLanguageSelected(language: AppLanguage) {
        uiState = uiState.copy(language = language)
        launchSettingsOperation {
            appSettingsRepository.setLanguage(language)
        }
    }

    fun onOpenFocusDistance() {
        uiState = uiState.copy(isFocusDistanceVisible = true)
    }

    fun onCloseFocusDistance() {
        uiState = uiState.copy(isFocusDistanceVisible = false)
    }

    fun onOpenMacroFocus() {
        uiState = uiState.copy(isFocusDistanceVisible = false, isMacroFocusVisible = true,
            isMagnificationSettingsVisible = true, isDiffractionInfoVisible = false,
            macroForm = newMacroForm())
        uiState = refreshMacroForm(uiState)
    }

    fun onClearMacroFocus() = onUseAutoMagnification()
    fun onCloseMacroFocus() = onCloseMagnificationSettings()
    fun onMacroFocusChanged(text: String) = onManualMagnificationChanged(text)
    fun onApplyMacroFocus() = onApplyMacroSettings()

    fun onOpenDiffractionInfo() { uiState = uiState.copy(isDiffractionInfoVisible = true) }
    fun onCloseDiffractionInfo() { uiState = uiState.copy(isDiffractionInfoVisible = false) }

    fun onOpenMagnificationSettings() {
        uiState = uiState.copy(isMagnificationSettingsVisible = true, isMacroFocusVisible = false,
            isDiffractionInfoVisible = false, isSettingsVisible = false,
            macroForm = newMacroForm())
        uiState = refreshMacroForm(uiState)
    }

    fun onCloseMagnificationSettings() {
        uiState = uiState.copy(isMagnificationSettingsVisible = false,
            isFocusDistanceVisible = uiState.isMacroFocusVisible, isMacroFocusVisible = false)
    }

    private fun plain(value: Double): String =
        java.math.BigDecimal.valueOf(value).stripTrailingZeros().toPlainString()

    private fun newMacroForm(): MacroFormUiState {
        val m = uiState.diffractionSettings.manualMagnification
        val a = if (m < 1.0 && (1.0 / m).isFinite()) "1" else plain(m)
        val b = if (m < 1.0 && (1.0 / m).isFinite()) plain(1.0 / m) else "1"
        // Require an explicit measured distance for each new editing session.
        return MacroFormUiState(a, b)
    }

    private fun refreshMacroForm(state: DofCalculatorUiState): DofCalculatorUiState {
        val form = state.macroForm
        val validation = ThinLensEquivalentCalculator.validate(form.numerator, form.denominator,
            form.distanceMeters, state.focalLengthText.toPositiveDecimalOrNull(),
            state.apertureText.toPositiveDecimalOrNull())
        return state.copy(macroForm = form.copy(validation = validation,
            pendingConfirmation = form.pendingConfirmation.takeIf { it == validation.preview }))
    }

    /** Field edits are unapplied drafts; they never alter the main calculator. */
    fun onMacroFieldChanged(field: MacroInputField, text: String) {
        val form = uiState.macroForm.copy(pendingConfirmation = null)
        uiState = refreshMacroForm(uiState.copy(macroForm = when (field) {
            MacroInputField.NUMERATOR -> form.copy(numerator = text)
            MacroInputField.DENOMINATOR -> form.copy(denominator = text)
            MacroInputField.DISTANCE -> form.copy(distanceMeters = text)
        }))
    }

    fun onManualMagnificationChanged(text: String) {
        val parts = text.split(':')
        uiState = refreshMacroForm(uiState.copy(macroForm = uiState.macroForm.copy(
            numerator = parts.first(), denominator = if (parts.size == 1) "1"
                else if (parts.size == 2) parts[1] else "", pendingConfirmation = null)))
    }

    fun onRestoreManualMacro() {
        val saved = uiState.diffractionSettings.manualDistanceMeters ?: return
        uiState = refreshMacroForm(uiState.copy(macroForm = newMacroForm()
            .copy(distanceMeters = plain(saved))))
    }

    fun onApplyMacroSettings() = applyMacroSettings(confirmUnusual = false)
    fun onConfirmMacroSettings() = applyMacroSettings(confirmUnusual = true)
    fun onCancelMacroConfirmation() {
        uiState = uiState.copy(macroForm = uiState.macroForm.copy(pendingConfirmation = null))
    }

    private fun applyMacroSettings(confirmUnusual: Boolean) {
        uiState = refreshMacroForm(uiState)
        val form = uiState.macroForm
        val preview = form.validation.preview ?: return
        if (preview.unusual && (!confirmUnusual || form.pendingConfirmation != preview)) {
            uiState = uiState.copy(macroForm = form.copy(pendingConfirmation = preview))
            return
        }
        val settings = uiState.diffractionSettings.copy(
            magnificationMode = MagnificationMode.MANUAL, manualMagnification = preview.magnification,
            manualDistanceMeters = preview.distanceMeters, manualFocalLengthMm = preview.nominalFocalLengthMm,
            manualConfirmed = true)
        uiState = recalculate(uiState.copy(diffractionSettings = settings,
            calculatorMode = CalculatorMode.DepthOfField, focusAtInfinity = false,
            focusDistanceText = plain(preview.distanceMeters), focusInputDraft = null))
        launchSettingsOperation { appSettingsRepository.saveDiffractionSettings(settings) }
        onCloseMagnificationSettings()
    }

    fun onUseAutoMagnification() {
        updateDiffractionSettings(uiState.diffractionSettings.copy(magnificationMode = MagnificationMode.AUTO))
    }

    fun onMacroWarningsEnabled(enabled: Boolean) {
        uiState = uiState.copy(macroWarningsEnabled = enabled)
        launchSettingsOperation { appSettingsRepository.setMacroWarningsEnabled(enabled) }
    }

    fun onDiffractionWarningsEnabled(enabled: Boolean) {
        updateDiffractionSettings(uiState.diffractionSettings.copy(warningsEnabled = enabled))
    }

    private fun updateDiffractionSettings(settings: DiffractionSettings) {
        uiState = withDiffraction(uiState.copy(diffractionSettings = settings))
        val evaluated = uiState.diffractionSettings
        launchSettingsOperation { appSettingsRepository.saveDiffractionSettings(evaluated) }
    }

    /** Pure one-way branch: nominal focal length and geometric DOF remain untouched. */
    private fun withDiffraction(state: DofCalculatorUiState): DofCalculatorUiState {
        val f = state.focalLengthText.toPositiveDecimalOrNull()
        val n = state.apertureText.toPositiveDecimalOrNull()
        val distance = state.focusDistance.takeUnless {
            state.focusDistanceHasError || (state.calculatorMode == CalculatorMode.Hyperfocal && state.result == null)
        }
        val assessment = ManualMagnificationValidator.assess(state.diffractionSettings, distance, f)
        val settings = if (state.diffractionSettings.manualConfirmed &&
            assessment.status != ManualMagnificationStatus.VALID)
            state.diffractionSettings.copy(manualConfirmed = false) else state.diffractionSettings
        val estimate = if (f == null || distance == null) MagnificationEstimate(null,
            suspension = ManualMagnificationSuspension.NEEDS_CONFIRMATION.takeIf {
                settings.magnificationMode == MagnificationMode.MANUAL })
            else MagnificationEstimator.resolve(f, distance, settings,
                state.calculatorMode == CalculatorMode.Hyperfocal)
        val result = if (n == null || state.effectiveCoCMm == null) null else runCatching {
            DiffractionCalculator.calculate(n, state.effectiveCoCMm, estimate)
        }.getOrNull()
        val macroInput = MacroDetectionInput(f, distance, estimate,
            ordinaryDof = state.calculatorMode == CalculatorMode.DepthOfField)
        // Keep the derived key and result in the same immutable snapshot. A delayed settings
        // emission must not replace the memo for a newer distance edit in progress.
        val macroDetection = if (macroInput == state.macroDetectionInput) state.macroDetection
            else if (!macroInput.ordinaryDof) MacroDetectionState.None
            else com.example.dofcalculator.calculation.MacroWarningEvaluator.evaluate(f, distance, estimate)
        val updated = state.copy(diffractionSettings = settings, diffractionResult = result,
            magnificationEstimate = estimate, manualMagnificationAssessment = assessment,
            macroDetection = macroDetection, macroDetectionInput = macroInput)
        return if (state.isMagnificationSettingsVisible) refreshMacroForm(updated) else updated
    }

    fun onOpenSettings() {
        uiState = uiState.copy(isSettingsVisible = true)
    }

    fun onCloseSettings() {
        uiState = uiState.copy(isSettingsVisible = false)
    }

    fun onOpenAbout() {
        uiState = uiState.copy(
            isSettingsVisible = false,
            isAboutVisible = true,
        )
    }

    fun onCloseAbout() {
        uiState = uiState.copy(isAboutVisible = false)
    }

    fun onDismissSettingsStorageError() {
        uiState = uiState.copy(settingsStorageError = false)
    }

    fun onOpenPresetManager() {
        uiState = uiState.copy(
            presets = uiState.presets.copy(
                isManagerVisible = true,
                storageError = false,
            ),
        )
    }

    fun onClosePresetManager() {
        uiState = uiState.copy(
            presets = uiState.presets.copy(
                isManagerVisible = false,
                cameraEditor = null,
                storageError = false,
            ),
        )
    }

    fun onCreateCamera() {
        uiState = uiState.copy(
            presets = uiState.presets.copy(
                cameraEditor = CameraEditorUiState(
                    frameFormat = uiState.selectedFrameFormat,
                ),
            ),
        )
    }

    fun onEditCamera(cameraId: String) {
        val camera = uiState.presets.cameras
            .firstOrNull { it.id == cameraId }
            ?.takeUnless { it.isBuiltIn }
            ?: return
        uiState = uiState.copy(
            presets = uiState.presets.copy(
                cameraEditor = CameraEditorUiState(
                    cameraId = camera.id,
                    nameText = camera.name,
                    frameFormat = camera.frameFormat,
                    widthText = camera.widthMm?.toString().orEmpty(),
                    heightText = camera.heightMm?.toString().orEmpty(),
                    customFormatName = camera.customFormatName,
                ),
            ),
        )
    }

    fun onCameraEditorNameChanged(value: String) {
        updateCameraEditor {
            copy(nameText = value, nameHasError = false)
        }
    }

    fun onCameraEditorFrameFormatSelected(frameFormat: FrameFormat) {
        updateCameraEditor {
            copy(
                frameFormat = frameFormat,
                dimensionsHaveError = false,
            )
        }
    }

    fun onCameraDimensionsChanged(width: Boolean, value: String) {
        updateCameraEditor {
            if (width) copy(widthText = value, dimensionsHaveError = false)
            else copy(heightText = value, dimensionsHaveError = false)
        }
    }

    fun onSaveCameraEditor() {
        val editor = uiState.presets.cameraEditor ?: return
        val custom = editor.frameFormat == FrameFormat.Custom
        val width = if (custom) SharpnessCriterionCalculator.positiveNumber(editor.widthText)
            else editor.frameFormat.widthMm
        val height = if (custom) SharpnessCriterionCalculator.positiveNumber(editor.heightText)
            else editor.frameFormat.heightMm
        val dimensionsError = width == null || height == null ||
            !((width * height).isFinite())
        if (editor.nameText.isBlank() || dimensionsError ||
            (custom && editor.customFormatName.isBlank())) {
            updateCameraEditor { copy(nameHasError = nameText.isBlank(),
                dimensionsHaveError = dimensionsError,
                customFormatHasError = custom && customFormatName.isBlank()) }
            return
        }
        val camera = CameraProfile(
            id = editor.cameraId ?: UUID.randomUUID().toString(),
            name = editor.nameText.trim(), frameFormat = editor.frameFormat,
            widthMm = width, heightMm = height,
            customFormatName = editor.customFormatName.trim(),
        )
        uiState = uiState.copy(presets = uiState.presets.copy(cameraEditor = null))
        launchRepositoryOperation { presetRepository.saveCamera(camera) }
    }

    fun onCancelCameraEditor() {
        uiState = uiState.copy(
            presets = uiState.presets.copy(cameraEditor = null),
        )
    }

    fun onDeleteCamera(cameraId: String) {
        if (uiState.presets.cameras.any { it.id == cameraId && !it.isBuiltIn }) {
            uiState = uiState.copy(presets = uiState.presets.copy(pendingDeleteId = cameraId))
        }
    }

    fun onCancelDeleteCamera() {
        uiState = uiState.copy(presets = uiState.presets.copy(pendingDeleteId = null))
    }

    fun onConfirmDeleteCamera() {
        val id = uiState.presets.pendingDeleteId ?: return
        onCancelDeleteCamera()
        launchRepositoryOperation { presetRepository.deleteCamera(id) }
    }

    fun onCameraCustomFormatNameChanged(value: String) {
        updateCameraEditor { copy(customFormatName = value, customFormatHasError = false) }
    }

    fun onDismissPresetStorageError() {
        uiState = uiState.copy(
            presets = uiState.presets.copy(storageError = false),
        )
    }

    override fun onCleared() {
        coroutineScope.cancel()
        super.onCleared()
    }

    private fun onSettingsChanged(settings: AppSettings) {
        if (!settingsRestored) {
            settingsRestored = true
            val calculator = settings.calculator
            sharpnessMigrationComplete = settings.sharpnessMigrated
            uiState = recalculate(
                uiState.copy(
                    sharpness = settings.sharpness,
                    legacyCalculatorCoCmm = settings.legacyCoCForMigration
                        ?.trim()?.replace(',', '.')?.toDoubleOrNull()
                        ?.takeIf { it.isFinite() && it > 0.0 },
                    diffractionSettings = settings.diffraction,
                    macroWarningsEnabled = settings.macroWarningsEnabled,
                    apertureIncrements = settings.apertureIncrements,
                    calculatorMode = calculator.mode,
                    selectedFrameFormat = calculator.frameFormat,
                    circleOfConfusionText =
                        calculator.circleOfConfusionText,
                    focalLengthText = calculator.focalLengthText,
                    apertureText = calculator.apertureText,
                    focusDistanceText = calculator.focusDistanceText,
                    focusAtInfinity = calculator.focusAtInfinity,
                    distanceUnit = settings.distanceUnit,
                    distanceRounding = settings.distanceRounding,
                    themeMode = settings.themeMode,
                    language = settings.language,
                ),
            )
            applySelectedCamera()
        } else {
            val incrementsChanged = uiState.apertureIncrements != settings.apertureIncrements
            // Diffraction inputs, like sharpness inputs, are restored once. A delayed
            // DataStore snapshot must not replace a newer manual edit in progress.
            val updated = uiState.copy(
                macroWarningsEnabled = settings.macroWarningsEnabled,
                apertureIncrements = settings.apertureIncrements,
                distanceUnit = settings.distanceUnit,
                distanceRounding = settings.distanceRounding,
                themeMode = settings.themeMode,
                language = settings.language,
            )
            uiState = if (incrementsChanged) recalculate(updated) else withDiffraction(updated)
        }
    }

    private fun onPresetLibraryChanged(library: PresetLibrary) {
        libraryRestored = true
        uiState = uiState.copy(
            lensManager = uiState.lensManager.takeIf { manager ->
                manager.cameraId == null || library.cameras.any { it.id == manager.cameraId }
            } ?: LensManagerUiState(),
            legacyCoCByCameraId = library.legacyCoCByCameraId,
            presets = uiState.presets.copy(
                cameras = library.cameras,
                selectedCameraId = library.selectedCameraId,
                lensKitsByCameraId = library.lensKitsByCameraId,
            ),
        )
        applySelectedCamera()
    }

    private fun applySelectedCamera() {
        val camera = uiState.presets.cameras.firstOrNull {
            it.id == uiState.presets.selectedCameraId
        } ?: return
        if (settingsRestored && libraryRestored && !sharpnessMigrationComplete) {
            // Keep the selected criterion and Custom slot unchanged. Legacy values remain
            // archived and are imported only by an explicit action in the sharpness sheet.
            sharpnessMigrationComplete = true
            val currentSharpness = uiState.sharpness
            launchSettingsOperation { appSettingsRepository.saveSharpnessSettings(currentSharpness) }
        }
        uiState = recalculate(uiState.copy(selectedFrameFormat = camera.frameFormat))
    }

    private fun updateCameraEditor(
        transform: CameraEditorUiState.() -> CameraEditorUiState,
    ) {
        val editor = uiState.presets.cameraEditor ?: return
        uiState = uiState.copy(
            presets = uiState.presets.copy(
                cameraEditor = editor.transform(),
            ),
        )
    }

    private fun launchRepositoryOperation(
        operation: suspend () -> Unit,
    ) {
        coroutineScope.launch {
            runCatching {
                operation()
            }.onFailure {
                uiState = uiState.copy(
                    presets = uiState.presets.copy(storageError = true),
                )
            }
        }
    }

    private fun launchSettingsOperation(
        operation: suspend () -> Unit,
    ) {
        coroutineScope.launch {
            runCatching {
                operation()
            }.onFailure {
                uiState = uiState.copy(settingsStorageError = true)
            }
        }
    }

    private fun persistCalculatorSettings(state: DofCalculatorUiState) {
        launchSettingsOperation {
            appSettingsRepository.saveCalculatorSettings(
                CalculatorSettings(
                    mode = state.calculatorMode,
                    frameFormat = state.selectedFrameFormat,
                    circleOfConfusionText = state.circleOfConfusionText,
                    focalLengthText = state.focalLengthText,
                    apertureText = state.apertureText,
                    focusDistanceText = lastValidFocusDistanceText,
                    focusAtInfinity = state.focusAtInfinity,
                ),
            )
        }
    }

    private fun stepAperture(forward: Boolean) {
        val currentAperture =
            uiState.apertureText.toPositiveDecimalOrNull() ?: return
        val targetAperture = if (forward) {
            uiState.aperturePresets.firstOrNull {
                it > (currentAperture + PRESET_TOLERANCE)
            }
        } else {
            uiState.aperturePresets.lastOrNull {
                it < (currentAperture - PRESET_TOLERANCE)
            }
        }

        targetAperture?.let(::onAperturePresetSelected)
    }

    private fun recalculate(
        input: DofCalculatorUiState,
    ): DofCalculatorUiState {
        // Hyperfocal switches manual Macro off; leaving it must not resurrect that mode.
        // Keep the saved pair available for an explicit later Apply, including after a restart.
        val normalized = if (input.calculatorMode == CalculatorMode.Hyperfocal &&
            input.diffractionSettings.magnificationMode == MagnificationMode.MANUAL)
            input.copy(diffractionSettings = input.diffractionSettings.copy(magnificationMode = MagnificationMode.AUTO))
        else input
        val kit = normalized.cameraLensKit
        val candidate = normalized.copy(
            focalLengthPresets = com.example.dofcalculator.calculation.FocalLengthPresetsGenerator.generate(
                kit.lenses, CalculatorPresets.typicalFocalLengthsMm.map(Int::toDouble), kit.showAllFocalLengths),
            aperturePresets = com.example.dofcalculator.calculation.AperturePresetsGenerator.generate(
                kit.lenses, normalized.apertureIncrements.apertures, kit.showAllApertures),
        )
        if (candidate.focusDistanceText.toPositiveDecimalOrNull() != null) {
            lastValidFocusDistanceText = candidate.focusDistanceText
        }
        val hasInvalidDraft = SharpnessField.active(candidate.sharpness).any { field ->
            candidate.sharpnessDrafts[field]?.let {
                !field.isValid(SharpnessCriterionCalculator.positiveNumber(it))
            } == true
        }
        val camera = candidate.presets.cameras.firstOrNull {
            it.id == candidate.presets.selectedCameraId && it.frameFormat == candidate.selectedFrameFormat
        } ?: CameraProfile("calculation", "calculation", candidate.selectedFrameFormat,
            customFormatName = "custom")
        // Optional print metadata never invalidates a sound optical criterion.
        val forCalculation = candidate.sharpness.copy(
            printPpi = candidate.sharpnessDrafts[SharpnessField.PRINT_PPI]?.let {
                SharpnessCriterionCalculator.positiveNumber(it)
            } ?: candidate.sharpness.printPpi.takeIf {
                SharpnessField.PRINT_PPI !in candidate.sharpnessDrafts
            },
            printDpi = candidate.sharpnessDrafts[SharpnessField.PRINT_DPI]?.let {
                SharpnessCriterionCalculator.positiveNumber(it)
            } ?: candidate.sharpness.printDpi.takeIf {
                SharpnessField.PRINT_DPI !in candidate.sharpnessDrafts
            },
        )
        val output = if (hasInvalidDraft) null else runCatching {
            when (forCalculation.mode) {
                SharpnessCriterionMode.PRINT_BASED -> com.example.dofcalculator.calculation.OutputSharpnessCalculator
                    .print(requireNotNull(camera.widthMm), requireNotNull(camera.heightMm), forCalculation)
                SharpnessCriterionMode.SCREEN_BASED -> com.example.dofcalculator.calculation.OutputSharpnessCalculator
                    .screen(requireNotNull(camera.widthMm), requireNotNull(camera.heightMm), forCalculation)
                else -> null
            }
        }.getOrNull()
        val videoMode = forCalculation.mode == SharpnessCriterionMode.VIDEO_BASED
        val invalidVideoDrafts = if (!videoMode) emptySet() else
            com.example.dofcalculator.model.VideoField.active(forCalculation.video).filter { field ->
                candidate.videoDrafts[field]?.let {
                    com.example.dofcalculator.calculation.VideoCoCCalculator.parseField(field, it) == null
                } == true
            }.toSet()
        val videoCalculation = if (!videoMode || invalidVideoDrafts.isNotEmpty()) null else runCatching {
            com.example.dofcalculator.calculation.VideoCoCCalculator.calculate(
                camera.widthMm, camera.heightMm, forCalculation.video)
        }
        val video = videoCalculation?.getOrNull()
        val videoFailure = videoCalculation?.exceptionOrNull() as?
            com.example.dofcalculator.calculation.VideoInputException
        val videoError = invalidVideoDrafts.firstOrNull()?.let {
            com.example.dofcalculator.calculation.VideoCoCCalculator.fieldError(it)
        } ?: videoFailure?.error
        val standard = if (hasInvalidDraft || forCalculation.mode != SharpnessCriterionMode.STANDARD) null
            else runCatching { StandardCoCResolver.resolve(camera) }.getOrNull()
        val coc = if (hasInvalidDraft) null else when (forCalculation.mode) {
            SharpnessCriterionMode.STANDARD -> standard?.coCmm
            SharpnessCriterionMode.VIDEO_BASED -> video?.effectiveCoCmm
            SharpnessCriterionMode.PRINT_BASED, SharpnessCriterionMode.SCREEN_BASED -> output?.effectiveCoCmm
            else -> runCatching { SharpnessCriterionCalculator.calculate(camera, forCalculation) }.getOrNull()
        }
        val calculatedState = calculateState(candidate.copy(effectiveCoCMm = coc, outputSharpness = output,
            standardCoC = standard,
            videoResult = video, videoError = videoError,
            videoInvalidFields = invalidVideoDrafts + videoFailure?.fields.orEmpty(),
            sharpnessCalculationFailed = !hasInvalidDraft && coc == null &&
                camera.widthMm != null && camera.heightMm != null,
            circleOfConfusionText = if (candidate.sharpness.mode == SharpnessCriterionMode.CUSTOM)
                candidate.sharpnessDrafts[SharpnessField.CUSTOM_COC] ?: coc?.toString().orEmpty()
            else coc?.toString().orEmpty()))
        val scale = runCatching {
            val f = candidate.focalLengthText.toPositiveDecimalOrNull() ?: return@runCatching null
            val n = candidate.apertureText.toPositiveDecimalOrNull() ?: return@runCatching null
            val c = coc ?: return@runCatching null
            FocusDistanceScale.create(
                DepthOfFieldCalculator.calculateHyperfocalDistanceMeters(f, n, c),
            )
        }.getOrNull()
        val calculated = calculatedState.copy(distanceScale = scale,
            distanceSliderPosition = calculatedState.focusDistance?.let {
                scale?.positionOf(it)
            } ?: 0.0)
        val evaluated = withDiffraction(calculated)
        if (settingsRestored && evaluated.diffractionSettings != input.diffractionSettings) {
            launchSettingsOperation { appSettingsRepository.saveDiffractionSettings(evaluated.diffractionSettings) }
        }
        // A measured Manual distance can be shorter than the nominal-lens DOF model permits.
        // Keep genuine optical inputs even when that geometric result is unavailable.
        val measuredInputs = evaluated.diffractionSettings.manualDistanceMeters != null &&
            candidate.focalLengthText.toPositiveDecimalOrNull() != null &&
            candidate.apertureText.toPositiveDecimalOrNull() != null &&
            (candidate.focusAtInfinity || candidate.focusDistanceText.toPositiveDecimalOrNull() != null)
        if (settingsRestored && sharpnessMigrationComplete && (evaluated.result != null || measuredInputs)) {
            persistCalculatorSettings(evaluated)
        }
        return evaluated
    }

    private fun calculateState(
        candidate: DofCalculatorUiState,
    ): DofCalculatorUiState {
        val focalLengthMm = candidate.focalLengthText.toPositiveDecimalOrNull()
        val aperture = candidate.apertureText.toPositiveDecimalOrNull()
        val focusDistanceMeters =
            candidate.focusDistanceText.toPositiveDecimalOrNull()
        val circleOfConfusionMm = candidate.effectiveCoCMm

        val focalLengthHasError = focalLengthMm == null
        val apertureHasError = aperture == null
        val focusDistanceHasError =
            (candidate.calculatorMode == CalculatorMode.DepthOfField) &&
                (focusDistanceMeters == null) && !candidate.focusAtInfinity
        val circleOfConfusionHasError = circleOfConfusionMm == null

        val candidateWithControlState = candidate.copy(
            focusDistanceMeters = focusDistanceMeters,
            focusDistance = if (candidate.focusAtInfinity) CalculatedDistance.Infinity
                else focusDistanceMeters?.let { CalculatedDistance.Finite(it) },
            selectedFocalLengthPresetMm =
                candidate.focalLengthPresets.firstOrNull { it == focalLengthMm },
            canDecreaseFocalLength =
                focalLengthMm?.let { it > 1.0 && it - 1.0 < it } == true,
            canIncreaseFocalLength =
                focalLengthMm?.let { (it + 1.0).isFinite() && it + 1.0 > it } == true,
            selectedStandardAperture =
                candidate.aperturePresets.firstOrNull { it == aperture },
            canSelectPreviousAperture =
                aperture?.let { value ->
                    candidate.aperturePresets.any {
                        it < (value - PRESET_TOLERANCE)
                    }
                } == true,
            canSelectNextAperture =
                aperture?.let { value ->
                    candidate.aperturePresets.any {
                        it > (value + PRESET_TOLERANCE)
                    }
                } == true,
        )

        if (
            focalLengthHasError ||
            apertureHasError ||
            focusDistanceHasError ||
            circleOfConfusionHasError
        ) {
            return candidateWithControlState.copy(
                result = null,
                focalLengthHasError = focalLengthHasError,
                apertureHasError = apertureHasError,
                focusDistanceHasError = focusDistanceHasError,
                circleOfConfusionHasError = circleOfConfusionHasError,
                inputError = DofInputError.InvalidNumber,
            )
        }

        return try {
            val calculationFocusDistanceMeters =
                if (candidate.calculatorMode == CalculatorMode.Hyperfocal) {
                    DepthOfFieldCalculator
                        .calculateHyperfocalDistanceMeters(
                            focalLengthMm = focalLengthMm,
                            aperture = aperture,
                            circleOfConfusionMm = circleOfConfusionMm,
                        )
                } else {
                    focusDistanceMeters
                }
            val result = if (candidate.calculatorMode == CalculatorMode.DepthOfField && candidate.focusAtInfinity) {
                DepthOfFieldCalculator.calculateAtInfinity(focalLengthMm, aperture, circleOfConfusionMm)
            } else DepthOfFieldCalculator.calculate(
                DepthOfFieldInput(
                    focalLengthMm = focalLengthMm,
                    aperture = aperture,
                    circleOfConfusionMm = circleOfConfusionMm,
                    focusDistanceMeters = checkNotNull(calculationFocusDistanceMeters),
                ),
            )
            candidateWithControlState.copy(
                result = result,
                focusDistanceMeters = if (candidate.calculatorMode == CalculatorMode.DepthOfField && candidate.focusAtInfinity)
                    null else calculationFocusDistanceMeters,
                focusDistance = if (candidate.calculatorMode == CalculatorMode.DepthOfField && candidate.focusAtInfinity)
                    CalculatedDistance.Infinity else CalculatedDistance.Finite(checkNotNull(calculationFocusDistanceMeters)),
                focalLengthHasError = false,
                apertureHasError = false,
                focusDistanceHasError = false,
                circleOfConfusionHasError = false,
                inputError = null,
            )
        } catch (_: IllegalArgumentException) {
            candidateWithControlState.copy(
                result = null,
                focalLengthHasError = false,
                apertureHasError = false,
                focusDistanceHasError = false,
                circleOfConfusionHasError = false,
                inputError = DofInputError.OutOfRange,
            )
        }
    }

    private fun String.toPositiveDecimalOrNull(): Double? {
        val trimmed = trim()
        if (
            trimmed.isEmpty() ||
            trimmed.endsWith('.') ||
            trimmed.endsWith(',')
        ) {
            return null
        }

        if (trimmed.any { character ->
                !character.isDigit() &&
                    (character != '.') &&
                    (character != ',')
            }
        ) {
            return null
        }

        val separatorCount = trimmed.count { (it == '.') || (it == ',') }
        if (separatorCount > 1) {
            return null
        }

        return trimmed
            .replace(',', '.')
            .toDoubleOrNull()
            ?.takeIf { it.isFinite() && (it > 0.0) }
    }

    private fun Double.toPresetInput(): String =
        java.math.BigDecimal.valueOf(this).stripTrailingZeros().toPlainString()

    private fun Double.toCircleOfConfusionInput(): String =
        String.format(Locale.ROOT, "%.3f", this).replace('.', ',')

    companion object {
        private const val PRESET_TOLERANCE = 1e-9

        fun factory(
            presetRepository: PresetRepository,
            appSettingsRepository: AppSettingsRepository,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(
                    modelClass: Class<T>,
                ): T {
                    require(
                        modelClass.isAssignableFrom(
                            DofCalculatorViewModel::class.java,
                        ),
                    )
                    return DofCalculatorViewModel(
                        presetRepository,
                        appSettingsRepository,
                    ) as T
                }
            }
    }
}
