package com.example.dofcalculator.calculation

import com.example.dofcalculator.model.CalculatedDistance
import com.example.dofcalculator.model.DepthOfFieldInput
import com.example.dofcalculator.model.DepthOfFieldResult

/**
 * Performs thin-lens depth-of-field calculations without Android dependencies.
 */
object DepthOfFieldCalculator {
    private const val MILLIMETRES_PER_METRE = 1_000.0

    /**
     * Calculates hyperfocal distance and depth-of-field limits.
     *
     * All formulae are evaluated in millimetres. Finite result distances are
     * converted to metres only when the result model is created.
     *
     * @throws IllegalArgumentException when an input is non-finite, non-positive,
     * physically invalid, or outside the numeric range supported by [Double].
     */
    fun calculate(input: DepthOfFieldInput): DepthOfFieldResult {
        validateInput(input)

        val focalLengthMm = input.focalLengthMm
        val focusDistanceMm = input.focusDistanceMeters * MILLIMETRES_PER_METRE
        requirePositiveFinite(focusDistanceMm, "focus distance in millimetres")
        require(focusDistanceMm > focalLengthMm) {
            "focusDistanceMeters must place the subject beyond the focal length."
        }

        val hyperfocalDistanceMm =
            calculateHyperfocalDistanceMeters(
                focalLengthMm = input.focalLengthMm,
                aperture = input.aperture,
                circleOfConfusionMm = input.circleOfConfusionMm,
            ) * MILLIMETRES_PER_METRE

        val hyperfocalMinusFocalMm = hyperfocalDistanceMm - focalLengthMm
        requirePositiveFinite(
            hyperfocalMinusFocalMm,
            "hyperfocal distance minus focal length",
        )

        val commonNumerator = focusDistanceMm * hyperfocalMinusFocalMm
        requirePositiveFinite(commonNumerator, "depth-of-field numerator")

        val nearDenominator =
            (hyperfocalDistanceMm + focusDistanceMm) -
                (2.0 * focalLengthMm)
        requirePositiveFinite(nearDenominator, "near-limit denominator")

        val nearLimitMm = commonNumerator / nearDenominator
        requirePositiveFinite(nearLimitMm, "near limit")

        val hyperfocalDistanceMeters =
            hyperfocalDistanceMm / MILLIMETRES_PER_METRE
        val nearLimitMeters = nearLimitMm / MILLIMETRES_PER_METRE
        val depthOfFieldInFrontMeters =
            input.focusDistanceMeters - nearLimitMeters
        requireNonNegativeFinite(
            depthOfFieldInFrontMeters,
            "depth of field in front",
        )

        if (focusDistanceMm >= hyperfocalDistanceMm) {
            return DepthOfFieldResult(
                hyperfocalDistanceMeters = hyperfocalDistanceMeters,
                nearLimitMeters = nearLimitMeters,
                farLimit = CalculatedDistance.Infinity,
                totalDepthOfField = CalculatedDistance.Infinity,
                depthOfFieldInFront = CalculatedDistance.Finite(depthOfFieldInFrontMeters),
                depthOfFieldBehind = CalculatedDistance.Infinity,
            )
        }

        val farDenominator = hyperfocalDistanceMm - focusDistanceMm
        requirePositiveFinite(farDenominator, "far-limit denominator")

        val farLimitMm = commonNumerator / farDenominator
        requirePositiveFinite(farLimitMm, "far limit")

        val farLimitMeters = farLimitMm / MILLIMETRES_PER_METRE
        val totalDepthOfFieldMeters = farLimitMeters - nearLimitMeters
        val depthOfFieldBehindMeters =
            farLimitMeters - input.focusDistanceMeters
        requireNonNegativeFinite(totalDepthOfFieldMeters, "total depth of field")
        requireNonNegativeFinite(
            depthOfFieldBehindMeters,
            "depth of field behind",
        )

        return DepthOfFieldResult(
            hyperfocalDistanceMeters = hyperfocalDistanceMeters,
            nearLimitMeters = nearLimitMeters,
            farLimit = CalculatedDistance.Finite(farLimitMeters),
            totalDepthOfField =
                CalculatedDistance.Finite(totalDepthOfFieldMeters),
            depthOfFieldInFront = CalculatedDistance.Finite(depthOfFieldInFrontMeters),
            depthOfFieldBehind =
                CalculatedDistance.Finite(depthOfFieldBehindMeters),
        )
    }

    /** Optical infinity is distinct from a finite focus distance beyond H. */
    fun calculateAtInfinity(
        focalLengthMm: Double,
        aperture: Double,
        circleOfConfusionMm: Double,
    ): DepthOfFieldResult {
        val h = calculateHyperfocalDistanceMeters(focalLengthMm, aperture, circleOfConfusionMm)
        val near = h - focalLengthMm / MILLIMETRES_PER_METRE
        requirePositiveFinite(near, "near limit at infinity")
        return DepthOfFieldResult(
            hyperfocalDistanceMeters = h,
            nearLimitMeters = near,
            farLimit = CalculatedDistance.Infinity,
            totalDepthOfField = CalculatedDistance.Infinity,
            depthOfFieldInFront = CalculatedDistance.Infinity,
            depthOfFieldBehind = CalculatedDistance.Infinity,
        )
    }

    /**
     * Calculates hyperfocal distance in metres from optical inputs in millimetres.
     */
    fun calculateHyperfocalDistanceMeters(
        focalLengthMm: Double,
        aperture: Double,
        circleOfConfusionMm: Double,
    ): Double {
        requirePositiveFinite(focalLengthMm, "focalLengthMm")
        requirePositiveFinite(aperture, "aperture")
        requirePositiveFinite(circleOfConfusionMm, "circleOfConfusionMm")

        val opticalDenominator = aperture * circleOfConfusionMm
        requirePositiveFinite(
            opticalDenominator,
            "aperture × circle of confusion",
        )

        val focalLengthSquared = focalLengthMm * focalLengthMm
        requirePositiveFinite(focalLengthSquared, "squared focal length")

        val hyperfocalDistanceMm =
            (focalLengthSquared / opticalDenominator) + focalLengthMm
        requirePositiveFinite(hyperfocalDistanceMm, "hyperfocal distance")
        return hyperfocalDistanceMm / MILLIMETRES_PER_METRE
    }

    private fun validateInput(input: DepthOfFieldInput) {
        requirePositiveFinite(input.focalLengthMm, "focalLengthMm")
        requirePositiveFinite(input.aperture, "aperture")
        requirePositiveFinite(
            input.circleOfConfusionMm,
            "circleOfConfusionMm",
        )
        requirePositiveFinite(input.focusDistanceMeters, "focusDistanceMeters")
    }

    private fun requirePositiveFinite(value: Double, name: String) {
        require(value.isFinite() && (value > 0.0)) {
            "$name must be finite and greater than zero."
        }
    }

    private fun requireNonNegativeFinite(value: Double, name: String) {
        require(value.isFinite() && (value >= 0.0)) {
            "Input values produce an invalid $name."
        }
    }
}
