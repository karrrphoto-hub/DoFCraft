package com.example.dofcalculator.calculation

import com.example.dofcalculator.model.*
import kotlin.math.PI
import kotlin.math.hypot
import kotlin.math.tan
import org.junit.Assert.*
import org.junit.Test

class OutputSharpnessCalculatorTest {
    private val s = SharpnessSettings()
    private fun print(value: SharpnessSettings = s) = OutputSharpnessCalculator.print(36.0, 24.0, value)
    private fun screen(value: SharpnessSettings = s) = OutputSharpnessCalculator.screen(36.0, 24.0, value)

    @Test fun `arcminutes centimetres and exact angular blur`() {
        assertEquals(500.0, OutputSharpnessCalculator.centimetresToMm(50.0), 0.0)
        for (minutes in 1..3) {
            val radians = minutes * PI / (180.0 * 60.0)
            assertEquals(radians, OutputSharpnessCalculator.arcMinutesToRadians(minutes.toDouble()), 1e-18)
            assertEquals(1000.0 * tan(radians / 2), OutputSharpnessCalculator.angularBlurMm(500.0,
                minutes.toDouble()), 1e-15)
        }
    }

    @Test fun `print control example and viewing conditions`() {
        val result = print()
        assertEquals(12.5, result.geometry.enlargement, 1e-12)
        assertEquals(.2908882168703159, result.outputBlurMm, 1e-14)
        assertEquals(.02327105734962527, result.effectiveCoCmm, 1e-14)
        assertTrue(print(s.copy(printWidthCm = 60.0, printHeightCm = 90.0)).effectiveCoCmm < result.effectiveCoCmm)
        assertEquals(result.effectiveCoCmm * 2, print(s.copy(printViewingCm = 100.0)).effectiveCoCmm, 1e-14)
        assertTrue(print(s.copy(printAngleMinutes = 1.0)).effectiveCoCmm < result.effectiveCoCmm)
        assertTrue(print(s.copy(printAngleMinutes = 3.0)).effectiveCoCmm > result.effectiveCoCmm)
    }

    @Test fun `PPI DPI are optional reference metadata and do not affect print CoC`() {
        for (ppi in listOf(null, 150.0, 240.0, 300.0, 360.0)) {
            for (dpi in listOf(null, 300.0, 600.0, 1200.0, 2400.0)) {
                val r = print(s.copy(printWidthCm = 45.0, printHeightCm = 30.0,
                    printPpi = ppi, printDpi = dpi))
                assertEquals(print().effectiveCoCmm, r.effectiveCoCmm, 0.0)
                if (ppi == null) assertNull(r.requiredWidthPx)
                else {
                    assertEquals(450.0 / 25.4 * ppi, r.requiredWidthPx!!, 1e-10)
                    assertEquals(300.0 / 25.4 * ppi, r.requiredHeightPx!!, 1e-10)
                }
            }
        }
    }

    @Test fun `unrepresentable PPI reference does not invalidate optical print CoC`() {
        val result = print(s.copy(printPpi = Double.MAX_VALUE))
        assertEquals(print().effectiveCoCmm, result.effectiveCoCmm, 0.0)
        assertNull(result.requiredWidthPx)
        assertNull(result.requiredHeightPx)
    }

    @Test fun `fit fill orientation and actual photograph dimensions are distinct from paper`() {
        val square = s.copy(printWidthCm = 30.0, printHeightCm = 30.0)
        val fit = print(square.copy(printPlacement = PrintPlacement.FIT))
        val fill = print(square.copy(printPlacement = PrintPlacement.FILL))
        assertEquals(300.0 / 36, fit.geometry.enlargement, 1e-12)
        assertEquals(300.0 / 24, fill.geometry.enlargement, 1e-12)
        assertEquals(300.0, fit.geometry.imageWidthMm, 1e-10)
        assertEquals(200.0, fit.geometry.imageHeightMm, 1e-10)
        assertEquals(300.0, fit.outputHeightMm, 0.0)
        assertEquals(450.0, fill.geometry.imageWidthMm, 1e-10)
        assertEquals(300.0, fill.geometry.visibleWidthMm, 0.0)
        assertTrue(fit.effectiveCoCmm > fill.effectiveCoCmm)
        for (placement in PrintPlacement.entries) {
            assertEquals(print(s.copy(printWidthCm = 45.0, printHeightCm = 30.0,
                printPlacement = placement)).effectiveCoCmm,
                print(s.copy(printWidthCm = 30.0, printHeightCm = 45.0,
                    printPlacement = placement)).effectiveCoCmm, 1e-14)
        }
    }

