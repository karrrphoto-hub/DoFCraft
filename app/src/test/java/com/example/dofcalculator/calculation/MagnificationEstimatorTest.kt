package com.example.dofcalculator.calculation

import com.example.dofcalculator.model.*
import com.example.dofcalculator.settings.DiffractionSettings
import org.junit.Assert.*
import org.junit.Test

class MagnificationEstimatorTest {
    private fun estimate(distance: Double) =
        MagnificationEstimator.estimate(100.0, CalculatedDistance.Finite(distance))

    @Test fun `thin lens sensor plane distance control examples`() {
        for ((distance, expected) in listOf(2.0 to .05572809000084121,
            1.0 to .12701665379258312, .6 to .2679491924311227, .4 to 1.0)) {
            assertEquals(expected, estimate(distance)!!, 1e-14)
        }
        assertEquals(0.0, MagnificationEstimator.estimate(100.0, CalculatedDistance.Infinity)!!, 0.0)
        assertNull(estimate(.399999999))
        assertNull(estimate(.2))
        assertNull(estimate(0.0))
    }

    @Test fun `invalid input is unavailable and huge distances smoothly tend to zero`() {
        for (f in listOf(0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY)) {
            assertNull(MagnificationEstimator.estimate(f, CalculatedDistance.Finite(2.0)))
            assertNull(MagnificationEstimator.estimate(f, CalculatedDistance.Infinity))
        }
        var previous = 1.0
        for (distance in listOf(.4, .5, 1.0, 10.0, 1e6, 1e100, Double.MAX_VALUE)) {
            val m = estimate(distance)!!
            assertTrue(m.isFinite() && m >= 0.0 && m <= previous)
            previous = m
        }
        assertTrue(previous < 1e-300)
        assertTrue(MagnificationEstimator.estimate(Double.MIN_VALUE,
            CalculatedDistance.Finite(Double.MAX_VALUE))!! >= 0.0)
    }

    @Test fun `manual ratio decimal comma and arbitrary positive scale parse correctly`() {
        for ((input, value) in listOf("1:10" to .1, "1:4" to .25, "1:2" to .5,
            "1:1" to 1.0, "2:1" to 2.0, "5:1" to 5.0, "0,5" to .5, "0.5" to .5,
            " 1,5 : 2 " to .75, "0.0000001" to 1e-7)) {
            assertEquals(value, MagnificationEstimator.parseManual(input)!!, 1e-14)
        }
        for (bad in listOf("", "1:", "1:0", "0:1", "-1", "NaN", "Infinity", "1:2:3",
            "0", "0,", "1.", "1..2", "1:abc", "1" + "0".repeat(400),
            "1:0." + "0".repeat(400) + "1")) {
            assertNull(bad, MagnificationEstimator.parseManual(bad))
        }
    }

    @Test fun `manual overrides persist but pause at infinity and in hyperfocal`() {
        val manual = DiffractionSettings(magnificationMode = MagnificationMode.MANUAL, manualMagnification = 2.0,
            manualDistanceMeters = .2, manualFocalLengthMm = 100.0, manualConfirmed = true)
        val distance = CalculatedDistance.Finite(.2) // Auto unavailable; Manual above 1:1 is allowed.
        assertNull(MagnificationEstimator.resolve(100.0, distance, DiffractionSettings(), false).value)
        val active = MagnificationEstimator.resolve(100.0, distance, manual, false)
        assertEquals(2.0, active.value!!, 0.0)
        assertEquals(MagnificationMode.MANUAL, active.source)
        val infinite = MagnificationEstimator.resolve(100.0, CalculatedDistance.Infinity, manual, false)
        assertEquals(0.0, infinite.value!!, 0.0)
        assertEquals(ManualMagnificationSuspension.INFINITY, infinite.suspension)
        val hyper = MagnificationEstimator.resolve(100.0, CalculatedDistance.Finite(10.0), manual, true)
        assertEquals(MagnificationMode.AUTO, hyper.source)
        assertEquals(ManualMagnificationSuspension.HYPERFOCAL, hyper.suspension)
        assertEquals(2.0, manual.manualMagnification, 0.0)
    }
}
