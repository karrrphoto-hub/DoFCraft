package com.example.dofcalculator.settings

import com.example.dofcalculator.model.LensDistanceUnit
import java.util.Locale
import org.junit.Assert.*
import org.junit.Test

class LensDistanceDisplayTest {
    @Test fun `forty feet has a compact metric reference without rounding the source distance`() {
        val meters = DistanceUnit.Feet.toMeters(40.0)
        assertEquals(LensDistanceReading("40", "12.19"),
            lensDistanceReading(meters, LensDistanceUnit.Feet, DistanceUnit.Meters, Locale.US))
        assertEquals(12.192, meters, 0.0)
    }

    @Test fun `Russian numbers use commas and reverse conversion uses the app unit`() {
        val ru = Locale.forLanguageTag("ru")
        assertEquals(LensDistanceReading("40", "12,19"),
            lensDistanceReading(12.192, LensDistanceUnit.Feet, DistanceUnit.Meters, ru))
        assertEquals(LensDistanceReading("12,19", "40"),
            lensDistanceReading(12.192, LensDistanceUnit.Meters, DistanceUnit.Feet, ru))
    }

    @Test fun `matching units omit a duplicate reference while all app units are supported`() {
        assertEquals(LensDistanceReading("12.19", null),
            lensDistanceReading(12.192, LensDistanceUnit.Meters, DistanceUnit.Meters, Locale.US))
        assertEquals(LensDistanceReading("40", null),
            lensDistanceReading(12.192, LensDistanceUnit.Feet, DistanceUnit.Feet, Locale.US))
        assertEquals(LensDistanceReading("40", "1219.2"),
            lensDistanceReading(12.192, LensDistanceUnit.Feet, DistanceUnit.Centimeters, Locale.US))
        assertEquals(LensDistanceReading("12.19", "480"),
            lensDistanceReading(12.192, LensDistanceUnit.Meters, DistanceUnit.Inches, Locale.US))
    }

    @Test fun `defaults follow the metric or imperial family`() {
        assertEquals(LensDistanceUnit.Meters, DistanceUnit.Meters.lensDistanceUnit())
        assertEquals(LensDistanceUnit.Meters, DistanceUnit.Centimeters.lensDistanceUnit())
        assertEquals(LensDistanceUnit.Feet, DistanceUnit.Feet.lensDistanceUnit())
        assertEquals(LensDistanceUnit.Feet, DistanceUnit.Inches.lensDistanceUnit())
    }

    @Test fun `invalid distances and overflowing conversions have no fabricated reading`() {
        for (bad in listOf(0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY)) {
            assertNull(lensDistanceReading(bad, LensDistanceUnit.Feet, DistanceUnit.Meters, Locale.US))
        }
        assertNull(lensDistanceReading(Double.MAX_VALUE, LensDistanceUnit.Feet, DistanceUnit.Meters, Locale.US))
    }

    @Test fun `a very short valid distance is not rounded to zero`() {
        val reading = lensDistanceReading(.001, LensDistanceUnit.Meters, DistanceUnit.Meters, Locale.US)
        assertEquals(LensDistanceReading("0.001", null), reading)
    }
}
