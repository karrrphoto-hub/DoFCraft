package com.example.dofcalculator

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.dofcalculator.model.*
import com.example.dofcalculator.settings.*
import com.example.dofcalculator.ui.AppLocale
import com.example.dofcalculator.ui.calculator.*
import com.example.dofcalculator.ui.theme.DOFCalculatorTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CloseFocusWarningUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val vm = DofCalculatorViewModel()
    private fun text(value: String) = compose.onAllNodesWithText(value).onLast()
    private fun card() = compose.onNodeWithTag("close-focus-warning")

    private fun flow(russian: Boolean) {
        fun t(en: String, ru: String) = if (russian) ru else en
        vm.onFocalLengthChanged("100")
        vm.onFocusDistanceChanged("1")
        vm.onApertureChanged("32")
        compose.setContent {
            AppLocale(if (russian) AppLanguage.Russian else AppLanguage.English) {
                DOFCalculatorTheme(themeMode = if (russian) ThemeMode.Dark else ThemeMode.Light,
                    dynamicColor = false) {
                    val density = LocalDensity.current
                    CompositionLocalProvider(LocalDensity provides Density(density.density,
                        if (russian) 1.5f else 1f)) {
                        DofCalculatorScreen(vm.uiState,
                            onOpenFocusDistance = vm::onOpenFocusDistance,
                            onFocalLengthChanged = vm::onFocalLengthChanged,
                            onCalculatorModeSelected = vm::onCalculatorModeSelected,
                            onOpenSettings = vm::onOpenSettings)
                        FocusDistanceDialogs(vm.uiState, vm::onCloseFocusDistance,
                            vm::onFocusDistanceInputChanged, vm::onDistanceSliderChanged, vm::onStepFocusDistance,
                            vm::onConfirmManualFocusDistance)
                        AppDialogs(vm.uiState, "test", {}, {}, {}, {}, {}, vm::onCloseSettings, {}, {},
                            onDiffractionWarningsEnabled = vm::onDiffractionWarningsEnabled,
                            onCloseFocusWarningsEnabled = vm::onCloseFocusWarningsEnabled)
                    }
                }
            }
        }
        fun setDistance(value: String) {
            text(t("Distance", "Дистанция")).performScrollTo().performClick()
            compose.onNodeWithContentDescription(t("Focus distance", "Дистанция фокусировки"))
                .performTextReplacement(if (russian) value.replace('.', ',') else value)
            androidx.test.espresso.Espresso.closeSoftKeyboard()
            text(t("Close", "Закрыть")).performClick()
        }
        fun assertNoMacroControls() {
            text("Macro").assertDoesNotExist()
            text(t("Refine magnification", "Уточнить масштаб")).assertDoesNotExist()
            text(t("Actual Magnification", "Фактический масштаб")).assertDoesNotExist()
            compose.onNodeWithTag("macro-warning-card").assertDoesNotExist()
        }

        card().assertDoesNotExist()
        for ((distance, ratio) in listOf("0.72" to "5", "0.6" to t("3.7", "3,7"), "0.5" to t("2.6", "2,6"), "0.4" to "1")) {
            setDistance(distance)
            card().performScrollTo().assertIsDisplayed()
            text(t("Estimated Magnification: ≈1:$ratio", "Расчётный масштаб: ≈1:$ratio")).assertExists()
            compose.onAllNodesWithTag("close-focus-warning").assertCountEquals(1)
            compose.runOnIdle {
                assertEquals(distance.toDouble(), vm.uiState.focusDistanceMeters!!, 1e-15)
                assertNotNull(vm.uiState.result)
                assertEquals(.042944, vm.uiState.diffractionResult!!.airyDiameterMm, 1e-15)
            }
        }
        setDistance("0.3")
        card().performScrollTo().assertIsDisplayed()
        text(t("Very Close Focus Distance", "Очень близкая дистанция фокусировки")).assertExists()
        compose.onAllNodes(hasText("Estimated Magnification:", substring = true) or
            hasText("Расчётный масштаб:", substring = true)).assertCountEquals(0)
        compose.runOnIdle { assertNotNull(vm.uiState.result) }
        assertNoMacroControls()
        setDistance("1")
        card().assertDoesNotExist()
        setDistance("0.6")
        val beforeHelp = vm.uiState
        text(t("Learn More", "Подробнее")).performScrollTo().performClick()
        text(t("Estimated Using a Thin-Lens Model", "Оценка выполнена по модели тонкой линзы"))
            .performScrollTo().assertIsDisplayed()
        compose.runOnIdle {
            assertSame(beforeHelp.result, vm.uiState.result)
            assertEquals(beforeHelp.focusDistance, vm.uiState.focusDistance)
            assertFalse(vm.uiState.isSettingsVisible)
            assertFalse(vm.uiState.isDiffractionInfoVisible)
        }
        text(t("Hide Details", "Скрыть подробности")).performScrollTo().performClick()
        assertNoMacroControls()

        // The focal tile still opens the ordinary focal editor.
        text(t("Focal length", "Фокусное")).performScrollTo().performClick()
        text(t("Focal length, mm", "Фокусное расстояние, мм")).performTextReplacement("50")
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        text(t("Close", "Закрыть")).performClick()
        card().assertDoesNotExist()
        text(t("Focal length", "Фокусное")).performScrollTo().performClick()
        text(t("Focal length, mm", "Фокусное расстояние, мм")).performTextReplacement("100")
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        text(t("Close", "Закрыть")).performClick()
        card().assertExists()

        val hyperfocal = t("Calculate hyperfocal distance", "Вычислять гиперфокальное расстояние")
        compose.onNodeWithContentDescription(hyperfocal).performScrollTo().performClick()
        card().assertDoesNotExist()
        compose.onNodeWithContentDescription(hyperfocal).performClick()
        card().assertExists()

        text(t("Distance", "Дистанция")).performScrollTo().performClick()
        val slider = compose.onNodeWithContentDescription(t("Focus distance slider", "Слайдер дистанции фокусировки"))
        slider.performSemanticsAction(SemanticsActions.SetProgress) { it(1f) }
        compose.runOnIdle { assertSame(CalculatedDistance.Infinity, vm.uiState.focusDistance) }
        text(t("Close", "Закрыть")).performClick()
        card().assertDoesNotExist()
        setDistance("0.6")

        text(t("Distance", "Дистанция")).performScrollTo().performClick()
        repeat(6) { index ->
            val distance = if (index % 2 == 0) 1.0 else .6
            val position = vm.uiState.distanceScale!!.positionOf(CalculatedDistance.Finite(distance)).toFloat()
            slider.performSemanticsAction(SemanticsActions.SetProgress) { it(position) }
        }
        text(t("Close", "Закрыть")).performClick()
        compose.onAllNodesWithTag("close-focus-warning").assertCountEquals(1)

        val optical = vm.uiState.result
        text(t("Settings", "Настройки")).performClick()
        text(t("Close Focus Warning", "Предупреждать о близкой дистанции съёмки"))
            .performScrollTo().performClick()
        text(t("Close", "Закрыть")).performClick()
        card().assertDoesNotExist()
        compose.runOnIdle {
            assertFalse(vm.uiState.closeFocusWarningsEnabled)
            assertTrue(vm.uiState.showDiffractionWarning)
            assertSame(optical, vm.uiState.result)
        }
        text(t("Settings", "Настройки")).performClick()
        text(t("Close Focus Warning", "Предупреждать о близкой дистанции съёмки"))
            .performScrollTo().performClick()
        text(t("Warn about diffraction", "Предупреждать о дифракции"))
            .performScrollTo().performClick()
        text(t("Close", "Закрыть")).performClick()
        card().performScrollTo().assertIsDisplayed()
        compose.runOnIdle {
            assertTrue(vm.uiState.showCloseFocusWarning)
            assertFalse(vm.uiState.showDiffractionWarning)
            assertEquals(.042944, vm.uiState.diffractionResult!!.airyDiameterMm, 1e-15)
        }
        setDistance("")
        card().assertDoesNotExist()
        assertNoMacroControls()
    }

    @Test fun diffractionAndCloseFocusCardsCoexistBeforeResults() {
        vm.onFocalLengthChanged("100")
        vm.onFocusDistanceChanged("0.6")
        vm.onApertureChanged("32")
        compose.setContent {
            AppLocale(AppLanguage.English) {
                DOFCalculatorTheme(dynamicColor = false) {
                    DofCalculatorScreen(vm.uiState)
                }
            }
        }
        fun assertBothCards() {
            compose.onAllNodesWithTag("diffraction-warning").assertCountEquals(1)
            compose.onAllNodesWithTag("close-focus-warning").assertCountEquals(1)
            compose.onNodeWithTag("diffraction-warning").performScrollTo().assertIsDisplayed()
            card().performScrollTo().assertIsDisplayed()
            text("Total depth of field").assertExists()
        }
        assertBothCards()
        compose.runOnIdle { vm.onFocusDistanceChanged("0.3") }
        assertBothCards()
        text("Very Close Focus Distance").assertExists()
        compose.runOnIdle { vm.onFocusDistanceChanged("2") }
        card().assertDoesNotExist()
        compose.onNodeWithTag("diffraction-warning").performScrollTo().assertIsDisplayed()
    }

    @Test fun englishCloseFocusInformationWithoutMacroMode() = flow(false)
    @Test fun russianDarkLargeFontCloseFocusInformationWithoutMacroMode() = flow(true)
}
