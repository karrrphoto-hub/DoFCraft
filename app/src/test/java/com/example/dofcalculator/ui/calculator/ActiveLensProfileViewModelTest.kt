package com.example.dofcalculator.ui.calculator

import com.example.dofcalculator.data.*
import com.example.dofcalculator.model.*
import com.example.dofcalculator.settings.*
import kotlinx.coroutines.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Test

class ActiveLensProfileViewModelTest {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
    private val camera = CameraProfile("camera", "Camera", FrameFormat.FullFrame35Mm)
    private val prime = Lens.Prime("prime", "Prime", 58.25, 1.7, 16.0, LensDistanceUnit.Feet)
    private val zoom = Lens.Zoom("zoom", "Zoom", 24.0, 2.8, 70.0, 2.8, 22.0, LensDistanceUnit.Meters)
    private val cameras = InMemoryPresetRepository(PresetLibrary(
        cameras = BuiltInCameraProfiles.cameras + camera, selectedCameraId = camera.id,
        lensKitsByCameraId = mapOf(camera.id to CameraLensKit(listOf(prime, zoom), selectedLensId = zoom.id))))
    private val settings = InMemoryAppSettingsRepository(AppSettings(sharpnessMigrated = true))
    private fun vm() = DofCalculatorViewModel(cameras, settings, scope)
    @After fun cleanup() { scope.cancel() }

    @Test fun `selecting a lens changes both shortcut rows and bounds only incompatible optical values`() {
        val vm = vm()
        vm.onFocusDistanceChanged("12.192")
        vm.onCircleOfConfusionChanged(".00876")
        val sharpness = vm.uiState.sharpness
        vm.onLensSelected(prime.id)
        assertEquals("58.25", vm.uiState.focalLengthText)
        assertEquals("8", vm.uiState.apertureText)
        assertEquals(listOf(58.25), vm.uiState.focalLengthPresets)
        assertEquals(listOf(1.7, 2.0, 2.8, 4.0, 5.6, 8.0, 11.0, 16.0), vm.uiState.aperturePresets)
        assertEquals(DistanceUnit.Feet, vm.uiState.effectiveFocusDistanceUnit)
        vm.onLensSelected(zoom.id)
        assertEquals("58.25", vm.uiState.focalLengthText) // no rounding to a standard button
        assertTrue(vm.uiState.focalLengthPresets.all { it in 24.0..70.0 })
        vm.onFocalLengthChanged("300")
        vm.onApertureChanged(".95")
        vm.onLensSelected(zoom.id)
        assertEquals("70", vm.uiState.focalLengthText)
        assertEquals("2.8", vm.uiState.apertureText)
        vm.onApertureChanged("64")
        vm.onLensSelected(prime.id)
        assertEquals("16", vm.uiState.apertureText)
        assertEquals(12.192, vm.uiState.focusDistanceMeters!!, 0.0)
        assertEquals(sharpness, vm.uiState.sharpness)
    }

    @Test fun `Ignore bypasses exact lens values aperture limits and custom units together without changing physics`() {
        val vm = vm()
        vm.onLensSelected(prime.id)
        vm.onFocusDistanceChanged("12.192")
        val before = vm.uiState
        vm.onIgnoreLensProfileChanged(true)
        assertEquals(CalculatorPresets.typicalFocalLengthsMm.map(Int::toDouble), vm.uiState.focalLengthPresets)
        assertEquals(ApertureIncrements.FullStop.apertures, vm.uiState.aperturePresets)
        assertFalse(58.25 in vm.uiState.focalLengthPresets)
        assertFalse(1.7 in vm.uiState.aperturePresets)
        assertNull(vm.uiState.activeLens)
        assertEquals(prime.id, vm.uiState.selectedLens!!.id)
        assertEquals(DistanceUnit.Meters, vm.uiState.effectiveFocusDistanceUnit)
        assertEquals(before.result, vm.uiState.result)
        assertEquals(before.focusDistance, vm.uiState.focusDistance)
        assertEquals(before.focalLengthText, vm.uiState.focalLengthText)
        assertEquals(before.apertureText, vm.uiState.apertureText)
        vm.onIgnoreLensProfileChanged(false)
        assertEquals(prime, vm.uiState.activeLens)
        assertEquals(DistanceUnit.Feet, vm.uiState.effectiveFocusDistanceUnit)
        assertEquals(before.result, vm.uiState.result)
        vm.onShowAllApertures(true)
        assertTrue(vm.uiState.cameraLensKit.ignoreLensProfile)
        assertEquals(DistanceUnit.Meters, vm.uiState.effectiveFocusDistanceUnit)
    }

