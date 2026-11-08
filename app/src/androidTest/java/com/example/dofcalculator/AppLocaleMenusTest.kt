package com.example.dofcalculator

import android.content.res.Configuration
import android.os.LocaleList
import androidx.activity.ComponentActivity
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.dofcalculator.settings.ApertureIncrements
import com.example.dofcalculator.settings.AppLanguage
import com.example.dofcalculator.ui.AppLocale
import com.example.dofcalculator.ui.calculator.*
import com.example.dofcalculator.ui.theme.DOFCalculatorTheme
import java.util.Locale
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Real dialogs and a modal sheet must not fall back to the window's system resources. */
@RunWith(AndroidJUnit4::class)
class AppLocaleMenusTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val focalViewModel = DofCalculatorViewModel()
    private val language = mutableStateOf(AppLanguage.English)
    private val increments = mutableStateOf(ApertureIncrements.FullStop)
    private val panel = mutableStateOf(Panel.NONE)
    private val editor = mutableStateOf<CameraEditorUiState?>(null)
    private enum class Panel { NONE, CAMERA, SHARPNESS, SETTINGS }

    @Test fun digitalCameraSearchShowsNamesAndLocalizedFormats() {
        mount(AppLanguage.English, "ru")
        text("Camera").performClick()
        visible("Select Camera")
        text("Search cameras").performTextReplacement("Fujifilm")
        visible("Fujifilm GFX")
        visible("Digital MF 44×33")
        text("Search cameras").performTextReplacement("Hasselblad")
        visible("Hasselblad X / 907X")
        visible("Hasselblad H6D-100c")
        visible("Digital MF 54×40")
        text("APS-C").assertDoesNotExist()
        compose.runOnIdle { language.value = AppLanguage.Russian }
        visible("Выбрать камеру")
        visible("Цифровой СФ 44×33")
        visible("Цифровой СФ 54×40")
        text("Hasselblad X / 907X").assertIsDisplayed()
        text("Hasselblad H6D-100c").assertIsDisplayed()
    }

    private fun mount(selected: AppLanguage, simulatedSystem: String) {
        language.value = selected
        compose.setContent {
            val activityContext = LocalContext.current
            val activityConfiguration = LocalConfiguration.current
            val configuration = remember(activityConfiguration) {
                Configuration(activityConfiguration).apply {
                    setLocales(LocaleList(Locale.forLanguageTag(simulatedSystem)))
                }
            }
            val systemContext = remember { activityContext.createConfigurationContext(configuration) }
            CompositionLocalProvider(LocalContext provides systemContext,
                LocalConfiguration provides configuration) {
                AppLocale(language.value) {
                    DOFCalculatorTheme(dynamicColor = false) {
                        val state = DofCalculatorUiState(
                            focalLengthText = focalViewModel.uiState.focalLengthText,
                            selectedFocalLengthPresetMm = focalViewModel.uiState.selectedFocalLengthPresetMm,
                            canDecreaseFocalLength = focalViewModel.uiState.canDecreaseFocalLength,
                            canIncreaseFocalLength = focalViewModel.uiState.canIncreaseFocalLength,
                            language = language.value,
                            apertureIncrements = increments.value,
                            isSettingsVisible = panel.value == Panel.SETTINGS,
                            presets = PresetManagerUiState(
                                isManagerVisible = panel.value == Panel.CAMERA,
                                cameraEditor = editor.value),
                        )
                        DofCalculatorScreen(
                            uiState = state,
                            onFocalLengthChanged = focalViewModel::onFocalLengthChanged,
                            onStepFocalLength = focalViewModel::onStepFocalLength,
                            onFocalLengthPresetSelected = focalViewModel::onFocalLengthPresetSelected,
                            onOpenPresetManager = { panel.value = Panel.CAMERA },
                            onOpenSharpness = { panel.value = Panel.SHARPNESS },
                            onOpenSettings = { panel.value = Panel.SETTINGS },
                        )
                        AppDialogs(
                            uiState = state, versionName = "test",
                            onThemeModeSelected = {}, onLanguageSelected = { language.value = it },
                            onDistanceUnitSelected = {}, onDistanceRoundingSelected = {},
                            onOpenAbout = {}, onCloseSettings = { panel.value = Panel.NONE },
                            onCloseAbout = {}, onDismissStorageError = {},
                            onApertureIncrementsSelected = { increments.value = it },
                        )
                        if (panel.value == Panel.SHARPNESS) {
                            SharpnessCriterionSheet(state, { panel.value = Panel.NONE },
                                {}, { _, _ -> }, {}, {})
                        }
                        PresetManagementDialogs(
                            state = state.presets,
                            onCloseManager = { panel.value = Panel.NONE },
                            onSelectCamera = {}, onCustomFormatChanged = {},
                            onConfirmDelete = {}, onCancelDelete = {},
                            onCreateCamera = { editor.value = CameraEditorUiState() },
                            onEditCamera = {}, onDeleteCamera = {}, onCameraNameChanged = {},
                            onCameraFrameFormatSelected = {}, onCameraDimensionsChanged = { _, _ -> },
                            onSaveCamera = {}, onCancelCamera = { editor.value = null },
                            onDismissStorageError = {},
                        )
                    }
                }
            }
        }
    }

    // The same label can remain on the dimmed calculator underneath a window.
    private fun text(value: String) = compose.onAllNodesWithText(value).onLast()
    private fun visible(value: String) {
        compose.waitUntil(timeoutMillis = 5_000) { text(value).isDisplayed() }
        text(value).assertIsDisplayed()
    }
    private fun dismissPanel() = compose.runOnIdle {
        panel.value = Panel.NONE
        editor.value = null
    }

    private fun checkMenus(russian: Boolean) {
        fun t(en: String, ru: String) = if (russian) ru else en

        text(t("Camera", "Камера")).performClick()
        visible(t("Select Camera", "Выбрать камеру"))
        text(t("Search cameras", "Поиск камер")).assertIsDisplayed()
        text(t("Nikon 1 series", "Серия Nikon 1")).assertIsDisplayed()
        text(t("Add camera", "Добавить камеру")).performClick()
        text(t("Camera name", "Название камеры")).assertIsDisplayed()
        text(t("Frame format", "Формат кадра")).assertIsDisplayed()
        text(t("Cancel", "Отмена")).performClick()
        dismissPanel()

        text(t("Focal length", "Фокусное")).performClick()
        visible(t("Focal length, mm", "Фокусное расстояние, мм"))
        text(t("Common focal lengths", "Типовые фокусные")).assertIsDisplayed()
        text(t("Close", "Закрыть")).performClick()

        text(t("Aperture", "Диафрагма")).performClick()
        visible(t("Aperture, f/", "Диафрагма, f/"))
        text(t("Standard series", "Стандартный ряд")).assertIsDisplayed()
        text(t("Close", "Закрыть")).performClick()

        text(t("Sharpness Criterion", "Критерий резкости")).performClick()
        visible(t("Standard", "Стандарт"))
        text(t("Pixel-Based", "По пикселям")).assertIsDisplayed()
        text(t("Video-Based", "Для видео")).assertIsDisplayed()
        text(t("Print-Based", "Для печати")).performScrollTo().assertIsDisplayed()
        text(t("Screen-Based", "Для экрана")).performScrollTo().assertIsDisplayed()
        text(t("Custom CoC", "Свой CoC")).performScrollTo().assertIsDisplayed()
        text(t("Calculated CoC: 0.0300 mm", "Рассчитанный CoC: 0,0300 мм")).assertIsDisplayed()
        dismissPanel()

        text(t("Settings", "Настройки")).performClick()
        visible(t("Theme", "Тема"))
        text(t("Language", "Язык")).assertIsDisplayed()
        text(t("English", "Английский")).assertIsDisplayed()
        text(t("Russian", "Русский")).performScrollTo().assertIsDisplayed()
        text(t("Aperture increments", "Шаги диафрагмы")).performScrollTo().assertIsDisplayed()
        text(t("Full stop", "Полный шаг")).performScrollTo().assertIsDisplayed()
        text(t("1/2 stop", "1/2 шага")).performScrollTo().assertIsDisplayed()
        text(t("1/3 stop", "1/3 шага")).performScrollTo().assertIsDisplayed()
        text(t("Close", "Закрыть")).performClick()
    }

    @Test fun englishMenusOverrideRussianSystemResources() {
        mount(AppLanguage.English, "ru")
        checkMenus(russian = false)
    }

    @Test fun russianMenusOverrideEnglishSystemResources() {
        mount(AppLanguage.Russian, "en")
        checkMenus(russian = true)
    }

    @Test fun focalLengthButtonsAndNewPresetsUpdateTheInput() {
        mount(AppLanguage.English, "ru")
        text("Focal length").performClick()
        visible("Focal length, mm")
        compose.onNodeWithContentDescription("Increase focal length by 1 mm").performClick()
        text("51").assertIsDisplayed()
        compose.onNodeWithContentDescription("Decrease focal length by 1 mm").performClick()
        text("50").assertIsDisplayed()
        text("80 mm").assertDoesNotExist()
        for (value in listOf(85, 200, 300)) {
            text("$value mm").performScrollTo().performClick()
            text(value.toString()).assertIsDisplayed()
        }
    }

    @Test fun apertureIncrementSettingChangesTheApertureMenu() {
        mount(AppLanguage.English, "ru")
        fun selectIncrement(label: String) {
            text("Settings").performClick()
            text(label).performScrollTo().performClick()
            text("Close").performClick()
            text("Aperture").performClick()
            visible("Aperture, f/")
            text("8").assertIsDisplayed()
        }
        selectIncrement("1/3 stop")
        text("f/1.1").performScrollTo().assertIsDisplayed()
        text("f/3.5").performScrollTo().assertIsDisplayed()
        text("Close").performClick()
        selectIncrement("1/2 stop")
        text("f/1.7").performScrollTo().assertIsDisplayed()
        text("Close").performClick()
        selectIncrement("Full stop")
        text("f/1.1").assertDoesNotExist()
        text("f/1.7").assertDoesNotExist()
        text("f/1.4").performScrollTo().assertIsDisplayed()
    }

    @Test fun languageChangesInsideAnOpenDialogAndSystemModeRestoresSystemLanguage() {
        mount(AppLanguage.English, "ru")
        text("Settings").performClick()
        text("Russian").performScrollTo().performClick()
        text("Тема").performScrollTo().assertIsDisplayed()
        text("Русский").performScrollTo().assertIsDisplayed()
        text("Английский").performScrollTo().performClick()
        text("Theme").performScrollTo().assertIsDisplayed()
        // There are separate System theme and System language options.
        text("Language").performScrollTo()
        compose.onAllNodesWithText("System").onLast().performScrollTo().performClick()
        text("Тема").performScrollTo().assertIsDisplayed()
        text("Закрыть").performClick()
        text("Камера").assertIsDisplayed()
    }
}
