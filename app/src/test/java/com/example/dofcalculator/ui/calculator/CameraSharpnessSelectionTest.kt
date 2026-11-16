package com.example.dofcalculator.ui.calculator

import com.example.dofcalculator.data.*
import com.example.dofcalculator.model.*
import com.example.dofcalculator.settings.*
import kotlinx.coroutines.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Test

class CameraSharpnessSelectionTest {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
    private val camera = CameraProfile("user", "My camera", FrameFormat.Custom,
        customFormatName = "Custom frame", widthMm = 36.0, heightMm = 24.0)
    private val other = camera.copy(id = "other", name = "Same dimensions, different camera")
    private val cameras = InMemoryPresetRepository(PresetLibrary(
        cameras = BuiltInCameraProfiles.cameras + listOf(camera, other),
        selectedCameraId = camera.id))
    private val saved = SharpnessSettings(megapixels = 60.0, customCoCmm = .00876)
    private val settings = InMemoryAppSettingsRepository(AppSettings(
        sharpnessMigrated = true, sharpness = saved))

    private fun viewModel() = DofCalculatorViewModel(cameras, settings, scope)

    @After fun cleanup() { scope.cancel() }

    @Test fun `different camera selects and saves Standard in both calculator modes without erasing parameters`() {
        val vm = viewModel()
        vm.onFocalLengthChanged("100")
        vm.onApertureChanged("11")
        vm.onFocusDistanceChanged("7.25")
        for (calculatorMode in CalculatorMode.entries) {
            vm.onCalculatorModeSelected(calculatorMode)
            for (criterion in SharpnessCriterionMode.entries) {
                vm.onCameraProfileSelected(camera.id)
                vm.onSharpnessModeSelected(criterion)
                val before = vm.uiState
                vm.onCameraProfileSelected("builtin.8x10")
                assertEquals(before.sharpness.copy(mode = SharpnessCriterionMode.STANDARD),
                    vm.uiState.sharpness)
                assertEquals(.220, vm.uiState.effectiveCoCMm!!, 0.0)
                assertEquals(vm.uiState.sharpness, settings.settings.value.sharpness)
                assertEquals(calculatorMode, vm.uiState.calculatorMode)
                assertEquals(before.focalLengthText, vm.uiState.focalLengthText)
                assertEquals(before.apertureText, vm.uiState.apertureText)
                assertEquals(before.focusDistanceText, vm.uiState.focusDistanceText)
            }
        }
        vm.onCalculatorModeSelected(CalculatorMode.DepthOfField)
        vm.onFocusAtInfinity()
        vm.onSharpnessModeSelected(SharpnessCriterionMode.CUSTOM)
        vm.onCameraProfileSelected(camera.id)
        assertSame(CalculatedDistance.Infinity, vm.uiState.focusDistance)
        assertEquals(1.0, vm.uiState.distanceSliderPosition, 0.0)
    }

    @Test fun `same camera and unknown ID keep criterion but repository ID changes reset even for identical geometry`() = runBlocking {
        val vm = viewModel()
        vm.onSharpnessModeSelected(SharpnessCriterionMode.CUSTOM)
        vm.onCameraProfileSelected(camera.id)
        vm.onCameraProfileSelected("missing")
        assertEquals(saved.copy(mode = SharpnessCriterionMode.CUSTOM), vm.uiState.sharpness)
        cameras.selectCamera(other.id)
        assertEquals(other.id, vm.uiState.presets.selectedCameraId)
        assertEquals(saved, vm.uiState.sharpness)
        assertEquals(saved, settings.settings.value.sharpness)
    }

    @Test fun `editing active camera dimensions name and lenses keeps every chosen criterion`() = runBlocking {
        val vm = viewModel()
        for ((index, mode) in SharpnessCriterionMode.entries.withIndex()) {
            vm.onSharpnessModeSelected(mode)
            val before = vm.uiState.sharpness
            vm.onEditCamera(camera.id)
            vm.onCameraEditorNameChanged("Renamed $index")
            vm.onCameraDimensionsChanged(true, (36 + index).toString())
            vm.onCameraDimensionsChanged(false, "25")
            vm.onSaveCameraEditor()
            assertNull(vm.uiState.presets.cameraEditor)
            assertEquals("Renamed $index", vm.uiState.presets.cameras.single { it.id == camera.id }.name)
            cameras.saveLens(camera.id, Lens.Prime("lens",
                focalLengthMm = 50.0 + index, maxAperture = 1.7))
            vm.onShowAllFocalLengths(index % 2 == 0)
            assertEquals(camera.id, vm.uiState.presets.selectedCameraId)
            assertEquals(before, vm.uiState.sharpness)
            assertEquals(before, settings.settings.value.sharpness)
        }
    }

    @Test fun `restoring a saved camera preserves the saved criterion instead of resetting it on startup`() {
        for (mode in SharpnessCriterionMode.entries) {
            val preferences = InMemoryAppSettingsRepository(AppSettings(
                sharpnessMigrated = true, sharpness = saved.copy(mode = mode)))
            val restored = DofCalculatorViewModel(cameras, preferences, scope)
            assertEquals(camera.id, restored.uiState.presets.selectedCameraId)
            assertEquals(saved.copy(mode = mode), restored.uiState.sharpness)
        }
    }

    @Test fun `switching camera recovers Standard from an invalid Custom draft without discarding the draft`() {
        val vm = viewModel()
        vm.onCircleOfConfusionChanged("0,")
        assertNull(vm.uiState.result)
        vm.onCameraProfileSelected("builtin.8x10")
        assertEquals(SharpnessCriterionMode.STANDARD, vm.uiState.sharpness.mode)
        assertEquals(.220, vm.uiState.effectiveCoCMm!!, 0.0)
        assertNotNull(vm.uiState.result)
        assertEquals("0,", vm.uiState.sharpnessDrafts[SharpnessField.CUSTOM_COC])
        vm.onSharpnessModeSelected(SharpnessCriterionMode.CUSTOM)
        assertNull(vm.uiState.result)
        assertEquals(.00876, vm.uiState.sharpness.customCoCmm, 0.0)
    }
}
