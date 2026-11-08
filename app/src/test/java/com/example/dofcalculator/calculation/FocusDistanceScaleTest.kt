package com.example.dofcalculator.calculation

import com.example.dofcalculator.model.CalculatedDistance
import kotlin.math.abs
import org.junit.Assert.*
import org.junit.Test

class FocusDistanceScaleTest {
    private fun h(f: Double) =
        DepthOfFieldCalculator.calculateHyperfocalDistanceMeters(f, 8.0, .030)
    private fun meters(value: CalculatedDistance) = (value as CalculatedDistance.Finite).meters

    @Test fun `optical examples and fixed minimum`() {
        assertEquals(.9525, h(15.0), 1e-12)
        assertEquals(2.424, h(24.0), 1e-12)
        assertEquals(166.86666666666667, h(200.0), 1e-10)
        assertEquals(.1, FocusDistanceScale.create(h(24.0)).minimumMeters, 0.0)
        assertEquals(.1, FocusDistanceScale.create(h(50.0)).minimumMeters, 0.0)
        assertEquals(.1, FocusDistanceScale.create(h(200.0)).minimumMeters, 0.0)
    }

    @Test fun `landmarks continuity and optical infinity`() {
        val scale = FocusDistanceScale(.3, h(24.0))
        assertEquals(.3, meters(scale.distanceAt(0.0)), 1e-14)
        assertEquals(h(24.0), meters(scale.distanceAt(.75)), 0.0)
        assertEquals(2 * h(24.0), meters(scale.distanceAt(.875)), 1e-12)
        assertEquals(10 * h(24.0), meters(scale.distanceAt(.975)), 1e-11)
        assertSame(CalculatedDistance.Infinity, scale.distanceAt(1.0))
        assertEquals(1.0, scale.positionOf(CalculatedDistance.Infinity), 0.0)
        assertTrue(abs(meters(scale.distanceAt(.75 - 1e-10)) - h(24.0)) < 1e-8)
        assertTrue(abs(meters(scale.distanceAt(.75 + 1e-10)) - h(24.0)) < 1e-8)
    }

    @Test fun `round trip monotonic finite positive scales including enormous H`() {
        for (scale in listOf(FocusDistanceScale(.3, 2.424),
            FocusDistanceScale(2.0, h(200.0)), FocusDistanceScale(.3, 1e290),
            FocusDistanceScale(.3, .1), FocusDistanceScale(.3, .3),
            FocusDistanceScale(.3, .3 * (1 + 1e-10)))) {
            var previous = 0.0
            for (i in 0..999) {
                val t = i / 1000.0
                val value = scale.distanceAt(t)
                val s = meters(value)
                assertTrue(s.isFinite() && s > 0.0 && s >= previous)
                assertEquals(t, scale.positionOf(value), 1e-12)
                previous = s
            }
            assertTrue(meters(scale.distanceAt(Math.nextDown(1.0))).isFinite())
        }
    }

    @Test fun `fallback hides unreachable H and fixed scale clamps positions below minimum`() {
        val fallback = FocusDistanceScale(2.0, 1.0)
        assertFalse(fallback.hasHyperfocalMarker)
        assertEquals(4.0, meters(fallback.distanceAt(.5)), 0.0)
        val fixed = FocusDistanceScale.create(h(200.0))
        assertEquals(.1, fixed.minimumMeters, 0.0)
        assertEquals(.1, meters(fixed.distanceAt(0.0)), 0.0)
        assertEquals(0.0, fixed.positionOf(CalculatedDistance.Finite(.05)), 0.0)
    }

    @Test fun `invalid inputs are rejected and finite overflow saturates safely`() {
        for (v in listOf(0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY)) {
            assertTrue(runCatching { FocusDistanceScale(v, 1.0) }.isFailure)
            assertTrue(runCatching { FocusDistanceScale(1.0, v) }.isFailure)
        }
        val scale = FocusDistanceScale(1.0, Double.MAX_VALUE)
        assertTrue(runCatching { scale.distanceAt(Double.NaN) }.isFailure)
        assertEquals(Double.MAX_VALUE, meters(scale.distanceAt(.99)), 0.0)
        assertEquals(0.0, scale.positionOf(CalculatedDistance.Finite(.1)), 0.0)
    }
}
