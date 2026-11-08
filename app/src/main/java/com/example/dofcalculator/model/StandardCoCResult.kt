package com.example.dofcalculator.model

enum class StandardCoCSource {
    CATALOG,
    DIAGONAL_FALLBACK,
}

/** Resolved traditional blur diameter in millimetres and its provenance. */
data class StandardCoCResult(
    val coCmm: Double,
    val source: StandardCoCSource,
)
