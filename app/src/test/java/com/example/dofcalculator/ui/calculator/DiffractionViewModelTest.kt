package com.example.dofcalculator.ui.calculator

import com.example.dofcalculator.model.*
import com.example.dofcalculator.settings.*
import org.junit.Assert.*
import org.junit.Test

class DiffractionViewModelTest {
    @Test fun `focal length distance infinity and mode never change the nominal Airy diameter`() {
        val vm = DofCalculatorViewModel()
        vm.onApertureChanged("8")
        for (focal in listOf("35", "100", "500")) {
            vm.onFocalLengthChanged(focal)
            for (distance in listOf("5", "0.72", "0.6", "0.4", "0.3", "0.01")) {
                vm.onFocusDistanceChanged(distance)
                val baseline = vm.uiState.result
                assertEquals(.010736, vm.uiState.diffractionResult!!.airyDiameterMm, 1e-15)
                assertEquals(distance.toDouble(), vm.uiState.focusDistanceMeters!!, 0.0)
                vm.onDiffractionWarningsEnabled(false)
                assertFalse(vm.uiState.showDiffractionWarning)
                assertEquals(baseline, vm.uiState.result)
                vm.onDiffractionWarningsEnabled(true)
            }
        }
        vm.onFocusAtInfinity()
        assertSame(CalculatedDistance.Infinity, vm.uiState.focusDistance)
        assertEquals(.010736, vm.uiState.diffractionResult!!.airyDiameterMm, 1e-15)
        vm.onCalculatorModeSelected(CalculatorMode.Hyperfocal)
        assertEquals(.010736, vm.uiState.diffractionResult!!.airyDiameterMm, 1e-15)
        vm.onCalculatorModeSelected(CalculatorMode.DepthOfField)
        assertSame(CalculatedDistance.Infinity, vm.uiState.focusDistance)
    }

    @Test fun `all six criteria feed the same CoC to conventional diffraction and DOF`() {
        val vm = DofCalculatorViewModel()
        vm.onFocusDistanceChanged("0.6")
        vm.onFocalLengthChanged("100")
        vm.onApertureChanged("16")
        for (criterion in SharpnessCriterionMode.entries) {
            vm.onSharpnessModeSelected(criterion)
            val result = vm.uiState.diffractionResult!!
            assertEquals(vm.uiState.effectiveCoCMm!!, result.effectiveCoCmm, 0.0)
            assertEquals(result.airyDiameterMm / vm.uiState.effectiveCoCMm!!, result.ratio, 0.0)
            assertEquals(.021472, result.airyDiameterMm, 1e-15)
            assertEquals(.6, vm.uiState.focusDistanceMeters!!, 0.0)
            assertNotNull(vm.uiState.result)
        }
    }

