package com.example.dofcalculator.settings

import java.util.Locale
import org.junit.Assert.*
import org.junit.Test

class DistanceDisplayTest {
    @Test fun `unit conversion is reversible`() {
        assertEquals(100.0, DistanceUnit.Centimeters.fromMeters(1.0), 0.0)
        assertEquals(1.0, DistanceUnit.Inches.fromMeters(.0254), 0.0)
        assertEquals(1.0, DistanceUnit.Feet.fromMeters(.3048), 0.0)
        for (unit in DistanceUnit.entries) {
            for (meters in listOf(.01, 5.4108355795, 254.048253968254, 1e100)) {
                assertEquals(meters, unit.toMeters(unit.fromMeters(meters)), meters * 1e-14)
            }
        }
    }
    @Test fun `rounding choices locale and no rounding preserve meaningful digits`() {
        val original = 5.4108355795
        val display = DistanceDisplay(locale = Locale.US)
        assertEquals("5.41", display.copy(rounding = DistanceRounding.Two).number(original))
        assertEquals("5.411", display.number(original))
        assertEquals("5.4108355795", display.copy(rounding = DistanceRounding.None).number(original))
        assertEquals("5,411", display.copy(locale = Locale.forLanguageTag("ru")).number(original))
        assertEquals("541.084", display.copy(unit = DistanceUnit.Centimeters).number(original))
        assertEquals("1.00", display.copy(unit = DistanceUnit.Inches,
            rounding = DistanceRounding.Two).number(.0254))
        assertEquals(original, original, 0.0)
    }
    @Test fun `very small and large distances are shown without scientific notation or truncation`() {
        val display = DistanceDisplay(rounding = DistanceRounding.None, locale = Locale.US)
        assertEquals("0.000000000001", display.number(1e-12))
        assertEquals("100000000000000000000", display.number(1e20))
        assertTrue(runCatching { display.number(Double.NaN) }.isFailure)
    }
}
