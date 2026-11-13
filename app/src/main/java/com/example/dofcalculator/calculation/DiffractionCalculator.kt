package com.example.dofcalculator.calculation

import com.example.dofcalculator.model.*

/** Informational Airy/CoC comparison; never modifies geometric DOF. */
object DiffractionCalculator {
    const val WAVELENGTH_MM = 0.00055
    const val AIRY_DIAMETER_FACTOR = 2.44
    // App heuristics, not universal optical sharpness standards.
    const val POSSIBLE_THRESHOLD = 1.0
    const val NOTICEABLE_THRESHOLD = 1.5

    private fun positive(value: Double): Double {
        require(value.isFinite() && value > 0.0)
        return value
    }

    fun airyDiameterMm(aperture: Double): Double =
        positive(AIRY_DIAMETER_FACTOR * WAVELENGTH_MM * positive(aperture))

    fun level(ratio: Double): DiffractionLevel {
        require(ratio.isFinite() && ratio >= 0.0)
        return when {
            ratio >= NOTICEABLE_THRESHOLD -> DiffractionLevel.NOTICEABLE
            ratio >= POSSIBLE_THRESHOLD -> DiffractionLevel.POSSIBLE
            else -> DiffractionLevel.NONE
        }
    }

    fun calculate(aperture: Double, effectiveCoCmm: Double): DiffractionResult {
        val airy = airyDiameterMm(aperture)
        val ratio = airy / positive(effectiveCoCmm)
        require(ratio.isFinite() && ratio >= 0.0)
        return DiffractionResult(aperture, airy, effectiveCoCmm, ratio, level(ratio))
    }
}
