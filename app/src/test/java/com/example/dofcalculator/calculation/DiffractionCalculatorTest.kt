package com.example.dofcalculator.calculation

import com.example.dofcalculator.model.*
import org.junit.Assert.*
import org.junit.Test

class DiffractionCalculatorTest {
    @Test fun `550 nm Airy disk control examples in micrometres`() {
        for ((n, microns) in listOf(4.0 to 5.368, 8.0 to 10.736, 11.0 to 14.762,
            16.0 to 21.472, 22.0 to 29.524)) {
            assertEquals(microns, DiffractionCalculator.airyDiameterMm(n) * 1000.0, 1e-12)
        }
    }

    @Test fun `macro effective apertures and Airy diameters are continuous`() {
        for ((m, n) in listOf(0.0 to 8.0, .1 to 8.8, .25 to 10.0, .5 to 12.0,
            1.0 to 16.0, 2.0 to 24.0, 5.0 to 48.0)) {
            assertEquals(n, DiffractionCalculator.effectiveAperture(8.0, m), 1e-12)
        }
        for ((m, microns) in listOf(0.0 to 10.736, .5 to 16.104, 1.0 to 21.472, 2.0 to 32.208)) {
            val result = DiffractionCalculator.calculate(8.0, .03, MagnificationEstimate(m))
            assertEquals(microns, result.airyDiameterMm * 1000.0, 1e-12)
        }
        assertTrue(DiffractionCalculator.effectiveAperture(8.0, .09999) <
            DiffractionCalculator.effectiveAperture(8.0, .10001))
        assertTrue(DiffractionCalculator.effectiveAperture(8.0, .24999) <
            DiffractionCalculator.effectiveAperture(8.0, .25001))
    }

    @Test fun `warning levels match all control examples`() {
        data class Case(val n: Double, val m: Double, val coc: Double, val level: DiffractionLevel)
        for (case in listOf(
            Case(8.0, 0.0, .03, DiffractionLevel.NONE),
            Case(16.0, 0.0, .03, DiffractionLevel.POSSIBLE),
            Case(32.0, 0.0, .03, DiffractionLevel.NOTICEABLE),
            Case(8.0, 1.0, .03, DiffractionLevel.POSSIBLE),
            Case(8.0, 2.0, .03, DiffractionLevel.NOTICEABLE),
            Case(8.0, 0.0, .013, DiffractionLevel.POSSIBLE),
            Case(11.0, 0.0, .013, DiffractionLevel.NOTICEABLE),
            Case(8.0, 1.0, .013, DiffractionLevel.NOTICEABLE))) {
            val result = DiffractionCalculator.calculate(case.n, case.coc, MagnificationEstimate(case.m))
            assertEquals(case.level, result.level)
            assertEquals(result.airyDiameterMm / case.coc, result.ratio, 0.0)
        }
        assertEquals(DiffractionLevel.NONE, DiffractionCalculator.level(.7 - 1e-12))
        assertEquals(DiffractionLevel.POSSIBLE, DiffractionCalculator.level(.7))
        assertEquals(DiffractionLevel.POSSIBLE, DiffractionCalculator.level(1.0 - 1e-12))
        assertEquals(DiffractionLevel.NOTICEABLE, DiffractionCalculator.level(1.0))
    }

    @Test fun `unknown scale stays unknown and is explicitly uncorrected`() {
        val result = DiffractionCalculator.calculate(16.0, .03, MagnificationEstimate(null))
        assertNull(result.magnification.value)
        assertEquals(16.0, result.effectiveAperture, 0.0)
        assertEquals(DiffractionLevel.POSSIBLE, result.level)
    }

    @Test fun `invalid values overflow and extreme ratios never yield NaN`() {
        for (bad in listOf(0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY)) {
            assertTrue(runCatching { DiffractionCalculator.airyDiameterMm(bad) }.isFailure)
            assertTrue(runCatching { DiffractionCalculator.calculate(8.0, bad, MagnificationEstimate(0.0)) }.isFailure)
        }
        for (bad in listOf(-1.0, Double.NaN, Double.POSITIVE_INFINITY)) {
            assertTrue(runCatching { DiffractionCalculator.effectiveAperture(8.0, bad) }.isFailure)
            assertTrue(runCatching { DiffractionCalculator.level(bad) }.isFailure)
        }
        assertTrue(runCatching { DiffractionCalculator.effectiveAperture(Double.MAX_VALUE, 5.0) }.isFailure)
        assertTrue(runCatching {
            DiffractionCalculator.calculate(8.0, Double.MIN_VALUE, MagnificationEstimate(1.0))
        }.isFailure)
        for (coc in listOf(1e-250, 1e250)) {
            val result = DiffractionCalculator.calculate(8.0, coc, MagnificationEstimate(1.0))
            assertTrue(result.ratio.isFinite() && result.ratio >= 0.0)
        }
    }
}
