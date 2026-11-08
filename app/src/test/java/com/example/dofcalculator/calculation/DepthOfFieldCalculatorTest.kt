package com.example.dofcalculator.calculation

import com.example.dofcalculator.model.CalculatedDistance
import com.example.dofcalculator.model.DepthOfFieldInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class DepthOfFieldCalculatorTest {
    @Test
    fun `50 mm at f8 and 5 m matches control calculation`() {
        val result = DepthOfFieldCalculator.calculate(
            DepthOfFieldInput(
                focalLengthMm = 50.0,
                aperture = 8.0,
                circleOfConfusionMm = 0.03,
                focusDistanceMeters = 5.0,
            ),
        )

        assertEquals(
            10.466666666666669,
            result.hyperfocalDistanceMeters,
            TOLERANCE_METERS,
        )
        assertEquals(
            3.3893709327548804,
            result.nearLimitMeters,
            TOLERANCE_METERS,
        )
        assertEquals(
            9.527439024390242,
            result.farLimit.requireFiniteMeters(),
            TOLERANCE_METERS,
        )
        assertEquals(
            6.138068091635361,
            result.totalDepthOfField.requireFiniteMeters(),
            TOLERANCE_METERS,
        )
        assertEquals(
            1.6106290672451196,
            (result.depthOfFieldInFront as CalculatedDistance.Finite).meters,
            TOLERANCE_METERS,
        )
        assertEquals(
            4.527439024390242,
            result.depthOfFieldBehind.requireFiniteMeters(),
            TOLERANCE_METERS,
        )
    }

    @Test
    fun `direct hyperfocal calculation reuses the control formula`() {
        val hyperfocalDistanceMeters =
            DepthOfFieldCalculator.calculateHyperfocalDistanceMeters(
                focalLengthMm = 50.0,
                aperture = 8.0,
                circleOfConfusionMm = 0.03,
            )

        assertEquals(
            10.466666666666669,
            hyperfocalDistanceMeters,
            TOLERANCE_METERS,
        )
        assertThrows(IllegalArgumentException::class.java) {
            DepthOfFieldCalculator.calculateHyperfocalDistanceMeters(
                focalLengthMm = 50.0,
                aperture = 0.0,
                circleOfConfusionMm = 0.03,
            )
        }
    }

    @Test
    fun `focus exactly at hyperfocal distance has an infinite far limit`() {
        val referenceInput = DepthOfFieldInput(
            focalLengthMm = 50.0,
            aperture = 8.0,
            circleOfConfusionMm = 0.03,
            focusDistanceMeters = 5.0,
        )
        val hyperfocalDistanceMeters =
            DepthOfFieldCalculator.calculate(referenceInput)
                .hyperfocalDistanceMeters

        val result = DepthOfFieldCalculator.calculate(
            referenceInput.copy(
                focusDistanceMeters = hyperfocalDistanceMeters,
            ),
        )

        assertEquals(
            hyperfocalDistanceMeters / 2.0,
            result.nearLimitMeters,
            TOLERANCE_METERS,
        )
        assertSame(CalculatedDistance.Infinity, result.farLimit)
        assertSame(CalculatedDistance.Infinity, result.totalDepthOfField)
        assertSame(CalculatedDistance.Infinity, result.depthOfFieldBehind)
    }

    @Test
    fun `focus beyond hyperfocal distance has infinite rear distances`() {
        val result = DepthOfFieldCalculator.calculate(
            DepthOfFieldInput(
                focalLengthMm = 50.0,
                aperture = 8.0,
                circleOfConfusionMm = 0.03,
                focusDistanceMeters = 20.0,
            ),
        )

        assertSame(CalculatedDistance.Infinity, result.farLimit)
        assertSame(CalculatedDistance.Infinity, result.totalDepthOfField)
        assertSame(CalculatedDistance.Infinity, result.depthOfFieldBehind)
        assertTrue(result.nearLimitMeters > 0.0)
        assertTrue((result.depthOfFieldInFront as CalculatedDistance.Finite).meters >= 0.0)
    }

    @Test
    fun `different focal lengths and apertures produce expected limits`() {
        val cases = listOf(
            ExpectedFiniteResult(
                input = DepthOfFieldInput(
                    focalLengthMm = 24.0,
                    aperture = 4.0,
                    circleOfConfusionMm = 0.03,
                    focusDistanceMeters = 2.0,
                ),
                hyperfocalMeters = 4.824,
                nearMeters = 1.4167650531286895,
                farMeters = 3.3994334277620397,
            ),
            ExpectedFiniteResult(
                input = DepthOfFieldInput(
                    focalLengthMm = 85.0,
                    aperture = 2.0,
                    circleOfConfusionMm = 0.03,
                    focusDistanceMeters = 10.0,
                ),
                hyperfocalMeters = 120.50166666666667,
                nearMeters = 9.23924858374148,
                farMeters = 10.897271534365998,
            ),
            ExpectedFiniteResult(
                input = DepthOfFieldInput(
                    focalLengthMm = 35.0,
                    aperture = 16.0,
                    circleOfConfusionMm = 0.02,
                    focusDistanceMeters = 3.0,
                ),
                hyperfocalMeters = 3.863125,
                nearMeters = 1.6905879105713497,
                farMeters = 13.30557566980449,
            ),
        )

        cases.forEach { expected ->
            val result = DepthOfFieldCalculator.calculate(expected.input)

            assertEquals(
                expected.hyperfocalMeters,
                result.hyperfocalDistanceMeters,
                TOLERANCE_METERS,
            )
            assertEquals(
                expected.nearMeters,
                result.nearLimitMeters,
                TOLERANCE_METERS,
            )
            assertEquals(
                expected.farMeters,
                result.farLimit.requireFiniteMeters(),
                TOLERANCE_METERS,
            )
        }
    }

    @Test
    fun `zero and negative inputs are rejected`() {
        val validInput = validInput()

        listOf(
            validInput.copy(focalLengthMm = 0.0),
            validInput.copy(focalLengthMm = -1.0),
            validInput.copy(aperture = 0.0),
            validInput.copy(aperture = -1.0),
            validInput.copy(circleOfConfusionMm = 0.0),
            validInput.copy(circleOfConfusionMm = -0.01),
            validInput.copy(focusDistanceMeters = 0.0),
            validInput.copy(focusDistanceMeters = -1.0),
        ).forEach(::assertInvalid)
    }

    @Test
    fun `NaN and infinite inputs are rejected`() {
        val validInput = validInput()

        listOf(
            validInput.copy(focalLengthMm = Double.NaN),
            validInput.copy(aperture = Double.POSITIVE_INFINITY),
            validInput.copy(circleOfConfusionMm = Double.NEGATIVE_INFINITY),
            validInput.copy(focusDistanceMeters = Double.NaN),
        ).forEach(::assertInvalid)
    }

    @Test
    fun `focus distance must be beyond focal length`() {
        val validInput = validInput()

        assertInvalid(validInput.copy(focusDistanceMeters = 0.05))
        assertInvalid(validInput.copy(focusDistanceMeters = 0.049))
    }

    @Test
    fun `values outside supported Double range are rejected`() {
        assertInvalid(
            validInput().copy(
                focalLengthMm = Double.MAX_VALUE,
                focusDistanceMeters = Double.MAX_VALUE,
            ),
        )
        assertInvalid(
            validInput().copy(
                aperture = Double.MIN_VALUE,
                circleOfConfusionMm = Double.MIN_VALUE,
            ),
        )
    }

    @Test
    fun `small valid boundary values remain non-negative`() {
        val result = DepthOfFieldCalculator.calculate(
            DepthOfFieldInput(
                focalLengthMm = 1.0,
                aperture = 1.0,
                circleOfConfusionMm = 1.0,
                focusDistanceMeters = 0.002,
            ),
        )

        assertEquals(0.002, result.hyperfocalDistanceMeters, TOLERANCE_METERS)
        assertEquals(0.001, result.nearLimitMeters, TOLERANCE_METERS)
        assertEquals(
            0.001,
            (result.depthOfFieldInFront as CalculatedDistance.Finite).meters,
            TOLERANCE_METERS,
        )
        assertSame(CalculatedDistance.Infinity, result.farLimit)
    }

    private fun validInput() = DepthOfFieldInput(
        focalLengthMm = 50.0,
        aperture = 8.0,
        circleOfConfusionMm = 0.03,
        focusDistanceMeters = 5.0,
    )

    private fun assertInvalid(input: DepthOfFieldInput) {
        assertThrows(IllegalArgumentException::class.java) {
            DepthOfFieldCalculator.calculate(input)
        }
    }

    private fun CalculatedDistance.requireFiniteMeters(): Double {
        assertTrue(this is CalculatedDistance.Finite)
        return (this as CalculatedDistance.Finite).meters
    }

    private data class ExpectedFiniteResult(
        val input: DepthOfFieldInput,
        val hyperfocalMeters: Double,
        val nearMeters: Double,
        val farMeters: Double,
    )

    private companion object {
        const val TOLERANCE_METERS = 1e-9
    }
}
