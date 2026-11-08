package com.example.dofcalculator.ui.calculator

import java.util.Locale
import org.junit.Assert.*
import org.junit.Test

class MagnificationFormatterTest {
    @Test fun `automatic ratios use one decimal no trailing zero and localized separators`() {
        val ru = Locale.forLanguageTag("ru")
        for ((m, en, russian) in listOf(
            Triple(.2, "1:5", "1:5"), Triple(.267949, "1:3.7", "1:3,7"),
            Triple(.5, "1:2", "1:2"), Triple(1.0, "1:1", "1:1"))) {
            assertEquals(en, MagnificationFormatter.ratio(m, Locale.US))
            assertEquals(russian, MagnificationFormatter.ratio(m, ru))
        }
        assertEquals("2:1", MagnificationFormatter.ratio(2.0, Locale.US))
        assertEquals("0", MagnificationFormatter.ratio(0.0, Locale.US))
    }
    @Test fun `invalid and extreme inputs never produce NaN Infinity or a zero ratio side`() {
        for (bad in listOf(-1.0, Double.NaN, Double.POSITIVE_INFINITY))
            assertNull(MagnificationFormatter.ratio(bad, Locale.US))
        for (m in listOf(Double.MIN_VALUE, 1e-300, 1e-12, 1e12, Double.MAX_VALUE)) {
            val result = MagnificationFormatter.ratio(m, Locale.US)!!
            assertFalse(result.contains("NaN"))
            assertFalse(result.contains("Infinity"))
            assertFalse(result.contains("∞"))
            assertFalse(result == "1:0" || result == "0:1")
        }
    }
    @Test fun `existing diffraction labels can retain their three decimal precision`() {
        assertEquals("1:3.732", MagnificationFormatter.ratio(.2679491924311227, Locale.US, 3))
        assertEquals("1:3,732", MagnificationFormatter.ratio(.2679491924311227, Locale.forLanguageTag("ru"), 3))
    }
}
