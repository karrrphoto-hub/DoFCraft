package com.example.dofcalculator.ui.calculator

import java.util.Locale
import org.junit.Assert.*
import org.junit.Test

class CoCDisplayTest {
    @Test fun `CoC keeps four decimals and tiny values never display zero`() {
        assertEquals("0.0300", formatCoC(.03, Locale.US))
        assertEquals("0,0300", formatCoC(.03, Locale.forLanguageTag("ru")))
        assertEquals("0.00876", formatCoC(.00876, Locale.US))
        assertNotEquals("0.0000", formatCoC(1e-300, Locale.US))
        assertTrue(formatCoC(1e-300, Locale.US).contains("E"))
        assertEquals("—", formatCoC(Double.NaN, Locale.US))
        assertEquals("—", formatCoC(null, Locale.US))
    }
}
