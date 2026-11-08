package com.example.dofcalculator

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.text.AnnotatedString
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
class DiffractionUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val vm = DofCalculatorViewModel()
    private fun text(value: String) = compose.onAllNodesWithText(value).onLast()
    private fun visible(value: String) {
        compose.waitUntil(5_000) { text(value).isDisplayed() }
    }
    private fun mount(language: AppLanguage) {
        vm.onFocusAtInfinity()
        vm.onApertureChanged("16")
        compose.setContent {
            AppLocale(language) {
                DOFCalculatorTheme(dynamicColor = false) {
                    DofCalculatorScreen(vm.uiState,
                        onFocalLengthChanged = vm::onFocalLengthChanged,
                        onStepFocalLength = vm::onStepFocalLength,
                        onFocalLengthPresetSelected = vm::onFocalLengthPresetSelected,
                        onOpenDiffractionInfo = vm::onOpenDiffractionInfo,
                        onOpenMagnificationSettings = vm::onOpenMagnificationSettings,
                        onOpenSettings = vm::onOpenSettings,
                        onUseAutoMagnification = vm::onUseAutoMagnification)
                    DiffractionDialogs(vm.uiState, vm::onCloseDiffractionInfo, vm::onCloseMagnificationSettings,
                        vm::onMacroFieldChanged,
                        vm::onManualMagnificationChanged, vm::onApplyMacroSettings,
                        vm::onConfirmMacroSettings, vm::onCancelMacroConfirmation,
                        vm::onRestoreManualMacro, vm::onFocalLengthChanged,
                        vm::onStepFocalLength, vm::onFocalLengthPresetSelected)
                    AppDialogs(vm.uiState, "test", {}, {}, {}, {}, {},
                        vm::onCloseSettings, {}, {},
                        onDiffractionWarningsEnabled = vm::onDiffractionWarningsEnabled,
                        onOpenMagnificationSettings = vm::onOpenMagnificationSettings)
                }
            }
        }
    }

    private fun checkManualFlow(russian: Boolean) {
        fun t(en: String, ru: String) = if (russian) ru else en
        mount(if (russian) AppLanguage.Russian else AppLanguage.English)
        text(t("Diffraction may affect detail", "Дифракция может влиять на детализацию")).performScrollTo().performClick()
        visible(t("Diffraction", "Дифракция"))
        text(t("Refine magnification", "Уточнить масштаб")).assertDoesNotExist()
        text(t("Close", "Закрыть")).performScrollTo().performClick()
        text(t("Settings", "Настройки")).performClick()
        text(t("Refine magnification", "Уточнить масштаб")).performScrollTo().performClick()
        visible(t("Magnification", "Масштаб съёмки"))
        text(t("Auto", "Авто")).assertDoesNotExist()
        text(t("Manual", "Вручную")).assertDoesNotExist()
        text(t("Use Auto", "Использовать Auto")).assertDoesNotExist()
        text(t("Actual Magnification", "Фактический масштаб")).performScrollTo().assertIsDisplayed()
        text("1:1").performScrollTo().performClick()
        text(t("Apply", "Применить")).assertIsNotEnabled()
        text(t("Focus Distance, m", "Дистанция фокусировки, м")).performScrollTo()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("")))
            .performClick().performTextReplacement("0.3")
        compose.runOnIdle {
            assertSame(CalculatedDistance.Infinity, vm.uiState.focusDistance)
            vm.onFocalLengthChanged("100")
        }
        text(t("Thin-Lens Equivalent: 75 mm", "Расчётное фокусное: 75 мм")).performScrollTo().assertIsDisplayed()
        text(t("Apply", "Применить")).assertIsDisplayed().performClick()
        compose.runOnIdle {
            assertEquals(.3, vm.uiState.focusDistanceMeters!!, 0.0)
            assertEquals(32.0, vm.uiState.diffractionResult!!.effectiveAperture, 0.0)
        }
        text(t("Macro 1:1 · Manual", "Макро 1:1 · Вручную")).performScrollTo().performClick()
        text(t("Focus Distance, m", "Дистанция фокусировки, м")).performScrollTo().performTextReplacement("0.03")
        text(t("Check Parameters", "Проверьте параметры")).performScrollTo().assertExists()
        text(t("Apply", "Применить")).performClick()
        visible(t("Confirm Values", "Подтвердить значения"))
        compose.runOnIdle { assertEquals(.3, vm.uiState.focusDistanceMeters!!, 0.0) }
        text(t("Confirm Values", "Подтвердить значения")).performClick()
        compose.runOnIdle {
            assertEquals(.03, vm.uiState.focusDistanceMeters!!, 0.0)
            assertEquals(1.0, vm.uiState.magnificationEstimate.value!!, 0.0)
            assertEquals("100", vm.uiState.focalLengthText)
        }
        text(t("Macro 1:1 · Manual", "Макро 1:1 · Вручную")).performScrollTo().performClick()
        text(t("B (subject)", "B (объект)")).performScrollTo().performTextReplacement("0")
        text(t("Apply", "Применить")).assertIsNotEnabled()
        text(t("Enter a finite positive number. Both decimal separators are accepted.",
            "Введите конечное положительное число. Допускаются точка и запятая.")).assertExists()
        text(t("Cancel", "Отмена")).performClick()
        compose.runOnIdle {
            assertEquals(.03, vm.uiState.focusDistanceMeters!!, 0.0)
            assertEquals(1.0, vm.uiState.magnificationEstimate.value!!, 0.0)
            vm.onFocusAtInfinity()
        }
        text(t("Manual magnification paused", "Ручной масштаб приостановлен")).performScrollTo().performClick()
        text(t("Restore saved pair", "Восстановить сохранённую пару")).performScrollTo().performClick()
        text(t("Focus Distance, m", "Дистанция фокусировки, м")).performScrollTo().assertTextContains("0.03")
        text(t("Cancel", "Отмена")).performClick()
        compose.runOnIdle {
            vm.onUseAutoMagnification()
            assertEquals(MagnificationMode.AUTO, vm.uiState.magnificationEstimate.source)
            assertEquals(0.0, vm.uiState.magnificationEstimate.value!!, 0.0)
            assertEquals(1.0, vm.uiState.diffractionSettings.manualMagnification, 0.0)
        }
    }

    private fun checkSharedMacroFocal(russian: Boolean) {
        fun t(en: String, ru: String) = if (russian) ru else en
        mount(if (russian) AppLanguage.Russian else AppLanguage.English)
        val focal = t("Focal length", "Фокусное")
        val field = t("Focal length, mm", "Фокусное расстояние, мм")
        val macro = t("Magnification", "Масштаб съёмки")
        // Macro off: the existing focal-length dialog remains available.
        text(focal).performClick()
        visible(t("Common focal lengths", "Типовые фокусные"))
        text(field).performTextReplacement("100")
        text(t("Close", "Закрыть")).performClick()
        compose.runOnIdle { vm.onOpenMacroFocus() }
        visible(macro)
        text(field).performScrollTo().assertTextContains("100")
        text("1:1").performScrollTo().performClick()
        text(t("Focus Distance, m", "Дистанция фокусировки, м"))
            .performScrollTo().performTextReplacement("0.3")
        text(t("Apply", "Применить")).performClick()
        compose.runOnIdle {
            assertTrue(vm.uiState.isMacroModeActive)
            assertEquals(ManualMagnificationStatus.VALID, vm.uiState.manualMagnificationAssessment.status)
        }
        text(focal).performScrollTo().performClick()
        visible(macro)
        text(t("Apply", "Применить")).assertIsNotEnabled()
        text(t("Focus Distance, m", "Дистанция фокусировки, м"))
            .performScrollTo()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("")))
            .performTextReplacement("0.3")
        text(field).performScrollTo().assertTextContains("100")
        compose.onNodeWithContentDescription(t("Increase focal length by 1 mm", "Увеличить фокусное расстояние на 1 мм"))
            .performScrollTo().performClick()
        text(field).assertTextContains("101")
        compose.onNodeWithContentDescription(t("Decrease focal length by 1 mm", "Уменьшить фокусное расстояние на 1 мм"))
            .performClick()
        text(field).assertTextContains("100")
        text(t("85 mm", "85 мм")).performScrollTo().performClick()
        text(field).assertTextContains("85")
        compose.runOnIdle {
            assertEquals("85", vm.uiState.focalLengthText)
            assertEquals(.3, vm.uiState.focusDistanceMeters!!, 0.0)
            assertEquals(1.0, vm.uiState.diffractionSettings.manualMagnification, 0.0)
            assertEquals(85.0, vm.uiState.macroForm.validation.preview!!.nominalFocalLengthMm, 0.0)
            assertTrue(vm.uiState.manualNeedsConfirmation)
        }
        // Cancel affects the scale/distance draft; shared focal length stays current.
        text(t("Cancel", "Отмена")).performClick()
        text(t("85 mm", "85 мм")).assertIsDisplayed()
        // A pair awaiting confirmation still routes the focal tile to Macro.
        text(focal).performClick()
        visible(macro)
        text(field).performScrollTo().assertTextContains("85")
        text(field).performTextReplacement("100")
        text(t("Apply", "Применить")).assertIsNotEnabled()
        text(t("Focus Distance, m", "Дистанция фокусировки, м"))
            .performScrollTo().performTextReplacement("0.3")
        text(t("Apply", "Применить")).performClick()
        compose.runOnIdle {
            assertEquals(100.0, vm.uiState.diffractionSettings.manualFocalLengthMm!!, 0.0)
            vm.onUseAutoMagnification()
            vm.onFocusDistanceChanged("0.4") // Automatic 1:1 does not enable manual Macro.
            assertFalse(vm.uiState.isMacroModeActive)
        }
        text(focal).performClick()
        visible(t("Common focal lengths", "Типовые фокусные"))
        text(macro).assertDoesNotExist()
        text(t("Close", "Закрыть")).performClick()
    }

    @Test fun englishSharedMacroFocalAndMainTileRouting() = checkSharedMacroFocal(false)
    @Test fun russianSharedMacroFocalAndMainTileRouting() = checkSharedMacroFocal(true)

    @Test fun englishWarningDetailsAndManualSheet() = checkManualFlow(false)
    @Test fun russianWarningDetailsAndManualSheet() = checkManualFlow(true)

    @Test fun settingsCanDisableWarningAndOpenManualScaleEvenWhenNoWarningIsVisible() {
        mount(AppLanguage.English)
        val optical = vm.uiState.result
        text("Settings").performClick()
        text("Warn about diffraction").performScrollTo().performClick()
        compose.runOnIdle {
            assertFalse(vm.uiState.showDiffractionWarning)
            assertSame(optical, vm.uiState.result)
        }
        text("Refine magnification").performScrollTo().performClick()
        visible("Magnification")
        text("Auto").assertDoesNotExist()
        text("Manual").assertDoesNotExist()
        text("Actual Magnification").assertIsDisplayed()
        text("2:1").performClick()
        text("Focus Distance, m").performScrollTo().performTextReplacement("0.3")
        text("Apply").performClick()
        compose.runOnIdle {
            assertEquals(2.0, vm.uiState.magnificationEstimate.value!!, 0.0)
            assertFalse(vm.uiState.showDiffractionWarning)
            assertEquals(.3, vm.uiState.focusDistanceMeters!!, 0.0)
        }
    }
}
