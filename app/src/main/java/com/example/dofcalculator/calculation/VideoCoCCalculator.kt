package com.example.dofcalculator.calculation

import com.example.dofcalculator.model.*
import kotlin.math.abs

class VideoInputException(
    val error: VideoError,
    val fields: Set<VideoField> = emptySet(),
) : IllegalArgumentException(error.name)

/** Geometric blur in output video pixels, expressed back in the recording plane. */
object VideoCoCCalculator {
    // Allows rounded physical dimensions such as 23.5 × 13.22 mm for 16:9.
    const val ASPECT_RELATIVE_TOLERANCE = 0.001

    fun fieldError(field: VideoField): VideoError = when (field) {
        VideoField.WIDTH, VideoField.HEIGHT -> VideoError.INVALID_RESOLUTION
        VideoField.BLUR -> VideoError.INVALID_BLUR
        VideoField.CROP -> VideoError.INVALID_CROP
        VideoField.ACTIVE_WIDTH, VideoField.ACTIVE_HEIGHT -> VideoError.INVALID_AREA
    }

    fun parseField(field: VideoField, text: String): Double? {
        if (field.integer && !text.trim().matches(Regex("[0-9]+"))) return null
        return SharpnessCriterionCalculator.positiveNumber(text)?.takeIf(field::isValid)
    }

    private fun positive(value: Double?, error: VideoError = VideoError.NUMERIC_RANGE,
                         fields: Set<VideoField> = emptySet()): Double {
        if (value == null || !value.isFinite() || value <= 0.0) throw VideoInputException(error, fields)
        return value
    }

    fun calculate(sensorWidthMm: Double?, sensorHeightMm: Double?, s: VideoBasedSettings): VideoCoCResult {
        val sw = positive(sensorWidthMm, VideoError.MISSING_SENSOR)
        val sh = positive(sensorHeightMm, VideoError.MISSING_SENSOR)
        val width = s.widthPx
        val height = s.heightPx
        if (width <= 0 || height <= 0) throw VideoInputException(VideoError.INVALID_RESOLUTION,
            setOf(VideoField.WIDTH, VideoField.HEIGHT))
        val k = positive(s.blurPixels, VideoError.INVALID_BLUR, setOf(VideoField.BLUR))
        val videoAspect = positive(width.toDouble() / height)
        val sensorAspect = positive(sw / sh)
        val base = if (sensorAspect > videoAspect)
            VideoActiveArea(positive(sh * videoAspect), sh)
        else VideoActiveArea(sw, positive(sw / videoAspect))

        val active = when (s.areaMode) {
            VideoAreaMode.AUTO -> {
                if (!VideoField.CROP.isValid(s.cropFactor)) throw VideoInputException(
                    VideoError.INVALID_CROP, setOf(VideoField.CROP))
                VideoActiveArea(
                    positive(base.widthMm / s.cropFactor, fields = setOf(VideoField.CROP)),
                    positive(base.heightMm / s.cropFactor, fields = setOf(VideoField.CROP)))
            }
            VideoAreaMode.MANUAL -> {
                val w = positive(s.manualWidthMm, VideoError.INVALID_AREA, setOf(VideoField.ACTIVE_WIDTH))
                val h = positive(s.manualHeightMm, VideoError.INVALID_AREA, setOf(VideoField.ACTIVE_HEIGHT))
                val exceeded = buildSet {
                    if (w > sw) add(VideoField.ACTIVE_WIDTH)
                    if (h > sh) add(VideoField.ACTIVE_HEIGHT)
                }
                if (exceeded.isNotEmpty()) throw VideoInputException(VideoError.AREA_EXCEEDS_SENSOR, exceeded)
                val ratio = w / h / videoAspect
                if (!ratio.isFinite() || abs(ratio - 1.0) > ASPECT_RELATIVE_TOLERANCE)
                    throw VideoInputException(VideoError.ASPECT_MISMATCH,
                        setOf(VideoField.ACTIVE_WIDTH, VideoField.ACTIVE_HEIGHT))
                VideoActiveArea(w, h)
            }
        }
        val pitchX = positive(active.widthMm / width)
        val pitchY = positive(active.heightMm / height)
        if (abs(pitchX / pitchY - 1.0) > ASPECT_RELATIVE_TOLERANCE)
            throw VideoInputException(VideoError.ASPECT_MISMATCH,
                setOf(VideoField.ACTIVE_WIDTH, VideoField.ACTIVE_HEIGHT))
        val coc = positive(k * pitchX, fields = setOf(VideoField.BLUR))
        positive(k * pitchY, fields = setOf(VideoField.BLUR)) // Both axes must remain representable.
        return VideoCoCResult(VideoActiveArea(sw, sh), base, active, width, height,
            s.cropFactor.takeIf { s.areaMode == VideoAreaMode.AUTO }, pitchX, pitchY, k, coc)
    }
}
