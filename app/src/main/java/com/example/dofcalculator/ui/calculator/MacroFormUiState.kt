package com.example.dofcalculator.ui.calculator

import com.example.dofcalculator.model.*

/** Unapplied form. Distance starts empty and becomes canonical only after Apply. */
data class MacroFormUiState(
    val numerator: String = "1",
    val denominator: String = "1",
    val distanceMeters: String = "",
    val validation: MacroInputValidation = MacroInputValidation(),
    val pendingConfirmation: ManualMacroPreview? = null,
) {
    val canApply: Boolean get() = validation.preview != null
}
