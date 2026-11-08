package com.example.dofcalculator.model

/** Physical dimensions in the camera profile's width/height orientation, in millimetres. */
data class VideoActiveArea(val widthMm: Double, val heightMm: Double)

enum class VideoError {
    INVALID_RESOLUTION, INVALID_BLUR, INVALID_CROP, INVALID_AREA,
    AREA_EXCEEDS_SENSOR, ASPECT_MISMATCH, MISSING_SENSOR, NUMERIC_RANGE,
}

data class VideoCoCResult(
    val sensorArea: VideoActiveArea,
    val baseActiveArea: VideoActiveArea,
    val activeArea: VideoActiveArea,
    val videoWidthPx: Int,
    val videoHeightPx: Int,
    val cropFactor: Double?,
    val pixelPitchXmm: Double,
    val pixelPitchYmm: Double,
    val blurPixels: Double,
    val effectiveCoCmm: Double,
) {
    val aspectRatioText: String get() {
        var a = videoWidthPx
        var b = videoHeightPx
        while (b != 0) { val next = a % b; a = b; b = next }
        return "${videoWidthPx / a}:${videoHeightPx / a}"
    }
}
