package com.example.dofcalculator.calculation

import com.example.dofcalculator.model.*
import org.junit.Assert.*
import org.junit.Test

class StandardCoCResolverTest {
    private fun fallback(w: Double, h: Double) = StandardCoCResolver.resolve(w, h, null)

    @Test fun `reference and rotated reference reproduce exactly 0 point 030`() {
        for ((w, h) in listOf(36.0 to 24.0, 24.0 to 36.0)) {
            val result = fallback(w, h)
            assertEquals(.030, result.coCmm, 0.0)
            assertEquals(StandardCoCSource.DIAGONAL_FALLBACK, result.source)
        }
    }

    @Test fun `uncatalogued half frame square and panoramic control values use the reference diagonal`() {
        assertEquals(.020392777756239697, fallback(17.0, 24.0).coCmm, 1e-15)
        assertEquals(.05491251783869153, fallback(56.0, 56.0).coCmm, 1e-15)
        // 24 x 65 yields 0.04804345 with the specified reference, not 0.0455.
        assertEquals(.04804344988573443, fallback(24.0, 65.0).coCmm, 1e-15)
    }

    @Test fun `orientation does not affect the result and uniform scaling is linear`() {
        for ((w, h) in listOf(17.0 to 24.0, 56.0 to 56.0, 24.0 to 65.0, 0.01 to 10_000.0)) {
            assertEquals(fallback(w, h), fallback(h, w))
            assertEquals(fallback(w, h).coCmm * 2, fallback(w * 2, h * 2).coCmm, 1e-14)
        }
    }

    @Test fun `catalogue recommendation takes priority and is returned without rounding`() {
        val result = StandardCoCResolver.resolve(56.0, 56.0, .053)
        assertEquals(.053, result.coCmm, 0.0)
        assertEquals(StandardCoCSource.CATALOG, result.source)
        assertNotEquals(fallback(56.0, 56.0).coCmm, result.coCmm)
        assertEquals(.0365, StandardCoCResolver.resolve(43.8, 32.9, .0365).coCmm, 0.0)
    }

    @Test fun `all existing format recommendations are preserved through their actual links`() {
        for (format in FrameFormat.entries.filter { it.suggestedCircleOfConfusionMm != null }) {
            val camera = CameraProfile("linked", "Imported camera", format)
            val result = StandardCoCResolver.resolve(camera)
            assertEquals(format.suggestedCircleOfConfusionMm!!, result.coCmm, 0.0)
            assertEquals(StandardCoCSource.CATALOG, result.source)
        }
    }

    @Test fun `same size and name do not invent a catalogue link and built in flag does not select the algorithm`() {
        val custom = CameraProfile("uncatalogued", "Medium Format 6x6", FrameFormat.Custom,
            customFormatName = "6x6", widthMm = 56.0, heightMm = 56.0)
        for (camera in listOf(custom, custom.copy(isBuiltIn = true))) {
            assertEquals(fallback(56.0, 56.0), StandardCoCResolver.resolve(camera))
        }
        val linked = CameraProfile("user-created", "Half-frame film", FrameFormat.MediumFormat6x6)
        assertEquals(.053, StandardCoCResolver.resolve(linked).coCmm, 0.0)
    }

    @Test fun `absent catalogue value falls back for any valid geometry`() {
        val unknown = CameraProfile("old-stable-id", "Imported format", FrameFormat.Custom,
            customFormatName = "Uncatalogued", widthMm = 17.0, heightMm = 24.0)
        assertEquals(StandardCoCSource.DIAGONAL_FALLBACK, StandardCoCResolver.resolve(unknown).source)
        assertEquals(.020392777756239697, StandardCoCResolver.resolve(unknown).coCmm, 1e-15)
    }

    @Test fun `invalid dimensions are rejected even when a valid catalogue CoC is provided`() {
        for (bad in listOf(null, 0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY)) {
            for (catalog in listOf(null, .03)) {
                assertThrows(IllegalArgumentException::class.java) { StandardCoCResolver.resolve(bad, 24.0, catalog) }
                assertThrows(IllegalArgumentException::class.java) { StandardCoCResolver.resolve(36.0, bad, catalog) }
            }
        }
    }

    @Test fun `present invalid catalogue values are errors rather than a request for fallback`() {
        for (bad in listOf(0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY)) {
            assertThrows(IllegalArgumentException::class.java) { StandardCoCResolver.resolve(36.0, 24.0, bad) }
        }
    }

    @Test fun `hypot avoids unnecessary overflow and valid panoramas have no arbitrary aspect limit`() {
        for ((w, h) in listOf(1e300 to 1e-100, 1e308 to 1e308, 10_000.0 to .01)) {
            val result = fallback(w, h)
            assertTrue(result.coCmm.isFinite() && result.coCmm > 0.0)
        }
        for (catalog in listOf(null, .03)) {
            assertThrows(IllegalArgumentException::class.java) {
                StandardCoCResolver.resolve(Double.MAX_VALUE, Double.MAX_VALUE, catalog)
            }
        }
        assertThrows(IllegalArgumentException::class.java) { fallback(Double.MIN_VALUE, Double.MIN_VALUE) }
    }

    @Test fun `sharpness calculator uses the same resolver and other modes ignore the catalogue value`() {
        val standard = SharpnessSettings()
        assertEquals(fallback(17.0, 24.0).coCmm,
            SharpnessCriterionCalculator.calculate(17.0, 24.0, null, standard), 0.0)
        for (mode in SharpnessCriterionMode.entries.filter { it != SharpnessCriterionMode.STANDARD }) {
            val settings = standard.copy(mode = mode)
            assertEquals(SharpnessCriterionCalculator.calculate(36.0, 24.0, .03, settings),
                SharpnessCriterionCalculator.calculate(36.0, 24.0, Double.NaN, settings), 0.0)
        }
    }
}
