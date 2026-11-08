package com.example.dofcalculator

import androidx.activity.ComponentActivity
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
class FocusDistanceDialogsTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val vm = DofCalculatorViewModel()
    private fun text(value: String) = compose.onAllNodesWithText(value).onLast()
    private fun visible(value: String) {
        compose.waitUntil(5_000) { text(value).isDisplayed() }
    }

    private fun mount(russian: Boolean) {
        vm.onFocalLengthChanged("100")
        compose.setContent {
            AppLocale(if (russian) AppLanguage.Russian else AppLanguage.English) {
                DOFCalculatorTheme(dynamicColor = false) {
                    DofCalculatorScreen(vm.uiState, onOpenFocusDistance = vm::onOpenFocusDistance,
                        onCalculatorModeSelected = vm::onCalculatorModeSelected,
                        onOpenMagnificationSettings = vm::onOpenMagnificationSettings,
                        onUseAutoMagnification = vm::onUseAutoMagnification)
                    FocusDistanceDialogs(vm.uiState, vm::onCloseFocusDistance,
                        vm::onFocusDistanceInputChanged, vm::onDistanceSliderChanged, vm::onStepFocusDistance)
                    DiffractionDialogs(vm.uiState, vm::onCloseDiffractionInfo, vm::onCloseMagnificationSettings,
                        vm::onMacroFieldChanged,
                        vm::onManualMagnificationChanged, vm::onApplyMacroSettings,
                        vm::onConfirmMacroSettings, vm::onCancelMacroConfirmation,
                        vm::onRestoreManualMacro, vm::onFocalLengthChanged,
                        vm::onStepFocalLength, vm::onFocalLengthPresetSelected)
                }
            }
        }
    }

    private fun flow(russian: Boolean) {
        fun t(en: String, ru: String) = if (russian) ru else en
        mount(russian)
        val slider = t("Focus distance slider", "Слайдер дистанции фокусировки")
        val field = t("Focus distance", "Дистанция фокусировки")
        val hDescription = t("Calculate hyperfocal distance", "Вычислять гиперфокальное расстояние")
        fun h() = compose.onNodeWithContentDescription(hDescription)
        fun macro() = compose.onAllNodes(hasText("Macro") and hasClickAction()).onLast()
        fun noModesInDialog() {
            compose.onAllNodes(hasContentDescription(hDescription) and hasAnyAncestor(isDialog())).assertCountEquals(0)
            compose.onAllNodes(hasText("Macro") and hasClickAction() and hasAnyAncestor(isDialog())).assertCountEquals(0)
        }

        // Both mode buttons are on the calculator, without opening a distance window.
        h().performScrollTo().assertIsNotSelected()
        macro().assertIsNotSelected()
        compose.onNodeWithContentDescription(slider).assertDoesNotExist()
        text(t("Distance", "Дистанция")).performClick()
        visible(field)
        noModesInDialog()
        compose.onNodeWithContentDescription(field).assertIsEnabled()
        compose.onNodeWithContentDescription(slider).assertIsEnabled()
        text(t("Close", "Закрыть")).performClick()

        h().performClick()
        h().assertIsSelected()
        macro().assertIsNotSelected()
        compose.onNodeWithContentDescription(slider).assertDoesNotExist()
        compose.runOnIdle {
            assertEquals(CalculatorMode.Hyperfocal, vm.uiState.calculatorMode)
            assertEquals(vm.uiState.result!!.hyperfocalDistanceMeters, vm.uiState.focusDistanceMeters!!, 0.0)
        }
        text(t("Distance", "Дистанция")).performClick()
        noModesInDialog()
        compose.onNodeWithContentDescription(field).assertIsNotEnabled()
        compose.onNodeWithContentDescription(slider).assertIsNotEnabled()
        text(t("Close", "Закрыть")).performClick()
        h().performClick()
        h().assertIsNotSelected()

        // Cancel Macro from the main card returns straight to the calculator.
        macro().performClick()
        visible(t("Magnification", "Масштаб съёмки"))
        text(t("Cancel", "Отмена")).performClick()
        compose.onNodeWithContentDescription(slider).assertDoesNotExist()
        macro().assertIsNotSelected()
        macro().performClick()
        text("1:1").performScrollTo().performClick()
        text(t("Focus Distance, m", "Дистанция фокусировки, м")).performScrollTo().performTextReplacement("0,3")
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        text(t("Thin-Lens Equivalent: 75 mm", "Расчётное фокусное: 75 мм")).performScrollTo().assertIsDisplayed()
        text(t("Apply", "Применить")).performClick()
        compose.onNodeWithContentDescription(slider).assertDoesNotExist()
        macro().assertIsSelected()
        h().assertIsNotSelected()
        compose.runOnIdle {
            assertEquals(.3, vm.uiState.focusDistanceMeters!!, 0.0)
            assertEquals(ManualMagnificationStatus.VALID, vm.uiState.manualMagnificationAssessment.status)
        }

        // Direct distance changes preserve the saved pair and request confirmation.
        text(t("Distance", "Дистанция")).performClick()
        noModesInDialog()
        compose.onNodeWithContentDescription(field).performTextReplacement("0.6")
        compose.runOnIdle { assertTrue(vm.uiState.manualNeedsConfirmation) }
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        text(t("Close", "Закрыть")).performClick()
        macro().performScrollTo().assertIsSelected()
        macro().performClick()
        macro().assertIsNotSelected()
        compose.runOnIdle {
            assertEquals(MagnificationMode.AUTO, vm.uiState.diffractionSettings.magnificationMode)
            assertEquals(1.0, vm.uiState.diffractionSettings.manualMagnification, 0.0)
        }

        // Applying Macro exits Hyperfocal, leaving exactly one selected main-screen button.
        h().performClick()
        h().assertIsSelected()
        macro().performClick()
        text("1:2").performScrollTo().performClick()
        text(t("Focus Distance, m", "Дистанция фокусировки, м")).performScrollTo().performTextReplacement("0.6")
        text(t("Apply", "Применить")).performClick()
        h().assertIsNotSelected()
        macro().assertIsSelected()
        compose.runOnIdle {
            assertEquals(.5, vm.uiState.magnificationEstimate.value!!, 0.0)
            assertEquals(CalculatorMode.DepthOfField, vm.uiState.calculatorMode)
        }
        macro().performClick()
        macro().assertIsNotSelected()
        text(t("Distance", "Дистанция")).performClick()
        compose.onNodeWithContentDescription(slider).performSemanticsAction(SemanticsActions.SetProgress) { it(1f) }
        compose.runOnIdle {
            assertSame(CalculatedDistance.Infinity, vm.uiState.focusDistance)
            assertEquals(0.0, vm.uiState.magnificationEstimate.value!!, 0.0)
        }
        noModesInDialog()
    }

    @Test fun englishDistanceHyperfocalAndMacroFlow() = flow(false)
    @Test fun russianDistanceHyperfocalAndMacroFlow() = flow(true)
}
