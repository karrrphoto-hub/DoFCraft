package com.example.dofcalculator.ui.calculator

import com.example.dofcalculator.data.*
import com.example.dofcalculator.model.*
import com.example.dofcalculator.settings.*
import kotlinx.coroutines.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Test

class LensDistanceUnitsViewModelTest {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
    private val camera = CameraProfile("unit-camera", "My camera", FrameFormat.FullFrame35Mm)
    private val cameras = InMemoryPresetRepository(PresetLibrary(
        cameras = BuiltInCameraProfiles.cameras + camera, selectedCameraId = camera.id))
    private val preferences = InMemoryAppSettingsRepository()
    private fun vm() = DofCalculatorViewModel(cameras, preferences, scope)

    @After fun cleanup() { scope.cancel() }

    private fun completePrime(vm: DofCalculatorViewModel) {
        vm.onLensFieldChanged(LensField.PRIME_FOCAL, "58")
        vm.onLensFieldChanged(LensField.PRIME_APERTURE, "1.7")
    }

    @Test fun `each new lens starts with the app unit family and a saved choice survives global changes`() {
        val vm = vm()
        vm.onOpenCameraLenses(camera.id)
        for (unit in DistanceUnit.entries) {
            vm.onDistanceUnitSelected(unit)
            vm.onAddLens()
            assertEquals(unit.lensDistanceUnit(), vm.uiState.lensManager.editor!!.draft.distanceScaleUnit)
            completePrime(vm)
            vm.onSaveLens()
        }
        val before = vm.uiState.cameraLensKit
        assertEquals(listOf(LensDistanceUnit.Meters, LensDistanceUnit.Meters,
            LensDistanceUnit.Feet, LensDistanceUnit.Feet), before.lenses.map { it.distanceScaleUnit })
        vm.onDistanceUnitSelected(DistanceUnit.Meters)
        assertEquals(before, vm.uiState.cameraLensKit)
        assertEquals(before, vm().uiState.cameraLensKit)
    }

    @Test fun `changing the scale edits only that lens and preserves all current calculations`() = runBlocking {
        cameras.saveLens(camera.id, Lens.Prime("metric", "Metric", 50.0, 1.4,
            distanceScaleUnit = LensDistanceUnit.Meters))
        cameras.saveLens(camera.id, Lens.Prime("feet", "Feet", 58.0, 1.7,
            distanceScaleUnit = LensDistanceUnit.Meters))
        val vm = vm()
        vm.onFocalLengthChanged("80")
        vm.onApertureChanged("11")
        vm.onFocusDistanceChanged("12.192")
        vm.onCircleOfConfusionChanged(".00876")
        vm.onOpenCameraLenses(camera.id)
        val before = vm.uiState
        vm.onEditLens("feet")
        vm.onLensDistanceUnitChanged(LensDistanceUnit.Feet)
        vm.onLensZoomChanged(true)
        assertEquals(LensDistanceUnit.Feet, vm.uiState.lensManager.editor!!.draft.distanceScaleUnit)
        vm.onLensZoomChanged(false)
        vm.onSaveLens()
        assertEquals(listOf(LensDistanceUnit.Meters, LensDistanceUnit.Feet),
            vm.uiState.cameraLensKit.lenses.map { it.distanceScaleUnit })
        assertEquals(before.focalLengthText, vm.uiState.focalLengthText)
        assertEquals(before.apertureText, vm.uiState.apertureText)
        assertEquals(before.focusDistance, vm.uiState.focusDistance)
        assertEquals(before.sharpness, vm.uiState.sharpness)
        assertEquals(before.result, vm.uiState.result)
        assertEquals(before.diffractionResult, vm.uiState.diffractionResult)
        assertEquals(before.distanceScale, vm.uiState.distanceScale)
        vm.onFocusAtInfinity()
        vm.onEditLens("feet")
        vm.onLensDistanceUnitChanged(LensDistanceUnit.Meters)
        vm.onSaveLens()
        assertSame(CalculatedDistance.Infinity, vm.uiState.focusDistance)
    }

    @Test fun `legacy lens defaults are resolved on editing and cancel does not rewrite its record`() = runBlocking {
        val legacy = Lens.Zoom("legacy", minFocalLengthMm = 18.0, maxFocalLengthMm = 55.0,
            maxApertureAtMinFocal = 3.5, maxApertureAtMaxFocal = 5.6)
        cameras.saveLens(camera.id, legacy)
        val vm = vm()
        vm.onDistanceUnitSelected(DistanceUnit.Feet)
        vm.onOpenCameraLenses(camera.id)
        vm.onEditLens(legacy.id)
        assertEquals(LensDistanceUnit.Feet, vm.uiState.lensManager.editor!!.draft.distanceScaleUnit)
        vm.onLensDistanceUnitChanged(LensDistanceUnit.Meters)
        vm.onCancelLensEditor()
        assertEquals(legacy, vm.uiState.cameraLensKit.lenses.single())
        vm.onEditLens(legacy.id)
        vm.onSaveLens()
        assertEquals(legacy.copy(distanceScaleUnit = LensDistanceUnit.Feet), vm.uiState.cameraLensKit.lenses.single())
    }
}
