package com.example.dofcalculator.calculation

import com.example.dofcalculator.model.Lens
import org.junit.Assert.*
import org.junit.Test

class LensParameterRangeTest {
    @Test fun `prime selects its exact fractional focal and supplied aperture bounds`() {
        val range = LensParameterRange.of(Lens.Prime("p", focalLengthMm = 58.25, maxAperture = 1.7, minAperture = 16.0))
        assertEquals(58.25, range.focalLength(300.0), 0.0)
        assertEquals(1.7, range.aperture(.95), 0.0)
        assertEquals(16.0, range.aperture(64.0), 0.0)
        assertEquals(8.0, range.aperture(8.0), 0.0)
    }

    @Test fun `zoom clamps to exact endpoints without interpolating its aperture or inventing a closure limit`() {
        val range = LensParameterRange.of(Lens.Zoom("z", minFocalLengthMm = 48.0, maxFocalLengthMm = 217.0,
            maxApertureAtMinFocal = 3.5, maxApertureAtMaxFocal = 5.6))
        assertEquals(48.0, range.focalLength(10.0), 0.0)
        assertEquals(217.0, range.focalLength(300.0), 0.0)
        assertEquals(135.5, range.focalLength(135.5), 0.0)
        assertEquals(3.5, range.aperture(1.0), 0.0)
        assertEquals(64.0, range.aperture(64.0), 0.0)
    }

    @Test fun `invalid manual drafts receive a valid lens default only when selecting its profile`() {
        val range = LensParameterRange.of(Lens.Prime("p", focalLengthMm = 50.0, maxAperture = 1.4))
        for (bad in listOf(null, 0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY)) {
            assertEquals(50.0, range.focalLength(bad), 0.0)
            assertEquals(1.4, range.aperture(bad), 0.0)
        }
    }
}
