package com.example.dofcalculator.ui.calculator

import com.example.dofcalculator.calculation.CloseFocusEvaluator
import com.example.dofcalculator.calculation.DepthOfFieldCalculator
import com.example.dofcalculator.data.InMemoryPresetRepository
import com.example.dofcalculator.model.*
import com.example.dofcalculator.settings.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.*
import org.junit.Test

class CloseFocusViewModelTest {
    private fun close() = DofCalculatorViewModel().apply {
        onFocalLengthChanged("100")
        onFocusDistanceChanged("0.6")
    }

    @Test fun `distance and nominal focal edits update information without changing optics`() {
        val vm = close()
        assertTrue(vm.uiState.showCloseFocusWarning)
        assertEquals(.2679491924311227,
            (vm.uiState.closeFocusWarning as CloseFocusWarningState.EstimatedMacro).magnification, 1e-14)
        assertEquals(.6, vm.uiState.focusDistanceMeters!!, 0.0)
        assertEquals("100", vm.uiState.focalLengthText)
        assertEquals(DepthOfFieldCalculator.calculate(DepthOfFieldInput(
            100.0, 8.0, vm.uiState.effectiveCoCMm!!, .6)), vm.uiState.result)
        vm.onFocusDistanceChanged("1")
        assertFalse(vm.uiState.showCloseFocusWarning)
        vm.onFocalLengthChanged("200")
        assertTrue(vm.uiState.showCloseFocusWarning)
        vm.onFocusDistanceChanged("0.3")
        assertSame(CloseFocusWarningState.TooCloseToEstimate, vm.uiState.closeFocusWarning)
        assertNotNull(vm.uiState.result)
        assertEquals(.3, vm.uiState.focusDistanceMeters!!, 0.0)
        assertEquals(DepthOfFieldCalculator.calculate(DepthOfFieldInput(
            200.0, 8.0, vm.uiState.effectiveCoCMm!!, .3)), vm.uiState.result)
    }

    @Test fun `aperture CoC camera and output settings reuse the cached estimate`() {
        val vm = close()
        val state = vm.uiState.closeFocusWarning
        val input = vm.uiState.closeFocusInput
        fun unchanged() {
            assertSame(state, vm.uiState.closeFocusWarning)
            assertSame(input, vm.uiState.closeFocusInput)
            assertEquals(.6, vm.uiState.focusDistanceMeters!!, 0.0)
        }
        vm.onApertureChanged("16")
        unchanged()
        vm.onCircleOfConfusionChanged("0.013")
        unchanged()
        vm.onCameraProfileSelected("builtin.6x6")
        unchanged()
        for (mode in SharpnessCriterionMode.entries) {
            vm.onSharpnessModeSelected(mode)
            unchanged()
        }
        vm.onVideoResolutionSelected(VideoResolution.UHD_8K)
        unchanged()
        vm.onSharpnessFieldChanged(SharpnessField.PRINT_VIEWING, "75")
        unchanged()
        vm.onDistanceUnitSelected(DistanceUnit.Centimeters)
        vm.onDistanceRoundingSelected(DistanceRounding.Two)
        unchanged()
    }

    @Test fun `invalid focal and distance drafts clear stale estimates without a crash`() {
        val vm = close()
        for (bad in listOf("", "0", "-1", "NaN", "Infinity", "1,", "1" + "0".repeat(400))) {
            vm.onFocusDistanceChanged(bad)
            assertSame(CloseFocusWarningState.InvalidInput, vm.uiState.closeFocusWarning)
            assertFalse(vm.uiState.showCloseFocusWarning)
        }
        vm.onFocusDistanceChanged("0.3")
        assertSame(CloseFocusWarningState.TooCloseToEstimate, vm.uiState.closeFocusWarning)
        for (bad in listOf("", "0", "-1", "NaN", "Infinity")) {
            vm.onFocalLengthChanged(bad)
            assertSame(CloseFocusWarningState.InvalidInput, vm.uiState.closeFocusWarning)
            assertFalse(vm.uiState.showCloseFocusWarning)
        }
        vm.onFocalLengthChanged("100")
        vm.onFocusDistanceChanged("0.03")
        assertSame(CloseFocusWarningState.TooCloseToEstimate, vm.uiState.closeFocusWarning)
        assertEquals(.03, vm.uiState.focusDistanceMeters!!, 0.0)
        // The pre-existing geometric validator still handles a distance below the focal length.
        assertNull(vm.uiState.result)
    }

