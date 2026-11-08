package com.example.dofcalculator.calculation

import com.example.dofcalculator.model.Lens

object FocalLengthPresetsGenerator {
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
