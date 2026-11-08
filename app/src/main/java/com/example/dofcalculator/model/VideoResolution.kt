package com.example.dofcalculator.model

/** Output recording sizes; independent of camera model or sensor pixel count. */
enum class VideoResolution(val widthPx: Int?, val heightPx: Int?) {
    HD(1280, 720), FHD(1920, 1080), DCI_2K(2048, 1080), QHD(2560, 1440),
    UHD_4K(3840, 2160), DCI_4K(4096, 2160), UHD_8K(7680, 4320), CUSTOM(null, null),
}