    @Test fun `distance entry follows the selected scale and changing context clears only its draft`() {
        val vm = vm()
        vm.onLensSelected(prime.id)
        vm.onFocusDistanceInputChanged("40")
        assertEquals(12.192, vm.uiState.focusDistanceMeters!!, 0.0)
        assertEquals("40", vm.uiState.focusInputDraft)
        vm.onLensSelected(zoom.id)
        assertNull(vm.uiState.focusInputDraft)
        assertEquals(12.192, vm.uiState.focusDistanceMeters!!, 0.0)
        vm.onLensSelected(prime.id)
        vm.onFocusDistanceInputChanged("40")
        vm.onIgnoreLensProfileChanged(true)
        assertNull(vm.uiState.focusInputDraft)
        assertEquals(12.192, vm.uiState.focusDistanceMeters!!, 0.0)
        vm.onFocusDistanceInputChanged("40")
        assertEquals(40.0, vm.uiState.focusDistanceMeters!!, 0.0)
        assertEquals(DistanceUnit.Meters, vm.uiState.distanceUnit)
        assertEquals(DistanceUnit.Meters, settings.settings.value.distanceUnit)
        vm.onFocusAtInfinity()
        vm.onIgnoreLensProfileChanged(false)
        vm.onLensSelected(zoom.id)
        assertSame(CalculatedDistance.Infinity, vm.uiState.focusDistance)
    }

    @Test fun `selection and bypass are remembered per camera and startup preserves last physical inputs`() {
        val vm = vm()
        vm.onLensSelected(prime.id)
        vm.onFocalLengthChanged("300")
        vm.onIgnoreLensProfileChanged(true)
        vm.onCameraProfileSelected(BuiltInCameraProfiles.DEFAULT_ID)
        vm.onCameraProfileSelected(camera.id)
        assertEquals("300", vm.uiState.focalLengthText)
        assertEquals(prime.id, vm.uiState.selectedLens!!.id)
        assertTrue(vm.uiState.cameraLensKit.ignoreLensProfile)
        val restored = vm()
        assertEquals("300", restored.uiState.focalLengthText)
        assertEquals(prime.id, restored.uiState.selectedLens!!.id)
        restored.onIgnoreLensProfileChanged(false)
        assertEquals("300", restored.uiState.focalLengthText)
        assertEquals(prime, restored.uiState.activeLens)
    }

    @Test fun `lens edits preserve selection and optical inputs and deleted selections have a safe fallback`() = runBlocking {
        val vm = vm()
        vm.onLensSelected(prime.id)
        vm.onFocusDistanceInputChanged("40")
        val before = vm.uiState
        cameras.saveLens(camera.id, zoom.copy(minFocalLengthMm = 20.0))
        assertEquals(prime.id, vm.uiState.activeLens!!.id)
        cameras.saveLens(camera.id, prime.copy(focalLengthMm = 80.0, distanceScaleUnit = LensDistanceUnit.Meters))
        assertEquals(prime.id, vm.uiState.activeLens!!.id)
        assertEquals(before.focalLengthText, vm.uiState.focalLengthText)
        assertEquals(before.result, vm.uiState.result)
        assertNull(vm.uiState.focusInputDraft)
        assertEquals(before.focusDistance, vm.uiState.focusDistance)
        cameras.deleteLens(camera.id, prime.id)
        assertEquals(zoom.id, vm.uiState.activeLens!!.id)
        cameras.deleteLens(camera.id, zoom.id)
        assertNull(vm.uiState.activeLens)
        assertEquals(DistanceUnit.Meters, vm.uiState.effectiveFocusDistanceUnit)
        val empty = vm.uiState
        vm.onLensSelected("missing")
        assertEquals(empty, vm.uiState)
    }
}
