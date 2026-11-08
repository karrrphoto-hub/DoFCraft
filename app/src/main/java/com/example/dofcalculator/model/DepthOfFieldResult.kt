package com.example.dofcalculator.model

/**
 * Result of a depth-of-field calculation.
 *
 * Every finite distance is expressed in metres. Values that can become
 * unbounded use [CalculatedDistance] instead of a numeric sentinel.
 */
data class DepthOfFieldResult(
    val hyperfocalDistanceMeters: Double,
    val nearLimitMeters: Double,
    val farLimit: CalculatedDistance,
    val totalDepthOfField: CalculatedDistance,
    val depthOfFieldInFront: CalculatedDistance,
    val depthOfFieldBehind: CalculatedDistance,
)
