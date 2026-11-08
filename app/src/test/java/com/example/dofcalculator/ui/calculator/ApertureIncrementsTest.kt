package com.example.dofcalculator.ui.calculator

import com.example.dofcalculator.data.InMemoryPresetRepository
import com.example.dofcalculator.settings.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class ApertureIncrementsTest {
    @Test fun `each row is ordered unique and retains the full stop endpoints`() {
        for (increments in ApertureIncrements.entries) {
            val values = increments.apertures
            assertEquals(1.0, values.first(), 0.0)
            assertEquals(45.0, values.last(), 0.0)
            assertTrue(values.zipWithNext().all { (a, b) -> a < b })
            assertTrue(values.containsAll(ApertureIncrements.FullStop.apertures))
        }
        assertTrue(1.7 in ApertureIncrements.HalfStop.apertures)
        assertFalse(1.7 in ApertureIncrements.ThirdStop.apertures)
        assertTrue(1.1 in ApertureIncrements.ThirdStop.apertures)
        assertFalse(1.1 in ApertureIncrements.FullStop.apertures)
    }

    @Test fun `step buttons and boundaries follow the selected increment`() {
        val expected = mapOf(ApertureIncrements.FullStop to (2.8 to 5.6),
            ApertureIncrements.HalfStop to (3.3 to 4.8),
            ApertureIncrements.ThirdStop to (3.5 to 4.5))
        for ((increments, neighbours) in expected) {
            val vm = DofCalculatorViewModel()
            vm.onApertureIncrementsSelected(increments)
            vm.onApertureChanged("4")
            vm.onPreviousAperture()
            assertEquals(neighbours.first, vm.uiState.apertureText.toDouble(), 0.0)
            vm.onApertureChanged("4")
            vm.onNextAperture()
            assertEquals(neighbours.second, vm.uiState.apertureText.toDouble(), 0.0)
            vm.onApertureChanged("1")
            assertFalse(vm.uiState.canSelectPreviousAperture)
            vm.onPreviousAperture()
            assertEquals("1", vm.uiState.apertureText)
            vm.onApertureChanged("45")
            assertFalse(vm.uiState.canSelectNextAperture)
            vm.onNextAperture()
            assertEquals("45", vm.uiState.apertureText)
        }
    }

    @Test fun `changing increments preserves manual aperture optics result and focus`() {
        val vm = DofCalculatorViewModel()
        vm.onApertureChanged("3,5")
        vm.onFocusAtInfinity()
        val result = vm.uiState.result
        val h = vm.uiState.distanceScale!!.hyperfocalMeters
        vm.onApertureIncrementsSelected(ApertureIncrements.ThirdStop)
        assertEquals(3.5, vm.uiState.selectedStandardAperture!!, 0.0)
        vm.onApertureIncrementsSelected(ApertureIncrements.FullStop)
        assertNull(vm.uiState.selectedStandardAperture)
        assertEquals("3,5", vm.uiState.apertureText)
        assertEquals(result, vm.uiState.result)
        assertEquals(h, vm.uiState.distanceScale!!.hyperfocalMeters, 0.0)
        assertTrue(vm.uiState.focusAtInfinity)
    }

    @Test fun `nonstandard and incomplete manual input remain supported`() {
        val expectedNext = mapOf(ApertureIncrements.FullStop to 5.6,
            ApertureIncrements.HalfStop to 4.8, ApertureIncrements.ThirdStop to 4.5)
        for ((increments, next) in expectedNext) {
            val vm = DofCalculatorViewModel()
            vm.onApertureIncrementsSelected(increments)
            vm.onApertureChanged("4,2")
            assertNull(vm.uiState.selectedStandardAperture)
            vm.onPreviousAperture()
            assertEquals("4", vm.uiState.apertureText)
            vm.onApertureChanged("4,2")
            vm.onNextAperture()
            assertEquals(next, vm.uiState.apertureText.toDouble(), 0.0)
            vm.onApertureChanged("4,")
            vm.onApertureIncrementsSelected(ApertureIncrements.ThirdStop)
            vm.onNextAperture()
            assertEquals("4,", vm.uiState.apertureText)
            assertNull(vm.uiState.result)
        }
    }

    @Test fun `restored increments determine initial selection and survive viewmodel recreation`() {
        val settings = InMemoryAppSettingsRepository(AppSettings(
            apertureIncrements = ApertureIncrements.HalfStop,
            calculator = CalculatorSettings(apertureText = "1.7")))
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val presets = InMemoryPresetRepository()
            val first = DofCalculatorViewModel(presets, settings, scope)
            assertEquals(ApertureIncrements.HalfStop, first.uiState.apertureIncrements)
            assertEquals(1.7, first.uiState.selectedStandardAperture!!, 0.0)
            first.onApertureIncrementsSelected(ApertureIncrements.ThirdStop)
            val second = DofCalculatorViewModel(presets, settings, scope)
            assertEquals(ApertureIncrements.ThirdStop, second.uiState.apertureIncrements)
            assertEquals("1.7", second.uiState.apertureText)
            assertNull(second.uiState.selectedStandardAperture)
        } finally { scope.cancel() }
    }
}
