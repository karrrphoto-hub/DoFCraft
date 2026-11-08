package com.example.dofcalculator.ui.calculator

import com.example.dofcalculator.calculation.*
import com.example.dofcalculator.data.InMemoryPresetRepository
import com.example.dofcalculator.model.*
import com.example.dofcalculator.settings.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class DiffractionViewModelTest {
    @Test fun `aperture CoC focus distance focal length and all criteria update informational branch`() {
        val vm = DofCalculatorViewModel()
        vm.onFocusAtInfinity()
        assertEquals(DiffractionLevel.NONE, vm.uiState.diffractionResult!!.level)
        assertFalse(vm.uiState.showDiffractionWarning)
        vm.onApertureChanged("16")
        assertEquals(DiffractionLevel.POSSIBLE, vm.uiState.diffractionResult!!.level)
        assertTrue(vm.uiState.showDiffractionWarning)
        vm.onCircleOfConfusionChanged("0.013")
        assertEquals(DiffractionLevel.NOTICEABLE, vm.uiState.diffractionResult!!.level)
        vm.onApertureChanged("8")
        vm.onFocalLengthChanged("100")
        vm.onFocusDistanceChanged("0.6")
        assertEquals(.2679491924311227, vm.uiState.magnificationEstimate.value!!, 1e-14)
        assertTrue(vm.uiState.showMacroIndicator)
        vm.onFocusDistanceChanged("1")
        assertEquals(.12701665379258312, vm.uiState.magnificationEstimate.value!!, 1e-14)
        assertFalse(vm.uiState.showMacroIndicator)
        vm.onFocalLengthChanged("50")
        assertTrue(vm.uiState.magnificationEstimate.value!! < .1)
        for (criterion in SharpnessCriterionMode.entries) {
            vm.onSharpnessModeSelected(criterion)
            val result = vm.uiState.diffractionResult!!
            assertEquals(vm.uiState.effectiveCoCMm!!, result.effectiveCoCmm, 0.0)
            assertEquals(result.airyDiameterMm / vm.uiState.effectiveCoCMm!!, result.ratio, 0.0)
        }
    }

    @Test fun `unavailable automatic macro and extreme inputs are handled without invented scale`() {
        val vm = DofCalculatorViewModel()
        vm.onFocalLengthChanged("100")
        vm.onApertureChanged("32")
        vm.onFocusDistanceChanged("0.2")
        assertNull(vm.uiState.magnificationEstimate.value)
        assertNull(vm.uiState.diffractionResult!!.magnification.value)
        assertEquals(32.0, vm.uiState.diffractionResult!!.effectiveAperture, 0.0)
        vm.onFocusDistanceChanged("0.000000000001")
        assertNull(vm.uiState.magnificationEstimate.value)
        vm.onFocusDistanceChanged("1" + "0".repeat(100))
        assertTrue(vm.uiState.magnificationEstimate.value!! >= 0.0)
        vm.onFocusDistanceChanged("1,")
        assertNull(vm.uiState.magnificationEstimate.value)
        vm.onFocalLengthChanged("NaN")
        assertNull(vm.uiState.magnificationEstimate.value)
    }


    @Test fun `manual pair changes diffraction without replacing nominal optical inputs`() {
        val vm = DofCalculatorViewModel()
        vm.onFocalLengthChanged("100")
        vm.onFocusDistanceChanged("0.6")
        val before = vm.uiState
        vm.onOpenMagnificationSettings()
        vm.onManualMagnificationChanged("1:2")
        vm.onMacroFieldChanged(MacroInputField.DISTANCE, "0.6")
        assertSame(before.result, vm.uiState.result)
        assertEquals(MagnificationMode.AUTO, vm.uiState.magnificationEstimate.source)
        vm.onApplyMacroSettings()
        assertEquals(.5, vm.uiState.magnificationEstimate.value!!, 0.0)
        assertEquals(12.0, vm.uiState.diffractionResult!!.effectiveAperture, 0.0)
        assertEquals(before.result, vm.uiState.result)
        assertEquals(before.distanceScale!!.hyperfocalMeters, vm.uiState.distanceScale!!.hyperfocalMeters, 0.0)
        assertEquals(before.distanceScale.minimumMeters, vm.uiState.distanceScale!!.minimumMeters, 0.0)
        assertEquals(before.distanceSliderPosition, vm.uiState.distanceSliderPosition, 0.0)
        vm.onDiffractionWarningsEnabled(false)
        assertFalse(vm.uiState.showDiffractionWarning)
        assertEquals(.5, vm.uiState.magnificationEstimate.value!!, 0.0)
        vm.onFocusDistanceChanged("2")
        assertEquals(.5, vm.uiState.diffractionSettings.manualMagnification, 0.0)
        assertEquals(MagnificationMode.AUTO, vm.uiState.magnificationEstimate.source)
        vm.onUseAutoMagnification()
        assertFalse(vm.uiState.manualNeedsConfirmation)
        assertEquals(MagnificationEstimator.estimate(100.0, CalculatedDistance.Finite(2.0)),
            vm.uiState.magnificationEstimate.value)
    }

    @Test fun `invalid drafts never suppress or mutate an already confirmed diffraction estimate`() {
        val vm = DofCalculatorViewModel()
        vm.onFocalLengthChanged("100")
        vm.onOpenMacroFocus()
        vm.onManualMagnificationChanged("1:1")
        vm.onMacroFieldChanged(MacroInputField.DISTANCE, "0.3")
        vm.onApplyMacroSettings()
        val before = vm.uiState
        vm.onOpenMagnificationSettings()
        vm.onRestoreManualMacro()
        for (bad in listOf("", "1:", "0", "-1", "NaN", "Infinity", "1" + "0".repeat(400))) {
            vm.onManualMagnificationChanged(bad)
            assertFalse(vm.uiState.macroForm.canApply)
            assertSame(before.diffractionResult, vm.uiState.diffractionResult)
            assertSame(before.result, vm.uiState.result)
        }
        vm.onCloseMagnificationSettings()
        assertEquals(1.0, vm.uiState.magnificationEstimate.value!!, 0.0)
    }

    @Test fun `delayed settings snapshots never replace the applied pair or form draft`() {
        val initial = AppSettings(sharpnessMigrated = true)
        val emissions = kotlinx.coroutines.flow.MutableStateFlow(initial)
        val repo = object : AppSettingsRepository by InMemoryAppSettingsRepository() {
            override val settings = emissions
            override suspend fun saveDiffractionSettings(settings: DiffractionSettings) = Unit
        }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val vm = DofCalculatorViewModel(InMemoryPresetRepository(), repo, scope)
            vm.onFocalLengthChanged("100")
            vm.onOpenMacroFocus()
            vm.onManualMagnificationChanged("2:1")
            vm.onMacroFieldChanged(MacroInputField.DISTANCE, "0.45")
            vm.onApplyMacroSettings()
            emissions.value = initial.copy(themeMode = ThemeMode.Dark)
            assertEquals(MagnificationMode.MANUAL, vm.uiState.diffractionSettings.magnificationMode)
            assertEquals(2.0, vm.uiState.magnificationEstimate.value!!, 0.0)
            vm.onOpenMagnificationSettings()
            vm.onMacroFieldChanged(MacroInputField.DENOMINATOR, "")
            emissions.value = initial.copy(themeMode = ThemeMode.Light)
            assertEquals("", vm.uiState.macroForm.denominator)
            assertFalse(vm.uiState.macroForm.canApply)
            assertEquals(2.0, vm.uiState.magnificationEstimate.value!!, 0.0)
        } finally { scope.cancel() }
    }
}
