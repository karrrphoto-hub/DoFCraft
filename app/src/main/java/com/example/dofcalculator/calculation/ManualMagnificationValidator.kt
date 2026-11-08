package com.example.dofcalculator.calculation

import com.example.dofcalculator.model.*
import com.example.dofcalculator.settings.DiffractionSettings
import kotlin.math.abs
import kotlin.math.max

object ManualMagnificationValidator {
    // Relative comparison in canonical units; approximately 1 nm at 1 metre.
    const val RELATIVE_TOLERANCE = 1e-9

    fun same(a: Double?, b: Double?): Boolean =
        a != null && b != null && a.isFinite() && b.isFinite() && a > 0.0 && b > 0.0 &&
            abs(a - b) <= max(abs(a), abs(b)) * RELATIVE_TOLERANCE

    fun assess(settings: DiffractionSettings, distance: CalculatedDistance?,
               nominalMm: Double?): ManualMagnificationAssessment {
        if (distance == CalculatedDistance.Infinity)
            return ManualMagnificationAssessment(ManualMagnificationStatus.SUSPENDED_AT_INFINITY, emptySet())
        val bound = settings.manualDistanceMeters
        val focal = settings.manualFocalLengthMm
        val actual = (distance as? CalculatedDistance.Finite)?.meters
        val changes = buildSet {
            if (!settings.manualConfirmed || bound == null || focal == null)
                add(ManualMagnificationChange.UNCONFIRMED)
            if (!same(bound, actual)) add(ManualMagnificationChange.DISTANCE)
            if (!same(focal, nominalMm)) add(ManualMagnificationChange.FOCAL_LENGTH)
        }
        if (bound == null || focal == null) return ManualMagnificationAssessment(
            ManualMagnificationStatus.NEEDS_CONFIRMATION, changes)
        val equivalent = runCatching {
            ThinLensEquivalentCalculator.focalLengthFromMeters(bound, settings.manualMagnification)
        }.getOrNull()
        val ratio = nominalMm?.let { runCatching {
            ThinLensEquivalentCalculator.focalRatio(requireNotNull(equivalent), it)
        }.getOrNull() }
        if (equivalent == null || ratio == null || actual == null)
            return ManualMagnificationAssessment(ManualMagnificationStatus.INVALID,
                changes + ManualMagnificationChange.INVALID_INPUT, equivalent)
        return ManualMagnificationAssessment(
            if (changes.isEmpty()) ManualMagnificationStatus.VALID else ManualMagnificationStatus.NEEDS_CONFIRMATION,
            changes, equivalent, ratio, ThinLensEquivalentCalculator.unusual(ratio))
    }
}
