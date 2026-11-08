package com.example.dofcalculator.model

/** Transient derived memo key, never an editable or persisted focus-distance source. */
data class MacroDetectionInput(
    val focalMm: Double?,
    val distance: CalculatedDistance?,
    val estimate: MagnificationEstimate,
    val ordinaryDof: Boolean,
)

/** Informational states; none of them changes the optical inputs or results. */
sealed interface MacroDetectionState {
    data object None : MacroDetectionState
    data object InvalidInput : MacroDetectionState
    data object MagnificationUnavailableAtCloseDistance : MacroDetectionState

    sealed interface WithMagnification : MacroDetectionState {
        val magnification: Double
        val source: MagnificationMode
    }
    data class MacroEstimated(override val magnification: Double) : WithMagnification {
        override val source = MagnificationMode.AUTO
        init { require(magnification.isFinite() && magnification > 0.0 && magnification <= 1.0) }
    }
    data class ManualConfirmed(override val magnification: Double) : WithMagnification {
        override val source = MagnificationMode.MANUAL
        init { require(magnification.isFinite() && magnification > 0.0) }
    }
}
