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
        vm.onApertureChanged("22")
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
        compose.onNodeWithTag("diffraction-warning").assertDoesNotExist()
        compose.runOnIdle { vm.onApertureChanged("32") }
        val possibleTitle = t("Possible Diffraction Impact", "Возможно влияние дифракции")
        text(possibleTitle).performScrollTo().assertIsDisplayed()
        text(t(
            "Fine details may become less sharp. This is the effect of diffraction: it becomes more noticeable as you stop down the aperture. A smaller aperture can still improve the overall impression of sharpness: greater depth of field may make the foreground and background look clearer.",
            "Мелкие детали могут стать менее чёткими. Это влияние дифракции: оно тем заметнее, чем сильнее закрыта диафрагма. При этом закрытая диафрагма по-прежнему может повысить общую визуальную резкость изображения: передний план и фон могут стать чётче благодаря увеличению глубины резкости.")).assertExists()
        compose.runOnIdle { vm.onApertureChanged("45") }
        text(possibleTitle).assertDoesNotExist()
        text(t("Noticeable Diffraction Impact", "Заметное влияние дифракции"))
            .performScrollTo().assertIsDisplayed()
        compose.onAllNodesWithTag("diffraction-warning").assertCountEquals(1)
        text(t(
            "Fine details may become noticeably less sharp because of diffraction. Stopping down further will strengthen this effect while also increasing depth of field. A clearer foreground and background may matter more than preserving the finest details.",
            "Мелкие детали могут заметно терять чёткость из-за дифракции. Дальнейшее закрытие диафрагмы усилит этот эффект, но одновременно увеличит глубину резкости. Более чёткие передний план и фон могут быть важнее сохранения самых мелких деталей.")).assertExists()
        text(t("Noticeable Diffraction Impact", "Заметное влияние дифракции")).performClick()
        text(t("Airy disk: 60.39 µm", "Диск Эйри: 60,39 мкм")).performScrollTo().assertIsDisplayed()
        text(t(
            "At high magnifications, this estimate may be less accurate. In these conditions, the lens may behave as though the aperture were stopped down further. The app uses the f-number you entered and does not adjust it for magnification.",
            "При съёмке с большим увеличением эта оценка может быть менее точной. В таких условиях объектив может вести себя так, будто диафрагма закрыта сильнее. Приложение использует диафрагменное число, которое вы указали, и не делает поправку на масштаб съёмки."))
            .performScrollTo().assertIsDisplayed()
        text(t("Close", "Закрыть")).performScrollTo().performClick()
        text(t("Settings", "Настройки")).performClick()
        text(t("Refine magnification", "Уточнить масштаб")).assertDoesNotExist()
        text(t("Macro Photography Warning", "Предупреждать о макросъёмке")).assertDoesNotExist()
        text(t("Warn about diffraction", "Предупреждать о дифракции")).performScrollTo().performClick()
        compose.runOnIdle {
            assertFalse(vm.uiState.showDiffractionWarning)
            assertEquals(.060390, vm.uiState.diffractionResult!!.airyDiameterMm, 1e-15)
            assertEquals(.3, vm.uiState.focusDistanceMeters!!, 0.0)
        }
        text(t("Close", "Закрыть")).performClick()
        compose.onNodeWithTag("diffraction-warning").assertDoesNotExist()
        compose.onNodeWithTag("macro-warning-card").assertDoesNotExist()
    }

    @Test fun englishConventionalDiffractionAndSettings() = check(false)
    @Test fun russianDarkLargeFontConventionalDiffractionAndSettings() = check(true)
}
