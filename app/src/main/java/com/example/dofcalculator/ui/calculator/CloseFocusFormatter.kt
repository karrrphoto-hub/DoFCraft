package com.example.dofcalculator.ui.calculator

import java.text.NumberFormat
import java.util.Locale

/** Formats only the numeric denominator; the localized approximate-ratio template is a resource. */
internal object CloseFocusFormatter {
    fun denominator(magnification: Double, locale: Locale): String? {
        if (!magnification.isFinite() || magnification <= 0.0 || magnification > 1.0) return null
        val denominator = 1.0 / magnification
        if (!denominator.isFinite()) return null
        return NumberFormat.getNumberInstance(locale).apply {
            minimumFractionDigits = 0
            maximumFractionDigits = 1
            isGroupingUsed = false
        }.format(denominator)
    }
}
