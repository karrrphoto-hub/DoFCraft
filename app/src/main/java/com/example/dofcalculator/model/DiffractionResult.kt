package com.example.dofcalculator.model

enum class DiffractionLevel { NONE, POSSIBLE, NOTICEABLE }

data class DiffractionResult(
    val nominalAperture: Double,
    val airyDiameterMm: Double,
    val effectiveCoCmm: Double,
    val ratio: Double,
    val level: DiffractionLevel,
)
