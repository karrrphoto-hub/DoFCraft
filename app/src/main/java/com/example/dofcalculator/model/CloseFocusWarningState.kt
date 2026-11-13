package com.example.dofcalculator.model

/** Derived informational state; never an optical input or a diffraction correction. */
sealed interface CloseFocusWarningState {
    data object None : CloseFocusWarningState
    data object InvalidInput : CloseFocusWarningState
    data object TooCloseToEstimate : CloseFocusWarningState
    data class EstimatedMacro(val magnification: Double) : CloseFocusWarningState {
        init { require(magnification.isFinite() && magnification > 0.0 && magnification <= 1.0) }
    }
}

/** Transient memo key in canonical units, kept with its result in one UI snapshot. */
data class CloseFocusInput(
    val focalLengthMm: Double?,
    val focusDistance: CalculatedDistance?,
)
