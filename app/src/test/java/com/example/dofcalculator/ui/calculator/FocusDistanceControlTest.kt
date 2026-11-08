package com.example.dofcalculator.ui.calculator

import com.example.dofcalculator.model.CalculatedDistance
import com.example.dofcalculator.settings.CalculatorMode
import org.junit.Assert.*
import org.junit.Test

class FocusDistanceControlTest {
    @Test fun `manual distance is source of truth when optical parameters change`() {
        val vm = DofCalculatorViewModel()
        vm.onFocusDistanceChanged("3,25")
        val first = vm.uiState.distanceSliderPosition
        vm.onFocalLengthChanged("200")
        assertEquals("3,25", vm.uiState.focusDistanceText)
        assertTrue(vm.uiState.distanceSliderPosition < first)
        vm.onApertureChanged("11")
        assertEquals(3.25, (vm.uiState.focusDistance as CalculatedDistance.Finite).meters, 0.0)
        vm.onCircleOfConfusionChanged("0.018")
        assertEquals(3.25, vm.uiState.focusDistanceMeters!!, 0.0)
        assertEquals(vm.uiState.distanceSliderPosition,
            vm.uiState.distanceScale!!.positionOf(vm.uiState.focusDistance!!), 1e-12)
        vm.onFocusDistanceChanged("0,25")
        assertEquals(.1, vm.uiState.distanceScale!!.minimumMeters, 0.0)
        assertTrue(vm.uiState.distanceSliderPosition > 0.0)
    }

    @Test fun `fixed lower bound does not drift during dragging`() {
        val vm = DofCalculatorViewModel()
        vm.onFocalLengthChanged("200")
        vm.onFocusDistanceChanged("0.25")
        vm.onDistanceSliderChanged(.5)
        assertEquals(.1, vm.uiState.distanceScale!!.minimumMeters, 0.0)
        assertEquals(.5, vm.uiState.distanceSliderPosition, 1e-12)
        vm.onDistanceSliderChanged(.6)
        assertEquals(.6, vm.uiState.distanceSliderPosition, 1e-12)
    }

    @Test fun `fixed minimum survives optical changes and preserves shorter manual input`() {
        val vm = DofCalculatorViewModel()
        vm.onFocalLengthChanged("24")
        vm.onFocusDistanceChanged("0.05")
        assertEquals(.05, vm.uiState.focusDistanceMeters!!, 0.0)
        assertEquals(.1, vm.uiState.distanceScale!!.minimumMeters, 0.0)
        assertEquals(0.0, vm.uiState.distanceSliderPosition, 0.0)
        vm.onFocalLengthChanged("15")
        vm.onApertureChanged("16")
        vm.onCircleOfConfusionChanged("0.053")
        assertEquals(.05, vm.uiState.focusDistanceMeters!!, 0.0)
        assertEquals(.1, vm.uiState.distanceScale!!.minimumMeters, 0.0)
        vm.onDistanceSliderChanged(0.0)
        assertEquals(.1, vm.uiState.focusDistanceMeters!!, 0.0)
    }

    @Test fun `slider H infinity and finite return calculate correct boundaries`() {
        val vm = DofCalculatorViewModel()
        vm.onFocalLengthChanged("24")
        vm.onFocusAtHyperfocal()
        val h = vm.uiState.result!!.hyperfocalDistanceMeters
        assertEquals(.75, vm.uiState.distanceSliderPosition, 1e-12)
        assertEquals(h / 2, vm.uiState.result!!.nearLimitMeters, 1e-12)
        assertSame(CalculatedDistance.Infinity, vm.uiState.result!!.farLimit)
        vm.onFocusAtInfinity()
        assertTrue(vm.uiState.focusAtInfinity)
        assertSame(CalculatedDistance.Infinity, vm.uiState.focusDistance)
        assertNull(vm.uiState.focusDistanceMeters)
        assertEquals(h - .024, vm.uiState.result!!.nearLimitMeters, 1e-12)
        assertSame(CalculatedDistance.Infinity, vm.uiState.result!!.farLimit)
        vm.onDistanceSliderChanged(.875)
        assertFalse(vm.uiState.focusAtInfinity)
        assertEquals(2 * h, vm.uiState.focusDistanceMeters!!, 1e-12)
        assertSame(CalculatedDistance.Infinity, vm.uiState.result!!.farLimit)
    }

