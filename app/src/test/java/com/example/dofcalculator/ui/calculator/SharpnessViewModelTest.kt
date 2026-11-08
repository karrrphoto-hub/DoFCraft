package com.example.dofcalculator.ui.calculator

import com.example.dofcalculator.calculation.DepthOfFieldCalculator
import com.example.dofcalculator.data.*
import com.example.dofcalculator.model.*
import com.example.dofcalculator.settings.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class SharpnessViewModelTest {
    @Test fun `all methods feed DOF hyperfocal and slider with the same CoC and preserve optical inputs`() {
        val vm = DofCalculatorViewModel()
        vm.onFocalLengthChanged("80")
        vm.onApertureChanged("5.6")
        vm.onFocusDistanceChanged("7.25")
        val camera = vm.uiState.presets.selectedCameraId
        for (method in SharpnessCriterionMode.entries) {
            vm.onSharpnessModeSelected(method)
            val c = vm.uiState.effectiveCoCMm!!
            val h = DepthOfFieldCalculator.calculateHyperfocalDistanceMeters(80.0, 5.6, c)
            assertEquals(h, vm.uiState.result!!.hyperfocalDistanceMeters, 1e-10)
            assertEquals(h, vm.uiState.distanceScale!!.hyperfocalMeters, 1e-10)
            assertEquals(vm.uiState.distanceScale!!.positionOf(CalculatedDistance.Finite(7.25)),
                vm.uiState.distanceSliderPosition, 1e-12)
            vm.onCalculatorModeSelected(CalculatorMode.Hyperfocal)
            assertEquals(h, vm.uiState.result!!.hyperfocalDistanceMeters, 1e-10)
            vm.onCalculatorModeSelected(CalculatorMode.DepthOfField)
            assertEquals("7.25", vm.uiState.focusDistanceText)
            assertEquals(7.25, vm.uiState.focusDistanceMeters!!, 0.0)
            assertEquals("80", vm.uiState.focalLengthText)
            assertEquals("5.6", vm.uiState.apertureText)
            assertEquals(camera, vm.uiState.presets.selectedCameraId)
        }
    }

    @Test fun `switching methods keeps individual values and camera changes keep custom criterion`() {
        val vm = DofCalculatorViewModel()
        vm.onSharpnessModeSelected(SharpnessCriterionMode.PIXEL_BASED)
        vm.onSharpnessFieldChanged(SharpnessField.MEGAPIXELS, "45,7")
        vm.onSharpnessFieldChanged(SharpnessField.PIXEL_BLUR, "2")
        val pixelCoC = vm.uiState.effectiveCoCMm
        vm.onSharpnessModeSelected(SharpnessCriterionMode.PRINT_BASED)
        vm.onSharpnessFieldChanged(SharpnessField.PRINT_WIDTH, "45")
        vm.onPrintPlacementSelected(PrintPlacement.FIT)
        vm.onSharpnessModeSelected(SharpnessCriterionMode.STANDARD)
        assertEquals(.030, vm.uiState.effectiveCoCMm!!, 0.0)
        vm.onSharpnessModeSelected(SharpnessCriterionMode.PIXEL_BASED)
        assertEquals(pixelCoC, vm.uiState.effectiveCoCMm)
        assertEquals(45.7, vm.uiState.sharpness.megapixels, 0.0)
        assertEquals(2.0, vm.uiState.sharpness.pixelBlur, 0.0)
        vm.onCircleOfConfusionChanged("0.00876")
        vm.onCameraProfileSelected("builtin.6x6")
        assertEquals(SharpnessCriterionMode.CUSTOM, vm.uiState.sharpness.mode)
        assertEquals(.00876, vm.uiState.effectiveCoCMm!!, 0.0)
        assertEquals(45.0, vm.uiState.sharpness.printWidthCm, 0.0)
        assertEquals(PrintPlacement.FIT, vm.uiState.sharpness.printPlacement)
    }

    @Test fun `incomplete fields hide results without destroying other method values or optical infinity`() {
        val vm = DofCalculatorViewModel()
        vm.onFocusAtInfinity()
        vm.onSharpnessModeSelected(SharpnessCriterionMode.PIXEL_BASED)
        vm.onSharpnessFieldChanged(SharpnessField.MEGAPIXELS, "45,")
        assertNull(vm.uiState.effectiveCoCMm)
        assertNull(vm.uiState.distanceScale)
        assertNull(vm.uiState.result)
        vm.onSharpnessModeSelected(SharpnessCriterionMode.STANDARD)
        assertNotNull(vm.uiState.result)
        assertTrue(vm.uiState.focusAtInfinity)
        vm.onSharpnessModeSelected(SharpnessCriterionMode.PIXEL_BASED)
        assertEquals("45,", vm.uiState.sharpnessDrafts[SharpnessField.MEGAPIXELS])
        assertNull(vm.uiState.result)
        vm.onSharpnessFieldChanged(SharpnessField.MEGAPIXELS, "45.7")
        assertNotNull(vm.uiState.result)
        assertSame(CalculatedDistance.Infinity, vm.uiState.focusDistance)
    }

    @Test fun `screen parameters and camera dimensions update CoC without resetting calculator inputs`() {
        val vm = DofCalculatorViewModel()
        vm.onFocalLengthChanged("80")
        vm.onApertureChanged("5.6")
        vm.onFocusDistanceChanged("7.25")
        vm.onSharpnessModeSelected(SharpnessCriterionMode.SCREEN_BASED)
        val original = vm.uiState.effectiveCoCMm!!
        val h = vm.uiState.distanceScale!!.hyperfocalMeters
        vm.onSharpnessFieldChanged(SharpnessField.SCREEN_VIEWING, "120")
        assertEquals(original * 2, vm.uiState.effectiveCoCMm!!, 1e-14)
        assertNotEquals(h, vm.uiState.distanceScale!!.hyperfocalMeters)
        assertEquals(7.25, vm.uiState.focusDistanceMeters!!, 0.0)
        vm.onCameraProfileSelected("builtin.6x6")
        assertNotEquals(original * 2, vm.uiState.effectiveCoCMm!!)
        assertEquals(120.0, vm.uiState.sharpness.screenViewingCm, 0.0)
        assertEquals("80", vm.uiState.focalLengthText)
        assertEquals("5.6", vm.uiState.apertureText)
        assertEquals("7.25", vm.uiState.focusDistanceText)
        vm.onScreenCriterionSelected(ScreenCriterion.DISPLAY_PIXELS)
        vm.onScreenPlacementSelected(PrintPlacement.FILL)
        vm.onSharpnessFieldChanged(SharpnessField.SCREEN_BLUR, "2.5")
        vm.onPrintSizeSelected(PrintSizePreset.CM60X90)
        vm.onSharpnessModeSelected(SharpnessCriterionMode.PRINT_BASED)
        assertEquals(60.0, vm.uiState.sharpness.printWidthCm, 0.0)
        assertEquals(90.0, vm.uiState.sharpness.printHeightCm, 0.0)
        vm.onSharpnessModeSelected(SharpnessCriterionMode.SCREEN_BASED)
        assertEquals(ScreenCriterion.DISPLAY_PIXELS, vm.uiState.sharpness.screenCriterion)
        assertEquals(PrintPlacement.FILL, vm.uiState.sharpness.screenPlacement)
        assertEquals(2.5, vm.uiState.sharpness.screenBlurPx, 0.0)
        vm.onFocusAtInfinity()
        vm.onScreenDeviceSelected(ScreenDevice.TV)
        assertEquals(200.0, vm.uiState.sharpness.screenViewingCm, 0.0)
        assertSame(CalculatedDistance.Infinity, vm.uiState.focusDistance)
        assertEquals(1.0, vm.uiState.distanceSliderPosition, 0.0)
    }

    @Test fun `invalid output fields hide results while optional print metadata never changes optical CoC`() {
        val vm = DofCalculatorViewModel()
        vm.onSharpnessModeSelected(SharpnessCriterionMode.PRINT_BASED)
        val coc = vm.uiState.effectiveCoCMm
        for (ppi in listOf("150", "300", "", "0", "NaN")) {
            vm.onSharpnessFieldChanged(SharpnessField.PRINT_PPI, ppi)
            assertEquals(coc, vm.uiState.effectiveCoCMm)
        }
        for (dpi in listOf("300", "2400", "", "0", "Infinity")) {
            vm.onSharpnessFieldChanged(SharpnessField.PRINT_DPI, dpi)
            assertEquals(coc, vm.uiState.effectiveCoCMm)
        }
        vm.onSharpnessModeSelected(SharpnessCriterionMode.SCREEN_BASED)
        vm.onSharpnessFieldChanged(SharpnessField.SCREEN_DIAGONAL, "27,")
        assertNull(vm.uiState.result)
        assertNull(vm.uiState.distanceScale)
        vm.onSharpnessFieldChanged(SharpnessField.SCREEN_DIAGONAL, "27.5")
        assertNotNull(vm.uiState.result)
        vm.onSharpnessFieldChanged(SharpnessField.SCREEN_ANGLE, "10800")
        assertNull(vm.uiState.result)
        vm.onScreenCriterionSelected(ScreenCriterion.DISPLAY_PIXELS)
        assertNotNull(vm.uiState.result)
    }

    @Test fun `legacy calculator CoC is archived without overriding Standard`() {
        for (legacy in listOf(null, "0.017")) {
            val repo = InMemoryPresetRepository(PresetLibrary(selectedCameraId = "builtin.6x6"))
            val settings = InMemoryAppSettingsRepository(AppSettings(legacyCoCForMigration = legacy))
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
            try {
                val vm = DofCalculatorViewModel(repo, settings, scope)
                assertEquals(SharpnessCriterionMode.STANDARD, vm.uiState.sharpness.mode)
                assertEquals(.053, vm.uiState.effectiveCoCMm!!, 0.0)
                assertEquals(legacy?.toDouble(), vm.uiState.legacyCalculatorCoCmm)
                if (legacy != null) {
                    vm.onImportLegacyCoC(null)
                    assertEquals(SharpnessCriterionMode.CUSTOM, vm.uiState.sharpness.mode)
                    assertEquals(.017, vm.uiState.effectiveCoCMm!!, 0.0)
                }
            } finally { scope.cancel() }
        }
    }

    @Test fun `migration preserves Standard and imports archived camera CoC only on request`() = runBlocking {
        val camera = CameraProfile("old", "Nikon D850", FrameFormat.FullFrame35Mm)
        val repo = InMemoryPresetRepository(PresetLibrary(
            cameras = BuiltInCameraProfiles.cameras + camera, selectedCameraId = "old",
            legacyCoCByCameraId = mapOf("old" to .015, "other" to .00876)))
        val settings = InMemoryAppSettingsRepository(AppSettings(
            themeMode = ThemeMode.Dark, language = AppLanguage.Russian,
            calculator = CalculatorSettings(focalLengthText = "80", apertureText = "4",
                focusDistanceText = "12", circleOfConfusionText = "0.015")))
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val first = DofCalculatorViewModel(repo, settings, scope)
            assertEquals(SharpnessCriterionMode.STANDARD, first.uiState.sharpness.mode)
            assertEquals(.030, first.uiState.effectiveCoCMm!!, 0.0)
            assertEquals(.015, first.uiState.legacyCoCByCameraId["old"]!!, 0.0)
            first.onImportLegacyCoC("old")
            assertEquals(.015, first.uiState.effectiveCoCMm!!, 0.0)
            assertEquals("12", first.uiState.focusDistanceText)
            assertEquals(ThemeMode.Dark, first.uiState.themeMode)
            first.onSharpnessModeSelected(SharpnessCriterionMode.STANDARD)
            first.onCameraProfileSelected("builtin.6x6")
            assertEquals(SharpnessCriterionMode.STANDARD, first.uiState.sharpness.mode)
            val second = DofCalculatorViewModel(repo, settings, scope)
            assertEquals(SharpnessCriterionMode.STANDARD, second.uiState.sharpness.mode)
            assertEquals(.053, second.uiState.effectiveCoCMm!!, 0.0)
            second.onImportLegacyCoC("other")
            assertEquals(.00876, second.uiState.effectiveCoCMm!!, 0.0)
            assertEquals("builtin.6x6", second.uiState.presets.selectedCameraId)
        } finally { scope.cancel() }
    }
}
