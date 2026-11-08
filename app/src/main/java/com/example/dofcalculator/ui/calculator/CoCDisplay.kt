package com.example.dofcalculator.ui.calculator

import com.example.dofcalculator.model.SharpnessCriterionMode
import com.example.dofcalculator.R
import java.math.BigDecimal
import java.math.MathContext
import java.util.Locale

internal fun SharpnessCriterionMode.labelResource(): Int = when (this) {
    SharpnessCriterionMode.STANDARD -> R.string.sharpness_standard
    SharpnessCriterionMode.PIXEL_BASED -> R.string.sharpness_pixel
    SharpnessCriterionMode.VIDEO_BASED -> R.string.sharpness_video
    SharpnessCriterionMode.PRINT_BASED -> R.string.sharpness_print
    SharpnessCriterionMode.SCREEN_BASED -> R.string.sharpness_screen
    SharpnessCriterionMode.CUSTOM -> R.string.sharpness_custom
}

/** Presentation only: at least four decimals; tiny/large values use scientific notation. */
internal fun formatCoC(value: Double?, locale: Locale = Locale.getDefault()): String {
    if (value == null || !value.isFinite() || value <= 0.0) return "—"
    val decimal = BigDecimal.valueOf(value).round(MathContext(4))
    val text = if (value < 0.000001 || value >= 10000.0) decimal.toString()
        else decimal.setScale(maxOf(4, decimal.scale())).toPlainString()
    return if (java.text.DecimalFormatSymbols.getInstance(locale).decimalSeparator == ',')
        text.replace('.', ',') else text
}
