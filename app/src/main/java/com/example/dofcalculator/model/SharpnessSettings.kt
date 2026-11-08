package com.example.dofcalculator.model

enum class SharpnessCriterionMode {
    STANDARD, PIXEL_BASED, PRINT_BASED, SCREEN_BASED, CUSTOM, VIDEO_BASED;

    companion object {
        // Keep existing enum names/ordinals stable; display order is a separate UI concern.
        val displayOrder = listOf(STANDARD, PIXEL_BASED, VIDEO_BASED, PRINT_BASED, SCREEN_BASED, CUSTOM)
    }
}
enum class PrintPlacement { FIT, FILL }
enum class ScreenCriterion { VISUAL, DISPLAY_PIXELS }
enum class PrintSizePreset(val widthCm: Double?, val heightCm: Double?) {
    CM10X15(10.0, 15.0), CM15X20(15.0, 20.0), CM20X30(20.0, 30.0),
    CM30X45(30.0, 45.0), CM40X60(40.0, 60.0), CM60X90(60.0, 90.0), CUSTOM(null, null)
}
enum class ScreenDevice(val diagonal: Double, val widthPx: Double, val heightPx: Double,
                        val viewingCm: Double) {
    SMARTPHONE(6.7, 2400.0, 1080.0, 30.0),
    TABLET(11.0, 2560.0, 1600.0, 50.0),
    MONITOR(27.0, 3840.0, 2160.0, 60.0),
    TV(55.0, 3840.0, 2160.0, 200.0),
    CUSTOM(27.0, 3840.0, 2160.0, 60.0)
}

data class SharpnessSettings(
    val mode: SharpnessCriterionMode = SharpnessCriterionMode.STANDARD,
    val megapixels: Double = 24.0,
    val pixelBlur: Double = 3.0,
    val printWidthCm: Double = 30.0,
    val printHeightCm: Double = 45.0,
    val printPpi: Double? = null,
    val printDpi: Double? = null,
    val printPlacement: PrintPlacement = PrintPlacement.FIT,
    val customCoCmm: Double = 0.030,
    val printSize: PrintSizePreset = PrintSizePreset.CM30X45,
    val printViewingCm: Double = 50.0,
    val printAngleMinutes: Double = 2.0,
    val screenDevice: ScreenDevice = ScreenDevice.MONITOR,
    val screenDiagonalInches: Double = 27.0,
    val screenWidthPx: Double = 3840.0,
    val screenHeightPx: Double = 2160.0,
    val screenViewingCm: Double = 60.0,
    val screenAngleMinutes: Double = 1.0,
    val screenCriterion: ScreenCriterion = ScreenCriterion.VISUAL,
    val screenBlurPx: Double = 2.0,
    val screenPlacement: PrintPlacement = PrintPlacement.FIT,
    val printModelNoticePending: Boolean = false,
    val video: VideoBasedSettings = VideoBasedSettings(),
)

enum class SharpnessField {
    MEGAPIXELS, PIXEL_BLUR, PRINT_WIDTH, PRINT_HEIGHT, PRINT_PPI, PRINT_DPI,
    PRINT_VIEWING, PRINT_ANGLE, SCREEN_DIAGONAL, SCREEN_WIDTH, SCREEN_HEIGHT,
    SCREEN_VIEWING, SCREEN_ANGLE, SCREEN_BLUR, CUSTOM_COC;

    val optional: Boolean get() = this == PRINT_PPI || this == PRINT_DPI

    fun isValid(value: Double?): Boolean {
        if (value == null || !value.isFinite() || value <= 0.0) return false
        return when (this) {
            PRINT_ANGLE, SCREEN_ANGLE -> value < 10800.0 &&
                value * (kotlin.math.PI / 10800.0) > 0.0
            PRINT_WIDTH, PRINT_HEIGHT, PRINT_VIEWING, SCREEN_VIEWING -> (value * 10.0).isFinite()
            SCREEN_DIAGONAL -> (value * 25.4).isFinite()
            MEGAPIXELS -> (value * 1_000_000.0).isFinite()
            else -> true
        }
    }

    fun value(s: SharpnessSettings): Double? = when (this) {
        MEGAPIXELS -> s.megapixels
        PIXEL_BLUR -> s.pixelBlur
        PRINT_WIDTH -> s.printWidthCm
        PRINT_HEIGHT -> s.printHeightCm
        PRINT_PPI -> s.printPpi
        PRINT_DPI -> s.printDpi
        PRINT_VIEWING -> s.printViewingCm
        PRINT_ANGLE -> s.printAngleMinutes
        SCREEN_DIAGONAL -> s.screenDiagonalInches
        SCREEN_WIDTH -> s.screenWidthPx
        SCREEN_HEIGHT -> s.screenHeightPx
        SCREEN_VIEWING -> s.screenViewingCm
        SCREEN_ANGLE -> s.screenAngleMinutes
        SCREEN_BLUR -> s.screenBlurPx
        CUSTOM_COC -> s.customCoCmm
    }

    fun update(s: SharpnessSettings, value: Double?): SharpnessSettings {
        if (this == PRINT_PPI) return s.copy(printPpi = value)
        if (this == PRINT_DPI) return s.copy(printDpi = value)
        val v = requireNotNull(value)
        return when (this) {
            MEGAPIXELS -> s.copy(megapixels = v)
            PIXEL_BLUR -> s.copy(pixelBlur = v)
            PRINT_WIDTH -> s.copy(printWidthCm = v, printSize = PrintSizePreset.CUSTOM)
            PRINT_HEIGHT -> s.copy(printHeightCm = v, printSize = PrintSizePreset.CUSTOM)
            PRINT_VIEWING -> s.copy(printViewingCm = v)
            PRINT_ANGLE -> s.copy(printAngleMinutes = v)
            SCREEN_DIAGONAL -> s.copy(screenDiagonalInches = v)
            SCREEN_WIDTH -> s.copy(screenWidthPx = v)
            SCREEN_HEIGHT -> s.copy(screenHeightPx = v)
            SCREEN_VIEWING -> s.copy(screenViewingCm = v)
            SCREEN_ANGLE -> s.copy(screenAngleMinutes = v)
            SCREEN_BLUR -> s.copy(screenBlurPx = v)
            CUSTOM_COC -> s.copy(customCoCmm = v)
            PRINT_PPI, PRINT_DPI -> error("Handled above")
        }
    }

    companion object {
        fun active(s: SharpnessSettings): List<SharpnessField> = when (s.mode) {
            SharpnessCriterionMode.STANDARD, SharpnessCriterionMode.VIDEO_BASED -> emptyList()
            SharpnessCriterionMode.PIXEL_BASED -> listOf(MEGAPIXELS, PIXEL_BLUR)
            SharpnessCriterionMode.PRINT_BASED -> listOf(PRINT_WIDTH, PRINT_HEIGHT, PRINT_VIEWING, PRINT_ANGLE)
            SharpnessCriterionMode.SCREEN_BASED -> listOf(SCREEN_DIAGONAL, SCREEN_WIDTH, SCREEN_HEIGHT,
                SCREEN_VIEWING, if (s.screenCriterion == ScreenCriterion.VISUAL) SCREEN_ANGLE else SCREEN_BLUR)
            SharpnessCriterionMode.CUSTOM -> listOf(CUSTOM_COC)
        }
    }
}
