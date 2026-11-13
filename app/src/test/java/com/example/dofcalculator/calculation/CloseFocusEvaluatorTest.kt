package com.example.dofcalculator.calculation

import com.example.dofcalculator.model.CalculatedDistance
import com.example.dofcalculator.model.CloseFocusWarningState
import org.junit.Assert.*
import org.junit.Test

class CloseFocusEvaluatorTest {
    private fun evaluate(meters: Double, focal: Double = 100.0) =
        CloseFocusEvaluator.evaluate(focal, CalculatedDistance.Finite(meters))

    @Test fun `one to five threshold uses unrounded numbers with only floating point tolerance`() {
        assertSame(CloseFocusWarningState.None, evaluate(2.0))
        assertSame(CloseFocusWarningState.None, evaluate(1.0))
        val boundary = evaluate(.72) as CloseFocusWarningState.EstimatedMacro
        assertEquals(.2, boundary.magnification, 1e-15)
        assertSame(CloseFocusWarningState.None, evaluate(.720000001))
        assertTrue(evaluate(.719999999) is CloseFocusWarningState.EstimatedMacro)
        assertTrue(evaluate(Math.nextUp(.72)) is CloseFocusWarningState.EstimatedMacro)
    }

    @Test fun `reference close distance estimates match the thin lens equation`() {
        for ((distance, expected) in listOf(.72 to .2, .6 to .2679491924311227,
            .5 to .3819660112501051, .4 to 1.0)) {
            val state = evaluate(distance) as CloseFocusWarningState.EstimatedMacro
            assertEquals(expected, state.magnification, 1e-14)
            val reconstructedDistance = .1 * (2.0 + state.magnification + 1.0 / state.magnification)
            assertEquals(distance, reconstructedDistance, 1e-14)
        }
    }

    @Test fun `below four focal lengths is unavailable without an invented magnification`() {
        assertSame(CloseFocusWarningState.TooCloseToEstimate, evaluate(.3))
        assertSame(CloseFocusWarningState.TooCloseToEstimate, evaluate(.399999999))
        assertSame(CloseFocusWarningState.TooCloseToEstimate, evaluate(Double.MIN_VALUE))
        assertEquals(1.0, (evaluate(.4) as CloseFocusWarningState.EstimatedMacro).magnification, 0.0)
        assertEquals(1.0, (evaluate(Math.nextDown(.4)) as CloseFocusWarningState.EstimatedMacro).magnification, 0.0)
        assertEquals(1.0, (evaluate(Math.nextUp(.4)) as CloseFocusWarningState.EstimatedMacro).magnification, 0.0)
    }

    @Test fun `the threshold and four focal boundary scale with nominal focal length only`() {
        for (f in listOf(1.0, 17.0, 58.0, 100.0, 135.5, 1200.0, 1e100, 1e-100)) {
            val atThreshold = evaluate((f / 1000.0) * 7.2, f) as CloseFocusWarningState.EstimatedMacro
            assertEquals(.2, atThreshold.magnification, 1e-14)
            val atBoundary = evaluate((f / 1000.0) * 4.0, f) as CloseFocusWarningState.EstimatedMacro
            assertEquals(1.0, atBoundary.magnification, 0.0)
        }
    }

    @Test fun `invalid inputs are distinct from optical infinity`() {
        for (bad in listOf(null, 0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY))
            assertSame(CloseFocusWarningState.InvalidInput,
                CloseFocusEvaluator.evaluate(bad, CalculatedDistance.Finite(.6)))
        assertSame(CloseFocusWarningState.InvalidInput, CloseFocusEvaluator.evaluate(100.0, null))
        assertSame(CloseFocusWarningState.InvalidInput, evaluate(0.0))
        assertSame(CloseFocusWarningState.None, CloseFocusEvaluator.evaluate(100.0, CalculatedDistance.Infinity))
    }

    @Test fun `extreme positive inputs do not overflow the quadratic or invent infinity focus`() {
        assertSame(CloseFocusWarningState.None, evaluate(Double.MAX_VALUE))
        assertSame(CloseFocusWarningState.None, evaluate(Double.MAX_VALUE, Double.MIN_VALUE))
        assertSame(CloseFocusWarningState.TooCloseToEstimate, evaluate(Double.MIN_VALUE, Double.MAX_VALUE))
        assertSame(CloseFocusWarningState.None, evaluate(Double.MAX_VALUE, Double.MAX_VALUE))
    }
}
