package com.example.dofcalculator.model

enum class LensType { PRIME, ZOOM }
enum class LensField { PRIME_FOCAL, PRIME_APERTURE, WIDE_FOCAL, WIDE_APERTURE, TELE_FOCAL, TELE_APERTURE, MIN_APERTURE }
enum class LensInputError { POSITIVE_NUMBER, ZOOM_RANGE, APERTURE_RANGE }

data class LensDraft(
    val id: String,
    val name: String = "",
    val type: LensType = LensType.PRIME,
    val fields: Map<LensField, String> = emptyMap(),
) {
    companion object {
        fun from(lens: Lens): LensDraft {
            val fields = when (lens) {
                is Lens.Prime -> mapOf(LensField.PRIME_FOCAL to lens.focalLengthMm.toString(),
                    LensField.PRIME_APERTURE to lens.maxAperture.toString())
                is Lens.Zoom -> mapOf(LensField.WIDE_FOCAL to lens.minFocalLengthMm.toString(),
                    LensField.WIDE_APERTURE to lens.maxApertureAtMinFocal.toString(),
                    LensField.TELE_FOCAL to lens.maxFocalLengthMm.toString(),
                    LensField.TELE_APERTURE to lens.maxApertureAtMaxFocal.toString())
            } + (LensField.MIN_APERTURE to lens.minAperture?.toString().orEmpty())
            return LensDraft(lens.id, lens.name,
                if (lens is Lens.Zoom) LensType.ZOOM else LensType.PRIME, fields)
        }
    }
}

data class LensInputValidation(
    val lens: Lens? = null,
    val errors: Map<LensField, LensInputError> = emptyMap(),
)
