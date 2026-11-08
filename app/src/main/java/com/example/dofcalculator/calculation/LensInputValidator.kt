package com.example.dofcalculator.calculation

import com.example.dofcalculator.model.*

object LensInputValidator {
    fun validate(draft: LensDraft): LensInputValidation {
        val errors = mutableMapOf<LensField, LensInputError>()
        fun read(field: LensField, optional: Boolean = false): Double? {
            val raw = draft.fields[field].orEmpty().trim()
            if (optional && raw.isEmpty()) return null
            val value = raw.replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() && it > 0.0 }
            if (value == null) errors[field] = LensInputError.POSITIVE_NUMBER
            return value
        }
        val prime = draft.type == LensType.PRIME
        val f1 = read(if (prime) LensField.PRIME_FOCAL else LensField.WIDE_FOCAL)
        val a1 = read(if (prime) LensField.PRIME_APERTURE else LensField.WIDE_APERTURE)
        val f2 = if (!prime) read(LensField.TELE_FOCAL) else null
        val a2 = if (!prime) read(LensField.TELE_APERTURE) else null
        val closed = read(LensField.MIN_APERTURE, optional = true)
        if (!prime && f1 != null && f2 != null && f2 <= f1)
            errors[LensField.TELE_FOCAL] = LensInputError.ZOOM_RANGE
        if (closed != null && listOfNotNull(a1, a2).any { closed < it })
            errors[LensField.MIN_APERTURE] = LensInputError.APERTURE_RANGE
        if (errors.isNotEmpty() || draft.id.isBlank()) return LensInputValidation(errors = errors)
        val lens = if (prime) Lens.Prime(draft.id, draft.name.trim(), f1!!, a1!!, closed)
            else Lens.Zoom(draft.id, draft.name.trim(), f1!!, a1!!, f2!!, a2!!, closed)
        return LensInputValidation(lens)
    }
}
