package com.example.dofcalculator.calculation

import com.example.dofcalculator.model.*
import org.junit.Assert.*
import org.junit.Test

class MacroWarningEvaluatorTest {
    private fun evaluate(meters: Double) = MacroWarningEvaluator.evaluate(100.0, CalculatedDistance.Finite(meters))

    @Test fun `threshold compares the unrounded thin lens estimate`() {
        assertSame(MacroDetectionState.None, evaluate(2.0))
        assertSame(MacroDetectionState.None, evaluate(1.0))
        val boundary = evaluate(.72) as MacroDetectionState.MacroEstimated
        assertEquals(.2, boundary.magnification, 1e-15)
        assertEquals(MagnificationMode.AUTO, boundary.source)
        assertSame(MacroDetectionState.None, evaluate(.720000001))
        assertTrue(evaluate(.719999999) is MacroDetectionState.MacroEstimated)
        assertEquals(.2, MacroWarningEvaluator.MACRO_WARNING_THRESHOLD, 0.0)
    }

    @Test fun `close distance control values use the existing estimator without rounding`() {
        for ((d, m) in listOf(.6 to .2679491924311227, .5 to .3819660112501051, .4 to 1.0)) {
            val result = evaluate(d) as MacroDetectionState.MacroEstimated
            assertEquals(m, result.magnification, 1e-14)
            assertEquals(MagnificationEstimator.estimate(100.0, CalculatedDistance.Finite(d))!!,
                result.magnification, 0.0)
        }
    }

    @Test fun `below four focal lengths is explicitly unavailable rather than a fabricated scale`() {
        for (d in listOf(.3, .1, .001, Double.MIN_VALUE))
            assertSame(MacroDetectionState.MagnificationUnavailableAtCloseDistance, evaluate(d))
        assertNull(MagnificationEstimator.estimate(100.0, CalculatedDistance.Finite(.3)))
        assertSame(MacroDetectionState.MagnificationUnavailableAtCloseDistance,
            MacroWarningEvaluator.evaluate(Double.MAX_VALUE, CalculatedDistance.Finite(1.0)))
    }

    @Test fun `invalid inputs are distinct from optical infinity`() {
        for (bad in listOf(null, 0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY))
            assertSame(MacroDetectionState.InvalidInput,
                MacroWarningEvaluator.evaluate(bad, CalculatedDistance.Finite(.6)))
        assertSame(MacroDetectionState.InvalidInput, MacroWarningEvaluator.evaluate(100.0, null))
        assertSame(MacroDetectionState.InvalidInput, evaluate(0.0))
        assertSame(MacroDetectionState.None, MacroWarningEvaluator.evaluate(100.0, CalculatedDistance.Infinity,
            MagnificationEstimate(1.0, MagnificationMode.MANUAL)))
        for (bad in listOf(Double.NaN, Double.POSITIVE_INFINITY, -1.0))
            assertThrows(IllegalArgumentException::class.java) { CalculatedDistance.Finite(bad) }
    }

    @Test fun `very large distances converge to no warning without a finite infinity surrogate`() {
        assertSame(MacroDetectionState.None, evaluate(1e300))
        assertSame(MacroDetectionState.None, evaluate(Double.MAX_VALUE))
        assertSame(MacroDetectionState.None,
            MacroWarningEvaluator.evaluate(Double.MIN_VALUE, CalculatedDistance.Finite(1.0)))
    }

    @Test fun `confirmed Manual is labelled Manual and stale Manual uses Auto`() {
        val distance = CalculatedDistance.Finite(.6)
        val manual = MacroWarningEvaluator.evaluate(100.0, distance, MagnificationEstimate(.5, MagnificationMode.MANUAL))
            as MacroDetectionState.ManualConfirmed
        assertEquals(.5, manual.magnification, 0.0)
        assertEquals(MagnificationMode.MANUAL, manual.source)
        val stale = MacroWarningEvaluator.evaluate(100.0, distance,
            MagnificationEstimate(.5, MagnificationMode.MANUAL, ManualMagnificationSuspension.NEEDS_CONFIRMATION))
            as MacroDetectionState.MacroEstimated
        assertEquals(.2679491924311227, stale.magnification, 1e-14)
        assertEquals(MagnificationMode.AUTO, stale.source)
        assertSame(MacroDetectionState.MagnificationUnavailableAtCloseDistance,
            MacroWarningEvaluator.evaluate(100.0, CalculatedDistance.Finite(.3),
                MagnificationEstimate(.5, MagnificationMode.MANUAL, ManualMagnificationSuspension.NEEDS_CONFIRMATION)))
    }
}
