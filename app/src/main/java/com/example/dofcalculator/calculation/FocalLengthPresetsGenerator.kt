package com.example.dofcalculator.calculation

import com.example.dofcalculator.model.Lens

object FocalLengthPresetsGenerator {
    /** The kit's physical lower bound, independent of standard rows and Show All. */
    fun minimumFocalLengthMm(lenses: List<Lens>): Double? = lenses.minOfOrNull { lens ->
        when (lens) {
            is Lens.Prime -> lens.focalLengthMm
            is Lens.Zoom -> lens.minFocalLengthMm
        }
    }

    fun generate(lenses: List<Lens>, standardPresets: List<Double>, showAll: Boolean = false): List<Double> {
        if (lenses.isEmpty()) return standardPresets
        val values = lenses.flatMap { lens ->
            when (lens) {
                is Lens.Prime -> listOf(lens.focalLengthMm)
                is Lens.Zoom -> standardPresets.filter {
                    it >= lens.minFocalLengthMm && it <= lens.maxFocalLengthMm
                } + lens.minFocalLengthMm + lens.maxFocalLengthMm
            }
        }
        return (values + if (showAll) standardPresets else emptyList()).distinct().sorted()
    }
}
