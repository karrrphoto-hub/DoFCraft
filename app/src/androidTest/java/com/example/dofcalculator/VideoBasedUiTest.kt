package com.example.dofcalculator

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.dofcalculator.model.*
import com.example.dofcalculator.settings.AppLanguage
import com.example.dofcalculator.ui.AppLocale
import com.example.dofcalculator.ui.calculator.*
import com.example.dofcalculator.ui.theme.DOFCalculatorTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VideoBasedUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val vm = DofCalculatorViewModel()
    private fun text(value: String) = compose.onAllNodesWithText(value).onLast()
    private fun visible(value: String) {
        compose.waitUntil(5_000) { text(value).isDisplayed() }
        text(value).assertIsDisplayed()
    }
    private fun mount(russian: Boolean) {
        compose.setContent {
            AppLocale(if (russian) AppLanguage.Russian else AppLanguage.English) {
                DOFCalculatorTheme(themeMode = if (russian) com.example.dofcalculator.settings.ThemeMode.Dark
                    else com.example.dofcalculator.settings.ThemeMode.Light, dynamicColor = false) {
                    DofCalculatorScreen(vm.uiState, onOpenSharpness = vm::onOpenSharpness)
                    if (vm.uiState.isSharpnessVisible) SharpnessCriterionSheet(
                        vm.uiState, vm::onCloseSharpness, vm::onSharpnessModeSelected,
                        vm::onSharpnessFieldChanged, vm::onPrintPlacementSelected, vm::onImportLegacyCoC,
                        onVideoResolution = vm::onVideoResolutionSelected,
                        onVideoField = vm::onVideoFieldChanged,
                        onVideoAreaMode = vm::onVideoAreaModeSelected)
                }
            }
        }
    }

    private fun check(russian: Boolean) {
        fun t(en: String, ru: String) = if (russian) ru else en
        fun number(en: String) = if (russian) en.replace('.', ',') else en
        fun coc(value: String) = t("Calculated CoC: $value mm",
            "Рассчитанный CoC: ${number(value)} мм")
        mount(russian)
        text(t("Sharpness Criterion", "Критерий резкости")).performClick()
        visible(t("Standard", "Стандарт"))
        text(t("Video-Based", "Для видео")).performScrollTo().performClick()
        visible(coc("0.028125"))
        text(t("Video Resolution", "Разрешение видео")).performScrollTo().performClick()
        text("Full HD (FHD) — 1920×1080").performScrollTo().performClick()
        visible(coc("0.05625"))
        text(t("Advanced", "Дополнительно")).performScrollTo().performClick()
        text(t("1.5×", "1,5×")).performScrollTo().performClick()
        visible(coc("0.0375"))
        text(t("Video calculation details", "Параметры видеокадра")).performScrollTo().performClick()
        text(t("Active sensor area: 24 × 13.5 mm", "Активная область: 24 × 13,5 мм"))
            .performScrollTo().assertIsDisplayed()
        text(t("Manual Active Area", "Ручная область сенсора")).performScrollTo().performClick()
        text(t("Active width, mm", "Ширина активной области, мм"))
            .performScrollTo().performTextReplacement("40")
        text(t("The recording area is larger than the selected camera sensor. Check its width and height or switch to automatic area selection.",
            "Указанная область записи больше матрицы выбранной камеры. Проверьте её ширину и высоту или переключитесь на автоматический выбор области."))
            .assertExists()
        compose.runOnIdle { assertNull(vm.uiState.result) }
        text(t("Use Auto area", "Использовать область Авто")).performScrollTo().performClick()
        visible(coc("0.0375"))
        text(t("Video Resolution", "Разрешение видео")).performScrollTo().performClick()
        text(t("Custom — 3840×2160", "Свой формат — 3840×2160")).performScrollTo().performClick()
        text(t("Video width, px", "Ширина видео, пикс.")).performScrollTo().performTextReplacement("1920.5")
        text(t("Enter a whole number of pixels, from 1 to 2147483647.",
            "Введите число пикселей без дробной части — от 1 до 2147483647.")).assertExists()
        compose.runOnIdle { assertNull(vm.uiState.effectiveCoCMm) }
        text(t("Video width, px", "Ширина видео, пикс.")).performScrollTo().performTextReplacement("1920")
        text(t("Video height, px", "Высота видео, пикс.")).performScrollTo().performTextReplacement("1080")
        text(t("Acceptable blur, px", "Допустимое размытие, пикс."))
            .performScrollTo().performTextReplacement("1,5")
        visible(coc("0.01875"))
        // Dismiss without changing any preferences in the device's real DataStore.
        compose.runOnIdle { vm.onCloseSharpness() }
        visible(t("Video-Based · 1920×1080", "Для видео · 1920×1080"))
        text(t("Sharpness Criterion", "Критерий резкости")).performClick()
        text(t("Standard", "Стандарт")).performScrollTo().performClick()
        text(t("Video-Based", "Для видео")).performScrollTo().performClick()
        visible(coc("0.01875"))
        compose.runOnIdle {
            assertEquals(VideoResolution.CUSTOM, vm.uiState.sharpness.video.resolution)
            assertEquals(1.5, vm.uiState.sharpness.video.cropFactor, 0.0)
            assertEquals(40.0, vm.uiState.sharpness.video.manualWidthMm, 0.0)
        }
    }

    @Test fun englishVideoControlsAndValidation() = check(false)
    @Test fun russianVideoControlsAndValidationInDarkTheme() = check(true)
}
