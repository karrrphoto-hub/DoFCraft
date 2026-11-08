package com.example.dofcalculator.settings

import com.example.dofcalculator.model.MagnificationMode

/** Informational preferences, independent of cameras and optical DOF inputs. */
data class DiffractionSettings(
    val warningsEnabled: Boolean = true,
    val magnificationMode: MagnificationMode = MagnificationMode.AUTO,
    val manualMagnification: Double = 1.0,
    val manualDistanceMeters: Double? = null,
    val manualFocalLengthMm: Double? = null,
    val manualConfirmed: Boolean = false,
) {
    init { require(manualMagnification.isFinite() && manualMagnification > 0.0) }
}
