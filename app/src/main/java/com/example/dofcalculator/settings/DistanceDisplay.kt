package com.example.dofcalculator.settings

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormatSymbols
import java.util.Locale

enum class DistanceUnit(val metersPerUnit: Double) {
    Centimeters(0.01), Meters(1.0), Inches(0.0254), Feet(0.3048);
    fun toMeters(value: Double): Double = value * metersPerUnit
    fun fromMeters(value: Double): Double = value / metersPerUnit
}

enum class DistanceRounding(val decimals: Int?) {
    Two(2), Three(3), None(null),
}

/** Presentation only: never feed a rounded number back into calculation state. */
data class DistanceDisplay(
    val unit: DistanceUnit = DistanceUnit.Meters,
    val rounding: DistanceRounding = DistanceRounding.Three,
    val locale: Locale = Locale.getDefault(),
) {
    fun number(meters: Double): String {
        require(meters.isFinite())
        val converted = unit.fromMeters(meters)
        require(converted.isFinite())
        val decimal = BigDecimal.valueOf(converted)
        val text = rounding.decimals?.let {
            decimal.setScale(it, RoundingMode.HALF_UP).toPlainString()
        } ?: decimal.stripTrailingZeros().toPlainString()
        return text.replace('.', DecimalFormatSymbols.getInstance(locale).decimalSeparator)
    }
}
