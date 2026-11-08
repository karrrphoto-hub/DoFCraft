package com.example.dofcalculator.calculation

import com.example.dofcalculator.model.*
import org.junit.Assert.*
import org.junit.Test

class VideoCoCCalculatorTest {
    private fun full(s: VideoBasedSettings = VideoBasedSettings()) =
        VideoCoCCalculator.calculate(36.0, 24.0, s)

    @Test fun `presets have distinct exact recorded dimensions`() {
        val expected = listOf(1280 to 720, 1920 to 1080, 2048 to 1080, 2560 to 1440,
            3840 to 2160, 4096 to 2160, 7680 to 4320)
        assertEquals(expected, VideoResolution.entries.dropLast(1).map { it.widthPx to it.heightPx })
        assertNull(VideoResolution.CUSTOM.widthPx)
        assertEquals(VideoResolution.UHD_4K, VideoBasedSettings().resolution)
        assertEquals("16:9", full().aspectRatioText)
        assertEquals("256:135", full(VideoBasedSettings(resolution = VideoResolution.DCI_4K)).aspectRatioText)
    }

    @Test fun `maximum aspect matched areas use the actual sensor including square and four thirds`() {
        for ((w, h, aw, ah) in listOf(
            listOf(36.0, 24.0, 36.0, 20.25),
            listOf(18.0, 13.5, 18.0, 10.125),
            listOf(56.0, 56.0, 56.0, 31.5),
            listOf(43.8, 32.9, 43.8, 24.6375))) {
            val result = VideoCoCCalculator.calculate(w, h, VideoBasedSettings())
            assertEquals(aw, result.activeArea.widthMm, 1e-12)
            assertEquals(ah, result.activeArea.heightMm, 1e-12)
            assertEquals(result.pixelPitchXmm, result.pixelPitchYmm, 1e-15)
        }
        val square = full(VideoBasedSettings(resolution = VideoResolution.CUSTOM,
            customWidthPx = 1080, customHeightPx = 1080))
        assertEquals(VideoActiveArea(24.0, 24.0), square.activeArea)
        val portrait = full(VideoBasedSettings(resolution = VideoResolution.CUSTOM,
            customWidthPx = 1080, customHeightPx = 1920))
        assertEquals(VideoActiveArea(13.5, 24.0), portrait.activeArea)
    }

    @Test fun `reference video CoCs are calculated without intermediate rounding`() {
        for ((resolution, coc) in listOf(VideoResolution.FHD to .05625,
            VideoResolution.UHD_4K to .028125, VideoResolution.UHD_8K to .0140625,
            VideoResolution.DCI_4K to .0263671875)) {
            assertEquals(coc, full(VideoBasedSettings(resolution = resolution)).effectiveCoCmm, 1e-15)
        }
        val cropped = full(VideoBasedSettings(cropFactor = 1.5))
        assertEquals(VideoActiveArea(36.0, 20.25), cropped.baseActiveArea)
        assertEquals(VideoActiveArea(24.0, 13.5), cropped.activeArea)
        assertEquals(.01875, cropped.effectiveCoCmm, 1e-15)
        assertEquals(.0140625, full(VideoBasedSettings(cropFactor = 2.0)).effectiveCoCmm, 1e-15)
    }

    @Test fun `resolution blur and crop obey their independent scaling laws`() {
        val base = full().effectiveCoCmm
        assertEquals(base / 2, full(VideoBasedSettings(resolution = VideoResolution.UHD_8K)).effectiveCoCmm, 1e-15)
        assertEquals(base / 2, full(VideoBasedSettings(blurPixels = 1.5)).effectiveCoCmm, 1e-15)
        assertEquals(base * 2, full(VideoBasedSettings(blurPixels = 6.0)).effectiveCoCmm, 1e-15)
        assertEquals(base / 3, full(VideoBasedSettings(cropFactor = 3.0)).effectiveCoCmm, 1e-15)
    }

