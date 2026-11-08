package com.example.dofcalculator.calculation

import com.example.dofcalculator.model.*
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.tan

data class OutputGeometry(
    val enlargement: Double,
    val imageWidthMm: Double,
    val imageHeightMm: Double,
    val visibleWidthMm: Double,
    val visibleHeightMm: Double,
)

data class OutputSharpnessResult(
    val effectiveCoCmm: Double,
    val outputBlurMm: Double,
    val geometry: OutputGeometry,
    val outputWidthMm: Double,
    val outputHeightMm: Double,
    val requiredWidthPx: Double? = null,
    val requiredHeightPx: Double? = null,
    val screenPpi: Double? = null,
    val pixelPitchMm: Double? = null,
    val equivalentBlurPx: Double? = null,
)

/** Shared output-image geometry; all physical lengths here are millimetres. */
object OutputSharpnessCalculator {
    fun positive(value: Double): Double {
        require(value.isFinite() && value > 0.0) { "Expected a finite positive number" }
        return value
    }
    fun centimetresToMm(cm: Double): Double = positive(positive(cm) * 10.0)

    fun arcMinutesToRadians(minutes: Double): Double {
        positive(minutes)
        require(minutes < 10800.0) { "Blur angle must be less than 180 degrees" }
        return positive(minutes * (PI / (180.0 * 60.0)))
    }

    fun angularBlurMm(distanceMm: Double, minutes: Double): Double =
        positive(positive(distanceMm) * (2.0 * tan(arcMinutesToRadians(minutes) / 2.0)))

    /** Pair matching long/short sides, then return dimensions in output orientation. */
    fun geometry(sensorW: Double, sensorH: Double, outputW: Double, outputH: Double,
                 placement: PrintPlacement): OutputGeometry {
        positive(sensorW); positive(sensorH); positive(outputW); positive(outputH)
        val rotate = (sensorW >= sensorH) != (outputW >= outputH)
        val w = if (rotate) sensorH else sensorW
        val h = if (rotate) sensorW else sensorH
        val sx = positive(outputW / w)
        val sy = positive(outputH / h)
        val enlargement = if (placement == PrintPlacement.FIT) minOf(sx, sy) else maxOf(sx, sy)
        val imageW = positive(w * enlargement)
        val imageH = positive(h * enlargement)
        return OutputGeometry(enlargement, imageW, imageH,
            minOf(imageW, outputW), minOf(imageH, outputH))
    }

    fun toSensorCoC(outputBlurMm: Double, enlargement: Double): Double =
        positive(positive(outputBlurMm) / positive(enlargement))

    fun screenPpi(widthPx: Double, heightPx: Double, diagonalInches: Double): Double =
        positive(positive(hypot(positive(widthPx), positive(heightPx))) / positive(diagonalInches))

    fun pixelPitchMm(ppi: Double): Double = positive(25.4 / positive(ppi))

    fun print(sensorW: Double, sensorH: Double, s: SharpnessSettings): OutputSharpnessResult {
        val width = centimetresToMm(s.printWidthCm)
        val height = centimetresToMm(s.printHeightCm)
        val g = geometry(sensorW, sensorH, width, height, s.printPlacement)
        val blur = angularBlurMm(centimetresToMm(s.printViewingCm), s.printAngleMinutes)
        // PPI/DPI are optional print preparation metadata, never an optical blur criterion.
        s.printPpi?.let(::positive)
        s.printDpi?.let(::positive)
        // An unrepresentable reference file size must not invalidate the optical CoC.
        val requiredW = s.printPpi?.let { (width / 25.4) * it }?.takeIf { it.isFinite() && it > 0.0 }
        val requiredH = s.printPpi?.let { (height / 25.4) * it }?.takeIf { it.isFinite() && it > 0.0 }
        return OutputSharpnessResult(toSensorCoC(blur, g.enlargement), blur, g, width, height,
            requiredW, requiredH)
    }

    fun screen(sensorW: Double, sensorH: Double, s: SharpnessSettings): OutputSharpnessResult {
        val ppi = screenPpi(s.screenWidthPx, s.screenHeightPx, s.screenDiagonalInches)
        val pitch = pixelPitchMm(ppi)
        val width = positive(s.screenWidthPx * pitch)
        val height = positive(s.screenHeightPx * pitch)
        val diagonalMm = positive(s.screenDiagonalInches * 25.4)
        require(abs(hypot(width, height) / diagonalMm - 1.0) <= 1e-12)
        val g = geometry(sensorW, sensorH, width, height, s.screenPlacement)
        val blur = when (s.screenCriterion) {
            ScreenCriterion.VISUAL -> angularBlurMm(centimetresToMm(s.screenViewingCm), s.screenAngleMinutes)
            ScreenCriterion.DISPLAY_PIXELS -> {
                // Viewing distance is still a positive physical setting, even in pixel mode.
                positive(s.screenViewingCm)
                positive(positive(s.screenBlurPx) * pitch)
            }
        }
        return OutputSharpnessResult(toSensorCoC(blur, g.enlargement), blur, g, width, height,
            screenPpi = ppi, pixelPitchMm = pitch, equivalentBlurPx = positive(blur / pitch))
    }
}
