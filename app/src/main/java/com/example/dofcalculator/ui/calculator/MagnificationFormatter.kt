package com.example.dofcalculator.ui.calculator

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/** Presentation only. Threshold decisions always use the original Double. */
internal object MagnificationFormatter {
    fun ratio(magnification: Double, locale: Locale, fractionDigits: Int = 1): String? {
        if (!magnification.isFinite() || magnification < 0.0) return null
        if (magnification == 0.0) return "0"
        fun number(value: Double): String {
            val scientific = value < .001 || value >= 1e6
            val pattern = "0" + if (fractionDigits > 0) "." + "#".repeat(fractionDigits) else ""
            return DecimalFormat(pattern + if (scientific) "E0" else "",
                DecimalFormatSymbols.getInstance(locale)).format(value)
        }
        val inverse = 1.0 / magnification
        return if (magnification >= 1.0 || !inverse.isFinite()) number(magnification) + ":1"
            else "1:" + number(inverse)
    }
}
