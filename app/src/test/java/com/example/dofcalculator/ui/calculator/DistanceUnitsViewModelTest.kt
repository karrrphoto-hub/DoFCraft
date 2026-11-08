package com.example.dofcalculator.ui.calculator

import com.example.dofcalculator.model.CalculatedDistance
import com.example.dofcalculator.settings.*
import java.util.Locale
import org.junit.Assert.*
import org.junit.Test

class DistanceUnitsViewModelTest {
    @Test fun `units and rounding never change actual focus H or results`() {
        val vm = DofCalculatorViewModel()
        vm.onFocusAtHyperfocal()
        val result = vm.uiState.result
        val actual = vm.uiState.focusDistanceMeters
        for (unit in DistanceUnit.entries) {
            vm.onDistanceUnitSelected(unit)
            for (rounding in DistanceRounding.entries) {
                vm.onDistanceRoundingSelected(rounding)
                assertEquals(actual, vm.uiState.focusDistanceMeters)
                assertEquals(result, vm.uiState.result)
                val display = DistanceDisplay(unit, rounding, Locale.US)
                assertEquals(display.number(actual!!),
                    display.number(vm.uiState.focusDistanceText.toDouble()))
            }
        }
    }
    @Test fun `manual entry uses chosen units and keeps incomplete drafts`() {
        val vm = DofCalculatorViewModel()
        vm.onDistanceUnitSelected(DistanceUnit.Centimeters)
        vm.onFocusDistanceInputChanged("250,5")
        assertEquals(2.505, vm.uiState.focusDistanceMeters!!, 1e-12)
        assertEquals("250,5", vm.uiState.focusInputDraft)
        vm.onDistanceUnitSelected(DistanceUnit.Feet)
        assertNull(vm.uiState.focusInputDraft)
        assertEquals(2.505, vm.uiState.focusDistanceMeters!!, 1e-12)
        vm.onFocusDistanceInputChanged("10")
        assertEquals(3.048, vm.uiState.focusDistanceMeters!!, 1e-12)
        vm.onFocusDistanceInputChanged("10,")
        assertNull(vm.uiState.result)
        assertEquals("10,", vm.uiState.focusInputDraft)
        vm.onDistanceSliderChanged(.75)
        assertNull(vm.uiState.focusInputDraft)
        assertNotNull(vm.uiState.result)
    }
    @Test fun `infinity remains explicit and scale minimum stays fixed across units`() {
        val vm = DofCalculatorViewModel()
        vm.onFocusAtInfinity()
        vm.onDistanceUnitSelected(DistanceUnit.Inches)
        assertSame(CalculatedDistance.Infinity, vm.uiState.focusDistance)
        assertEquals(.1, vm.uiState.distanceScale!!.minimumMeters, 0.0)
        vm.onDistanceUnitSelected(DistanceUnit.Centimeters)
        assertEquals(.1, vm.uiState.distanceScale!!.minimumMeters, 0.0)
        vm.onFocusDistanceInputChanged("500")
        assertEquals(5.0, vm.uiState.focusDistanceMeters!!, 1e-12)
    }
}
