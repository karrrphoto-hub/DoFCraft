package com.example.dofcalculator.ui.calculator

import com.example.dofcalculator.calculation.DepthOfFieldCalculator
import com.example.dofcalculator.calculation.SharpnessCriterionCalculator
import com.example.dofcalculator.calculation.StandardCoCResolver
import com.example.dofcalculator.data.*
import com.example.dofcalculator.model.*
import com.example.dofcalculator.settings.*
import kotlinx.coroutines.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Test

class StandardCoCViewModelTest {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
    private val half = CameraProfile("existing-half", "Half-frame film", FrameFormat.Custom,
        customFormatName = "Half-frame", widthMm = 17.0, heightMm = 24.0)
    private val cameras = InMemoryPresetRepository(PresetLibrary(
        cameras = BuiltInCameraProfiles.cameras + half, selectedCameraId = half.id))
    private val preferences = InMemoryAppSettingsRepository(AppSettings(
        sharpnessMigrated = true, sharpness = SharpnessSettings(customCoCmm = .00876)))

    @After fun cleanup() { scope.cancel() }

    private fun viewModel() = DofCalculatorViewModel(cameras, preferences, scope)

    private fun assertConsumers(vm: DofCalculatorViewModel, expected: Double) {
        val s = vm.uiState
        assertEquals(expected, s.effectiveCoCMm!!, 1e-14)
        val h = DepthOfFieldCalculator.calculateHyperfocalDistanceMeters(
            s.focalLengthText.toDouble(), s.apertureText.toDouble(), expected)
        assertEquals(h, s.result!!.hyperfocalDistanceMeters, 1e-10)
        assertEquals(h, s.distanceScale!!.hyperfocalMeters, 1e-10)
        assertEquals(expected, s.diffractionResult!!.effectiveCoCmm, 0.0)
        assertEquals(s.diffractionResult!!.airyDiameterMm / expected, s.diffractionResult!!.ratio, 1e-12)
    }

    @Test fun `existing half frame works in Standard and dimension edits update every consumer`() {
        val vm = viewModel()
        vm.onFocalLengthChanged("100")
        vm.onApertureChanged("8")
        vm.onFocusDistanceChanged("2.75")
        val firstDof = vm.uiState.result
        val diffraction = vm.uiState.diffractionSettings
        for ((w, h) in listOf(17.0 to 24.0, 24.0 to 36.0, 56.0 to 56.0, 24.0 to 65.0)) {
            vm.onEditCamera(half.id)
            vm.onCameraDimensionsChanged(true, w.toString())
            vm.onCameraDimensionsChanged(false, h.toString())
            vm.onSaveCameraEditor()
            assertNull(vm.uiState.presets.cameraEditor)
            assertConsumers(vm, StandardCoCResolver.resolve(w, h, null).coCmm)
            assertEquals(StandardCoCSource.DIAGONAL_FALLBACK, vm.uiState.standardCoC!!.source)
            assertEquals(SharpnessCriterionMode.STANDARD, vm.uiState.sharpness.mode)
            assertEquals("100", vm.uiState.focalLengthText)
            assertEquals("8", vm.uiState.apertureText)
            assertEquals(2.75, vm.uiState.focusDistanceMeters!!, 0.0)
            assertEquals(diffraction, vm.uiState.diffractionSettings)
            assertEquals(half.id, vm.uiState.presets.selectedCameraId)
            assertEquals(.00876, vm.uiState.sharpness.customCoCmm, 0.0)
        }
        assertNotEquals(firstDof, vm.uiState.result)
        vm.onCalculatorModeSelected(CalculatorMode.Hyperfocal)
        assertConsumers(vm, StandardCoCResolver.resolve(24.0, 65.0, null).coCmm)
        vm.onCalculatorModeSelected(CalculatorMode.DepthOfField)
        assertEquals(2.75, vm.uiState.focusDistanceMeters!!, 0.0)
    }

    @Test fun `catalogue link retains exact recommendation and identical unlinked geometry uses fallback`() = runBlocking {
        val vm = viewModel()
        cameras.saveCamera(half.copy(widthMm = 56.0, heightMm = 56.0, name = "6×6"))
        assertConsumers(vm, .05491251783869153)
        vm.onCameraProfileSelected("builtin.6x6")
        assertConsumers(vm, .053)
        assertEquals(StandardCoCSource.CATALOG, vm.uiState.standardCoC!!.source)
        vm.onCameraProfileSelected(half.id)
        assertConsumers(vm, .05491251783869153)
        assertEquals(StandardCoCSource.DIAGONAL_FALLBACK, vm.uiState.standardCoC!!.source)
    }

