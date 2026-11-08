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
