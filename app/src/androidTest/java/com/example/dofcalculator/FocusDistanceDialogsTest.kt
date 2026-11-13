package com.example.dofcalculator

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.dofcalculator.model.CalculatedDistance
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

    private fun flow(russian: Boolean) {
        fun t(en: String, ru: String) = if (russian) ru else en
        vm.onFocalLengthChanged("100")
        compose.setContent {
            AppLocale(if (russian) AppLanguage.Russian else AppLanguage.English) {
                DOFCalculatorTheme(dynamicColor = false) {
                    DofCalculatorScreen(vm.uiState, onOpenFocusDistance = vm::onOpenFocusDistance,
                        onCalculatorModeSelected = vm::onCalculatorModeSelected)
                    FocusDistanceDialogs(vm.uiState, vm::onCloseFocusDistance,
                        vm::onFocusDistanceInputChanged, vm::onDistanceSliderChanged, vm::onStepFocusDistance,
                        vm::onConfirmManualFocusDistance)
                }
            }
        }
        val slider = t("Focus distance slider", "Слайдер дистанции фокусировки")
        val field = t("Focus distance", "Дистанция фокусировки")
        val hDescription = t("Calculate hyperfocal distance", "Вычислять гиперфокальное расстояние")
        fun h() = compose.onNodeWithContentDescription(hDescription)
        fun noModesInDialog() {
            compose.onAllNodes(hasContentDescription(hDescription) and hasAnyAncestor(isDialog())).assertCountEquals(0)
            text("Macro").assertDoesNotExist()
            compose.onNodeWithTag("macro-warning-card").assertDoesNotExist()
        }

        h().performScrollTo().assertIsNotSelected()
        text("Macro").assertDoesNotExist()
        text(t("Distance", "Дистанция")).performClick()
        noModesInDialog()
        compose.onNodeWithContentDescription(field).assertIsEnabled()
        compose.onNodeWithContentDescription(slider).assertIsEnabled()
        text(t("Close", "Закрыть")).performClick()

        h().performClick()
        h().assertIsSelected()
        compose.runOnIdle {
            assertEquals(CalculatorMode.Hyperfocal, vm.uiState.calculatorMode)
            assertEquals(vm.uiState.result!!.hyperfocalDistanceMeters, vm.uiState.focusDistanceMeters!!, 0.0)
        }
        val question = t(
            "Exit hyperfocal mode and set the focus distance manually?",
            "Выйти из режима гиперфокала и задать дистанцию вручную?",
        )
        text(t("Distance", "Дистанция")).performClick()
        text(question).assertIsDisplayed()
        compose.onNodeWithContentDescription(field).assertDoesNotExist()
        compose.onNodeWithContentDescription(slider).assertDoesNotExist()
        text(t("No", "Нет")).performClick()
        text(question).assertDoesNotExist()
        h().assertIsSelected()
        compose.runOnIdle { assertEquals(CalculatorMode.Hyperfocal, vm.uiState.calculatorMode) }

        text(t("Distance", "Дистанция")).performClick()
        text(question).assertIsDisplayed()
        androidx.test.espresso.Espresso.pressBack()
        text(question).assertDoesNotExist()
        h().assertIsSelected()

        text(t("Distance", "Дистанция")).performClick()
        text(t("Yes", "Да")).performClick()
        text(question).assertDoesNotExist()
        noModesInDialog()
        compose.onNodeWithContentDescription(field).assertIsEnabled()
        compose.onNodeWithContentDescription(slider).assertIsEnabled()
        compose.runOnIdle {
            assertEquals(CalculatorMode.DepthOfField, vm.uiState.calculatorMode)
            assertEquals(5.0, vm.uiState.focusDistanceMeters!!, 0.0)
        }
        text(t("Close", "Закрыть")).performClick()
        h().assertIsNotSelected()

        text(t("Distance", "Дистанция")).performClick()
        compose.onNodeWithContentDescription(field).performTextReplacement("0,3")
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        compose.runOnIdle {
            assertEquals(.3, vm.uiState.focusDistanceMeters!!, 0.0)
            assertNotNull(vm.uiState.result)
        }
        noModesInDialog()
        compose.onNodeWithContentDescription(slider).performSemanticsAction(SemanticsActions.SetProgress) { it(1f) }
        compose.runOnIdle { assertSame(CalculatedDistance.Infinity, vm.uiState.focusDistance) }
        text(t("Close", "Закрыть")).performClick()
        text("Macro").assertDoesNotExist()
    }

    @Test fun englishDistanceHyperfocalAndInfinityFlow() = flow(false)
    @Test fun russianDistanceHyperfocalAndInfinityFlow() = flow(true)
}