    @Test fun `Infinity and warning preferences survive geometry changes`() = runBlocking {
        val vm = viewModel()
        vm.onFocusAtInfinity()
        val diffraction = vm.uiState.diffractionSettings
        cameras.saveCamera(half.copy(widthMm = 24.0, heightMm = 36.0))
        assertConsumers(vm, .030)
        assertSame(CalculatedDistance.Infinity, vm.uiState.focusDistance)
        assertEquals(1.0, vm.uiState.distanceSliderPosition, 0.0)
        assertEquals(diffraction, vm.uiState.diffractionSettings)
        val restarted = viewModel()
        assertSame(CalculatedDistance.Infinity, restarted.uiState.focusDistance)
        assertEquals(SharpnessCriterionMode.STANDARD, restarted.uiState.sharpness.mode)
        assertEquals(.00876, restarted.uiState.sharpness.customCoCmm, 0.0)
        assertEquals(half.id, restarted.uiState.presets.selectedCameraId)
    }

    @Test fun `close distance and nominal diffraction stay unchanged when Standard CoC changes`() = runBlocking {
        val settings = InMemoryAppSettingsRepository(AppSettings(sharpnessMigrated = true,
            calculator = CalculatorSettings(focalLengthText = "100", apertureText = "8", focusDistanceText = "0.3")))
        val vm = DofCalculatorViewModel(cameras, settings, scope)
        cameras.saveCamera(half.copy(widthMm = 56.0, heightMm = 56.0))
        assertConsumers(vm, .05491251783869153)
        assertEquals(.010736, vm.uiState.diffractionResult!!.airyDiameterMm, 1e-15)
        assertEquals(.3, vm.uiState.focusDistanceMeters!!, 0.0)
        assertEquals("100", vm.uiState.focalLengthText)
        assertEquals("8", vm.uiState.apertureText)
    }

    @Test fun `unknown or overflowing geometry clears Standard rather than reusing last CoC`() = runBlocking {
        val vm = viewModel()
        for (bad in listOf(half.copy(widthMm = null), half.copy(
            widthMm = Double.MAX_VALUE, heightMm = Double.MAX_VALUE))) {
            cameras.saveCamera(bad)
            assertNull(vm.uiState.standardCoC)
            assertNull(vm.uiState.effectiveCoCMm)
            assertNull(vm.uiState.result)
            assertNull(vm.uiState.distanceScale)
            assertNull(vm.uiState.diffractionResult)
        }
        cameras.saveCamera(half)
        assertConsumers(vm, .020392777756239697)
    }

    @Test fun `incomplete camera editor dimensions are rejected without committing a different format`() {
        val vm = viewModel()
        val original = vm.uiState.effectiveCoCMm
        vm.onEditCamera(half.id)
        for (bad in listOf("", "0", "-1", "NaN", "Infinity", "9".repeat(400))) {
            vm.onCameraDimensionsChanged(true, bad)
            vm.onSaveCameraEditor()
            assertTrue(vm.uiState.presets.cameraEditor!!.dimensionsHaveError)
            assertEquals(original, vm.uiState.effectiveCoCMm)
            assertEquals(half, vm.uiState.presets.cameras.first { it.id == half.id })
        }
        vm.onCancelCameraEditor()
    }

    @Test fun `other five criteria retain their formulas and clear Standard provenance`() {
        val vm = viewModel()
        for (mode in SharpnessCriterionMode.entries.filter { it != SharpnessCriterionMode.STANDARD }) {
            vm.onSharpnessModeSelected(mode)
            assertNull(vm.uiState.standardCoC)
            assertEquals(SharpnessCriterionCalculator.calculate(half, vm.uiState.sharpness),
                vm.uiState.effectiveCoCMm!!, 1e-14)
        }
        vm.onSharpnessModeSelected(SharpnessCriterionMode.STANDARD)
        assertConsumers(vm, .020392777756239697)
    }

    @Test fun `pre migration profile and Custom slot survive without automatic legacy override`() {
        val archived = InMemoryPresetRepository(PresetLibrary(
            cameras = BuiltInCameraProfiles.cameras + half, selectedCameraId = half.id,
            legacyCoCByCameraId = mapOf(half.id to .015)))
        val settings = InMemoryAppSettingsRepository(AppSettings(
            sharpness = SharpnessSettings(customCoCmm = .00876),
            legacyCoCForMigration = ".017", sharpnessMigrated = false))
        val vm = DofCalculatorViewModel(archived, settings, scope)
        assertConsumers(vm, .020392777756239697)
        assertEquals(.00876, vm.uiState.sharpness.customCoCmm, 0.0)
        val restarted = DofCalculatorViewModel(archived, settings, scope)
        assertEquals(SharpnessCriterionMode.STANDARD, restarted.uiState.sharpness.mode)
        assertEquals(half, restarted.uiState.presets.cameras.first { it.id == half.id })
        restarted.onImportLegacyCoC(null)
        assertEquals(.017, restarted.uiState.effectiveCoCMm!!, 0.0)
        restarted.onImportLegacyCoC(half.id)
        assertEquals(.015, restarted.uiState.effectiveCoCMm!!, 0.0)
    }
}
