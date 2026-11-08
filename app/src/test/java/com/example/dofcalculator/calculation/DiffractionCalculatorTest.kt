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

    @Test fun `warning levels retain the existing heuristic thresholds`() {
        for ((n, coc, expected) in listOf(
            Triple(8.0, .03, DiffractionLevel.NONE),
            Triple(16.0, .03, DiffractionLevel.POSSIBLE),
            Triple(32.0, .03, DiffractionLevel.NOTICEABLE),
            Triple(8.0, .013, DiffractionLevel.POSSIBLE),
            Triple(11.0, .013, DiffractionLevel.NOTICEABLE))) {
            assertEquals(expected, DiffractionCalculator.calculate(n, coc).level)
        }
        assertEquals(DiffractionLevel.NONE, DiffractionCalculator.level(.7 - 1e-12))
        assertEquals(DiffractionLevel.POSSIBLE, DiffractionCalculator.level(.7))
        assertEquals(DiffractionLevel.POSSIBLE, DiffractionCalculator.level(1.0 - 1e-12))
        assertEquals(DiffractionLevel.NOTICEABLE, DiffractionCalculator.level(1.0))
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
