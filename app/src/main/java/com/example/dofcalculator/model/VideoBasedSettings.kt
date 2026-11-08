package com.example.dofcalculator.model

enum class VideoAreaMode { AUTO, MANUAL }

data class VideoBasedSettings(
    val resolution: VideoResolution = VideoResolution.UHD_4K,
    val customWidthPx: Int = 3840,
    val customHeightPx: Int = 2160,
    val blurPixels: Double = 3.0,
    val areaMode: VideoAreaMode = VideoAreaMode.AUTO,
    val cropFactor: Double = 1.0,
    val manualWidthMm: Double = 36.0,
    val manualHeightMm: Double = 20.25,
) {
    val widthPx: Int get() = resolution.widthPx ?: customWidthPx
    val heightPx: Int get() = resolution.heightPx ?: customHeightPx
}

enum class VideoField {
    WIDTH, HEIGHT, BLUR, CROP, ACTIVE_WIDTH, ACTIVE_HEIGHT;

    val integer: Boolean get() = this == WIDTH || this == HEIGHT

    fun isValid(value: Double?): Boolean =
        value != null && value.isFinite() && value > 0.0 && when {
            integer -> value <= Int.MAX_VALUE.toDouble() && value % 1.0 == 0.0
            this == CROP -> value >= 1.0
            else -> true
        }

    fun value(s: VideoBasedSettings): Double = when (this) {
        WIDTH -> s.customWidthPx.toDouble()
        HEIGHT -> s.customHeightPx.toDouble()
        BLUR -> s.blurPixels
        CROP -> s.cropFactor
        ACTIVE_WIDTH -> s.manualWidthMm
        ACTIVE_HEIGHT -> s.manualHeightMm
    }

    fun update(s: VideoBasedSettings, value: Double): VideoBasedSettings {
        require(isValid(value))
        return when (this) {
            WIDTH -> s.copy(customWidthPx = value.toInt())
            HEIGHT -> s.copy(customHeightPx = value.toInt())
            BLUR -> s.copy(blurPixels = value)
            CROP -> s.copy(cropFactor = value)
            ACTIVE_WIDTH -> s.copy(manualWidthMm = value)
            ACTIVE_HEIGHT -> s.copy(manualHeightMm = value)
        }
    }

    companion object {
        fun active(s: VideoBasedSettings): Set<VideoField> = buildSet {
            if (s.resolution == VideoResolution.CUSTOM) { add(WIDTH); add(HEIGHT) }
            add(BLUR)
            if (s.areaMode == VideoAreaMode.AUTO) add(CROP)
            else { add(ACTIVE_WIDTH); add(ACTIVE_HEIGHT) }
        }
    }
}
