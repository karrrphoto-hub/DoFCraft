package com.example.dofcalculator.calculation

import com.example.dofcalculator.model.*
import org.junit.Assert.*
import org.junit.Test

class SharpnessCriterionCalculatorTest {
    private val defaults = SharpnessSettings()
    private fun coc(s: SharpnessSettings = defaults, f: FrameFormat = FrameFormat.FullFrame35Mm) =
        SharpnessCriterionCalculator.calculate(f.widthMm, f.heightMm, f.suggestedCircleOfConfusionMm, s)

    @Test fun `standard uses exact format recommendation and ignores pixel parameters`() {
        assertEquals(.030, coc(), 0.0)
        assertEquals(.053, coc(f = FrameFormat.MediumFormat6x6), 0.0)
        assertEquals(.018, coc(f = FrameFormat.ApsCCanon), 0.0)
        assertEquals(coc(), coc(defaults.copy(megapixels = 45.7, pixelBlur = 1.0)), 0.0)
    }

    @Test fun `pixel control examples use full physical frame area`() {
        val s = defaults.copy(mode = SharpnessCriterionMode.PIXEL_BASED)
        assertEquals(.018, coc(s), 1e-14)
        assertEquals(.012, coc(s.copy(pixelBlur = 2.0)), 1e-14)
        assertEquals(.013145341380123988, coc(s.copy(megapixels = 45.0)), 1e-14)
        assertEquals(.008763560920082658, coc(s.copy(megapixels = 45.0, pixelBlur = 2.0)), 1e-14)
        assertTrue(coc(s.copy(megapixels = 45.7)) < coc(s))
        assertEquals(coc(s) * 2, coc(s.copy(pixelBlur = 6.0)), 1e-14)
    }

    @Test fun `print visual model replaces the previous PPI based criterion`() {
        val s = defaults.copy(mode = SharpnessCriterionMode.PRINT_BASED)
        assertEquals(.02327105734962527, coc(s), 1e-14)
        assertEquals(coc(s), coc(s.copy(printWidthCm = 45.0, printHeightCm = 30.0)), 1e-14)
        assertEquals(coc(s), coc(s.copy(printPpi = 600.0, printDpi = 2400.0)), 0.0)
    }

    @Test fun `custom keeps literal precision`() {
        val s = defaults.copy(mode = SharpnessCriterionMode.CUSTOM)
        for (value in listOf(.015, .00876, .0087654321098765, 1e-200)) {
            assertEquals(value, coc(s.copy(customCoCmm = value)), 0.0)
        }
    }

    @Test fun `all invalid active inputs are rejected centrally`() {
        for (invalid in listOf(0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY)) {
            for (field in SharpnessField.entries) {
                val mode = when (field) {
                    SharpnessField.MEGAPIXELS, SharpnessField.PIXEL_BLUR -> SharpnessCriterionMode.PIXEL_BASED
                    SharpnessField.CUSTOM_COC -> SharpnessCriterionMode.CUSTOM
                    SharpnessField.SCREEN_DIAGONAL, SharpnessField.SCREEN_WIDTH, SharpnessField.SCREEN_HEIGHT,
                    SharpnessField.SCREEN_VIEWING, SharpnessField.SCREEN_ANGLE, SharpnessField.SCREEN_BLUR ->
                        SharpnessCriterionMode.SCREEN_BASED
                    else -> SharpnessCriterionMode.PRINT_BASED
                }
                assertTrue("$field $invalid", runCatching { coc(field.update(defaults.copy(mode = mode,
                    screenCriterion = if (field == SharpnessField.SCREEN_BLUR) ScreenCriterion.DISPLAY_PIXELS
                        else ScreenCriterion.VISUAL), invalid)) }.isFailure)
            }
            for (mode in listOf(SharpnessCriterionMode.PIXEL_BASED, SharpnessCriterionMode.PRINT_BASED)) {
                assertTrue(runCatching { SharpnessCriterionCalculator.calculate(invalid, 24.0, .03, defaults.copy(mode = mode)) }.isFailure)
                assertTrue(runCatching { SharpnessCriterionCalculator.calculate(36.0, invalid, .03, defaults.copy(mode = mode)) }.isFailure)
            }
        }
    }

    @Test fun `missing geometry and numeric overflows never yield zero NaN or infinity`() {
        val pixel = defaults.copy(mode = SharpnessCriterionMode.PIXEL_BASED)
        assertTrue(runCatching { SharpnessCriterionCalculator.calculate(null, null, null, pixel) }.isFailure)
        assertTrue(runCatching { coc(pixel.copy(megapixels = Double.MAX_VALUE)) }.isFailure)
        assertTrue(runCatching { coc(defaults.copy(mode = SharpnessCriterionMode.PRINT_BASED,
            printWidthCm = Double.MAX_VALUE)) }.isFailure)
        assertTrue(runCatching { SharpnessCriterionCalculator.calculate(Double.MAX_VALUE,
            Double.MAX_VALUE, .03, pixel) }.isFailure)
        assertTrue(runCatching { coc(defaults, FrameFormat.Custom) }.isFailure)
    }

    @Test fun `decimal input accepts dot comma and fractional MP but rejects drafts and nonnumbers`() {
        assertEquals(45.7, SharpnessCriterionCalculator.positiveNumber("45,7")!!, 0.0)
        assertEquals(.00876, SharpnessCriterionCalculator.positiveNumber("0.00876")!!, 0.0)
        for (text in listOf("", " ", "0", "-1", "NaN", "Infinity", "45,", "1.2.3", "1e9999", "9".repeat(400))) {
            assertNull(text, SharpnessCriterionCalculator.positiveNumber(text))
        }
    }
}
