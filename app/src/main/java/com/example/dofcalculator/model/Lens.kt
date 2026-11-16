package com.example.dofcalculator.model

/** Input shortcuts and distance-scale metadata; no lens is passed to optical calculations. */
sealed interface Lens {
    val id: String
    val name: String
    val minAperture: Double?
    /** Null in older records means use the app's current distance-unit family. */
    val distanceScaleUnit: LensDistanceUnit?

    data class Prime(
        override val id: String,
        override val name: String = "",
        val focalLengthMm: Double,
        val maxAperture: Double,
        override val minAperture: Double? = null,
        override val distanceScaleUnit: LensDistanceUnit? = null,
    ) : Lens {
        init {
            require(id.isNotBlank())
            require(positive(focalLengthMm) && positive(maxAperture))
            require(minAperture == null || positive(minAperture) && minAperture >= maxAperture)
        }
    }

    data class Zoom(
        override val id: String,
        override val name: String = "",
        val minFocalLengthMm: Double,
        val maxApertureAtMinFocal: Double,
        val maxFocalLengthMm: Double,
        val maxApertureAtMaxFocal: Double,
        override val minAperture: Double? = null,
        override val distanceScaleUnit: LensDistanceUnit? = null,
    ) : Lens {
        init {
            require(id.isNotBlank())
            require(positive(minFocalLengthMm) && positive(maxFocalLengthMm) &&
                maxFocalLengthMm > minFocalLengthMm)
            require(positive(maxApertureAtMinFocal) && positive(maxApertureAtMaxFocal))
            require(minAperture == null || positive(minAperture) &&
                minAperture >= maxOf(maxApertureAtMinFocal, maxApertureAtMaxFocal))
        }
    }
}

private fun positive(value: Double) = value.isFinite() && value > 0.0

/** User attachments and independent visibility preferences, keyed by a camera's stable ID. */
data class CameraLensKit(
    val lenses: List<Lens> = emptyList(),
    val showAllFocalLengths: Boolean = false,
    val showAllApertures: Boolean = false,
    val selectedLensId: String? = null,
) {
    init { require(lenses.map { it.id }.distinct().size == lenses.size) }

    /** Reuses the previously saved Show All Focal Lengths flag, now a complete profile bypass. */
    val ignoreLensProfile: Boolean get() = showAllFocalLengths
    val selectedLens: Lens? get() = lenses.firstOrNull { it.id == selectedLensId }
        ?: lenses.minByOrNull { lens -> when (lens) {
            is Lens.Prime -> lens.focalLengthMm
            is Lens.Zoom -> lens.minFocalLengthMm
        } }
    val activeLens: Lens? get() = selectedLens.takeUnless { ignoreLensProfile }
}
