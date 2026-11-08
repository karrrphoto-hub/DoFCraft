package com.example.dofcalculator.settings

import com.example.dofcalculator.model.*

/** v2 is named and independent of camera records. Raw v1 data remains in DataStore. */
internal object SharpnessSettingsCodec {
    fun encode(s: SharpnessSettings): String = buildList {
        add("v2")
        add("mode=" + s.mode.name)
        SharpnessField.entries.forEach { add(it.name + "=" + it.value(s)?.toString().orEmpty()) }
        add("printPlacement=" + s.printPlacement.name)
        add("printSize=" + s.printSize.name)
        add("screenDevice=" + s.screenDevice.name)
        add("screenCriterion=" + s.screenCriterion.name)
        add("screenPlacement=" + s.screenPlacement.name)
        add("printNotice=" + s.printModelNoticePending)
        add("videoResolution=" + s.video.resolution.name)
        add("videoAreaMode=" + s.video.areaMode.name)
        VideoField.entries.forEach { add("video." + it.name + "=" + it.value(s.video)) }
    }.joinToString("\n")

    fun decode(text: String?): SharpnessSettings {
        if (text == null) return SharpnessSettings()
        return if (text.startsWith("v2\n")) decodeV2(text) else decodeLegacy(text)
    }

    private fun decodeV2(text: String): SharpnessSettings = runCatching {
        val values = text.lineSequence().drop(1).associate {
            it.substringBefore('=') to it.substringAfter('=', "")
        }
        var result = SharpnessSettings()
        for (field in SharpnessField.entries) {
            val raw = values[field.name] ?: continue
            if (field.optional && raw.isEmpty()) result = field.update(result, null)
            else raw.toDoubleOrNull()?.takeIf(field::isValid)?.let { result = field.update(result, it) }
        }
        var video = result.video
        for (field in VideoField.entries) {
            values["video." + field.name]?.toDoubleOrNull()?.takeIf(field::isValid)?.let {
                video = field.update(video, it)
            }
        }
        result.copy(
            video = video.copy(
                resolution = enumValue(values["videoResolution"], video.resolution),
                areaMode = enumValue(values["videoAreaMode"], video.areaMode)),
            mode = enumValue(values["mode"], result.mode),
            printPlacement = enumValue(values["printPlacement"], result.printPlacement),
            printSize = enumValue(values["printSize"], result.printSize),
            screenDevice = enumValue(values["screenDevice"], result.screenDevice),
            screenCriterion = enumValue(values["screenCriterion"], result.screenCriterion),
            screenPlacement = enumValue(values["screenPlacement"], result.screenPlacement),
            printModelNoticePending = values["printNotice"] == "true",
        )
    }.getOrDefault(SharpnessSettings())

    private fun decodeLegacy(text: String): SharpnessSettings = runCatching {
        val r = text.split("\t")
        fun number(i: Int) = r[i].toDouble().also { require(it.isFinite() && it > 0.0) }
        SharpnessSettings(
            mode = SharpnessCriterionMode.valueOf(r[0]), megapixels = number(1), pixelBlur = number(2),
            printWidthCm = number(3), printHeightCm = number(4), printPpi = number(5),
            printPlacement = PrintPlacement.valueOf(r[7]), customCoCmm = number(8),
            printSize = PrintSizePreset.CUSTOM, printModelNoticePending = true,
            // v1 pixel blur tolerance is NOT a viewing angle. New visual defaults apply.
        )
    }.getOrDefault(SharpnessSettings())

    private inline fun <reified T : Enum<T>> enumValue(raw: String?, fallback: T): T =
        enumValues<T>().firstOrNull { it.name == raw } ?: fallback
}
