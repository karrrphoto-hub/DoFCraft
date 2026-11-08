package com.example.dofcalculator.calculation

import com.example.dofcalculator.model.*

/** Informational Airy/CoC comparison; never modifies geometric DOF. */
object DiffractionCalculator {
    const val WAVELENGTH_MM = 0.00055
    const val AIRY_DIAMETER_FACTOR = 2.44
    const val PUPIL_MAGNIFICATION = 1.0
    // App heuristics, not universal optical sharpness standards.
    const val POSSIBLE_THRESHOLD = 0.7
    const val NOTICEABLE_THRESHOLD = 1.0

    private fun positive(value: Double): Double {
        require(value.isFinite() && value > 0.0)
        return value
    }

    fun effectiveAperture(aperture: Double, magnification: Double): Double {
        require(magnification.isFinite() && magnification >= 0.0)
        return positive(positive(aperture) * (1.0 + magnification / PUPIL_MAGNIFICATION))
    }

    fun airyDiameterMm(effectiveAperture: Double): Double =
        positive(AIRY_DIAMETER_FACTOR * WAVELENGTH_MM * positive(effectiveAperture))

    fun level(ratio: Double): DiffractionLevel {
        require(ratio.isFinite() && ratio >= 0.0)
        return when {
            ratio >= NOTICEABLE_THRESHOLD -> DiffractionLevel.NOTICEABLE
            ratio >= POSSIBLE_THRESHOLD -> DiffractionLevel.POSSIBLE
            else -> DiffractionLevel.NONE
        }
    }

    fun calculate(aperture: Double, effectiveCoCmm: Double,
                  magnification: MagnificationEstimate): DiffractionResult {
        // Unknown magnification allows a clearly labelled uncorrected baseline only.
        val effective = effectiveAperture(aperture, magnification.value ?: 0.0)
        val airy = airyDiameterMm(effective)
        val ratio = airy / positive(effectiveCoCmm)
        require(ratio.isFinite() && ratio >= 0.0)
        return DiffractionResult(aperture, effective, airy, effectiveCoCmm, ratio,
            level(ratio), magnification)
    }
}
