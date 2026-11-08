package com.example.dofcalculator.ui.calculator

import com.example.dofcalculator.calculation.DepthOfFieldCalculator
import com.example.dofcalculator.data.InMemoryPresetRepository
import com.example.dofcalculator.model.*
import com.example.dofcalculator.settings.*
import kotlinx.coroutines.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Test

class MacroWarningViewModelTest {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
    private val preferences = InMemoryAppSettingsRepository(AppSettings(sharpnessMigrated = true))
    private fun vm() = DofCalculatorViewModel(InMemoryPresetRepository(), preferences, scope).apply {
        onFocalLengthChanged("100")
        onApertureChanged("8")
    }
    @After fun cleanup() { scope.cancel() }

    @Test fun `distance and focal changes update the warning but do not activate Manual`() {
        val vm = vm()
        for (d in listOf("2", "1", "0.720000001")) {
            vm.onFocusDistanceChanged(d)
            assertSame(MacroDetectionState.None, vm.uiState.macroDetection)
            assertFalse(vm.uiState.showMacroWarning)
        }
        for (d in listOf("0.72", "0.6", "0.5", "0.4")) {
            vm.onFocusDistanceChanged(d)
            assertTrue(vm.uiState.macroDetection is MacroDetectionState.MacroEstimated)
            assertTrue(vm.uiState.showMacroWarning)
            assertEquals(d.toDouble(), vm.uiState.focusDistanceMeters!!, 0.0)
            assertEquals("100", vm.uiState.focalLengthText)
            assertEquals(CalculatorMode.DepthOfField, vm.uiState.calculatorMode)
            assertEquals(MagnificationMode.AUTO, vm.uiState.diffractionSettings.magnificationMode)
            assertEquals(DepthOfFieldCalculator.calculate(DepthOfFieldInput(100.0, 8.0,
                vm.uiState.effectiveCoCMm!!, d.toDouble())), vm.uiState.result)
        }
        vm.onFocalLengthChanged("50")
        assertFalse(vm.uiState.showMacroWarning)
        vm.onFocalLengthChanged("100")
        assertTrue(vm.uiState.showMacroWarning)
    }

    @Test fun `aperture CoC criterion camera and lens metadata changes reuse cached detection`() {
        val vm = vm()
        vm.onFocusDistanceChanged("0.6")
        val detection = vm.uiState.macroDetection
        for (mode in SharpnessCriterionMode.entries) {
            vm.onSharpnessModeSelected(mode)
            assertSame(detection, vm.uiState.macroDetection)
            vm.onApertureChanged("16")
            assertSame(detection, vm.uiState.macroDetection)
        }
        vm.onCircleOfConfusionChanged("0.013")
        assertSame(detection, vm.uiState.macroDetection)
        vm.onCameraProfileSelected("builtin.6x6")
        assertSame(detection, vm.uiState.macroDetection)
        vm.onShowAllApertures(true)
        assertSame(detection, vm.uiState.macroDetection)
    }

    @Test fun `close unavailable and invalid input are explicit and never fabricate magnification`() {
        val vm = vm()
        vm.onFocusDistanceChanged("0.3")
        assertSame(MacroDetectionState.MagnificationUnavailableAtCloseDistance, vm.uiState.macroDetection)
        assertNull(vm.uiState.magnificationEstimate.value)
        assertTrue(vm.uiState.showMacroWarning)
        assertNotNull(vm.uiState.result)
        for (bad in listOf("", "0", "-1", "NaN", "Infinity", "0,")) {
            vm.onFocusDistanceChanged(bad)
            assertSame(MacroDetectionState.InvalidInput, vm.uiState.macroDetection)
            assertFalse(vm.uiState.showMacroWarning)
        }
        vm.onFocusDistanceChanged("0.6")
        vm.onFocalLengthChanged("NaN")
        assertSame(MacroDetectionState.InvalidInput, vm.uiState.macroDetection)
        assertFalse(vm.uiState.showMacroWarning)
    }

    @Test fun `slider updates a single derived warning and keeps the chosen distance`() {
        val vm = vm()
        val scale = vm.uiState.distanceScale!!
        for (d in listOf(.6, 1.0, .5, .8, .4, 2.0, .72 - 1e-6, .72 + 1e-6)) {
            vm.onDistanceSliderChanged(scale.positionOf(CalculatedDistance.Finite(d)))
            assertEquals(d, vm.uiState.focusDistanceMeters!!, 1e-12)
            assertEquals(d < .72, vm.uiState.showMacroWarning)
            assertEquals("100", vm.uiState.focalLengthText)
        }
        vm.onDistanceSliderChanged(1.0)
        assertSame(CalculatedDistance.Infinity, vm.uiState.focusDistance)
        assertSame(MacroDetectionState.None, vm.uiState.macroDetection)
        assertFalse(vm.uiState.showMacroWarning)
        vm.onDistanceSliderChanged(scale.positionOf(CalculatedDistance.Finite(.6)))
        assertTrue(vm.uiState.showMacroWarning)
    }

