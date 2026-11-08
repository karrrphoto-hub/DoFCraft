package com.example.dofcalculator.ui.calculator

import com.example.dofcalculator.R
import com.example.dofcalculator.model.FrameFormat

internal fun FrameFormat.labelResource(): Int = when (this) {
    FrameFormat.Nikon1 -> R.string.format_0
    FrameFormat.FourThirds -> R.string.format_1
    FrameFormat.ApsC -> R.string.format_2
    FrameFormat.ApsCCanon -> R.string.format_3
    FrameFormat.ApsCNikonPentaxSony -> R.string.format_4
    FrameFormat.ApsHCanon -> R.string.format_5
    FrameFormat.FullFrame35Mm -> R.string.format_6
    FrameFormat.MediumFormat645 -> R.string.format_7
    FrameFormat.MediumFormat6x6 -> R.string.format_8
    FrameFormat.MediumFormat6x7 -> R.string.format_9
    FrameFormat.MediumFormat6x9 -> R.string.format_10
    FrameFormat.MediumFormat6x12 -> R.string.format_11
    FrameFormat.MediumFormat6x17 -> R.string.format_12
    FrameFormat.LargeFormat4x5 -> R.string.format_13
    FrameFormat.LargeFormat5x7 -> R.string.format_14
    FrameFormat.LargeFormat8x10 -> R.string.format_15
    FrameFormat.DigitalMediumFormat44x33 -> R.string.format_digital_mf_44x33
    FrameFormat.DigitalMediumFormat54x40 -> R.string.format_digital_mf_54x40
    FrameFormat.Custom -> R.string.format_16
}
