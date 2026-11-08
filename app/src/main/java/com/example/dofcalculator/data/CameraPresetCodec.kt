package com.example.dofcalculator.data

import com.example.dofcalculator.model.CameraProfile
import com.example.dofcalculator.model.FrameFormat
import java.net.URLDecoder
import java.net.URLEncoder

/** v3 stores geometry only. v1/v2 CoCs and obsolete lens records stay in their archive. */
internal object CameraPresetCodec {
    fun encode(camera: CameraProfile): String = listOf(
        "camera3", enc(camera.id), enc(camera.name), camera.frameFormat.name,
        camera.widthMm?.toString().orEmpty(), camera.heightMm?.toString().orEmpty(),
        enc(camera.customFormatName),
    ).joinToString("\t")

    fun decode(encoded: String): CameraProfile? = runCatching {
        val r = encoded.lineSequence().first().split("\t")
        require(r.size >= 5 && r[0] in listOf("camera", "camera2", "camera3"))
        val format = FrameFormat.valueOf(r[3])
        CameraProfile(
            id = dec(r[1]), name = dec(r[2]), frameFormat = format,
            widthMm = if (r[0] == "camera3") r[4].toDoubleOrNull() else format.widthMm,
            heightMm = if (r[0] == "camera3") r.getOrNull(5)?.toDoubleOrNull() else format.heightMm,
            customFormatName = if (format == FrameFormat.Custom)
                r.getOrNull(6)?.let(::dec)?.ifBlank { null } ?: dec(r[2]) else "",
        )
    }.getOrNull()

    fun legacyCoC(encoded: String): Double? = runCatching {
        val r = encoded.lineSequence().first().split("\t")
        if (r[0] !in listOf("camera", "camera2")) return null
        val value = r[4].toDouble()
        val format = FrameFormat.valueOf(r[3])
        value.takeIf { it.isFinite() && it > 0.0 &&
            (r[0] != "camera2" || r.getOrNull(5) != "true") }
    }.getOrNull()

    private fun enc(value: String) = URLEncoder.encode(value, "UTF-8")
    private fun dec(value: String) = URLDecoder.decode(value, "UTF-8")
}
