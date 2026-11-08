package com.example.dofcalculator.calculation

import com.example.dofcalculator.model.*
import kotlin.math.sqrt

/** All frame dimensions and the returned blur diameter are in millimetres. */
object SharpnessCriterionCalculator {
    fun positiveNumber(text: String): Double? {
        val normalized = text.trim().replace(',', '.')
        if (!normalized.matches(Regex("[0-9]+(\\.[0-9]+)?"))) return null
        return normalized.toDoubleOrNull()?.takeIf { it.isFinite() && it > 0.0 }
    }

    fun calculate(camera: CameraProfile, settings: SharpnessSettings): Double =
        calculate(camera.widthMm, camera.heightMm, camera.standardCoCmm, settings)

    fun calculate(widthMm: Double?, heightMm: Double?, standardCoCmm: Double?,
                  settings: SharpnessSettings): Double {
        fun positive(value: Double?): Double {
            require(value != null && value.isFinite() && value > 0.0)
            return value
        }
        val coc = when (settings.mode) {
            SharpnessCriterionMode.STANDARD -> StandardCoCResolver.resolve(widthMm, heightMm, standardCoCmm).coCmm
            SharpnessCriterionMode.CUSTOM -> positive(settings.customCoCmm)
            SharpnessCriterionMode.PIXEL_BASED -> {
                val width = positive(widthMm)
                val height = positive(heightMm)
                val pixels = positive(positive(settings.megapixels) * 1_000_000.0)
                val area = positive(width * height)
                positive(settings.pixelBlur) * sqrt(positive(area / pixels))
            }
            SharpnessCriterionMode.VIDEO_BASED ->
                VideoCoCCalculator.calculate(widthMm, heightMm, settings.video).effectiveCoCmm
            SharpnessCriterionMode.PRINT_BASED ->
                OutputSharpnessCalculator.print(positive(widthMm), positive(heightMm), settings).effectiveCoCmm
            SharpnessCriterionMode.SCREEN_BASED ->
                OutputSharpnessCalculator.screen(positive(widthMm), positive(heightMm), settings).effectiveCoCmm
        }
        return positive(coc)
    }
}
