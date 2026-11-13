package com.example.dofcalculator.calculation

import com.example.dofcalculator.model.CalculatedDistance
import com.example.dofcalculator.model.CloseFocusWarningState
import kotlin.math.sqrt

/** Thin-lens estimate from nominal focal length (mm) and sensor-plane distance (metres). */
object CloseFocusEvaluator {
    const val CLOSE_FOCUS_WARNING_THRESHOLD = 0.2
    private const val MAX_FOCAL_DISTANCE_RATIO = 0.25 // D = 4f
    // A few ULPs absorb conversion/rounding at the boundaries, not a physical hysteresis band.
    private val boundaryTolerance = 8.0 * Math.ulp(MAX_FOCAL_DISTANCE_RATIO)
    private val thresholdTolerance = 8.0 * Math.ulp(CLOSE_FOCUS_WARNING_THRESHOLD)

    fun evaluate(focalLengthMm: Double?, distance: CalculatedDistance?): CloseFocusWarningState {
        if (focalLengthMm == null || !focalLengthMm.isFinite() || focalLengthMm <= 0.0 || distance == null)
            return CloseFocusWarningState.InvalidInput
        if (distance == CalculatedDistance.Infinity) return CloseFocusWarningState.None
        val meters = (distance as CalculatedDistance.Finite).meters
        if (meters <= 0.0) return CloseFocusWarningState.InvalidInput

        // x = f/D. Do not multiply a very large metre distance by 1000.
        val x = (focalLengthMm / meters) / 1000.0
        if (x > MAX_FOCAL_DISTANCE_RATIO + boundaryTolerance)
            return CloseFocusWarningState.TooCloseToEstimate
        // Equivalent to m = 2 / (A + sqrt(A*A - 4)), A = D/f - 2.
        // Reciprocal scaling avoids cancellation and overflow of A*A at large distances.
        val boundedX = if (kotlin.math.abs(x - MAX_FOCAL_DISTANCE_RATIO) <= boundaryTolerance)
            MAX_FOCAL_DISTANCE_RATIO else x
        val inverseA = boundedX / (1.0 - 2.0 * boundedX)
        val m = (2.0 * inverseA) /
            (1.0 + sqrt((1.0 - 2.0 * inverseA) * (1.0 + 2.0 * inverseA)))
        if (!m.isFinite() || m !in 0.0..1.0) return CloseFocusWarningState.InvalidInput
        return if (m >= CLOSE_FOCUS_WARNING_THRESHOLD - thresholdTolerance)
            CloseFocusWarningState.EstimatedMacro(m)
        else CloseFocusWarningState.None
    }
}