    @Test fun `27 inch 4K screen visual control example`() {
        val r = screen()
        assertEquals(163.17830889498507, r.screenPpi!!, 1e-10)
        assertEquals(597.7265033600158, r.outputWidthMm, 1e-10)
        assertEquals(336.2211581400089, r.outputHeightMm, 1e-10)
        assertEquals(27.0 * 25.4, hypot(r.outputWidthMm, r.outputHeightMm), 1e-10)
        assertEquals(14.009214922500371, r.geometry.enlargement, 1e-10)
        assertEquals(.17453292643012205, r.outputBlurMm, 1e-14)
        assertEquals(.012458437349676362, r.effectiveCoCmm, 1e-14)
        assertEquals(36.0 * r.geometry.enlargement, r.geometry.visibleWidthMm, 1e-10)
        assertTrue(r.geometry.visibleWidthMm < r.outputWidthMm)
        assertEquals(r.outputBlurMm / r.pixelPitchMm!!, r.equivalentBlurPx!!, 1e-12)
    }

    @Test fun `display pixels are a separate explicitly chosen criterion`() {
        val r = screen(s.copy(screenCriterion = ScreenCriterion.DISPLAY_PIXELS))
        assertEquals(.022222222222222223, r.effectiveCoCmm, 1e-14)
        assertEquals(2.0, r.equivalentBlurPx!!, 1e-12)
        assertEquals(r.effectiveCoCmm * 2, screen(s.copy(screenCriterion = ScreenCriterion.DISPLAY_PIXELS,
            screenBlurPx = 4.0)).effectiveCoCmm, 1e-14)
    }

    @Test fun `changing only resolution preserves visual CoC but changes pixel based CoC`() {
        val doubleResolution = s.copy(screenWidthPx = 7680.0, screenHeightPx = 4320.0)
        val a = screen()
        val b = screen(doubleResolution)
        assertEquals(a.outputWidthMm, b.outputWidthMm, 1e-10)
        assertEquals(a.outputHeightMm, b.outputHeightMm, 1e-10)
        assertEquals(a.effectiveCoCmm, b.effectiveCoCmm, 1e-14)
        assertEquals(a.screenPpi!! * 2, b.screenPpi!!, 1e-10)
        assertEquals(screen(s.copy(screenCriterion = ScreenCriterion.DISPLAY_PIXELS)).effectiveCoCmm / 2,
            screen(doubleResolution.copy(screenCriterion = ScreenCriterion.DISPLAY_PIXELS)).effectiveCoCmm, 1e-14)
        assertEquals(a.effectiveCoCmm, screen(s.copy(screenWidthPx = 2160.0,
            screenHeightPx = 3840.0)).effectiveCoCmm, 1e-14)
    }

    @Test fun `invalid geometry angles and overflow are rejected`() {
        for (bad in listOf(0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY)) {
            assertTrue(runCatching { OutputSharpnessCalculator.geometry(bad, 24.0, 450.0, 300.0, PrintPlacement.FIT) }.isFailure)
            assertTrue(runCatching { print(s.copy(printViewingCm = bad)) }.isFailure)
            assertTrue(runCatching { print(s.copy(printAngleMinutes = bad)) }.isFailure)
            assertTrue(runCatching { screen(s.copy(screenDiagonalInches = bad)) }.isFailure)
            assertTrue(runCatching { screen(s.copy(screenWidthPx = bad)) }.isFailure)
            assertTrue(runCatching { screen(s.copy(screenHeightPx = bad)) }.isFailure)
            assertTrue(runCatching { print(s.copy(printPpi = bad)) }.isFailure)
        }
        assertTrue(runCatching { print(s.copy(printAngleMinutes = 10800.0)) }.isFailure)
        assertTrue(runCatching { print(s.copy(printAngleMinutes = 21600.0)) }.isFailure)
        assertTrue(runCatching { print(s.copy(printWidthCm = Double.MAX_VALUE)) }.isFailure)
        assertTrue(runCatching { screen(s.copy(screenDiagonalInches = Double.MAX_VALUE)) }.isFailure)
    }

    @Test fun `tiny and large finite CoCs keep precision without sentinel infinity`() {
        for (r in listOf(print(s.copy(printAngleMinutes = 1e-200)),
            print(s.copy(printViewingCm = 1e200)),
            screen(s.copy(screenAngleMinutes = 1e-200)),
            screen(s.copy(screenViewingCm = 1e200)))) {
            assertTrue(r.effectiveCoCmm.isFinite() && r.effectiveCoCmm > 0.0)
        }
        assertTrue(runCatching { print(s.copy(printViewingCm = Double.MAX_VALUE)) }.isFailure)
    }

    @Test fun `viewing slider round trips and leaves very large manual distances available`() {
        for (current in listOf(30.0, 50.0, 200.0, 1e200)) {
            val range = ViewingDistanceScale(current)
            assertEquals(current, range.distanceAt(range.positionOf(current)), current * 1e-12)
            for (t in listOf(0.0, .25, .5, .75, 1.0)) {
                assertEquals(t, range.positionOf(range.distanceAt(t)), 1e-12)
            }
        }
    }
}
