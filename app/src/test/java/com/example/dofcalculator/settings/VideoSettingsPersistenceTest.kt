package com.example.dofcalculator.settings

import androidx.datastore.preferences.core.*
import com.example.dofcalculator.model.*
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test

class VideoSettingsPersistenceTest {
    @Test fun `video and independent optical preferences survive a real DataStore restart`() = runBlocking {
        val dir = Files.createTempDirectory("video-restart").toFile()
        val file = File(dir, "settings.preferences_pb")
        var job = SupervisorJob()
        fun store() = PreferenceDataStoreFactory.create(
            scope = CoroutineScope(job + Dispatchers.IO), produceFile = { file })
        val video = VideoBasedSettings(resolution = VideoResolution.CUSTOM,
            customWidthPx = 4096, customHeightPx = 2160, blurPixels = 2.5,
            areaMode = VideoAreaMode.MANUAL, cropFactor = 1.75,
            manualWidthMm = 24.0, manualHeightMm = 12.65625)
        val expected = SharpnessSettings(mode = SharpnessCriterionMode.VIDEO_BASED,
            megapixels = 45.7, printViewingCm = 125.5, screenBlurPx = 3.25, video = video)
        try {
            var repo = DataStoreAppSettingsRepository(store())
            repo.setLanguage(AppLanguage.Russian)
            repo.saveCalculatorSettings(CalculatorSettings(focalLengthText = "85",
                apertureText = "11", focusDistanceText = "7.25"))
            repo.saveDiffractionSettings(DiffractionSettings(warningsEnabled = false))
            repo.saveSharpnessSettings(expected)
            job.cancelAndJoin()
            job = SupervisorJob()
            repo = DataStoreAppSettingsRepository(store())
            val restored = repo.settings.first()
            assertEquals(expected, restored.sharpness)
            assertEquals(AppLanguage.Russian, restored.language)
            assertEquals("85", restored.calculator.focalLengthText)
            assertEquals("7.25", restored.calculator.focusDistanceText)
            assertFalse(restored.diffraction.warningsEnabled)
            for (mode in SharpnessCriterionMode.entries) {
                repo.saveSharpnessSettings(expected.copy(mode = mode))
                assertEquals(video, repo.settings.first().sharpness.video)
            }
        } finally { job.cancelAndJoin(); dir.deleteRecursively() }
    }

    @Test fun `pre-video named records retain every existing criterion without migration reset`() = runBlocking {
        val dir = Files.createTempDirectory("pre-video-upgrade").toFile()
        val file = File(dir, "settings.preferences_pb")
        val job = SupervisorJob()
        val store = PreferenceDataStoreFactory.create(
            scope = CoroutineScope(job + Dispatchers.IO), produceFile = { file })
        try {
            for (name in listOf("STANDARD", "PIXEL_BASED", "PRINT_BASED", "SCREEN_BASED", "CUSTOM")) {
                store.edit { it[stringPreferencesKey("sharpness_settings_v2")] =
                    "v2\nmode=$name\nMEGAPIXELS=45.7\nCUSTOM_COC=0.008765\nprintPlacement=FILL" }
                val restored = DataStoreAppSettingsRepository(store).settings.first()
                assertEquals(name, restored.sharpness.mode.name)
                assertEquals(VideoBasedSettings(), restored.sharpness.video)
                assertEquals(45.7, restored.sharpness.megapixels, 0.0)
                assertEquals(.008765, restored.sharpness.customCoCmm, 0.0)
                assertEquals(PrintPlacement.FILL, restored.sharpness.printPlacement)
                assertTrue(restored.sharpnessMigrated)
                assertEquals(restored.sharpness,
                    SharpnessSettingsCodec.decode(SharpnessSettingsCodec.encode(restored.sharpness)))
            }
        } finally { job.cancelAndJoin(); dir.deleteRecursively() }
    }

    @Test fun `corrupt new fields fall back individually and old stable enum values are preserved`() {
        val s = SharpnessSettingsCodec.decode("v2\nmode=SCREEN_BASED\nMEGAPIXELS=45.7" +
            "\nvideoResolution=unknown\nvideoAreaMode=unknown\nvideo.WIDTH=1.5" +
            "\nvideo.HEIGHT=0\nvideo.BLUR=NaN\nvideo.CROP=0.5\nvideo.ACTIVE_WIDTH=Infinity")
        assertEquals(SharpnessCriterionMode.SCREEN_BASED, s.mode)
        assertEquals(45.7, s.megapixels, 0.0)
        assertEquals(VideoBasedSettings(), s.video)
        assertEquals(SharpnessCriterionMode.STANDARD, SharpnessSettingsCodec.decode(null).mode)
        assertEquals(listOf("STANDARD", "PIXEL_BASED", "PRINT_BASED", "SCREEN_BASED", "CUSTOM"),
            SharpnessCriterionMode.entries.take(5).map { it.name })
        assertEquals(listOf(SharpnessCriterionMode.STANDARD, SharpnessCriterionMode.PIXEL_BASED,
            SharpnessCriterionMode.VIDEO_BASED, SharpnessCriterionMode.PRINT_BASED,
            SharpnessCriterionMode.SCREEN_BASED, SharpnessCriterionMode.CUSTOM),
            SharpnessCriterionMode.displayOrder)
    }
}
