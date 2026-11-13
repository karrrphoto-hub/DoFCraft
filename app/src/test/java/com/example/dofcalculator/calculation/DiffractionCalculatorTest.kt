package com.example.dofcalculator.calculation

import com.example.dofcalculator.model.DiffractionLevel
import org.junit.Assert.*
import org.junit.Test

class DiffractionCalculatorTest {
    @Test fun `nominal apertures produce the conventional 550 nm Airy diameters`() {
        for ((n, microns) in listOf(4.0 to 5.368, 8.0 to 10.736, 11.0 to 14.762,
            16.0 to 21.472, 22.0 to 29.524)) {
            val result = DiffractionCalculator.calculate(n, .03)
            assertEquals(n, result.nominalAperture, 0.0)
            assertEquals(microns, result.airyDiameterMm * 1000.0, 1e-12)
            assertEquals(result.airyDiameterMm / .03, result.ratio, 0.0)
        }
    }

    @Test fun `full frame Standard uses the less alarming practical thresholds`() {
        for ((aperture, expected) in listOf(
            8.0 to DiffractionLevel.NONE,
            11.0 to DiffractionLevel.NONE,
            16.0 to DiffractionLevel.NONE,
            22.0 to DiffractionLevel.NONE,
            32.0 to DiffractionLevel.POSSIBLE,
            45.0 to DiffractionLevel.NOTICEABLE,
        )) {
            val result = DiffractionCalculator.calculate(aperture, .030)
            assertEquals(expected, result.level)
            assertEquals(.001342 * aperture, result.airyDiameterMm, 1e-15)
            assertEquals(result.airyDiameterMm / .030, result.ratio, 0.0)
        }
    }

    @Test fun `classification uses unrounded ratios at both exact boundaries`() {
        for ((ratio, expected) in listOf(
            .5 to DiffractionLevel.NONE,
            .7 to DiffractionLevel.NONE,
            .999999 to DiffractionLevel.NONE,
            Math.nextDown(1.0) to DiffractionLevel.NONE,
            1.0 to DiffractionLevel.POSSIBLE,
            Math.nextUp(1.0) to DiffractionLevel.POSSIBLE,
            1.2 to DiffractionLevel.POSSIBLE,
            1.499999 to DiffractionLevel.POSSIBLE,
            Math.nextDown(1.5) to DiffractionLevel.POSSIBLE,
            1.5 to DiffractionLevel.NOTICEABLE,
            Math.nextUp(1.5) to DiffractionLevel.NOTICEABLE,
            2.0 to DiffractionLevel.NOTICEABLE,
        )) assertEquals("R=$ratio", expected, DiffractionCalculator.level(ratio))
    }

    @Test fun `stricter CoC produces earlier warnings without changing Airy diameter`() {
        val standard = DiffractionCalculator.calculate(22.0, .030)
        val possible = DiffractionCalculator.calculate(22.0, .025)
        val noticeable = DiffractionCalculator.calculate(22.0, .013)
        assertEquals(DiffractionLevel.NONE, standard.level)
        assertEquals(DiffractionLevel.POSSIBLE, possible.level)
        assertEquals(DiffractionLevel.NOTICEABLE, noticeable.level)
        assertEquals(standard.airyDiameterMm, possible.airyDiameterMm, 0.0)
        assertEquals(standard.airyDiameterMm, noticeable.airyDiameterMm, 0.0)
    }

    @Test fun `invalid inputs and overflowing ratios never reach the UI`() {
        for (bad in listOf(0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY)) {
            assertTrue(runCatching { DiffractionCalculator.airyDiameterMm(bad) }.isFailure)
            assertTrue(runCatching { DiffractionCalculator.calculate(bad, .03) }.isFailure)
            assertTrue(runCatching { DiffractionCalculator.calculate(8.0, bad) }.isFailure)
        }
        for (bad in listOf(-1.0, Double.NaN, Double.POSITIVE_INFINITY))
            assertTrue(runCatching { DiffractionCalculator.level(bad) }.isFailure)
        assertTrue(runCatching { DiffractionCalculator.calculate(8.0, Double.MIN_VALUE) }.isFailure)
        for (coc in listOf(1e-250, 1e250)) {
            val result = DiffractionCalculator.calculate(8.0, coc)
            assertTrue(result.ratio.isFinite() && result.ratio >= 0.0)
        }
    }
}