    @Test fun `Infinity and Hyperfocal hide the prompt and finite DOF resumes current inputs`() {
        val vm = close()
        vm.onFocusAtInfinity()
        assertSame(CalculatedDistance.Infinity, vm.uiState.focusDistance)
        assertSame(CloseFocusWarningState.None, vm.uiState.closeFocusWarning)
        assertFalse(vm.uiState.showCloseFocusWarning)
        vm.onFocusDistanceChanged("0.6")
        val cached = vm.uiState.closeFocusWarning
        assertTrue(vm.uiState.showCloseFocusWarning)
        vm.onCalculatorModeSelected(CalculatorMode.Hyperfocal)
        assertFalse(vm.uiState.showCloseFocusWarning)
        assertSame(cached, vm.uiState.closeFocusWarning)
        vm.onFocalLengthChanged("200")
        assertSame(cached, vm.uiState.closeFocusWarning)
        vm.onCalculatorModeSelected(CalculatorMode.DepthOfField)
        assertEquals(.6, vm.uiState.focusDistanceMeters!!, 0.0)
        assertSame(CloseFocusWarningState.TooCloseToEstimate, vm.uiState.closeFocusWarning)
        assertTrue(vm.uiState.showCloseFocusWarning)
    }

    @Test fun `slider updates only the existing canonical distance and one derived warning`() {
        val vm = close()
        val scale = vm.uiState.distanceScale!!
        vm.onDistanceSliderChanged(scale.positionOf(CalculatedDistance.Finite(.72)))
        assertTrue(vm.uiState.showCloseFocusWarning)
        vm.onDistanceSliderChanged(scale.positionOf(CalculatedDistance.Finite(.7201)))
        assertFalse(vm.uiState.showCloseFocusWarning)
        repeat(30) { index ->
            val position = if (index % 2 == 0) .15 else .35
            val expected = scale.distanceAt(position)
            vm.onDistanceSliderChanged(position)
            assertEquals(expected, vm.uiState.focusDistance)
            assertEquals(CloseFocusEvaluator.evaluate(100.0, expected), vm.uiState.closeFocusWarning)
            assertEquals(position, vm.uiState.distanceSliderPosition, 1e-12)
        }
        vm.onDistanceSliderChanged(1.0)
        assertSame(CalculatedDistance.Infinity, vm.uiState.focusDistance)
        assertFalse(vm.uiState.showCloseFocusWarning)
    }

    @Test fun `warning preference is independent of diffraction and every optical value`() {
        val vm = close()
        vm.onApertureChanged("32")
        assertTrue(vm.uiState.showDiffractionWarning)
        assertTrue(vm.uiState.showCloseFocusWarning)
        val before = vm.uiState
        vm.onCloseFocusWarningsEnabled(false)
        assertFalse(vm.uiState.showCloseFocusWarning)
        assertTrue(vm.uiState.showDiffractionWarning)
        assertSame(before.result, vm.uiState.result)
        assertSame(before.diffractionResult, vm.uiState.diffractionResult)
        assertSame(before.closeFocusWarning, vm.uiState.closeFocusWarning)
        assertEquals(before.focusDistance, vm.uiState.focusDistance)
        assertEquals(before.focalLengthText, vm.uiState.focalLengthText)
        assertEquals(before.apertureText, vm.uiState.apertureText)
        assertEquals(before.effectiveCoCMm, vm.uiState.effectiveCoCMm)
        vm.onCloseFocusWarningsEnabled(true)
        vm.onDiffractionWarningsEnabled(false)
        assertTrue(vm.uiState.showCloseFocusWarning)
        assertFalse(vm.uiState.showDiffractionWarning)
        assertEquals(.042944, vm.uiState.diffractionResult!!.airyDiameterMm, 1e-15)
        vm.onFocusDistanceChanged("0.3")
        assertEquals(.042944, vm.uiState.diffractionResult!!.airyDiameterMm, 1e-15)
    }

