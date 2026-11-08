package com.example.dofcalculator.ui.calculator

import com.example.dofcalculator.calculation.DepthOfFieldCalculator
import com.example.dofcalculator.calculation.SharpnessCriterionCalculator
import com.example.dofcalculator.data.BuiltInCameraProfiles
import com.example.dofcalculator.data.InMemoryPresetRepository
import com.example.dofcalculator.model.*
import com.example.dofcalculator.settings.CalculatorMode
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.hypot
import kotlin.math.sqrt

class DigitalMediumFormatTest {
    private data class Expected(
        val id: String, val name: String, val format: FrameFormat,
        val width: Double, val height: Double, val coc: Double,
    )
    private val expected = listOf(
        Expected("fujifilm_gfx", "Fujifilm GFX", FrameFormat.DigitalMediumFormat44x33,
            43.8, 32.9, .0365),
        Expected("hasselblad_x", "Hasselblad X / 907X", FrameFormat.DigitalMediumFormat44x33,
            43.8, 32.9, .0365),
        Expected("hasselblad_h6d_100c", "Hasselblad H6D-100c", FrameFormat.DigitalMediumFormat54x40,
            53.4, 40.0, .0445),
    )
    private fun camera(e: Expected) = BuiltInCameraProfiles.cameras.single { it.id == e.id }

    @Test fun `named digital cameras have exact stable identity geometry and recommendations`() {
        expected.forEach { e ->
            val camera = camera(e)
            assertEquals(e.name, camera.name)
            assertEquals(e.format, camera.frameFormat)
            assertTrue(camera.isBuiltIn)
            assertEquals(e.width, camera.widthMm!!, 0.0)
            assertEquals(e.height, camera.heightMm!!, 0.0)
            assertEquals(e.coc, camera.standardCoCmm!!, 0.0)
            assertEquals(hypot(e.width, e.height) / 1500.0, e.coc, .00005)
            assertNotEquals(FrameFormat.MediumFormat645, camera.frameFormat)
        }
        assertEquals(56.0, FrameFormat.MediumFormat645.widthMm!!, 0.0)
        assertEquals(42.0, FrameFormat.MediumFormat645.heightMm!!, 0.0)
        assertEquals(.047, FrameFormat.MediumFormat645.suggestedCircleOfConfusionMm!!, 0.0)
    }

    @Test fun `standard uses rounded recommendation while megapixels remain user controlled`() {
        expected.forEach { e ->
            for (mp in listOf(24.0, 50.0, 100.0)) {
                val standard = SharpnessSettings(megapixels = mp)
                assertEquals(e.coc, SharpnessCriterionCalculator.calculate(camera(e), standard), 0.0)
                val pixel = standard.copy(mode = SharpnessCriterionMode.PIXEL_BASED)
                assertEquals(3.0 * sqrt(e.width * e.height / (mp * 1_000_000.0)),
                    SharpnessCriterionCalculator.calculate(camera(e), pixel), 1e-15)
            }
        }
    }

    @Test fun `print and screen use actual digital sensor geometry for fit and fill`() {
        expected.forEach { e ->
            for (placement in PrintPlacement.entries) {
                val print = SharpnessSettings(mode = SharpnessCriterionMode.PRINT_BASED,
                    printWidthCm = 45.0, printHeightCm = 30.0, printPlacement = placement)
                val printScale = if (placement == PrintPlacement.FIT)
                    minOf(450.0 / e.width, 300.0 / e.height)
                else maxOf(450.0 / e.width, 300.0 / e.height)
                assertEquals(.29088821687 / printScale,
                    SharpnessCriterionCalculator.calculate(camera(e), print), 1e-10)
                val screen = print.copy(mode = SharpnessCriterionMode.SCREEN_BASED,
                    screenPlacement = placement)
                val screenScale = if (placement == PrintPlacement.FIT)
                    minOf(597.7265033600158 / e.width, 336.2211581400089 / e.height)
                else maxOf(597.7265033600158 / e.width, 336.2211581400089 / e.height)
                assertEquals(.17453292643 / screenScale,
                    SharpnessCriterionCalculator.calculate(camera(e), screen), 1e-10)
                assertEquals(.00876, SharpnessCriterionCalculator.calculate(camera(e),
                    screen.copy(mode = SharpnessCriterionMode.CUSTOM, customCoCmm = .00876)), 0.0)
            }
        }
    }

