package com.example.dofcalculator.data

import com.example.dofcalculator.model.CameraLensKit
import com.example.dofcalculator.model.Lens
import com.example.dofcalculator.model.LensDistanceUnit
import java.net.URLDecoder
import java.net.URLEncoder

/** Separate versioned records. Never reads or overwrites obsolete pre-kit lens archives. */
internal object CameraLensKitCodec {
    fun encode(kit: CameraLensKit): String = buildList {
        add(listOf("lens-kit-3", kit.ignoreLensProfile, kit.showAllApertures,
            kit.selectedLensId?.let(::enc).orEmpty()).joinToString("\t"))
        kit.lenses.forEach { lens ->
            add(when (lens) {
                is Lens.Prime -> listOf("prime", enc(lens.id), enc(lens.name),
                    lens.focalLengthMm, lens.maxAperture, lens.minAperture ?: "")
                is Lens.Zoom -> listOf("zoom", enc(lens.id), enc(lens.name),
                    lens.minFocalLengthMm, lens.maxApertureAtMinFocal,
                    lens.maxFocalLengthMm, lens.maxApertureAtMaxFocal, lens.minAperture ?: "")
            }.plus(lens.distanceScaleUnit?.name.orEmpty()).joinToString("\t"))
        }
    }.joinToString("\n")

    fun decode(raw: String?): CameraLensKit {
        if (raw == null) return CameraLensKit()
        val lines = raw.lineSequence().toList()
        val header = lines.firstOrNull()?.split("\t") ?: return CameraLensKit()
        val version = header.firstOrNull()
        if (version !in setOf("lens-kit-1", "lens-kit-2", "lens-kit-3") ||
            header.size != if (version == "lens-kit-3") 4 else 3) return CameraLensKit()
        val hasDistanceUnit = version != "lens-kit-1"
        val lenses = lines.drop(1).mapNotNull { line -> runCatching {
            val r = line.split("\t")
            val distanceUnit = if (hasDistanceUnit)
                LensDistanceUnit.entries.firstOrNull { it.name == r.last() } else null
            when (r[0]) {
                "prime" -> {
                    require(r.size == if (hasDistanceUnit) 7 else 6)
                    Lens.Prime(dec(r[1]), dec(r[2]), r[3].toDouble(), r[4].toDouble(),
                        r[5].takeIf { it.isNotEmpty() }?.toDouble(), distanceUnit)
                }
                "zoom" -> {
                    require(r.size == if (hasDistanceUnit) 9 else 8)
                    Lens.Zoom(dec(r[1]), dec(r[2]), r[3].toDouble(), r[4].toDouble(),
                        r[5].toDouble(), r[6].toDouble(), r[7].takeIf { it.isNotEmpty() }?.toDouble(), distanceUnit)
                }
                else -> null
            }
        }.getOrNull() }.distinctBy { it.id }
        val selectedId = header.getOrNull(3)?.takeIf { it.isNotEmpty() }?.let {
            runCatching { dec(it) }.getOrNull()
        }?.takeIf { id -> lenses.any { it.id == id } }
        return CameraLensKit(lenses, header[1] == "true", header[2] == "true", selectedId)
    }

    private fun enc(value: String) = URLEncoder.encode(value, "UTF-8")
    private fun dec(value: String) = URLDecoder.decode(value, "UTF-8")
}
