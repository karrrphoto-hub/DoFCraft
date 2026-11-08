package com.example.dofcalculator.model

/** Camera identity and physical recording area only. Sharpness settings live separately. */
data class CameraProfile(
    val id: String,
    val name: String,
    val frameFormat: FrameFormat,
    val isBuiltIn: Boolean = false,
    val customFormatName: String = "",
    val widthMm: Double? = frameFormat.widthMm,
    val heightMm: Double? = frameFormat.heightMm,
) {
    /** Explicit catalogue value through the stable format link; StandardCoCResolver resolves any fallback. */
    val standardCoCmm: Double? get() = frameFormat.suggestedCircleOfConfusionMm
    init {
        require(id.isNotBlank() && name.isNotBlank())
        require(frameFormat != FrameFormat.Custom || customFormatName.isNotBlank())
        require(widthMm == null || (widthMm.isFinite() && widthMm > 0.0))
        require(heightMm == null || (heightMm.isFinite() && heightMm > 0.0))
    }
}
