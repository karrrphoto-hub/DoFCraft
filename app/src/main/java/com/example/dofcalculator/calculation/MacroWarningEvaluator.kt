package com.example.dofcalculator.calculation

import com.example.dofcalculator.model.*

object MacroWarningEvaluator {
    const val MACRO_WARNING_THRESHOLD = 0.2

    /**
     * Consume the existing resolved estimate when available. Standalone callers use
     * MagnificationEstimator, never a second magnification formula.
     */
    fun evaluate(
        focalLengthMm: Double?,
        distance: CalculatedDistance?,
        resolved: MagnificationEstimate? = null,
    ): MacroDetectionState {
        if (focalLengthMm == null || !focalLengthMm.isFinite() || focalLengthMm <= 0.0 || distance == null)
            return MacroDetectionState.InvalidInput
        if (distance == CalculatedDistance.Infinity) return MacroDetectionState.None
        val meters = (distance as CalculatedDistance.Finite).meters
        if (meters <= 0.0) return MacroDetectionState.InvalidInput
        if (resolved?.source == MagnificationMode.MANUAL && resolved.suspension == null &&
            resolved.value != null && resolved.value > 0.0)
            return MacroDetectionState.ManualConfirmed(resolved.value)
        // A suspended manual value must not be reused for a different distance.
        val m = if (resolved?.source == MagnificationMode.AUTO) resolved.value
            else MagnificationEstimator.estimate(focalLengthMm, distance)
        if (m == null) {
            // 4f in metres, divided first to avoid overflow at extreme finite focal lengths.
            return if (meters < focalLengthMm / 250.0)
                MacroDetectionState.MagnificationUnavailableAtCloseDistance
            else MacroDetectionState.InvalidInput
        }
        if (!m.isFinite() || m !in 0.0..1.0) return MacroDetectionState.InvalidInput
        return if (m >= MACRO_WARNING_THRESHOLD) MacroDetectionState.MacroEstimated(m)
            else MacroDetectionState.None
    }
}
