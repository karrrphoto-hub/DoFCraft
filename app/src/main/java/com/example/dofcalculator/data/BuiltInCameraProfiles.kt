package com.example.dofcalculator.data

import com.example.dofcalculator.model.CameraProfile
import com.example.dofcalculator.model.FrameFormat

object BuiltInCameraProfiles {
    const val DEFAULT_ID = "builtin.full-frame-35mm"
    val cameras: List<CameraProfile> = listOf(
        Triple("nikon-1", "Nikon 1 series", FrameFormat.Nikon1),
        Triple("four-thirds", "Four Thirds System", FrameFormat.FourThirds),
        Triple("aps-c", "APS-C", FrameFormat.ApsC),
        Triple("aps-c-canon", "APS-C Canon", FrameFormat.ApsCCanon),
        Triple("aps-c-nikon-pentax-sony", "APS-C Nikon/Pentax/Sony", FrameFormat.ApsCNikonPentaxSony),
        Triple("aps-h-canon", "APS-H Canon", FrameFormat.ApsHCanon),
        Triple("full-frame-35mm", "Full Frame / 35mm", FrameFormat.FullFrame35Mm),
        Triple("645", "Medium Format 645 (6×4.5)", FrameFormat.MediumFormat645),
        Triple("6x6", "Medium Format 6×6", FrameFormat.MediumFormat6x6),
        Triple("6x7", "Medium Format 6×7", FrameFormat.MediumFormat6x7),
        Triple("6x9", "Medium Format 6×9", FrameFormat.MediumFormat6x9),
        Triple("6x12", "Medium Format 6×12", FrameFormat.MediumFormat6x12),
        Triple("6x17", "Medium Format 6×17", FrameFormat.MediumFormat6x17),
        Triple("4x5", "Large Format 4×5", FrameFormat.LargeFormat4x5),
        Triple("5x7", "Large Format 5×7", FrameFormat.LargeFormat5x7),
        Triple("8x10", "Large Format 8×10", FrameFormat.LargeFormat8x10),
    ).map { (id, name, format) ->
        CameraProfile("builtin.$id", name, format, isBuiltIn = true)
    } + listOf(
        CameraProfile("fujifilm_gfx", "Fujifilm GFX",
            FrameFormat.DigitalMediumFormat44x33, isBuiltIn = true),
        CameraProfile("hasselblad_x", "Hasselblad X / 907X",
            FrameFormat.DigitalMediumFormat44x33, isBuiltIn = true),
        CameraProfile("hasselblad_h6d_100c", "Hasselblad H6D-100c",
            FrameFormat.DigitalMediumFormat54x40, isBuiltIn = true),
    )

    /** New named camera IDs need the same protection as the original builtin.* IDs. */
    internal fun isReservedId(id: String): Boolean =
        id.startsWith("builtin.") || cameras.any { it.id == id }
}
