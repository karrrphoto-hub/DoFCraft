package com.example.dofcalculator.data

import com.example.dofcalculator.model.CameraLensKit
import com.example.dofcalculator.model.Lens
import java.net.URLDecoder
import java.net.URLEncoder

/** Separate versioned records. Never reads or overwrites obsolete pre-kit lens archives. */
internal object CameraLensKitCodec {
    fun encode(kit: CameraLensKit): String = buildList {
        add(listOf("lens-kit-1", kit.showAllFocalLengths, kit.showAllApertures).joinToString("\t"))
        kit.lenses.forEach { lens ->
            add(when (lens) {
                is Lens.Prime -> listOf("prime", enc(lens.id), enc(lens.name),
                    lens.focalLengthMm, lens.maxAperture, lens.minAperture ?: "")
                is Lens.Zoom -> listOf("zoom", enc(lens.id), enc(lens.name),
                    lens.minFocalLengthMm, lens.maxApertureAtMinFocal,
                    lens.maxFocalLengthMm, lens.maxApertureAtMaxFocal, lens.minAperture ?: "")
            }.joinToString("\t"))
        }
    }.joinToString("\n")

    fun decode(raw: String?): CameraLensKit {
        if (raw == null) return CameraLensKit()
        val lines = raw.lineSequence().toList()
        val header = lines.firstOrNull()?.split("\t") ?: return CameraLensKit()
        if (header.size != 3 || header[0] != "lens-kit-1") return CameraLensKit()
        val lenses = lines.drop(1).mapNotNull { line -> runCatching {
            val r = line.split("\t")
            when (r[0]) {
                "prime" -> {
                    require(r.size == 6)
                    Lens.Prime(dec(r[1]), dec(r[2]), r[3].toDouble(), r[4].toDouble(),
                        r[5].takeIf { it.isNotEmpty() }?.toDouble())
                }
                "zoom" -> {
                    require(r.size == 8)
                    Lens.Zoom(dec(r[1]), dec(r[2]), r[3].toDouble(), r[4].toDouble(),
                        r[5].toDouble(), r[6].toDouble(), r[7].takeIf { it.isNotEmpty() }?.toDouble())
                }
                else -> null
            }
        }.getOrNull() }.distinctBy { it.id }
        return CameraLensKit(lenses, header[1] == "true", header[2] == "true")
    }

    private fun enc(value: String) = URLEncoder.encode(value, "UTF-8")
    private fun dec(value: String) = URLDecoder.decode(value, "UTF-8")
}