    @Test fun `disabling warning preserves optics diffraction and manual preferences and survives restart`() {
        val vm = vm()
        vm.onFocusDistanceChanged("0.6")
        vm.onApertureChanged("32")
        val before = vm.uiState
        assertTrue(before.showMacroWarning)
        assertTrue(before.showDiffractionWarning)
        vm.onMacroWarningsEnabled(false)
        assertFalse(vm.uiState.showMacroWarning)
        assertTrue(vm.uiState.showDiffractionWarning)
        assertSame(before.macroDetection, vm.uiState.macroDetection)
        assertEquals(before.diffractionSettings, vm.uiState.diffractionSettings)
        assertEquals(before.diffractionResult, vm.uiState.diffractionResult)
        assertEquals(before.result, vm.uiState.result)
        assertEquals(before.focusDistance, vm.uiState.focusDistance)
        assertEquals(before.distanceSliderPosition, vm.uiState.distanceSliderPosition, 0.0)
        val restarted = DofCalculatorViewModel(InMemoryPresetRepository(), preferences, scope)
        assertFalse(restarted.uiState.macroWarningsEnabled)
        assertFalse(restarted.uiState.showMacroWarning)
        restarted.onMacroWarningsEnabled(true)
        assertTrue(restarted.uiState.showMacroWarning)
        restarted.onDiffractionWarningsEnabled(false)
        assertTrue(restarted.uiState.showMacroWarning)
        assertFalse(restarted.uiState.showDiffractionWarning)
    }

    @Test fun `open Macro only opens the existing form without modifying current calculation`() {
        val vm = vm()
        vm.onFocusDistanceChanged("0.6")
        val before = vm.uiState
        vm.onOpenMagnificationSettings()
        assertTrue(vm.uiState.isMagnificationSettingsVisible)
        assertEquals("", vm.uiState.macroForm.distanceMeters)
        assertEquals(before.result, vm.uiState.result)
        assertEquals(before.focusDistance, vm.uiState.focusDistance)
        assertEquals(before.focalLengthText, vm.uiState.focalLengthText)
        assertEquals(before.apertureText, vm.uiState.apertureText)
        assertEquals(before.sharpness, vm.uiState.sharpness)
        assertEquals(before.diffractionSettings, vm.uiState.diffractionSettings)
        vm.onCloseMagnificationSettings()
        assertTrue(vm.uiState.showMacroWarning)
    }

    @Test fun `confirmed Manual stale pair Infinity and Hyperfocal retain existing behavior`() {
        val vm = vm()
        vm.onFocusDistanceChanged("0.4")
        vm.onOpenMagnificationSettings()
        vm.onMacroFieldChanged(MacroInputField.NUMERATOR, "1")
        vm.onMacroFieldChanged(MacroInputField.DENOMINATOR, "1")
        vm.onMacroFieldChanged(MacroInputField.DISTANCE, "0.4")
        vm.onApplyMacroSettings()
        val manual = vm.uiState.macroDetection as MacroDetectionState.ManualConfirmed
        assertEquals(MagnificationMode.MANUAL, manual.source)
        assertFalse(vm.uiState.showMacroWarning)
        vm.onFocusDistanceChanged("0.6")
        assertTrue(vm.uiState.macroDetection is MacroDetectionState.MacroEstimated)
        assertEquals(MagnificationMode.AUTO, (vm.uiState.macroDetection as MacroDetectionState.MacroEstimated).source)
        assertTrue(vm.uiState.showManualMagnificationNotice)
        assertFalse(vm.uiState.showMacroWarning) // existing Manual confirmation panel remains the single prompt
        vm.onFocusAtInfinity()
        assertSame(MacroDetectionState.None, vm.uiState.macroDetection)
        assertFalse(vm.uiState.showMacroWarning)
        assertEquals(1.0, vm.uiState.diffractionSettings.manualMagnification, 0.0)
        vm.onFocusDistanceChanged("0.6")
        vm.onUseAutoMagnification()
        assertTrue(vm.uiState.showMacroWarning)
        vm.onCalculatorModeSelected(CalculatorMode.Hyperfocal)
        assertSame(MacroDetectionState.None, vm.uiState.macroDetection)
        assertFalse(vm.uiState.showMacroWarning)
        vm.onCalculatorModeSelected(CalculatorMode.DepthOfField)
        assertTrue(vm.uiState.showMacroWarning)
    }
}
