package com.example.dofcalculator.ui.calculator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class DofVisualizationScaleTest {
    @Test
    fun `adaptive scale maps endpoints and remains monotonic`() {
        val near = adaptiveScaleFraction(
            distanceMeters = 3.0,
            scaleMaxMeters = 12.0,
        )
        val focus = adaptiveScaleFraction(
            distanceMeters = 5.0,
            scaleMaxMeters = 12.0,
        )

        assertEquals(0f, adaptiveScaleFraction(0.0, 12.0))
        assertEquals(1f, adaptiveScaleFraction(12.0, 12.0))
        assertTrue(near in 0f..1f)
        assertTrue(focus in 0f..1f)
        assertTrue(near < focus)
    }

    @Test
    fun `infinity cannot be mapped to a finite scale position`() {
        assertThrows(IllegalArgumentException::class.java) {
            adaptiveScaleFraction(
                distanceMeters = Double.POSITIVE_INFINITY,
                scaleMaxMeters = 12.0,
            )
        }
    }
}
