package com.example.dofcalculator.settings

/** Conventional displayed f-numbers; the chosen value is used directly by the optical model. */
enum class ApertureIncrements(val apertures: List<Double>) {
    FullStop(listOf(1.0, 1.4, 2.0, 2.8, 4.0, 5.6, 8.0, 11.0, 16.0, 22.0, 32.0, 45.0)),
    HalfStop(listOf(1.0, 1.2, 1.4, 1.7, 2.0, 2.4, 2.8, 3.3, 4.0, 4.8, 5.6, 6.7,
        8.0, 9.5, 11.0, 13.0, 16.0, 19.0, 22.0, 27.0, 32.0, 38.0, 45.0)),
    ThirdStop(listOf(1.0, 1.1, 1.2, 1.4, 1.6, 1.8, 2.0, 2.2, 2.5, 2.8, 3.2, 3.5,
        4.0, 4.5, 5.0, 5.6, 6.3, 7.1, 8.0, 9.0, 10.0, 11.0, 13.0, 14.0,
        16.0, 18.0, 20.0, 22.0, 25.0, 29.0, 32.0, 36.0, 40.0, 45.0)),
}
