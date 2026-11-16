package com.example.dofcalculator.calculation

import com.example.dofcalculator.model.Lens

/** Input conveniences only; no interpolation of a zoom's aperture or optical corrections. */
data class LensParameterRange(
    val minimumFocalMm: Double,
    val maximumFocalMm: Double,
    val minimumFNumber: Double,
    val maximumFNumber: Double?,
) {
    fun focalLength(value: Double?): Double =
        value?.takeIf { it.isFinite() && it > 0.0 }?.coerceIn(minimumFocalMm, maximumFocalMm)
            ?: minimumFocalMm

    fun aperture(value: Double?): Double {
        val number = value?.takeIf { it.isFinite() && it > 0.0 } ?: minimumFNumber
        return number.coerceAtLeast(minimumFNumber).let { open ->
            maximumFNumber?.let(open::coerceAtMost) ?: open
        }
    }

    companion object {
        fun of(lens: Lens): LensParameterRange = when (lens) {
            is Lens.Prime -> LensParameterRange(lens.focalLengthMm, lens.focalLengthMm,
                lens.maxAperture, lens.minAperture)
            is Lens.Zoom -> LensParameterRange(lens.minFocalLengthMm, lens.maxFocalLengthMm,
                minOf(lens.maxApertureAtMinFocal, lens.maxApertureAtMaxFocal), lens.minAperture)
        }
    }
}
