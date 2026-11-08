package com.example.dofcalculator.ui.calculator

import com.example.dofcalculator.model.LensDraft
import com.example.dofcalculator.model.LensInputValidation

data class LensManagerUiState(
    val cameraId: String? = null,
    val editor: LensEditorUiState? = null,
    val pendingDeleteId: String? = null,
)

data class LensEditorUiState(
    val draft: LensDraft,
    val validation: LensInputValidation = LensInputValidation(),
    val showErrors: Boolean = false,
    val isSaving: Boolean = false,
    val isNew: Boolean = true,
)
