package com.example.dofcalculator.model

enum class MagnificationMode { AUTO, MANUAL }
enum class ManualMagnificationSuspension { INFINITY, HYPERFOCAL, NEEDS_CONFIRMATION }

data class MagnificationEstimate(
    val value: Double?,
    val source: MagnificationMode = MagnificationMode.AUTO,
    val suspension: ManualMagnificationSuspension? = null,
) {
    init { require(value == null || (value.isFinite() && value >= 0.0)) }
}
