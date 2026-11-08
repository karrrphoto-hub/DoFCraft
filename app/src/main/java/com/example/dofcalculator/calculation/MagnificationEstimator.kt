package com.example.dofcalculator.calculation

import com.example.dofcalculator.model.*
import com.example.dofcalculator.settings.DiffractionSettings
import kotlin.math.sqrt

/** Ideal thin lens. Distance is measured from the recording plane, not the lens. */
object MagnificationEstimator {
    const val CLOSE_UP_THRESHOLD = 0.1
    const val MACRO_INDICATOR_THRESHOLD = 0.25

    /** Sensor-to-subject distance for a known magnification in the same thin-lens model. */
    fun distanceMeters(focalLengthMm: Double, magnification: Double): Double {
        require(focalLengthMm.isFinite() && focalLengthMm > 0.0)
        require(magnification.isFinite() && magnification > 0.0)
        val focalMeters = focalLengthMm / 1000.0
        val distance = focalMeters * 2.0 + focalMeters * magnification + focalMeters / magnification
        require(distance.isFinite() && distance > 0.0)
        return distance
    }

    fun estimate(focalLengthMm: Double, focusDistance: CalculatedDistance): Double? {
        if (!focalLengthMm.isFinite() || focalLengthMm <= 0.0) return null
        if (focusDistance == CalculatedDistance.Infinity) return 0.0
        val distance = (focusDistance as CalculatedDistance.Finite).meters
        if (distance <= 0.0) return null
        // x = f/D; divide in this order so even very large metre distances stay finite.
        val x = (focalLengthMm / distance) / 1000.0
        if (!x.isFinite() || x > 0.25) return null // D < 4f has no real solution.
        // Numerically scaled 2 / (A + sqrt(A*A - 4)), A = D/f - 2.
        // Avoid both cancellation and overflow of A*A or of the denominator.
        val inverseA = x / (1.0 - 2.0 * x)
        val m = (2.0 * inverseA) / (1.0 + sqrt((1.0 - 2.0 * inverseA) *
            (1.0 + 2.0 * inverseA)))
        return m.takeIf { it.isFinite() && it in 0.0..1.0 }
    }

    fun resolve(focalLengthMm: Double, distance: CalculatedDistance,
                settings: DiffractionSettings, hyperfocalMode: Boolean): MagnificationEstimate {
        if (settings.magnificationMode != MagnificationMode.MANUAL)
            return MagnificationEstimate(estimate(focalLengthMm, distance))
        val status = ManualMagnificationValidator.assess(settings, distance, focalLengthMm).status
        if (status == ManualMagnificationStatus.VALID)
            return MagnificationEstimate(settings.manualMagnification, MagnificationMode.MANUAL)
        val suspension = when {
            distance == CalculatedDistance.Infinity -> ManualMagnificationSuspension.INFINITY
            hyperfocalMode -> ManualMagnificationSuspension.HYPERFOCAL
            else -> ManualMagnificationSuspension.NEEDS_CONFIRMATION
        }
        return MagnificationEstimate(estimate(focalLengthMm, distance), suspension = suspension)
    }

    /** A:B denotes image size / object size; both decimal separators are accepted. */
    fun parseManual(text: String): Double? {
        fun positive(part: String): Double? {
            val s = part.trim().replace(',', '.')
            if (!s.matches(Regex("[0-9]+(\\.[0-9]+)?"))) return null
            return s.toDoubleOrNull()?.takeIf { it.isFinite() && it > 0.0 }
        }
        val parts = text.split(':')
        val value = when (parts.size) {
            1 -> positive(parts[0])
            2 -> {
                val a = positive(parts[0]) ?: return null
                val b = positive(parts[1]) ?: return null
                a / b
            }
            else -> null
        }
        return value?.takeIf { it.isFinite() && it > 0.0 }
    }
}