    @Test fun `aperture and effective Standard Pixel and Custom CoC update only the informational level`() {
        val vm = DofCalculatorViewModel()
        vm.onFocalLengthChanged("100")
        vm.onFocusDistanceChanged("0.6")
        vm.onSharpnessModeSelected(SharpnessCriterionMode.STANDARD)
        assertEquals(.030, vm.uiState.effectiveCoCMm!!, 0.0)
        for ((aperture, expected) in listOf(
            "8" to DiffractionLevel.NONE, "11" to DiffractionLevel.NONE,
            "16" to DiffractionLevel.NONE, "22" to DiffractionLevel.NONE,
            "32" to DiffractionLevel.POSSIBLE, "45" to DiffractionLevel.NOTICEABLE,
        )) {
            vm.onApertureChanged(aperture)
            assertEquals(expected, vm.uiState.diffractionResult!!.level)
            assertEquals(expected != DiffractionLevel.NONE, vm.uiState.showDiffractionWarning)
            assertEquals(aperture, vm.uiState.apertureText)
            val before = vm.uiState
            vm.onDiffractionWarningsEnabled(false)
            assertSame(before.result, vm.uiState.result)
            assertSame(before.distanceScale, vm.uiState.distanceScale)
            assertEquals(before.focusDistance, vm.uiState.focusDistance)
            vm.onDiffractionWarningsEnabled(true)
        }
        vm.onApertureChanged("22")
        vm.onSharpnessModeSelected(SharpnessCriterionMode.PIXEL_BASED)
        vm.onSharpnessFieldChanged(SharpnessField.MEGAPIXELS, "24")
        vm.onSharpnessFieldChanged(SharpnessField.PIXEL_BLUR, "2")
        assertEquals(.012, vm.uiState.effectiveCoCMm!!, 1e-15)
        assertEquals(.012, vm.uiState.diffractionResult!!.effectiveCoCmm, 1e-15)
        assertEquals(DiffractionLevel.NOTICEABLE, vm.uiState.diffractionResult!!.level)

        vm.onCircleOfConfusionChanged("0.025")
        assertEquals(SharpnessCriterionMode.CUSTOM, vm.uiState.sharpness.mode)
        assertEquals(DiffractionLevel.POSSIBLE, vm.uiState.diffractionResult!!.level)
        vm.onCircleOfConfusionChanged("0.013")
        assertEquals(DiffractionLevel.NOTICEABLE, vm.uiState.diffractionResult!!.level)
        vm.onSharpnessModeSelected(SharpnessCriterionMode.STANDARD)
        assertFalse(vm.uiState.showDiffractionWarning)
        assertEquals(.6, vm.uiState.focusDistanceMeters!!, 0.0)
        assertEquals("100", vm.uiState.focalLengthText)
        assertEquals("22", vm.uiState.apertureText)
        assertEquals(com.example.dofcalculator.calculation.DepthOfFieldCalculator.calculate(
            DepthOfFieldInput(100.0, 22.0, .030, .6)), vm.uiState.result)
        vm.onCalculatorModeSelected(CalculatorMode.Hyperfocal)
        assertFalse(vm.uiState.showDiffractionWarning)
        assertEquals(com.example.dofcalculator.calculation.DepthOfFieldCalculator
            .calculateHyperfocalDistanceMeters(100.0, 22.0, .030),
            vm.uiState.result!!.hyperfocalDistanceMeters, 0.0)
    }

    @Test fun `short positive distance is retained across restart even without a geometric result`() {
        val preferences = InMemoryAppSettingsRepository(AppSettings(sharpnessMigrated = true))
        val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Unconfined)
        try {
            val cameras = com.example.dofcalculator.data.InMemoryPresetRepository()
            val vm = DofCalculatorViewModel(cameras, preferences, scope)
            vm.onFocalLengthChanged("100")
            vm.onFocusDistanceChanged("0.03")
            assertNull(vm.uiState.result)
            val restarted = DofCalculatorViewModel(cameras, preferences, scope)
            assertEquals(.03, restarted.uiState.focusDistanceMeters!!, 0.0)
            assertEquals("100", restarted.uiState.focalLengthText)
            assertEquals(.010736, restarted.uiState.diffractionResult!!.airyDiameterMm, 1e-15)
        } finally { scope.coroutineContext[kotlinx.coroutines.Job]?.cancel() }
    }

    @Test fun `invalid aperture or CoC clears the warning without a stale result`() {
        val vm = DofCalculatorViewModel()
        vm.onApertureChanged("32")
        assertTrue(vm.uiState.showDiffractionWarning)
        for (bad in listOf("", "0", "NaN", "Infinity")) {
            vm.onApertureChanged(bad)
            assertNull(vm.uiState.diffractionResult)
            assertFalse(vm.uiState.showDiffractionWarning)
        }
        vm.onApertureChanged("32")
        vm.onCircleOfConfusionChanged("")
        assertNull(vm.uiState.diffractionResult)
        vm.onCircleOfConfusionChanged("0.03")
        assertTrue(vm.uiState.showDiffractionWarning)
        // Diffraction depends on aperture and CoC, even while another optical field is incomplete.
        vm.onFocusDistanceChanged("")
        vm.onFocalLengthChanged("")
        assertEquals(.042944, vm.uiState.diffractionResult!!.airyDiameterMm, 1e-15)
        assertNull(vm.uiState.result)
    }
}