    @Test fun `all criteria update DOF hyperfocal and slider without changing optical inputs`() {
        val vm = DofCalculatorViewModel()
        vm.onFocalLengthChanged("80")
        vm.onApertureChanged("5.6")
        vm.onFocusDistanceChanged("7.5")
        vm.onSharpnessFieldChanged(SharpnessField.MEGAPIXELS, "100")
        for (criterion in SharpnessCriterionMode.entries) {
            vm.onSharpnessModeSelected(criterion)
            vm.onCalculatorModeSelected(CalculatorMode.DepthOfField)
            vm.onCameraProfileSelected(BuiltInCameraProfiles.DEFAULT_ID)
            val previousHyperfocal = vm.uiState.result!!.hyperfocalDistanceMeters
            for (e in expected) {
                vm.onCameraProfileSelected(e.id)
                val coc = SharpnessCriterionCalculator.calculate(camera(e), vm.uiState.sharpness)
                val h = DepthOfFieldCalculator.calculateHyperfocalDistanceMeters(80.0, 5.6, coc)
                assertEquals(criterion, vm.uiState.sharpness.mode)
                assertEquals(coc, vm.uiState.effectiveCoCMm!!, 1e-15)
                assertEquals(100.0, vm.uiState.sharpness.megapixels, 0.0)
                if (criterion != SharpnessCriterionMode.CUSTOM) assertNotEquals(previousHyperfocal, h)
                for (mode in CalculatorMode.entries) {
                    vm.onCalculatorModeSelected(mode)
                    val expectedResult = if (mode == CalculatorMode.Hyperfocal)
                        DepthOfFieldCalculator.calculate(DepthOfFieldInput(80.0, 5.6, coc, h))
                    else DepthOfFieldCalculator.calculate(DepthOfFieldInput(80.0, 5.6, coc, 7.5))
                    assertEquals(expectedResult, vm.uiState.result)
                    assertEquals(h, vm.uiState.distanceScale!!.hyperfocalMeters, 1e-10)
                    assertEquals(e.id, vm.uiState.presets.selectedCameraId)
                    assertEquals("80", vm.uiState.focalLengthText)
                    assertEquals("5.6", vm.uiState.apertureText)
                    assertEquals("7.5", vm.uiState.focusDistanceText)
                }
                vm.onCalculatorModeSelected(CalculatorMode.DepthOfField)
                assertEquals(vm.uiState.distanceScale!!.positionOf(CalculatedDistance.Finite(7.5)),
                    vm.uiState.distanceSliderPosition, 1e-12)
            }
        }
        vm.onFocusAtInfinity()
        expected.forEach {
            vm.onCameraProfileSelected(it.id)
            assertSame(CalculatedDistance.Infinity, vm.uiState.focusDistance)
            assertEquals(1.0, vm.uiState.distanceSliderPosition, 0.0)
        }
    }

    @Test fun `new built ins cannot be edited deleted or overwritten as user cameras`() = runBlocking {
        val repo = InMemoryPresetRepository()
        val vm = DofCalculatorViewModel()
        expected.forEach {
            vm.onEditCamera(it.id)
            assertNull(vm.uiState.presets.cameraEditor)
            vm.onDeleteCamera(it.id)
            assertNull(vm.uiState.presets.pendingDeleteId)
            assertTrue(runCatching { repo.deleteCamera(it.id) }.isFailure)
            assertTrue(runCatching { repo.saveCamera(camera(it).copy(isBuiltIn = false)) }.isFailure)
        }
        assertEquals(19, repo.library.value.cameras.size)
    }
}
