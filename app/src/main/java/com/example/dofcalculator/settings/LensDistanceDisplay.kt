package com.example.dofcalculator.settings

import com.example.dofcalculator.model.LensDistanceUnit
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormatSymbols
import java.util.Locale

/** Metric/imperial defaults for a scale that offers only metres or feet. */
fun DistanceUnit.lensDistanceUnit(): LensDistanceUnit = when (this) {
    DistanceUnit.Centimeters, DistanceUnit.Meters -> LensDistanceUnit.Meters
    DistanceUnit.Inches, DistanceUnit.Feet -> LensDistanceUnit.Feet
}

fun LensDistanceUnit.distanceUnit(): DistanceUnit = when (this) {
    LensDistanceUnit.Meters -> DistanceUnit.Meters
    LensDistanceUnit.Feet -> DistanceUnit.Feet
}

data class LensDistanceReading(val value: String, val reference: String?)

/** Compact display only: full-precision canonical metres remain untouched. */
fun lensDistanceReading(
    meters: Double,
    scale: LensDistanceUnit,
    referenceUnit: DistanceUnit,
    locale: Locale,
): LensDistanceReading? {
    if (!meters.isFinite() || meters <= 0.0) return null
    fun number(unit: DistanceUnit): String? {
        val value = unit.fromMeters(meters)
        if (!value.isFinite() || value <= 0.0) return null
        val decimal = BigDecimal.valueOf(value)
        val rounded = decimal.setScale(2, RoundingMode.HALF_UP)
        // Do not turn a valid very small distance into zero.
        val text = (if (rounded.signum() == 0) decimal else rounded)
            .stripTrailingZeros().toPlainString()
        return text.replace('.', DecimalFormatSymbols.getInstance(locale).decimalSeparator)
    }
    val unit = scale.distanceUnit()
    val value = number(unit) ?: return null
    val reference = if (unit == referenceUnit) null else number(referenceUnit) ?: return null
    return LensDistanceReading(value, reference)
}
