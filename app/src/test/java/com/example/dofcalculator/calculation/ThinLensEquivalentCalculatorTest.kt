package com.example.dofcalculator.calculation

import com.example.dofcalculator.model.*
import com.example.dofcalculator.settings.DiffractionSettings
import org.junit.Assert.*
import org.junit.Test

class ThinLensEquivalentCalculatorTest {
    @Test fun `reference equivalents and metre conversion match the thin lens equation`() {
        for ((d, m, f) in listOf(Triple(300.0, 1.0, 75.0), Triple(600.0, .5, 133.33333333333333),
            Triple(30.0, 1.0, 7.5), Triple(400.0, 1.0, 100.0))) {
            assertEquals(f, ThinLensEquivalentCalculator.focalLengthMm(d, m), 1e-12)
            assertEquals(f, ThinLensEquivalentCalculator.focalLengthFromMeters(d / 1000, m), 1e-12)
        }
    }

    @Test fun `reciprocal branches are stable for tiny and huge magnifications`() {
        for (m in listOf(1e-300, 1e-100, .1, .5, 1.0, 2.0, 1e100, 1e300, Double.MAX_VALUE)) {
            val f = ThinLensEquivalentCalculator.focalLengthMm(400.0, m)
            assertTrue(f.isFinite() && f > 0)
            assertEquals(f, ThinLensEquivalentCalculator.focalLengthMm(400.0, 1 / m), f * 1e-14)
        }
        assertEquals(1.0, ThinLensEquivalentCalculator.focalLengthMm(Double.MAX_VALUE, Double.MAX_VALUE), 1e-14)
        assertTrue(ThinLensEquivalentCalculator.focalLengthFromMeters(Double.MIN_VALUE, 1.0) > 0)
        assertTrue(runCatching { ThinLensEquivalentCalculator.focalLengthMm(Double.MIN_VALUE, 1.0) }.isFailure)
    }

    @Test fun `nonfinite zero and negative physical inputs are rejected`() {
        for (bad in listOf(0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY)) {
            assertTrue(runCatching { ThinLensEquivalentCalculator.focalLengthMm(bad, 1.0) }.isFailure)
            assertTrue(runCatching { ThinLensEquivalentCalculator.focalLengthMm(300.0, bad) }.isFailure)
            assertTrue(runCatching { ThinLensEquivalentCalculator.focalLengthFromMeters(bad, 1.0) }.isFailure)
        }
    }

    @Test fun `consistency thresholds are warnings and D below four nominal focal lengths is allowed`() {
        val normal = ThinLensEquivalentCalculator.validate("1", "1", "0.30", 100.0, 8.0).preview!!
        assertEquals(.75, normal.focalRatio, 1e-14)
        assertFalse(normal.unusual) // 300 mm is below 4 × nominal 100 mm.
        assertEquals(75.0, normal.equivalentFocalLengthMm, 1e-14)
        val small = ThinLensEquivalentCalculator.validate("1", "1", "0.03", 100.0, 8.0).preview!!
        assertEquals(.075, small.focalRatio, 1e-14)
        assertTrue(small.unusual)
        assertFalse(ThinLensEquivalentCalculator.unusual(.25))
        assertFalse(ThinLensEquivalentCalculator.unusual(1.5))
        assertTrue(ThinLensEquivalentCalculator.unusual(1.500001))
    }

    @Test fun `two ratio fields preserve the meaning of one to two and two to one`() {
        for ((a, b, m) in listOf(Triple("1", "2", .5), Triple("2", "1", 2.0),
            Triple("1", "1", 1.0), Triple("1,5", "2", .75))) {
            val preview = ThinLensEquivalentCalculator.validate(a, b, "0,3", 100.0, 8.0).preview!!
            assertEquals(m, preview.magnification, 0.0)
            assertEquals(8.0 * (1 + m), preview.effectiveAperture, 1e-13)
        }
        for (bad in listOf("", "0", "-1", "NaN", "Infinity", "1,", "1:" , "x")) {
            assertTrue(MacroInputField.DENOMINATOR in
                ThinLensEquivalentCalculator.validate("1", bad, ".3", 100.0, 8.0).errors)
            assertNull(ThinLensEquivalentCalculator.validate(bad, "1", "0.3", 100.0, 8.0).preview)
            assertNull(ThinLensEquivalentCalculator.validate("1", "1", bad, 100.0, 8.0).preview)
        }
    }

    @Test fun `manual pair validation uses numeric tolerance and explicit confirmation`() {
        val pair = DiffractionSettings(magnificationMode = MagnificationMode.MANUAL,
            manualMagnification = 1.0, manualDistanceMeters = .3,
            manualFocalLengthMm = 100.0, manualConfirmed = true)
        fun status(d: CalculatedDistance, f: Double = 100.0, p: DiffractionSettings = pair) =
            ManualMagnificationValidator.assess(p, d, f).status
        assertEquals(ManualMagnificationStatus.VALID, status(CalculatedDistance.Finite(.30)))
        assertEquals(ManualMagnificationStatus.VALID, status(CalculatedDistance.Finite(.3 + 1e-12)))
        assertEquals(ManualMagnificationStatus.NEEDS_CONFIRMATION, status(CalculatedDistance.Finite(.3001)))
        assertEquals(ManualMagnificationStatus.NEEDS_CONFIRMATION, status(CalculatedDistance.Finite(.3), 85.0))
        assertEquals(ManualMagnificationStatus.NEEDS_CONFIRMATION,
            status(CalculatedDistance.Finite(.3), p = pair.copy(manualConfirmed = false)))
        assertEquals(ManualMagnificationStatus.SUSPENDED_AT_INFINITY, status(CalculatedDistance.Infinity))
        assertEquals(ManualMagnificationStatus.INVALID,
            status(CalculatedDistance.Finite(.3), p = pair.copy(manualDistanceMeters = -1.0)))
    }
}