    @Test fun `stopped down aperture shows diffraction alongside both close focus variants`() {
        val vm = close()
        vm.onApertureChanged("32")
        for (distance in listOf("0.72", "0.6", "0.5", "0.4", "0.3")) {
            vm.onFocusDistanceChanged(distance)
            assertTrue(vm.uiState.showCloseFocusWarning)
            assertTrue(vm.uiState.showDiffractionWarning)
            assertEquals(DiffractionLevel.POSSIBLE, vm.uiState.diffractionResult!!.level)
            assertEquals(.042944, vm.uiState.diffractionResult!!.airyDiameterMm, 1e-15)
            assertNotNull(vm.uiState.result)
            assertEquals(distance.toDouble(), vm.uiState.focusDistanceMeters!!, 0.0)
        }
        vm.onFocusDistanceChanged("2")
        assertFalse(vm.uiState.showCloseFocusWarning)
        assertTrue(vm.uiState.showDiffractionWarning)
        vm.onFocusDistanceChanged("0.6")
        vm.onApertureChanged("4")
        assertTrue(vm.uiState.showCloseFocusWarning)
        assertFalse(vm.uiState.showDiffractionWarning)
    }

    @Test fun `late settings snapshots cannot replace the memo for a newer optical edit`() {
        val initial = AppSettings(sharpnessMigrated = true)
        val emissions = MutableStateFlow(initial)
        val preferences = object : AppSettingsRepository by InMemoryAppSettingsRepository() {
            override val settings = emissions
        }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val vm = DofCalculatorViewModel(InMemoryPresetRepository(), preferences, scope)
            vm.onFocalLengthChanged("100")
            vm.onFocusDistanceChanged("0.5")
            val cached = vm.uiState.closeFocusWarning
            val input = vm.uiState.closeFocusInput
            emissions.value = initial.copy(themeMode = ThemeMode.Dark)
            assertSame(cached, vm.uiState.closeFocusWarning)
            assertSame(input, vm.uiState.closeFocusInput)
            vm.onApertureChanged("16")
            assertSame(cached, vm.uiState.closeFocusWarning)
            assertEquals(.5, vm.uiState.focusDistanceMeters!!, 0.0)
            assertEquals(.3819660112501051,
                (cached as CloseFocusWarningState.EstimatedMacro).magnification, 1e-14)
        } finally { scope.cancel() }
    }

    @Test fun `display unit conversion and rounding do not become independent distance sources`() {
        val vm = close()
        vm.onDistanceUnitSelected(DistanceUnit.Centimeters)
        vm.onFocusDistanceInputChanged("72")
        assertTrue(vm.uiState.showCloseFocusWarning)
        assertEquals(.72, vm.uiState.focusDistanceMeters!!, 1e-15)
        vm.onFocusDistanceInputChanged("72,0001")
        assertFalse(vm.uiState.showCloseFocusWarning)
        assertEquals(.720001, vm.uiState.focusDistanceMeters!!, 1e-15)
    }

    @Test fun `warning preference restores with the calculator without resetting current inputs`() {
        val preferences = InMemoryAppSettingsRepository(AppSettings(sharpnessMigrated = true))
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val cameras = InMemoryPresetRepository()
            val vm = DofCalculatorViewModel(cameras, preferences, scope)
            vm.onFocalLengthChanged("100")
            vm.onFocusDistanceChanged("0.6")
            vm.onCloseFocusWarningsEnabled(false)
            val restarted = DofCalculatorViewModel(cameras, preferences, scope)
            assertFalse(restarted.uiState.closeFocusWarningsEnabled)
            assertFalse(restarted.uiState.showCloseFocusWarning)
            assertTrue(restarted.uiState.closeFocusWarning is CloseFocusWarningState.EstimatedMacro)
            assertEquals(.6, restarted.uiState.focusDistanceMeters!!, 0.0)
        } finally { scope.cancel() }
    }
}
