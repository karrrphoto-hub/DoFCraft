package com.example.dofcalculator.model

/**
 * Input values for a depth-of-field calculation.
 *
 * Focal length and circle of confusion are expressed in millimetres, while the
 * focus distance is expressed in metres.
 */
data class DepthOfFieldInput(
    val focalLengthMm: Double,
    val aperture: Double,
    val circleOfConfusionMm: Double,
    val focusDistanceMeters: Double,
)
