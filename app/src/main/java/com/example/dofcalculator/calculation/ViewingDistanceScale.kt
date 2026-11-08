package com.example.dofcalculator.calculation

import kotlin.math.exp
import kotlin.math.ln

/** Slider convenience range in cm. Manual viewing distances are never clamped or overwritten. */
class ViewingDistanceScale(currentCm: Double) {
    val minimumCm = 5.0
    val maximumCm = maxOf(1000.0, OutputSharpnessCalculator.positive(currentCm))
    private val span = ln(maximumCm) - ln(minimumCm)

    fun positionOf(cm: Double): Double =
        ((ln(OutputSharpnessCalculator.positive(cm)) - ln(minimumCm)) / span).coerceIn(0.0, 1.0)

    fun distanceAt(position: Double): Double {
        require(position.isFinite())
        val t = position.coerceIn(0.0, 1.0)
        return when (t) {
            0.0 -> minimumCm
            1.0 -> maximumCm
            else -> exp(ln(minimumCm) + span * t).coerceAtMost(maximumCm)
        }
    }
}
