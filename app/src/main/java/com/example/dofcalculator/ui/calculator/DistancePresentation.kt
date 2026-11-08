package com.example.dofcalculator.ui.calculator

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import com.example.dofcalculator.R
import com.example.dofcalculator.settings.*

internal val LocalDistanceDisplay = staticCompositionLocalOf { DistanceDisplay() }

@Composable
internal fun currentDistanceDisplay(): DistanceDisplay =
    LocalDistanceDisplay.current.copy(locale = LocalResources.current.configuration.locales[0])

@Composable
internal fun distanceUnitLabel(unit: DistanceUnit = LocalDistanceDisplay.current.unit): String =
    stringResource(when (unit) {
        DistanceUnit.Centimeters -> R.string.unit_cm
        DistanceUnit.Meters -> R.string.meters_unit
        DistanceUnit.Inches -> R.string.unit_inch
        DistanceUnit.Feet -> R.string.unit_feet
    })
