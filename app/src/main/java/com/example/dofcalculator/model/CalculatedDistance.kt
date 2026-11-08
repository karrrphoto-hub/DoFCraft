package com.example.dofcalculator.model

/**
 * A calculated distance expressed in metres or an explicitly infinite distance.
 */
sealed interface CalculatedDistance {
    data class Finite(val meters: Double) : CalculatedDistance {
        init {
            require(meters.isFinite() && (meters >= 0.0)) {
                "A finite calculated distance must be non-negative and finite."
            }
        }
    }

    data object Infinity : CalculatedDistance
}
