package com.example.dofcalculator

import androidx.activity.ComponentActivity
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.dofcalculator.calculation.DepthOfFieldCalculator
import com.example.dofcalculator.model.*
import com.example.dofcalculator.settings.*
import com.example.dofcalculator.ui.AppLocale
import com.example.dofcalculator.ui.calculator.*
import com.example.dofcalculator.ui.theme.DOFCalculatorTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class HyperfocalLensUnitsUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private fun check(russian: Boolean) {
        val locale = if (russian) Locale.forLanguageTag("ru") else Locale.ENGLISH
        val meterLabel = if (russian) "м" else "m"
        val result = DepthOfFieldCalculator.calculate(DepthOfFieldInput(58.0, 11.0, .015, 3.818))
        var mode by mutableStateOf(CalculatorMode.DepthOfField)
        var lensUnit by mutableStateOf<LensDistanceUnit?>(LensDistanceUnit.Feet)
        var appUnit by mutableStateOf(DistanceUnit.Meters)
        fun expected(): String {
            val primary = DistanceDisplay(appUnit, DistanceRounding.Three, locale).number(result.hyperfocalDistanceMeters)
            if (lensUnit == null || lensUnit!!.distanceUnit() == appUnit)
                return primary + " " + if (appUnit == DistanceUnit.Meters) meterLabel else if (russian) "фут" else "feet"
            val secondary = lensDistanceReading(result.hyperfocalDistanceMeters, lensUnit!!, appUnit, locale)!!.value
            return if (appUnit == DistanceUnit.Meters) "$primary $meterLabel ($secondary ft)"
                else "$primary " + (if (russian) "фут" else "feet") + " ($secondary $meterLabel)"
        }
        compose.setContent {
            AppLocale(if (russian) AppLanguage.Russian else AppLanguage.English) {
                DOFCalculatorTheme(dynamicColor = false) {
                    CompositionLocalProvider(LocalDistanceDisplay provides
                        DistanceDisplay(appUnit, DistanceRounding.Three, locale)) {
                        ResultsInfographic(result, 3.818, null, mode, false, lensUnit)
                    }
                }
            }
        }
        val feetHelp = if (russian) "У выбранного объектива шкала дистанции в футах."
            else "The selected lens has a distance scale in feet."
        val metersHelp = if (russian) "У выбранного объектива шкала дистанции в метрах."
            else "The selected lens has a distance scale in meters."
        compose.onNodeWithText(feetHelp, substring = true).assertIsDisplayed()
        // Same order in the DOF footer and primary Hyperfocal result.
        compose.onNodeWithText(expected()).assertIsDisplayed()
        compose.runOnIdle { mode = CalculatorMode.Hyperfocal }
        compose.onNodeWithText(expected()).assertIsDisplayed()
        compose.onNodeWithText(feetHelp, substring = true).assertIsDisplayed()
        compose.runOnIdle { lensUnit = null } // Ignore/no explicit lens override
        compose.onNodeWithText(expected()).assertIsDisplayed()
        compose.onNodeWithText(feetHelp, substring = true).assertDoesNotExist()
        compose.runOnIdle { lensUnit = LensDistanceUnit.Meters }
        compose.onNodeWithText(expected()).assertIsDisplayed() // matching units are not duplicated
        compose.onNodeWithText(metersHelp, substring = true).assertDoesNotExist()
        compose.runOnIdle { appUnit = DistanceUnit.Feet }
        compose.onNodeWithText(expected()).assertIsDisplayed() // reverse conversion
        compose.onNodeWithText(metersHelp, substring = true).assertIsDisplayed()
    }

    @Test fun englishHyperfocalUnitsInBothModes() = check(false)
    @Test fun russianHyperfocalUnitsInBothModes() = check(true)
}
