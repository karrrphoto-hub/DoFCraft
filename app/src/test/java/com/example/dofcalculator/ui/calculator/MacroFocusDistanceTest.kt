package com.example.dofcalculator.ui.calculator

import com.example.dofcalculator.calculation.*
import com.example.dofcalculator.data.InMemoryPresetRepository
import com.example.dofcalculator.model.*
import com.example.dofcalculator.settings.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class MacroFocusDistanceTest {
    private fun form(vm: DofCalculatorViewModel, ratio: String = "1:1", distance: String = "0.3") {
        vm.onOpenMacroFocus()
        vm.onManualMagnificationChanged(ratio)
        vm.onMacroFieldChanged(MacroInputField.DISTANCE, distance)
    }
    private fun manual(vm: DofCalculatorViewModel, ratio: String = "1:1", distance: String = "0.3") {
        form(vm, ratio, distance)
        vm.onApplyMacroSettings()
        if (vm.uiState.macroForm.pendingConfirmation != null) vm.onConfirmMacroSettings()
    }
    private fun vm() = DofCalculatorViewModel().also { it.onFocalLengthChanged("100") }

    @Test fun `each macro opening requires explicit distance and optical updates never prefill the draft`() {
        val vm = vm()
        val before = vm.uiState
        vm.onOpenMacroFocus()
        assertEquals("", vm.uiState.macroForm.distanceMeters)
        assertFalse(vm.uiState.macroForm.canApply)
        vm.onApplyMacroSettings()
        assertEquals(before.focusDistance, vm.uiState.focusDistance)
        assertSame(before.result, vm.uiState.result)
        vm.onManualMagnificationChanged("1:1")
        vm.onStepFocalLength(true)
        vm.onFocalLengthPresetSelected(100)
        vm.onApertureChanged("8")
        vm.onThemeModeSelected(ThemeMode.Dark)
        vm.onFocusDistanceChanged("0.6")
        assertEquals("", vm.uiState.macroForm.distanceMeters)
        assertFalse(vm.uiState.macroForm.canApply)
        vm.onMacroFieldChanged(MacroInputField.DISTANCE, "0.3")
        vm.onFocusDistanceChanged("2")
        assertEquals("0.3", vm.uiState.macroForm.distanceMeters)
        assertEquals(2.0, vm.uiState.focusDistanceMeters!!, 0.0)
        vm.onApplyMacroSettings()
        assertEquals(.3, vm.uiState.focusDistanceMeters!!, 0.0)
        vm.onDistanceUnitSelected(DistanceUnit.Feet)
        vm.onDistanceRoundingSelected(DistanceRounding.Two)
        assertEquals(ManualMagnificationStatus.VALID, vm.uiState.manualMagnificationAssessment.status)
        vm.onOpenMagnificationSettings()
        assertEquals("", vm.uiState.macroForm.distanceMeters)
        assertFalse(vm.uiState.macroForm.canApply)
        assertEquals(.3, vm.uiState.focusDistanceMeters!!, 0.0)
        vm.onRestoreManualMacro()
        assertEquals("0.3", vm.uiState.macroForm.distanceMeters)
        assertTrue(vm.uiState.macroForm.canApply)
        vm.onCloseMagnificationSettings()
        vm.onOpenMacroFocus()
        assertEquals("", vm.uiState.macroForm.distanceMeters)
    }

    @Test fun `manual form applies one actual distance and preserves nominal optics`() {
        val vm = vm()
        val before = vm.uiState
        form(vm)
        assertEquals(before.focusDistance, vm.uiState.focusDistance)
        assertSame(before.result, vm.uiState.result)
        assertEquals(75.0, vm.uiState.macroForm.validation.preview!!.equivalentFocalLengthMm, 1e-12)
        vm.onApplyMacroSettings()
        assertEquals(.3, vm.uiState.focusDistanceMeters!!, 0.0)
        assertEquals("100", vm.uiState.focalLengthText)
        assertEquals("8", vm.uiState.apertureText)
        assertEquals(1.0, vm.uiState.magnificationEstimate.value!!, 0.0)
        assertEquals(MagnificationMode.MANUAL, vm.uiState.magnificationEstimate.source)
        assertEquals(ManualMagnificationStatus.VALID, vm.uiState.manualMagnificationAssessment.status)
        assertEquals(75.0, vm.uiState.manualMagnificationAssessment.equivalentFocalLengthMm!!, 1e-12)
        assertEquals(16.0, vm.uiState.diffractionResult!!.effectiveAperture, 0.0)
        assertEquals(.021472, vm.uiState.diffractionResult!!.airyDiameterMm, 1e-15)
        assertEquals(DepthOfFieldCalculator.calculate(DepthOfFieldInput(100.0, 8.0, .03, .3)), vm.uiState.result)
        assertEquals(before.distanceScale!!.hyperfocalMeters, vm.uiState.distanceScale!!.hyperfocalMeters, 0.0)
        assertEquals(before.distanceScale.minimumMeters, vm.uiState.distanceScale!!.minimumMeters, 0.0)
        assertTrue(vm.uiState.isFocusDistanceEditable)
    }

    @Test fun `unusual combination requires separate confirmation and can be cancelled or accepted`() {
        val vm = vm()
        val before = vm.uiState.focusDistance
        form(vm, distance = "0.03")
        vm.onApplyMacroSettings()
        assertNotNull(vm.uiState.macroForm.pendingConfirmation)
        assertEquals(before, vm.uiState.focusDistance)
        vm.onCancelMacroConfirmation()
        assertNull(vm.uiState.macroForm.pendingConfirmation)
        vm.onApplyMacroSettings()
        vm.onConfirmMacroSettings()
        assertEquals(.03, vm.uiState.focusDistanceMeters!!, 0.0)
        assertEquals(7.5, vm.uiState.manualMagnificationAssessment.equivalentFocalLengthMm!!, 1e-12)
        assertEquals(ManualMagnificationStatus.VALID, vm.uiState.manualMagnificationAssessment.status)
    }

    @Test fun `confirmation cannot approve a changed draft or lens`() {
        val vm = vm()
        form(vm, distance = "0.03")
        vm.onApplyMacroSettings()
        vm.onMacroFieldChanged(MacroInputField.DISTANCE, "0.04")
        assertNull(vm.uiState.macroForm.pendingConfirmation)
        vm.onConfirmMacroSettings()
        assertEquals(5.0, vm.uiState.focusDistanceMeters!!, 0.0)
        assertNotNull(vm.uiState.macroForm.pendingConfirmation)
        vm.onFocalLengthChanged("85")
        assertNull(vm.uiState.macroForm.pendingConfirmation)
    }

    @Test fun `invalid input blocks apply and cancellation never changes optical state`() {
        val vm = vm()
        val before = vm.uiState
        for (field in MacroInputField.entries) for (bad in listOf("", "0", "-1", "NaN", "Infinity", "1,")) {
            form(vm)
            vm.onMacroFieldChanged(field, bad)
            assertFalse(vm.uiState.macroForm.canApply)
            assertTrue(field in vm.uiState.macroForm.validation.errors)
            vm.onApplyMacroSettings()
            assertEquals(before.focusDistance, vm.uiState.focusDistance)
            assertSame(before.result, vm.uiState.result)
            vm.onCloseMagnificationSettings()
        }
        form(vm, "1:0")
        assertFalse(vm.uiState.macroForm.canApply)
    }

    @Test fun `distance edits invalidate the pair and use labelled Auto without changing saved scale`() {
        val vm = vm()
        manual(vm)
        vm.onFocusDistanceInputChanged("2")
        assertEquals(1.0, vm.uiState.diffractionSettings.manualMagnification, 0.0)
        assertEquals(.3, vm.uiState.diffractionSettings.manualDistanceMeters!!, 0.0)
        assertFalse(vm.uiState.diffractionSettings.manualConfirmed)
        assertTrue(vm.uiState.manualNeedsConfirmation)
        assertEquals(MagnificationMode.AUTO, vm.uiState.magnificationEstimate.source)
        assertEquals(MagnificationEstimator.estimate(100.0, CalculatedDistance.Finite(2.0)),
            vm.uiState.magnificationEstimate.value)
        vm.onOpenMagnificationSettings()
        assertEquals("", vm.uiState.macroForm.distanceMeters)
        vm.onMacroFieldChanged(MacroInputField.DISTANCE, "2")
        vm.onApplyMacroSettings()
        assertNotNull(vm.uiState.macroForm.pendingConfirmation) // f equivalent = 500 mm.
        vm.onConfirmMacroSettings()
        assertEquals(2.0, vm.uiState.diffractionSettings.manualDistanceMeters!!, 0.0)
        assertEquals(MagnificationMode.MANUAL, vm.uiState.magnificationEstimate.source)
    }

    @Test fun `slider and step invalidate Manual but do not erase its paired values`() {
        val vm = vm()
        manual(vm, "1:2", "0.6")
        vm.onStepFocusDistance(true)
        assertEquals(.61, vm.uiState.focusDistanceMeters!!, 1e-14)
        assertTrue(vm.uiState.manualNeedsConfirmation)
        assertEquals(.5, vm.uiState.diffractionSettings.manualMagnification, 0.0)
        vm.onDistanceSliderChanged(.5)
        assertEquals(.6, vm.uiState.diffractionSettings.manualDistanceMeters!!, 0.0)
        assertEquals(MagnificationMode.AUTO, vm.uiState.magnificationEstimate.source)
    }

    @Test fun `macro focal steps presets and typing share the canonical value without changing measured distance`() {
        val vm = vm()
        manual(vm)
        assertTrue(vm.uiState.isMacroModeActive)
        vm.onOpenMagnificationSettings()
        vm.onMacroFieldChanged(MacroInputField.DISTANCE, "0.3")
        vm.onStepFocalLength(true)
        assertEquals("101", vm.uiState.focalLengthText)
        assertEquals(101.0, vm.uiState.macroForm.validation.preview!!.nominalFocalLengthMm, 0.0)
        assertTrue(vm.uiState.isMacroModeActive) // Still routes to Macro while confirmation is pending.
        vm.onFocalLengthPresetSelected(85)
        assertEquals("85", vm.uiState.focalLengthText)
        assertEquals(85.0, vm.uiState.macroForm.validation.preview!!.nominalFocalLengthMm, 0.0)
        vm.onFocalLengthChanged("90,5")
        assertEquals(90.5, vm.uiState.macroForm.validation.preview!!.nominalFocalLengthMm, 0.0)
        assertEquals(.3, vm.uiState.focusDistanceMeters!!, 0.0)
        assertEquals(1.0, vm.uiState.diffractionSettings.manualMagnification, 0.0)
        vm.onCloseMagnificationSettings()
        assertEquals("90,5", vm.uiState.focalLengthText)
        vm.onOpenMagnificationSettings()
        assertEquals("", vm.uiState.macroForm.distanceMeters)
        vm.onMacroFieldChanged(MacroInputField.DISTANCE, "0.3")
        assertEquals(90.5, vm.uiState.macroForm.validation.preview!!.nominalFocalLengthMm, 0.0)
        vm.onCalculatorModeSelected(CalculatorMode.Hyperfocal)
        assertFalse(vm.uiState.isMacroModeActive)
        vm.onCalculatorModeSelected(CalculatorMode.DepthOfField)
        assertFalse(vm.uiState.isMacroModeActive)
        assertEquals(MagnificationMode.AUTO, vm.uiState.diffractionSettings.magnificationMode)
        vm.onUseAutoMagnification()
        assertFalse(vm.uiState.isMacroModeActive)
    }

    @Test fun `focal edits keep actual distance and require reconfirmation without substituting equivalent`() {
        val vm = vm()
        manual(vm)
        vm.onFocalLengthChanged("85")
        assertEquals(.3, vm.uiState.focusDistanceMeters!!, 0.0)
        assertEquals(1.0, vm.uiState.diffractionSettings.manualMagnification, 0.0)
        assertEquals(100.0, vm.uiState.diffractionSettings.manualFocalLengthMm!!, 0.0)
        assertEquals(75.0, vm.uiState.manualMagnificationAssessment.equivalentFocalLengthMm!!, 1e-12)
        assertTrue(vm.uiState.manualNeedsConfirmation)
        vm.onOpenMagnificationSettings()
        vm.onMacroFieldChanged(MacroInputField.DISTANCE, "0.3")
        vm.onApplyMacroSettings()
        assertEquals(85.0, vm.uiState.diffractionSettings.manualFocalLengthMm!!, 0.0)
        assertEquals("85", vm.uiState.focalLengthText)
        assertEquals(MagnificationMode.MANUAL, vm.uiState.magnificationEstimate.source)
    }

    @Test fun `Infinity suspends Manual and restoring a finite pair requires confirmation`() {
        val vm = vm()
        manual(vm)
        vm.onFocusAtInfinity()
        assertSame(CalculatedDistance.Infinity, vm.uiState.focusDistance)
        assertEquals(ManualMagnificationStatus.SUSPENDED_AT_INFINITY, vm.uiState.manualMagnificationAssessment.status)
        assertEquals(0.0, vm.uiState.magnificationEstimate.value!!, 0.0)
        assertEquals(MagnificationMode.AUTO, vm.uiState.magnificationEstimate.source)
        assertEquals(8.0, vm.uiState.diffractionResult!!.effectiveAperture, 0.0)
        vm.onFocusDistanceChanged("2")
        assertTrue(vm.uiState.manualNeedsConfirmation)
        vm.onOpenMagnificationSettings()
        assertEquals("", vm.uiState.macroForm.distanceMeters)
        vm.onRestoreManualMacro()
        assertEquals("0.3", vm.uiState.macroForm.distanceMeters)
        assertEquals(2.0, vm.uiState.focusDistanceMeters!!, 0.0)
        vm.onApplyMacroSettings()
        assertEquals(.3, vm.uiState.focusDistanceMeters!!, 0.0)
        assertEquals(MagnificationMode.MANUAL, vm.uiState.magnificationEstimate.source)
    }

    @Test fun `macro on hyperfocal on hyperfocal off returns to ordinary DOF and preserves the saved pair`() {
        val vm = vm()
        manual(vm)
        val opticalResult = vm.uiState.result
        vm.onCalculatorModeSelected(CalculatorMode.Hyperfocal)
        assertFalse(vm.uiState.isMacroModeActive)
        assertEquals(MagnificationMode.AUTO, vm.uiState.diffractionSettings.magnificationMode)
        assertFalse(vm.uiState.manualNeedsConfirmation)
        assertFalse(vm.uiState.showManualMagnificationNotice)
        assertFalse(vm.uiState.showMacroIndicator)
        val h = vm.uiState.result!!.hyperfocalDistanceMeters
        assertEquals(h, vm.uiState.focusDistanceMeters!!, 0.0)
        assertFalse(vm.uiState.isFocusDistanceEditable)
        assertEquals(1.0, vm.uiState.diffractionSettings.manualMagnification, 0.0)
        assertEquals(.3, vm.uiState.diffractionSettings.manualDistanceMeters!!, 0.0)
        assertEquals(100.0, vm.uiState.diffractionSettings.manualFocalLengthMm!!, 0.0)

        vm.onCalculatorModeSelected(CalculatorMode.DepthOfField)
        assertEquals(.3, vm.uiState.focusDistanceMeters!!, 0.0)
        assertEquals(opticalResult, vm.uiState.result)
        assertFalse(vm.uiState.isMacroModeActive)
        assertFalse(vm.uiState.showManualMagnificationNotice)
        assertEquals(MagnificationMode.AUTO, vm.uiState.magnificationEstimate.source)
        assertEquals("100", vm.uiState.focalLengthText)
        assertEquals("8", vm.uiState.apertureText)

        // Only an explicit Apply in the macro editor enables the saved configuration again.
        vm.onOpenMagnificationSettings()
        assertFalse(vm.uiState.isMacroModeActive)
        vm.onRestoreManualMacro()
        vm.onApplyMacroSettings()
        assertTrue(vm.uiState.isMacroModeActive)
        assertEquals(MagnificationMode.MANUAL, vm.uiState.magnificationEstimate.source)
    }

    @Test fun `hyperfocal disables Macro persistently including older combined settings`() {
        for (legacyCombined in listOf(false, true)) {
            val pair = DiffractionSettings(false, MagnificationMode.MANUAL, 1.0, .3, 100.0, true)
            val settings = InMemoryAppSettingsRepository(AppSettings(
                sharpnessMigrated = true,
                calculator = CalculatorSettings(focalLengthText = "100", focusDistanceText = "0.3",
                    mode = if (legacyCombined) CalculatorMode.Hyperfocal else CalculatorMode.DepthOfField),
                diffraction = pair.copy(magnificationMode = if (legacyCombined) MagnificationMode.MANUAL
                    else MagnificationMode.AUTO)))
            val cameras = InMemoryPresetRepository()
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
            try {
                var vm = DofCalculatorViewModel(cameras, settings, scope)
                if (!legacyCombined) {
                    manual(vm)
                    vm.onCalculatorModeSelected(CalculatorMode.Hyperfocal)
                }
                assertEquals(MagnificationMode.AUTO, settings.settings.value.diffraction.magnificationMode)
                vm = DofCalculatorViewModel(cameras, settings, scope)
                assertEquals(CalculatorMode.Hyperfocal, vm.uiState.calculatorMode)
                assertFalse(vm.uiState.isMacroModeActive)
                vm.onCalculatorModeSelected(CalculatorMode.DepthOfField)
                vm = DofCalculatorViewModel(cameras, settings, scope)
                assertEquals(CalculatorMode.DepthOfField, vm.uiState.calculatorMode)
                assertEquals(MagnificationMode.AUTO, vm.uiState.diffractionSettings.magnificationMode)
                assertEquals(MagnificationMode.AUTO, vm.uiState.magnificationEstimate.source)
                assertFalse(vm.uiState.isMacroModeActive)
                assertEquals(.3, vm.uiState.focusDistanceMeters!!, 0.0)
                assertEquals(pair.manualMagnification, vm.uiState.diffractionSettings.manualMagnification, 0.0)
                assertEquals(pair.manualDistanceMeters, vm.uiState.diffractionSettings.manualDistanceMeters)
                assertEquals(pair.manualFocalLengthMm, vm.uiState.diffractionSettings.manualFocalLengthMm)
                assertFalse(vm.uiState.diffractionSettings.warningsEnabled)
            } finally { scope.cancel() }
        }
    }

    @Test fun `Auto is explicit and ordinary close distance needs no manual action`() {
        val vm = vm()
        manual(vm)
        val distance = vm.uiState.focusDistance
        vm.onUseAutoMagnification()
        assertEquals(distance, vm.uiState.focusDistance)
        assertEquals(1.0, vm.uiState.diffractionSettings.manualMagnification, 0.0)
        vm.onFocusDistanceInputChanged("0.6")
        assertEquals(.2679491924311227, vm.uiState.magnificationEstimate.value!!, 1e-14)
        assertTrue(vm.uiState.showMacroIndicator)
        assertTrue(vm.uiState.isFocusDistanceEditable)
        assertEquals(MagnificationMode.AUTO, vm.uiState.magnificationEstimate.source)
    }

    @Test fun `all CoC criteria leave a confirmed pair and nominal optical math unchanged`() {
        val vm = vm()
        manual(vm)
        val manual = vm.uiState.diffractionSettings
        for (criterion in SharpnessCriterionMode.entries) {
            vm.onSharpnessModeSelected(criterion)
            val s = vm.uiState
            assertEquals(manual, s.diffractionSettings)
            assertEquals("100", s.focalLengthText)
            assertEquals(.3, s.focusDistanceMeters!!, 0.0)
            assertEquals(DepthOfFieldCalculator.calculateHyperfocalDistanceMeters(100.0, 8.0, s.effectiveCoCMm!!),
                s.result!!.hyperfocalDistanceMeters, 1e-12)
            assertEquals(s.result.hyperfocalDistanceMeters, s.distanceScale!!.hyperfocalMeters, 1e-12)
            assertEquals(s.distanceScale.positionOf(s.focusDistance!!), s.distanceSliderPosition, 1e-14)
            assertEquals(16.0, s.diffractionResult!!.effectiveAperture, 0.0)
        }
    }

    @Test fun `confirmed very short measured distance persists even when nominal DOF is unavailable`() {
        val settings = InMemoryAppSettingsRepository()
        val cameras = InMemoryPresetRepository()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            var vm = DofCalculatorViewModel(cameras, settings, scope)
            vm.onFocalLengthChanged("100")
            manual(vm, distance = "0.03")
            assertNull(vm.uiState.result)
            assertEquals(1.0, vm.uiState.magnificationEstimate.value!!, 0.0)
            vm = DofCalculatorViewModel(cameras, settings, scope)
            assertEquals(.03, vm.uiState.focusDistanceMeters!!, 0.0)
            assertEquals(MagnificationMode.MANUAL, vm.uiState.magnificationEstimate.source)
            vm.onFocalLengthChanged("85")
            vm = DofCalculatorViewModel(cameras, settings, scope)
            assertEquals("85", vm.uiState.focalLengthText)
            assertEquals(.03, vm.uiState.focusDistanceMeters!!, 0.0)
            assertTrue(vm.uiState.manualNeedsConfirmation)
            assertEquals(1.0, vm.uiState.diffractionSettings.manualMagnification, 0.0)
        } finally { scope.cancel() }
    }

    @Test fun `confirmed and stale pairs survive ViewModel restart and delayed snapshots preserve drafts`() {
        val settings = InMemoryAppSettingsRepository()
        val cameras = InMemoryPresetRepository()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            var vm = DofCalculatorViewModel(cameras, settings, scope)
            vm.onFocalLengthChanged("100")
            manual(vm)
            vm = DofCalculatorViewModel(cameras, settings, scope)
            assertEquals(MagnificationMode.MANUAL, vm.uiState.magnificationEstimate.source)
            vm.onFocusDistanceInputChanged("2")
            vm = DofCalculatorViewModel(cameras, settings, scope)
            assertTrue(vm.uiState.manualNeedsConfirmation)
            assertEquals(1.0, vm.uiState.diffractionSettings.manualMagnification, 0.0)
            vm.onOpenMagnificationSettings()
            vm.onMacroFieldChanged(MacroInputField.DISTANCE, "0,")
            vm.onThemeModeSelected(ThemeMode.Dark)
            assertEquals("0,", vm.uiState.macroForm.distanceMeters)
            assertFalse(vm.uiState.macroForm.canApply)
        } finally { scope.cancel() }
    }
}
