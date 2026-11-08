package com.example.dofcalculator.calculation

import com.example.dofcalculator.model.*
import com.example.dofcalculator.settings.ApertureIncrements
import org.junit.Assert.*
import org.junit.Test

class LensPresetsGeneratorTest {
    private val standards = listOf(12.0, 18.0, 20.0, 24.0, 28.0, 35.0, 50.0, 70.0, 85.0,
        120.0, 135.0, 200.0, 300.0)
    private fun prime(f: Double, a: Double = 4.0, closed: Double? = null) =
        Lens.Prime("prime-$f-$a", focalLengthMm = f, maxAperture = a, minAperture = closed)
    private fun zoom(w: Double, t: Double, a: Double = 4.0, b: Double = a, closed: Double? = null) =
        Lens.Zoom("zoom-$w-$t", minFocalLengthMm = w, maxFocalLengthMm = t,
            maxApertureAtMinFocal = a, maxApertureAtMaxFocal = b, minAperture = closed)
    private fun focals(vararg lenses: Lens) = FocalLengthPresetsGenerator.generate(lenses.toList(), standards)
    private fun apertures(vararg lenses: Lens, showAll: Boolean = false) =
        AperturePresetsGenerator.generate(lenses.toList(), ApertureIncrements.FullStop.apertures, showAll)

    @Test fun `no lenses retain the full original order regardless of show all`() {
        for (all in listOf(false, true)) {
            assertEquals(standards, FocalLengthPresetsGenerator.generate(emptyList(), standards, all))
            assertEquals(ApertureIncrements.FullStop.apertures,
                AperturePresetsGenerator.generate(emptyList(), ApertureIncrements.FullStop.apertures, all))
        }
    }
    @Test fun `single and multiple primes retain exact including fractional values`() {
        assertEquals(listOf(50.0), focals(prime(50.0)))
        assertEquals(listOf(50.0, 200.0), focals(prime(200.0), prime(50.0)))
        assertEquals(listOf(58.0, 105.0, 135.5), focals(prime(135.5), prime(58.0), prime(105.0)))
    }
    @Test fun `24 to 120 zoom and 200 prime do not fill the gap`() {
        assertEquals(listOf(24.0, 28.0, 35.0, 50.0, 70.0, 85.0, 120.0, 200.0),
            focals(zoom(24.0, 120.0), prime(200.0)))
    }
    @Test fun `nonstandard zoom boundaries survive filtering`() {
        assertEquals(listOf(48.0, 50.0, 70.0, 85.0, 120.0, 135.0, 200.0, 217.0),
            focals(zoom(48.0, 217.0)))
    }
    @Test fun `each disjoint range is processed independently`() {
        assertEquals(listOf(18.0, 20.0, 24.0, 28.0, 35.0, 50.0, 55.0,
            70.0, 85.0, 120.0, 135.0, 200.0, 210.0, 300.0),
            focals(zoom(18.0, 55.0), zoom(70.0, 210.0), prime(300.0)))
        val withGap = standards + listOf(58.0, 60.0, 65.0)
        val result = FocalLengthPresetsGenerator.generate(listOf(zoom(18.0, 55.0), zoom(70.0, 210.0)), withGap)
        assertFalse(result.any { it > 55 && it < 70 })
    }
    @Test fun `overlapping zooms and coincident primes deduplicate numerically and sort`() {
        assertEquals(listOf(24.0, 28.0, 35.0, 50.0, 70.0, 85.0, 120.0, 135.0, 200.0),
            focals(zoom(50.0, 200.0), prime(50.0), zoom(24.0, 120.0)))
    }
    @Test fun `show all keeps nonstandard focal endpoints`() {
        val lenses = listOf(zoom(48.0, 217.0), prime(135.5))
        val filtered = FocalLengthPresetsGenerator.generate(lenses, standards)
        assertEquals((standards + filtered).distinct().sorted(),
            FocalLengthPresetsGenerator.generate(lenses, standards, true))
        assertFalse(filtered.contains(12.0))
    }
    @Test fun `fastest opening across all optics starts the aperture list`() {
        assertEquals(4.0, apertures(prime(50.0)).first(), 0.0)
        assertEquals(1.4, apertures(prime(50.0, 1.4), zoom(24.0, 120.0)).first(), 0.0)
        assertEquals(3.5, apertures(zoom(18.0, 55.0, 3.5, 5.6)).first(), 0.0)
    }
    @Test fun `nonstandard openings and both zoom end apertures are exact shortcuts`() {
        val result = apertures(prime(55.0, 1.7), prime(50.0, 1.8), zoom(18.0, 55.0, 3.5, 4.5))
        for (n in listOf(1.7, 1.8, 3.5, 4.5)) assertTrue(result.contains(n))
        assertEquals(result.sorted().distinct(), result)
    }
    @Test fun `closed aperture limits use the largest provided value`() {
        assertEquals(listOf(1.4, 2.0, 2.8, 4.0, 5.6, 8.0, 11.0, 16.0, 22.0, 32.0),
            apertures(zoom(24.0, 120.0, closed = 22.0), prime(50.0, 1.4, 16.0), prime(200.0, 4.0, 32.0)))
        assertEquals(45.0, apertures(prime(50.0, closed = 45.0)).last(), 0.0)
        assertEquals(64.0, apertures(prime(50.0, .95, 64.0)).last(), 0.0)
    }
    @Test fun `unknown closure uses existing upper bound without assigning limits to lenses`() {
        assertEquals(ApertureIncrements.FullStop.apertures.last(), apertures(prime(50.0)).last(), 0.0)
        assertEquals(64.0, apertures(prime(50.0, 64.0)).last(), 0.0)
        assertEquals(8.0, apertures(prime(50.0, 4.0, 8.0), prime(100.0, 2.8)).last(), 0.0)
        assertEquals(16.0, apertures(prime(50.0, 4.0, 8.0), prime(100.0, 16.0)).last(), 0.0)
    }
    @Test fun `show all apertures unions current increment row with exact lens values`() {
        val lenses = listOf(prime(55.0, 1.7, 64.0))
        for (increments in ApertureIncrements.entries) {
            val filtered = AperturePresetsGenerator.generate(lenses, increments.apertures)
            assertTrue(filtered.contains(1.7))
            val all = AperturePresetsGenerator.generate(lenses, increments.apertures, true)
            assertEquals((filtered + increments.apertures).distinct().sorted(), all)
            assertTrue(all.contains(64.0))
        }
    }
}