    @Test fun `centimetre buttons and incomplete manual input`() {
        val vm = DofCalculatorViewModel()
        vm.onFocusDistanceChanged("5")
        vm.onStepFocusDistance(true)
        assertEquals(5.01, vm.uiState.focusDistanceMeters!!, 0.0)
        vm.onStepFocusDistance(false)
        assertEquals(5.0, vm.uiState.focusDistanceMeters!!, 0.0)
        assertEquals(.1, vm.uiState.distanceScale!!.minimumMeters, 0.0)
        assertNotNull(vm.uiState.result)
        vm.onFocusDistanceChanged("5,")
        assertNull(vm.uiState.result)
        vm.onDistanceSliderChanged(.5)
        assertNotNull(vm.uiState.result)
    }

    @Test fun `long lens hyperfocal is the slider landmark and selected focus target`() {
        val vm = DofCalculatorViewModel()
        vm.onFocalLengthChanged("900")
        vm.onApertureChanged("8")
        vm.onCircleOfConfusionChanged("0.015")
        vm.onFocusAtHyperfocal()
        assertEquals(6750.9, vm.uiState.focusDistanceMeters!!, 1e-9)
        assertEquals(.75, vm.uiState.distanceSliderPosition, 1e-12)
        assertTrue(vm.uiState.isFocusedAtHyperfocal)
        assertFalse(vm.uiState.focusAtInfinity)

        // Infinite far DOF is not optical infinity or an exact H selection.
        vm.onDistanceSliderChanged(.875)
        assertSame(CalculatedDistance.Infinity, vm.uiState.result!!.farLimit)
        assertFalse(vm.uiState.isFocusedAtHyperfocal)
        assertFalse(vm.uiState.focusAtInfinity)

        vm.onFocusAtInfinity()
        assertTrue(vm.uiState.focusAtInfinity)
        assertFalse(vm.uiState.isFocusedAtHyperfocal)
        assertEquals(1.0, vm.uiState.distanceSliderPosition, 0.0)

        vm.onDistanceSliderChanged(.75)
        assertTrue(vm.uiState.isFocusedAtHyperfocal)
        assertFalse(vm.uiState.focusAtInfinity)
        vm.onStepFocusDistance(true)
        assertFalse(vm.uiState.isFocusedAtHyperfocal)
    }

    @Test fun `focus target selection uses actual distance independent of display rounding`() {
        val vm = DofCalculatorViewModel()
        vm.onFocusAtHyperfocal()
        val h = vm.uiState.focusDistanceMeters!!
        vm.onDistanceRoundingSelected(com.example.dofcalculator.settings.DistanceRounding.Two)
        vm.onDistanceUnitSelected(com.example.dofcalculator.settings.DistanceUnit.Feet)
        assertTrue(vm.uiState.isFocusedAtHyperfocal)
        vm.onFocusDistanceChanged((h + 0.0001).toString())
        assertFalse(vm.uiState.isFocusedAtHyperfocal)
        vm.onFocusDistanceChanged(h.toString())
        assertTrue(vm.uiState.isFocusedAtHyperfocal)
        vm.onFocusDistanceInputChanged("")
        assertFalse(vm.uiState.isFocusedAtHyperfocal)
        vm.onFocusAtHyperfocal()
        vm.onFocalLengthChanged("200")
        assertFalse(vm.uiState.isFocusedAtHyperfocal)
        assertEquals(h, vm.uiState.focusDistanceMeters!!, 0.0)
    }

    @Test fun `hyperfocal mode leaves saved focus untouched including infinity`() {
        val vm = DofCalculatorViewModel()
        vm.onFocusAtInfinity()
        vm.onCalculatorModeSelected(CalculatorMode.Hyperfocal)
        val h = vm.uiState.result!!.hyperfocalDistanceMeters
        assertEquals(h / 2, vm.uiState.result!!.nearLimitMeters, 1e-12)
        vm.onCalculatorModeSelected(CalculatorMode.DepthOfField)
        assertSame(CalculatedDistance.Infinity, vm.uiState.focusDistance)
    }
}
