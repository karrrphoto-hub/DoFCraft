package com.example.dofcalculator.calculation

import com.example.dofcalculator.model.*
import org.junit.Assert.*
import org.junit.Test

class LensInputValidatorTest {
    private val prime = LensDraft("id", fields = mapOf(
        LensField.PRIME_FOCAL to "58", LensField.PRIME_APERTURE to "1,7"))
    private val zoom = LensDraft("zoom", type = LensType.ZOOM, fields = mapOf(
        LensField.WIDE_FOCAL to "48", LensField.TELE_FOCAL to "217",
        LensField.WIDE_APERTURE to "3.5", LensField.TELE_APERTURE to "5.6"))

    @Test fun `valid unusual prime and variable or constant zoom are accepted`() {
        assertEquals(Lens.Prime("id", focalLengthMm = 58.0, maxAperture = 1.7),
            LensInputValidator.validate(prime).lens)
        val lens = LensInputValidator.validate(zoom).lens as Lens.Zoom
        assertEquals(48.0, lens.minFocalLengthMm, 0.0)
        assertEquals(217.0, lens.maxFocalLengthMm, 0.0)
        assertEquals(5.6, lens.maxApertureAtMaxFocal, 0.0)
        assertNotNull(LensInputValidator.validate(zoom.copy(fields = zoom.fields +
            (LensField.WIDE_APERTURE to "4") + (LensField.TELE_APERTURE to "4"))).lens)
        assertNotNull(LensInputValidator.validate(prime.copy(fields = prime.fields +
            (LensField.PRIME_APERTURE to "0.95") + (LensField.MIN_APERTURE to "64"))).lens)
    }
    @Test fun `zero negative nonnumeric and nonfinite values are rejected per active field`() {
        for (draft in listOf(prime, zoom)) for (field in draft.fields.keys) {
            for (bad in listOf("", ".", "bad", "0", "-1", "NaN", "Infinity", "-Infinity", "1e9999")) {
                val result = LensInputValidator.validate(draft.copy(fields = draft.fields + (field to bad)))
                assertNull(result.lens)
                assertTrue(result.errors.containsKey(field))
            }
        }
    }
    @Test fun `zoom requires strictly ordered focal limits`() {
        for (bad in listOf("48", "47")) {
            val result = LensInputValidator.validate(zoom.copy(fields = zoom.fields + (LensField.TELE_FOCAL to bad)))
            assertEquals(LensInputError.ZOOM_RANGE, result.errors[LensField.TELE_FOCAL])
            assertNull(result.lens)
        }
    }
    @Test fun `optional closed aperture must cover both zoom ends`() {
        for (draft in listOf(prime, zoom)) {
            assertNotNull(LensInputValidator.validate(draft.copy(fields = draft.fields +
                (LensField.MIN_APERTURE to ""))).lens)
            val bad = LensInputValidator.validate(draft.copy(fields = draft.fields + (LensField.MIN_APERTURE to "1")))
            assertEquals(LensInputError.APERTURE_RANGE, bad.errors[LensField.MIN_APERTURE])
            assertNull(bad.lens)
        }
        val badTele = LensInputValidator.validate(zoom.copy(fields = zoom.fields + (LensField.MIN_APERTURE to "4")))
        assertEquals(LensInputError.APERTURE_RANGE, badTele.errors[LensField.MIN_APERTURE])
    }
    @Test fun `model also rejects invalid persisted optics and duplicate IDs`() {
        for (bad in listOf(0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY)) {
            assertThrows(IllegalArgumentException::class.java) { Lens.Prime("p", focalLengthMm = bad, maxAperture = 4.0) }
            assertThrows(IllegalArgumentException::class.java) { Lens.Prime("p", focalLengthMm = 50.0, maxAperture = bad) }
            assertThrows(IllegalArgumentException::class.java) { Lens.Zoom("z", minFocalLengthMm = 18.0,
                maxFocalLengthMm = 55.0, maxApertureAtMinFocal = 3.5, maxApertureAtMaxFocal = bad) }
        }
        assertThrows(IllegalArgumentException::class.java) {
            Lens.Prime("p", focalLengthMm = 50.0, maxAperture = 4.0, minAperture = 2.0)
        }
        val lens = LensInputValidator.validate(prime).lens!!
        assertThrows(IllegalArgumentException::class.java) { CameraLensKit(listOf(lens, lens)) }
    }
    @Test fun `draft roundtrip keeps type and exact doubles including extreme positive values`() {
        val lenses = listOf(Lens.Prime("p", "Name", 135.5, .95, 64.0),
            Lens.Zoom("z", "Zoom", 48.0, 3.5, 217.0, 4.5, 45.0),
            Lens.Prime("extreme", focalLengthMm = Double.MAX_VALUE, maxAperture = Double.MIN_VALUE))
        for (lens in lenses) assertEquals(lens, LensInputValidator.validate(LensDraft.from(lens)).lens)
    }
}
