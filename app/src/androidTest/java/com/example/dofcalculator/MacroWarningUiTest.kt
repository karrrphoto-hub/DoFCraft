package com.example.dofcalculator

import androidx.activity.ComponentActivity
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
class MacroWarningUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val vm = DofCalculatorViewModel()

    private fun text(value: String) = compose.onAllNodesWithText(value).onLast()
    private fun mount(russian: Boolean) {
        vm.onFocalLengthChanged("100")
        vm.onFocusDistanceChanged("1")
        compose.setContent {
            AppLocale(if (russian) AppLanguage.Russian else AppLanguage.English) {
                DOFCalculatorTheme(themeMode = if (russian) ThemeMode.Dark else ThemeMode.Light,
                    dynamicColor = false) {
                    DofCalculatorScreen(vm.uiState,
                        onOpenFocusDistance = vm::onOpenFocusDistance,
                        onOpenSettings = vm::onOpenSettings,
                        onOpenMagnificationSettings = vm::onOpenMagnificationSettings,
                        onUseAutoMagnification = vm::onUseAutoMagnification)
                    FocusDistanceDialogs(vm.uiState, vm::onCloseFocusDistance,
                        vm::onFocusDistanceInputChanged, vm::onDistanceSliderChanged, vm::onStepFocusDistance)
                    DiffractionDialogs(vm.uiState, vm::onCloseDiffractionInfo, vm::onCloseMagnificationSettings,
                        vm::onMacroFieldChanged, vm::onManualMagnificationChanged, vm::onApplyMacroSettings,
                        vm::onConfirmMacroSettings, vm::onCancelMacroConfirmation, vm::onRestoreManualMacro,
                        vm::onFocalLengthChanged, vm::onStepFocalLength, vm::onFocalLengthPresetSelected)
                    AppDialogs(vm.uiState, "test", {}, {}, {}, {}, {}, vm::onCloseSettings, {}, {},
                        onDiffractionWarningsEnabled = vm::onDiffractionWarningsEnabled,
                        onMacroWarningsEnabled = vm::onMacroWarningsEnabled,
                        onOpenMagnificationSettings = vm::onOpenMagnificationSettings)
                }
            }
        }
    }

    private fun check(russian: Boolean) {
        fun t(en: String, ru: String) = if (russian) ru else en
        fun setDistance(value: String) {
            text(t("Distance", "Дистанция")).performScrollTo().performClick()
            compose.onNodeWithContentDescription(t("Focus distance", "Дистанция фокусировки"))
                .performTextReplacement(value)
            text(t("Close", "Закрыть")).performClick()
        }
        fun card() = compose.onNodeWithTag("macro-warning-card")
        mount(russian)
        card().assertDoesNotExist()
        setDistance(if (russian) "0,72" else "0.72")
        card().performScrollTo().assertIsDisplayed()
        text(t("Estimated Magnification: ≈1:5", "Расчётный масштаб: ≈1:5")).assertExists()
        for ((distance, ratio) in listOf("0.6" to t("1:3.7", "1:3,7"), "0.5" to t("1:2.6", "1:2,6"))) {
            setDistance(if (russian) distance.replace('.', ',') else distance)
            card().performScrollTo().assertIsDisplayed()
            text(t("Estimated Magnification: ≈$ratio", "Расчётный масштаб: ≈$ratio")).assertExists()
            compose.onAllNodesWithTag("macro-warning-card").assertCountEquals(1)
        }
        setDistance("1")
        card().assertDoesNotExist()
        setDistance("0.6")
        text(t("Why approximate?", "Подробнее")).performScrollTo().performClick()
        text(t(
            "Macro settings refine the actual magnification and diffraction estimate. DOF remains approximate; opening these settings does not switch to a separate macro DOF model.",
            "В настройках «Макро» можно уточнить фактический масштаб и оценку дифракции. Расчёт ГРИП пока остаётся ориентировочным; отдельная модель макро-ГРИП при открытии настроек не включается."))
            .performScrollTo().assertIsDisplayed()
        val before = vm.uiState
        text(t("Refine Magnification in Macro", "Уточнить масштаб в Макро")).performScrollTo().performClick()
        text(t("Actual Magnification", "Фактический масштаб")).performScrollTo().assertIsDisplayed()
        compose.runOnIdle {
            assertEquals(before.result, vm.uiState.result)
            assertEquals(before.focusDistance, vm.uiState.focusDistance)
            assertEquals(MagnificationMode.AUTO, vm.uiState.diffractionSettings.magnificationMode)
            assertEquals("", vm.uiState.macroForm.distanceMeters)
        }
        text(t("Cancel", "Отмена")).performClick()
        setDistance("0.3")
        card().performScrollTo().assertIsDisplayed()
        text(t("Very Close Focus Distance", "Очень близкая дистанция фокусировки")).assertExists()
        compose.onAllNodes(hasText("Estimated Magnification:", substring = true) or
            hasText("Расчётный масштаб:", substring = true)).assertCountEquals(0)
        text(t("Refine Magnification", "Уточнить масштаб")).performScrollTo().performClick()
        text(t("Actual Magnification", "Фактический масштаб")).performScrollTo().assertIsDisplayed()
        text(t("Cancel", "Отмена")).performClick()
        compose.runOnIdle { vm.onFocusAtInfinity() }
        card().assertDoesNotExist()
        compose.runOnIdle {
            vm.onFocusDistanceChanged("0.6")
            vm.onApertureChanged("32")
        }
        card().performScrollTo().assertIsDisplayed()
        text(t("Diffraction in close-up photography", "Дифракция при макросъёмке")).assertExists()
        text(t("Settings", "Настройки")).performClick()
        text(t("Macro Photography Warning", "Предупреждать о макросъёмке")).performScrollTo().performClick()
        text(t("Close", "Закрыть")).performClick()
        card().assertDoesNotExist()
        compose.runOnIdle {
            assertFalse(vm.uiState.macroWarningsEnabled)
            assertTrue(vm.uiState.showDiffractionWarning)
            assertNotNull(vm.uiState.result)
        }
        text(t("Settings", "Настройки")).performClick()
        text(t("Macro Photography Warning", "Предупреждать о макросъёмке")).performScrollTo().performClick()
        text(t("Close", "Закрыть")).performClick()
        card().performScrollTo().assertIsDisplayed()
        setDistance("")
        card().assertDoesNotExist()
        compose.runOnIdle { assertNull(vm.uiState.result) }
    }

    @Test fun englishAutomaticMacroWarning() = check(false)
    @Test fun russianAutomaticMacroWarningInDarkTheme() = check(true)
}