    @Test fun `manual recording area replaces auto crop and permits rounded matching dimensions`() {
        val s = VideoBasedSettings(areaMode = VideoAreaMode.MANUAL,
            manualWidthMm = 24.0, manualHeightMm = 13.5, cropFactor = 2.0)
        val manual = full(s)
        assertEquals(.01875, manual.effectiveCoCmm, 1e-15)
        assertNull(manual.cropFactor)
        assertEquals(manual, full(s.copy(cropFactor = Double.NaN)))
        val rounded = full(s.copy(manualWidthMm = 23.5, manualHeightMm = 13.22))
        assertEquals(3 * 23.5 / 3840, rounded.effectiveCoCmm, 1e-15)
        assertTrue(kotlin.math.abs(rounded.pixelPitchXmm / rounded.pixelPitchYmm - 1) < .001)
    }

    private fun rejected(error: VideoError, s: VideoBasedSettings,
                         w: Double? = 36.0, h: Double? = 24.0) {
        val failure = runCatching { VideoCoCCalculator.calculate(w, h, s) }.exceptionOrNull()
        assertTrue("Expected typed rejection, got $failure", failure is VideoInputException)
        assertEquals(error, (failure as VideoInputException).error)
    }

    @Test fun `bad geometry and nonfinite inputs are rejected centrally`() {
        val s = VideoBasedSettings()
        for (bad in listOf(0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY)) {
            rejected(VideoError.MISSING_SENSOR, s, bad)
            rejected(VideoError.MISSING_SENSOR, s, h = bad)
            rejected(VideoError.INVALID_BLUR, s.copy(blurPixels = bad))
            rejected(VideoError.INVALID_AREA, s.copy(areaMode = VideoAreaMode.MANUAL, manualWidthMm = bad))
        }
        rejected(VideoError.MISSING_SENSOR, s, null)
        for (crop in listOf(.999, 0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY))
            rejected(VideoError.INVALID_CROP, s.copy(cropFactor = crop))
        for (n in listOf(0, -1, Int.MIN_VALUE)) {
            rejected(VideoError.INVALID_RESOLUTION, s.copy(resolution = VideoResolution.CUSTOM, customWidthPx = n))
            rejected(VideoError.INVALID_RESOLUTION, s.copy(resolution = VideoResolution.CUSTOM, customHeightPx = n))
        }
        rejected(VideoError.AREA_EXCEEDS_SENSOR, s.copy(areaMode = VideoAreaMode.MANUAL, manualWidthMm = 37.0))
        rejected(VideoError.AREA_EXCEEDS_SENSOR, s.copy(areaMode = VideoAreaMode.MANUAL, manualHeightMm = 25.0))
        rejected(VideoError.ASPECT_MISMATCH, s.copy(areaMode = VideoAreaMode.MANUAL, manualHeightMm = 24.0))
    }

    @Test fun `integer input and decimal comma are validated without crashing on drafts`() {
        for (text in listOf("", "0", "-2", "1.5", "1,5", "3840.0", "1e3",
            "NaN", "Infinity", "2147483648", "999999999999999999999999999999999")) {
            assertNull(text, VideoCoCCalculator.parseField(VideoField.WIDTH, text))
        }
        assertEquals(3840.0, VideoCoCCalculator.parseField(VideoField.WIDTH, "3840")!!, 0.0)
        assertEquals(1.5, VideoCoCCalculator.parseField(VideoField.BLUR, "1,5")!!, 0.0)
        assertEquals(2.5, VideoCoCCalculator.parseField(VideoField.CROP, "2.5")!!, 0.0)
        for (field in VideoField.entries)
            for (text in listOf("", ".", ",", "1,", "NaN", "Infinity", "1e999", "-1", "0"))
                assertNull("$field: $text", VideoCoCCalculator.parseField(field, text))
    }

    @Test fun `extreme valid CoCs remain positive and finite while overflow is rejected`() {
        for (s in listOf(VideoBasedSettings(blurPixels = 1e-200),
            VideoBasedSettings(blurPixels = 1e200), VideoBasedSettings(cropFactor = 1e200))) {
            val c = full(s).effectiveCoCmm
            assertTrue(c.isFinite() && c > 0)
        }
        rejected(VideoError.NUMERIC_RANGE, VideoBasedSettings(resolution = VideoResolution.CUSTOM,
            customWidthPx = 1, customHeightPx = 1, blurPixels = Double.MAX_VALUE))
        assertTrue(runCatching {
            VideoCoCCalculator.calculate(Double.MIN_VALUE, Double.MIN_VALUE,
                VideoBasedSettings(cropFactor = Double.MAX_VALUE))
        }.exceptionOrNull() is VideoInputException)
    }
}
