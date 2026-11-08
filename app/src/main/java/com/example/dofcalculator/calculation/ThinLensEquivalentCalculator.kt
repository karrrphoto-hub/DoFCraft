package com.example.dofcalculator.calculation

import com.example.dofcalculator.model.*

/** Informational thin-lens equivalent; never substitutes the lens's nominal focal length. */
object ThinLensEquivalentCalculator {
    const val LOW_RATIO_THRESHOLD = 0.25
    const val HIGH_RATIO_THRESHOLD = 1.5

    private fun positive(v: Double): Double {
        require(v.isFinite() && v > 0.0)
        return v
    }

    private fun factor(magnification: Double): Double {
        val m = positive(magnification)
        // m/(1+m)^2 is invariant under m -> 1/m. No squared large values.
        val r = if (m <= 1.0) m else 1.0 / m
        return (r / (1.0 + r)) / (1.0 + r)
    }

    fun focalLengthMm(distanceMm: Double, magnification: Double): Double =
        positive(positive(distanceMm) * factor(magnification))

    fun focalLengthFromMeters(distanceMeters: Double, magnification: Double): Double =
        positive(positive(distanceMeters) * (factor(magnification) * 1000.0))

    fun focalRatio(equivalentMm: Double, nominalMm: Double): Double =
        positive(positive(equivalentMm) / positive(nominalMm))

    fun unusual(ratio: Double): Boolean {
        positive(ratio)
        return ratio < LOW_RATIO_THRESHOLD || ratio > HIGH_RATIO_THRESHOLD
    }

    fun validate(aText: String, bText: String, distanceText: String,
                 nominalMm: Double?, aperture: Double?): MacroInputValidation {
        val values = mapOf(MacroInputField.NUMERATOR to aText,
            MacroInputField.DENOMINATOR to bText, MacroInputField.DISTANCE to distanceText)
            .mapValues { SharpnessCriterionCalculator.positiveNumber(it.value) }
        val errors = values.filterValues { it == null }.keys.associateWith { MacroInputError.POSITIVE_NUMBER }
        if (errors.isNotEmpty()) return MacroInputValidation(errors = errors)
        if (nominalMm == null || aperture == null || !nominalMm.isFinite() || nominalMm <= 0.0 ||
            !aperture.isFinite() || aperture <= 0.0) return MacroInputValidation(opticalInputError = true)
        val m = values.getValue(MacroInputField.NUMERATOR)!! / values.getValue(MacroInputField.DENOMINATOR)!!
        if (!m.isFinite() || m <= 0.0) return MacroInputValidation(errors = mapOf(
            MacroInputField.NUMERATOR to MacroInputError.NUMERIC_RANGE,
            MacroInputField.DENOMINATOR to MacroInputError.NUMERIC_RANGE))
        val d = values.getValue(MacroInputField.DISTANCE)!!
        val equivalent = runCatching { focalLengthFromMeters(d, m) }.getOrNull()
            ?: return MacroInputValidation(errors = mapOf(MacroInputField.DISTANCE to MacroInputError.NUMERIC_RANGE))
        val ratio = runCatching { focalRatio(equivalent, nominalMm) }.getOrNull()
            ?: return MacroInputValidation(opticalInputError = true)
        val effective = runCatching { DiffractionCalculator.effectiveAperture(aperture, m) }.getOrNull()
            ?: return MacroInputValidation(errors = mapOf(MacroInputField.NUMERATOR to MacroInputError.NUMERIC_RANGE))
        return MacroInputValidation(ManualMacroPreview(m, d, nominalMm, equivalent, ratio, effective, unusual(ratio)))
    }
}
