package com.example.dofcalculator.ui.calculator

import com.example.dofcalculator.calculation.DepthOfFieldCalculator
import com.example.dofcalculator.model.CalculatedDistance
import com.example.dofcalculator.model.DepthOfFieldInput
import com.example.dofcalculator.model.FrameFormat
import com.example.dofcalculator.settings.AppLanguage
import com.example.dofcalculator.settings.CalculatorMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class DofCalculatorViewModelTest {
    @Test
    fun `language selection updates UI state`() {
        val viewModel = DofCalculatorViewModel()

        viewModel.onLanguageSelected(AppLanguage.Russian)

        assertEquals(AppLanguage.Russian, viewModel.uiState.language)
    }

    @Test
    fun `default values produce the control calculation`() {
        val viewModel = DofCalculatorViewModel()

        val result = viewModel.uiState.result
        assertNotNull(result)
        assertEquals(
            10.466666666666669,
            requireNotNull(result).hyperfocalDistanceMeters,
            TOLERANCE_METERS,
        )
        assertEquals(
            FrameFormat.FullFrame35Mm,
            viewModel.uiState.selectedFrameFormat,
        )
        assertEquals("0.03", viewModel.uiState.circleOfConfusionText)
        assertFalse(viewModel.uiState.focalLengthHasError)
        assertFalse(viewModel.uiState.apertureHasError)
        assertFalse(viewModel.uiState.focusDistanceHasError)
        assertFalse(viewModel.uiState.circleOfConfusionHasError)
        assertEquals(50.0, viewModel.uiState.selectedFocalLengthPresetMm)
        assertEquals(8.0, viewModel.uiState.selectedStandardAperture)
        assertTrue(viewModel.uiState.canSelectPreviousAperture)
        assertTrue(viewModel.uiState.canSelectNextAperture)
    }

    @Test
    fun `APS-C selects 0_019 mm CoC`() {
        assertFrameFormatPreset(FrameFormat.ApsC, 0.018, "0,018")
    }

    @Test
    fun `35 mm selects 0_030 mm CoC`() {
        assertFrameFormatPreset(FrameFormat.FullFrame35Mm, 0.030, "0,030")
    }

    @Test
    fun `645 selects 0_045 mm CoC`() {
        assertFrameFormatPreset(FrameFormat.MediumFormat645, 0.047, "0,047")
    }

    @Test
    fun `6x6 selects 0_050 mm CoC`() {
        assertFrameFormatPreset(FrameFormat.MediumFormat6x6, 0.053, "0,053")
    }

    @Test
    fun `6x7 selects 0_060 mm CoC`() {
        assertFrameFormatPreset(FrameFormat.MediumFormat6x7, 0.059, "0,059")
    }

    @Test
    fun `6x9 selects 0_070 mm CoC`() {
        assertFrameFormatPreset(FrameFormat.MediumFormat6x9, 0.067, "0,067")
    }

    @Test
    fun `custom format accepts arbitrary CoC`() {
        val viewModel = DofCalculatorViewModel()

        viewModel.onFrameFormatSelected(FrameFormat.Custom)
        viewModel.onCircleOfConfusionChanged("0,025")

        val expected = calculateExpected(circleOfConfusionMm = 0.025)
        assertEquals(FrameFormat.Custom, viewModel.uiState.selectedFrameFormat)
        assertEquals("0,025", viewModel.uiState.circleOfConfusionText)
        assertEquals(expected, viewModel.uiState.result)
    }

    @Test
    fun `manual CoC override keeps selected preset`() {
        val viewModel = DofCalculatorViewModel()

        viewModel.onFrameFormatSelected(FrameFormat.ApsC)
        viewModel.onCircleOfConfusionChanged("0.040")

        val expected = calculateExpected(circleOfConfusionMm = 0.040)
        assertEquals(FrameFormat.ApsC, viewModel.uiState.selectedFrameFormat)
        assertEquals("0.040", viewModel.uiState.circleOfConfusionText)
        assertEquals(expected, viewModel.uiState.result)
    }

    @Test
    fun `camera profile applies its format and CoC without changing lens`() {
        val viewModel = DofCalculatorViewModel()
        viewModel.onCreateCamera()
        viewModel.onCameraEditorNameChanged("Studio camera")
        viewModel.onCameraEditorFrameFormatSelected(
            FrameFormat.MediumFormat6x6,
        )
        viewModel.onSaveCameraEditor()
        val cameraId = viewModel.uiState.presets.cameras.single { !it.isBuiltIn }.id

        viewModel.onFocalLengthChanged("105")
        viewModel.onApertureChanged("5,6")
        viewModel.onFrameFormatSelected(FrameFormat.ApsC)
        viewModel.onCameraProfileSelected(cameraId)

        assertEquals(
            FrameFormat.MediumFormat6x6,
            viewModel.uiState.selectedFrameFormat,
        )
        assertEquals(0.053, viewModel.uiState.circleOfConfusionText.toDouble(), 0.0)
        assertEquals("105", viewModel.uiState.focalLengthText)
        assertEquals("5,6", viewModel.uiState.apertureText)
    }

    @Test
    fun `incomplete CoC hides results`() {
        val viewModel = DofCalculatorViewModel()

        viewModel.onCircleOfConfusionChanged("0,")

        assertNull(viewModel.uiState.result)
        assertTrue(viewModel.uiState.circleOfConfusionHasError)
        assertEquals(DofInputError.InvalidNumber, viewModel.uiState.inputError)
    }

    @Test
    fun `all standard apertures update the calculation`() {
        val viewModel = DofCalculatorViewModel()

        CalculatorPresets.standardApertures.forEach { aperture ->
            viewModel.onAperturePresetSelected(aperture)

            assertEquals(aperture, viewModel.uiState.selectedStandardAperture)
            assertEquals(
                calculateExpected(aperture = aperture),
                viewModel.uiState.result,
            )
        }
    }

    @Test
    fun `aperture buttons move to adjacent standard values`() {
        val viewModel = DofCalculatorViewModel()

        viewModel.onPreviousAperture()
        assertEquals("5.6", viewModel.uiState.apertureText)

        viewModel.onNextAperture()
        assertEquals("8", viewModel.uiState.apertureText)

        viewModel.onNextAperture()
        assertEquals("11", viewModel.uiState.apertureText)
    }

    @Test
    fun `aperture buttons bracket a nonstandard manual value`() {
        val viewModel = DofCalculatorViewModel()

        viewModel.onApertureChanged("3,5")
        assertNull(viewModel.uiState.selectedStandardAperture)

        viewModel.onPreviousAperture()
        assertEquals("2.8", viewModel.uiState.apertureText)

        viewModel.onApertureChanged("3,5")
        viewModel.onNextAperture()
        assertEquals("4", viewModel.uiState.apertureText)
    }

    @Test
    fun `all focal length presets update the calculation`() {
        val viewModel = DofCalculatorViewModel()

        CalculatorPresets.typicalFocalLengthsMm.forEach { focalLengthMm ->
            viewModel.onFocalLengthPresetSelected(focalLengthMm)

            assertEquals(
                focalLengthMm.toDouble(),
                viewModel.uiState.selectedFocalLengthPresetMm,
            )
            assertEquals(focalLengthMm.toString(), viewModel.uiState.focalLengthText)
            assertEquals(
                calculateExpected(focalLengthMm = focalLengthMm.toDouble()),
                viewModel.uiState.result,
            )
        }
    }

    @Test
    fun `focal length buttons step by one mm and preserve other optical inputs`() {
        val viewModel = DofCalculatorViewModel()
        val before = viewModel.uiState
        viewModel.onStepFocalLength(increase = true)
        assertEquals("51", viewModel.uiState.focalLengthText)
        assertEquals(calculateExpected(focalLengthMm = 51.0), viewModel.uiState.result)
        assertEquals(before.apertureText, viewModel.uiState.apertureText)
        assertEquals(before.focusDistance, viewModel.uiState.focusDistance)
        assertEquals(before.effectiveCoCMm, viewModel.uiState.effectiveCoCMm)
        viewModel.onStepFocalLength(increase = false)
        assertEquals("50", viewModel.uiState.focalLengthText)

        for (input in listOf("52,5", "52.5")) {
            viewModel.onFocalLengthChanged(input)
            viewModel.onStepFocalLength(increase = true)
            assertEquals("53.5", viewModel.uiState.focalLengthText)
            viewModel.onStepFocalLength(increase = false)
            assertEquals("52.5", viewModel.uiState.focalLengthText)
        }
    }

    @Test
    fun `focal length buttons reject incomplete invalid and unrepresentable steps`() {
        val viewModel = DofCalculatorViewModel()
        for (input in listOf("", "50,", "50.", "NaN", "Infinity", "-1", "0", "1" + "0".repeat(308))) {
            viewModel.onFocalLengthChanged(input)
            viewModel.onStepFocalLength(increase = true)
            viewModel.onStepFocalLength(increase = false)
            assertEquals(input, viewModel.uiState.focalLengthText)
            assertFalse(viewModel.uiState.canDecreaseFocalLength)
            assertFalse(viewModel.uiState.canIncreaseFocalLength)
        }
        viewModel.onFocalLengthChanged("1")
        viewModel.onStepFocalLength(increase = false)
        assertEquals("1", viewModel.uiState.focalLengthText)
        assertFalse(viewModel.uiState.canDecreaseFocalLength)
        assertTrue(viewModel.uiState.canIncreaseFocalLength)
        viewModel.onFocalLengthChanged("0.5")
        viewModel.onStepFocalLength(increase = false)
        assertEquals("0.5", viewModel.uiState.focalLengthText)
        viewModel.onStepFocalLength(increase = true)
        assertEquals("1.5", viewModel.uiState.focalLengthText)
    }

    @Test
    fun `focal length shortcuts include new values while manual 80 mm remains valid`() {
        assertEquals(
            listOf(12, 14, 16, 20, 24, 28, 35, 50, 55, 58, 85, 90, 105, 135, 180, 200, 300),
            CalculatorPresets.typicalFocalLengthsMm,
        )
        val viewModel = DofCalculatorViewModel()
        viewModel.onFocalLengthChanged("80")
        assertNull(viewModel.uiState.selectedFocalLengthPresetMm)
        assertEquals(calculateExpected(focalLengthMm = 80.0), viewModel.uiState.result)
        viewModel.onFocalLengthChanged("84")
        viewModel.onStepFocalLength(increase = true)
        assertEquals(85.0, viewModel.uiState.selectedFocalLengthPresetMm)
    }

    @Test
    fun `focal length buttons also recalculate hyperfocal mode`() {
        val viewModel = DofCalculatorViewModel()
        viewModel.onCalculatorModeSelected(CalculatorMode.Hyperfocal)
        viewModel.onStepFocalLength(increase = true)
        assertEquals(
            DepthOfFieldCalculator.calculateHyperfocalDistanceMeters(51.0, 8.0, 0.030),
            viewModel.uiState.result!!.hyperfocalDistanceMeters,
            TOLERANCE_METERS,
        )
        assertEquals("5", viewModel.uiState.focusDistanceText)
    }

    @Test
    fun `manual nonstandard focal length remains available`() {
        val viewModel = DofCalculatorViewModel()

        viewModel.onFocalLengthChanged("52,5")

        assertNull(viewModel.uiState.selectedFocalLengthPresetMm)
        assertNotNull(viewModel.uiState.result)
        assertEquals("52,5", viewModel.uiState.focalLengthText)
    }

    @Test
    fun `comma decimal separator updates calculation`() {
        val viewModel = DofCalculatorViewModel()

        viewModel.onFocalLengthChanged("35,5")
        viewModel.onApertureChanged("5,6")
        viewModel.onFocusDistanceChanged("2,5")

        val expected = DepthOfFieldCalculator.calculate(
            DepthOfFieldInput(
                focalLengthMm = 35.5,
                aperture = 5.6,
                circleOfConfusionMm = 0.030,
                focusDistanceMeters = 2.5,
            ),
        )
        assertEquals(expected, viewModel.uiState.result)
        assertNull(viewModel.uiState.inputError)
    }

    @Test
    fun `dot decimal separator updates calculation`() {
        val viewModel = DofCalculatorViewModel()

        viewModel.onFocusDistanceChanged("7.25")

        assertNotNull(viewModel.uiState.result)
        assertEquals("7.25", viewModel.uiState.focusDistanceText)
    }

    @Test
    fun `incomplete decimal input hides results`() {
        val viewModel = DofCalculatorViewModel()

        viewModel.onApertureChanged("8,")

        assertNull(viewModel.uiState.result)
        assertTrue(viewModel.uiState.apertureHasError)
        assertEquals(DofInputError.InvalidNumber, viewModel.uiState.inputError)
    }

    @Test
    fun `out of range distance hides results without NaN values`() {
        val viewModel = DofCalculatorViewModel()

        viewModel.onFocusDistanceChanged("0,05")

        assertNull(viewModel.uiState.result)
        assertEquals(DofInputError.OutOfRange, viewModel.uiState.inputError)
    }

    @Test
    fun `distance beyond hyperfocal displays infinite results`() {
        val viewModel = DofCalculatorViewModel()

        viewModel.onFocusDistanceChanged("20")

        val result = requireNotNull(viewModel.uiState.result)
        assertSame(CalculatedDistance.Infinity, result.farLimit)
        assertSame(CalculatedDistance.Infinity, result.totalDepthOfField)
    }

    @Test
    fun `hyperfocal mode focuses at H and keeps optical parameters`() {
        val viewModel = DofCalculatorViewModel()
        viewModel.onFrameFormatSelected(FrameFormat.ApsC)
        viewModel.onFocalLengthChanged("35,5")
        viewModel.onApertureChanged("5,6")
        viewModel.onFocusDistanceChanged("2,75")

        viewModel.onCalculatorModeSelected(CalculatorMode.Hyperfocal)

        val result = requireNotNull(viewModel.uiState.result)
        val expectedHyperfocal =
            DepthOfFieldCalculator.calculateHyperfocalDistanceMeters(
                focalLengthMm = 35.5,
                aperture = 5.6,
                circleOfConfusionMm = 0.018,
            )
        assertEquals(CalculatorMode.Hyperfocal, viewModel.uiState.calculatorMode)
        assertEquals(FrameFormat.ApsC, viewModel.uiState.selectedFrameFormat)
        assertEquals(0.018, viewModel.uiState.effectiveCoCMm!!, 0.0)
        assertEquals("35,5", viewModel.uiState.focalLengthText)
        assertEquals("5,6", viewModel.uiState.apertureText)
        assertEquals("2,75", viewModel.uiState.focusDistanceText)
        assertEquals(
            expectedHyperfocal,
            result.hyperfocalDistanceMeters,
            TOLERANCE_METERS,
        )
        assertEquals(
            expectedHyperfocal / 2.0,
            result.nearLimitMeters,
            TOLERANCE_METERS,
        )
        assertSame(CalculatedDistance.Infinity, result.farLimit)

        viewModel.onCalculatorModeSelected(CalculatorMode.DepthOfField)

        assertEquals("35,5", viewModel.uiState.focalLengthText)
        assertEquals("5,6", viewModel.uiState.apertureText)
        assertEquals("2,75", viewModel.uiState.focusDistanceText)
        assertNotNull(viewModel.uiState.result)
    }

    @Test
    fun `hyperfocal mode does not require focus distance input`() {
        val viewModel = DofCalculatorViewModel()
        viewModel.onFocusDistanceChanged("5,")
        assertNull(viewModel.uiState.result)

        viewModel.onCalculatorModeSelected(CalculatorMode.Hyperfocal)

        assertNotNull(viewModel.uiState.result)
        assertFalse(viewModel.uiState.focusDistanceHasError)
        assertEquals("5,", viewModel.uiState.focusDistanceText)
    }

    private fun assertFrameFormatPreset(
        frameFormat: FrameFormat,
        expectedCircleOfConfusionMm: Double,
        expectedText: String,
    ) {
        val viewModel = DofCalculatorViewModel()
        val focalLengthBeforeSelection = viewModel.uiState.focalLengthText

        viewModel.onFrameFormatSelected(frameFormat)

        assertEquals(frameFormat, viewModel.uiState.selectedFrameFormat)
        assertEquals(expectedText.replace(',', '.').toDouble(), viewModel.uiState.effectiveCoCMm!!, 0.0)
        assertEquals(
            focalLengthBeforeSelection,
            viewModel.uiState.focalLengthText,
        )
        assertEquals(
            calculateExpected(expectedCircleOfConfusionMm),
            viewModel.uiState.result,
        )
    }

    private fun calculateExpected(
        circleOfConfusionMm: Double = 0.030,
        focalLengthMm: Double = 50.0,
        aperture: Double = 8.0,
        focusDistanceMeters: Double = 5.0,
    ) = DepthOfFieldCalculator.calculate(
        DepthOfFieldInput(
            focalLengthMm = focalLengthMm,
            aperture = aperture,
            circleOfConfusionMm = circleOfConfusionMm,
            focusDistanceMeters = focusDistanceMeters,
        ),
    )

    private companion object {
        const val TOLERANCE_METERS = 1e-9
    }
}
