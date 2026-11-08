package com.example.dofcalculator.calculation

import com.example.dofcalculator.model.CalculatedDistance
import kotlin.math.exp
import kotlin.math.ln

/** Pure UI scale. Minimum is a UI bound, never a lens specification. */
class FocusDistanceScale(val minimumMeters: Double, val hyperfocalMeters: Double) {
    init {
        require(minimumMeters.isFinite() && minimumMeters > 0.0)
        require(hyperfocalMeters.isFinite() && hyperfocalMeters > 0.0)
    }
    private val logSpan = ln(hyperfocalMeters) - ln(minimumMeters)
    val hasHyperfocalMarker: Boolean get() = logSpan > 1e-9

    fun distanceAt(position: Double): CalculatedDistance {
        require(position.isFinite())
        val t = position.coerceIn(0.0, 1.0)
        if (t == 0.0) return CalculatedDistance.Finite(minimumMeters)
        if (t == 1.0) return CalculatedDistance.Infinity
        val meters = if (!hasHyperfocalMarker) {
            minimumMeters / (1.0 - t)
        } else if (t <= 0.75) {
            if (t == 0.75) hyperfocalMeters
            else exp(ln(minimumMeters) + logSpan * (t / 0.75))
        } else {
            hyperfocalMeters / ((1.0 - t) / 0.25)
        }
        // Saturate only at Double's representational limit, never treat it as infinity.
        return CalculatedDistance.Finite(meters.coerceAtMost(Double.MAX_VALUE))
    }

    fun positionOf(distance: CalculatedDistance): Double = when (distance) {
        CalculatedDistance.Infinity -> 1.0
        is CalculatedDistance.Finite -> {
            require(distance.meters > 0.0)
            val s = distance.meters
            (if (!hasHyperfocalMarker) 1.0 - minimumMeters / s
            else if (s <= hyperfocalMeters)
                0.75 * (ln(s) - ln(minimumMeters)) / logSpan
            else 0.75 + 0.25 * (1.0 - hyperfocalMeters / s)).coerceIn(0.0, 1.0)
        }
    }

    companion object {
        const val MINIMUM_METERS = 0.1

        fun create(hyperfocalMeters: Double): FocusDistanceScale =
            FocusDistanceScale(MINIMUM_METERS, hyperfocalMeters)
    }
}
