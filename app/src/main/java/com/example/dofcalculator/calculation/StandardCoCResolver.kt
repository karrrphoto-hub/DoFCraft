package com.example.dofcalculator.calculation

import com.example.dofcalculator.model.CameraProfile
import com.example.dofcalculator.model.StandardCoCResult
import com.example.dofcalculator.model.StandardCoCSource
import kotlin.math.hypot

/** Traditional photographic CoC. Catalogue identity comes from the profile's format link. */
object StandardCoCResolver {
    const val REFERENCE_WIDTH_MM = 36.0
    const val REFERENCE_HEIGHT_MM = 24.0
    const val REFERENCE_COC_MM = 0.030
    private val referenceDiagonalMm = hypot(REFERENCE_WIDTH_MM, REFERENCE_HEIGHT_MM)

    fun resolve(camera: CameraProfile): StandardCoCResult =
        resolve(camera.widthMm, camera.heightMm, camera.standardCoCmm)

    /**
     * [catalogCoCmm] is the linked catalogue recommendation, never an archived user override.
     * Null means absent; a present but invalid recommendation is an error.
     * Geometry is validated even when the catalogue supplies CoC.
     */
    fun resolve(widthMm: Double?, heightMm: Double?, catalogCoCmm: Double?): StandardCoCResult {
        val width = positive(widthMm, "Frame width")
        val height = positive(heightMm, "Frame height")
        val diagonal = positive(hypot(width, height), "Frame diagonal")
        return if (catalogCoCmm != null) {
            StandardCoCResult(positive(catalogCoCmm, "Catalogue CoC"), StandardCoCSource.CATALOG)
        } else {
            StandardCoCResult(scaledDiagonalCoC(diagonal), StandardCoCSource.DIAGONAL_FALLBACK)
        }
    }

    private fun scaledDiagonalCoC(diagonalMm: Double): Double =
        positive(REFERENCE_COC_MM * diagonalMm / referenceDiagonalMm, "Calculated CoC")

    private fun positive(value: Double?, name: String): Double {
        require(value != null && value.isFinite() && value > 0.0) {
            "$name must be finite and positive"
        }
        return value
    }
}
