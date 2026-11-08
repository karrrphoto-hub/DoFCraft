package com.example.dofcalculator.model

enum class ManualMagnificationStatus { VALID, NEEDS_CONFIRMATION, INVALID, SUSPENDED_AT_INFINITY }
enum class ManualMagnificationChange { UNCONFIRMED, DISTANCE, FOCAL_LENGTH, INVALID_INPUT }

data class ManualMagnificationAssessment(
    val status: ManualMagnificationStatus = ManualMagnificationStatus.NEEDS_CONFIRMATION,
    val changes: Set<ManualMagnificationChange> = setOf(ManualMagnificationChange.UNCONFIRMED),
    val equivalentFocalLengthMm: Double? = null,
    val focalRatio: Double? = null,
    val unusual: Boolean = false,
)

enum class MacroInputField { NUMERATOR, DENOMINATOR, DISTANCE }
enum class MacroInputError { POSITIVE_NUMBER, NUMERIC_RANGE }

data class ManualMacroPreview(
    val magnification: Double,
    val distanceMeters: Double,
    val nominalFocalLengthMm: Double,
    val equivalentFocalLengthMm: Double,
    val focalRatio: Double,
    val effectiveAperture: Double,
    val unusual: Boolean,
)

data class MacroInputValidation(
    val preview: ManualMacroPreview? = null,
    val errors: Map<MacroInputField, MacroInputError> = emptyMap(),
    val opticalInputError: Boolean = false,
)
