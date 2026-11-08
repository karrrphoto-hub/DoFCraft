package com.example.dofcalculator

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
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

    private fun check(russian: Boolean) {
        fun t(en: String, ru: String) = if (russian) ru else en
        vm.onFocalLengthChanged("100")
        vm.onApertureChanged("16")
        vm.onFocusDistanceChanged("0.3")
        compose.setContent {
            AppLocale(if (russian) AppLanguage.Russian else AppLanguage.English) {
                DOFCalculatorTheme(themeMode = if (russian) ThemeMode.Dark else ThemeMode.Light,
                    dynamicColor = false) {
                    val density = LocalDensity.current
                    CompositionLocalProvider(LocalDensity provides Density(density.density,
                        if (russian) 1.5f else 1f)) {
                        DofCalculatorScreen(vm.uiState,
                            onOpenDiffractionInfo = vm::onOpenDiffractionInfo,
                            onOpenSettings = vm::onOpenSettings)
                        DiffractionDialogs(vm.uiState, vm::onCloseDiffractionInfo)
                        AppDialogs(vm.uiState, "test", {}, {}, {}, {}, {},
                            vm::onCloseSettings, {}, {},
                            onDiffractionWarningsEnabled = vm::onDiffractionWarningsEnabled)
                    }
                }
            }
        }
        text("Macro").assertDoesNotExist()
        compose.onNodeWithTag("macro-warning-card").assertDoesNotExist()
        text(t("Diffraction may affect detail", "Дифракция может влиять на детализацию"))
            .performScrollTo().performClick()
        text(t("Airy disk: 21.472 µm", "Диск Эйри: 21,472 мкм")).performScrollTo().assertIsDisplayed()
        text(t(
            "Diffraction is estimated for conventional photography. At high magnifications, the effective f-number may differ from the set aperture, so the estimate may be inaccurate.",
            "Оценка дифракции рассчитана для обычной съёмки. При значительных масштабах изображения эффективное диафрагменное число может отличаться от установленного, поэтому оценка может быть неточной"))
            .performScrollTo().assertIsDisplayed()
        text(t("Close", "Закрыть")).performScrollTo().performClick()
        text(t("Settings", "Настройки")).performClick()
        text(t("Refine magnification", "Уточнить масштаб")).assertDoesNotExist()
        text(t("Macro Photography Warning", "Предупреждать о макросъёмке")).assertDoesNotExist()
        text(t("Warn about diffraction", "Предупреждать о дифракции")).performScrollTo().performClick()
        compose.runOnIdle {
            assertFalse(vm.uiState.showDiffractionWarning)
            assertEquals(.021472, vm.uiState.diffractionResult!!.airyDiameterMm, 1e-15)
            assertEquals(.3, vm.uiState.focusDistanceMeters!!, 0.0)
        }
        text(t("Close", "Закрыть")).performClick()
        text(t("Diffraction may affect detail", "Дифракция может влиять на детализацию")).assertDoesNotExist()
        compose.onNodeWithTag("macro-warning-card").assertDoesNotExist()
    }

    @Test fun englishConventionalDiffractionAndSettings() = check(false)
    @Test fun russianDarkLargeFontConventionalDiffractionAndSettings() = check(true)
}
