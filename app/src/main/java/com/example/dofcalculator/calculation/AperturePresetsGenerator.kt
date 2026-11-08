package com.example.dofcalculator.calculation

import com.example.dofcalculator.model.Lens

object AperturePresetsGenerator {
    fun generate(lenses: List<Lens>, standardPresets: List<Double>, showAll: Boolean = false): List<Double> {
        if (lenses.isEmpty()) return standardPresets
        val openings = lenses.flatMap {
            when (it) {
                is Lens.Prime -> listOf(it.maxAperture)
                is Lens.Zoom -> listOf(it.maxApertureAtMinFocal, it.maxApertureAtMaxFocal)
            }
        }
        val closed = lenses.mapNotNull { it.minAperture }
        val lower = openings.min()
        // Missing limits are unknown, not a physical limit assigned to individual lenses.
        val upper = maxOf(closed.maxOrNull() ?: standardPresets.maxOrNull() ?: openings.max(), openings.max())
        val values = (standardPresets + openings + closed).filter { it >= lower && it <= upper }
        return (values + if (showAll) standardPresets else emptyList()).distinct().sorted()
    }
}
