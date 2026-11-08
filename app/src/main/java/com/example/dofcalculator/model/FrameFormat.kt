package com.example.dofcalculator.model

/** Photographic recommendations, not absolute physical sharpness limits. */
enum class FrameFormat(
    val suggestedCircleOfConfusionMm: Double?,
    val widthMm: Double? = null,
    val heightMm: Double? = null,
) {
    Nikon1(0.011, 13.2, 8.8),
    FourThirds(0.015, 18.0, 13.5),
    ApsC(0.018, 22.5, 15.0),
    ApsCCanon(0.018, 22.2, 14.8),
    ApsCNikonPentaxSony(0.019, 23.6, 15.7),
    ApsHCanon(0.023, 28.7, 19.0),
    FullFrame35Mm(0.030, 36.0, 24.0),
    MediumFormat645(0.047, 56.0, 42.0),
    MediumFormat6x6(0.053, 56.0, 56.0),
    MediumFormat6x7(0.059, 56.0, 69.0),
    MediumFormat6x9(0.067, 56.0, 84.0),
    MediumFormat6x12(0.083, 56.0, 112.0),
    MediumFormat6x17(0.120, 56.0, 168.0),
    LargeFormat4x5(0.110, 102.0, 127.0),
    LargeFormat5x7(0.150, 127.0, 178.0),
    LargeFormat8x10(0.220, 203.0, 254.0),
    DigitalMediumFormat44x33(0.0365, 43.8, 32.9),
    DigitalMediumFormat54x40(0.0445, 53.4, 40.0),
    Custom(null),
}
