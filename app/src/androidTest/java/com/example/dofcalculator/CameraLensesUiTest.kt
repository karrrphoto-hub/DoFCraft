package com.example.dofcalculator

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.dofcalculator.data.*
import com.example.dofcalculator.settings.*
import com.example.dofcalculator.ui.AppLocale
import com.example.dofcalculator.ui.calculator.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CameraLensesUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private fun check(russian: Boolean) {
        fun t(en: String, ru: String) = if (russian) ru else en
        fun text(value: String) = compose.onAllNodesWithText(value).onLast()
        fun number(en: String) = if (russian) en.replace('.', ',') else en
        fun lensField(en: String, ru: String, value: String) {
            compose.onNodeWithContentDescription(t(en, ru)).performScrollTo().performTextReplacement(value)
        }
        // In-memory dependencies: tests never read or write a user's stored profiles.
        val vm = DofCalculatorViewModel()
        vm.onCreateCamera()
        vm.onCameraEditorNameChanged("Lens test camera")
        vm.onSaveCameraEditor()
        val customId = vm.uiState.presets.cameras.single { !it.isBuiltIn }.id
        vm.onCameraProfileSelected(customId)
        compose.setContent {
            AppLocale(if (russian) AppLanguage.Russian else AppLanguage.English) {
                com.example.dofcalculator.ui.theme.DOFCalculatorTheme(
                    themeMode = if (russian) ThemeMode.Dark else ThemeMode.Light, dynamicColor = false) {
                    DofCalculatorScreen(vm.uiState,
                        onOpenPresetManager = vm::onOpenPresetManager,
                        onFocalLengthChanged = vm::onFocalLengthChanged,
                        onFocalLengthPresetSelected = vm::onFocalLengthPresetSelected,
                        onApertureChanged = vm::onApertureChanged,
                        onAperturePresetSelected = vm::onAperturePresetSelected,
                        onOpenCameraLenses = vm::onOpenCameraLenses,
                        onShowAllFocalLengths = vm::onShowAllFocalLengths,
                        onShowAllApertures = vm::onShowAllApertures)
                    CameraManagementDialogs(vm)
                }
            }
        }
        compose.runOnIdle { vm.onOpenCameraLenses(BuiltInCameraProfiles.DEFAULT_ID) }
        text(t("Add lens", "Добавить объектив")).assertDoesNotExist()
        text(t("Close", "Закрыть")).performClick()
        text(t("Camera", "Камера")).performClick()
        text(t("Search cameras", "Поиск камер")).performTextReplacement("Lens test camera")
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        compose.onAllNodes(hasText(t("Lenses", "Объективы")) and hasClickAction() and hasAnyAncestor(isDialog()))
            .assertCountEquals(0)
        text(t("Edit", "Изменить")).performClick()
        text(t("Add lens", "Добавить объектив")).performScrollTo().performClick()
        lensField("Focal Length, mm", "Фокусное расстояние, мм", "58")
        lensField("Maximum Aperture, f/", "Светосила, f/", "1,7")
        text(t("Save", "Сохранить")).performClick()
        text(t("58 mm · f/1.7", "58 мм · f/1,7")).performScrollTo().assertIsDisplayed()
        text(t("Camera name", "Название камеры")).assertTextContains("Lens test camera")
        text(t("Add lens", "Добавить объектив")).performScrollTo().performClick()
        text(t("Zoom Lens", "Зум-объектив")).performClick()
        lensField("Minimum Focal Length, mm", "Минимальное фокусное, мм", "48")
        lensField("Maximum Aperture at Wide End, f/", "Светосила на коротком конце, f/", "3.5")
        lensField("Maximum Focal Length, mm", "Максимальное фокусное, мм", "217")
        lensField("Maximum Aperture at Tele End, f/", "Светосила на длинном конце, f/", "5.6")
        lensField("Minimum Aperture, f/ (optional)", "Максимально закрытая диафрагма, f/ (необязательно)", "22")
        text(t("Save", "Сохранить")).performClick()
        val zoomSummary = t("48–217 mm · f/3.5–5.6", "48–217 мм · f/3,5–5,6")
        text(zoomSummary).performScrollTo().assertExists()
        compose.onAllNodesWithText(t("Edit Lens", "Редактировать объектив")).onFirst()
            .performScrollTo().performClick()
        lensField("Focal Length, mm", "Фокусное расстояние, мм", "60")
        text(t("Save", "Сохранить")).performClick()
        text(t("60 mm · f/1.7", "60 мм · f/1,7")).performScrollTo().assertExists()
        text(t("Save", "Сохранить")).performClick()
        text(t("Search cameras", "Поиск камер")).performTextReplacement("Lens test camera")
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        text(zoomSummary).assert(hasClickAction().not())
        text(t("60 mm · f/1.7", "60 мм · f/1,7")).assert(hasClickAction().not())
        compose.onAllNodes(hasText(t("Lenses", "Объективы")) and hasClickAction() and hasAnyAncestor(isDialog()))
            .assertCountEquals(0)
        text(t("Close", "Закрыть")).performClick()
        text(t("2 lenses", "2 объектива")).assertIsDisplayed()
        compose.runOnIdle {
            vm.onFocalLengthChanged("300")
            vm.onCameraProfileSelected(BuiltInCameraProfiles.DEFAULT_ID)
            vm.onCameraProfileSelected(customId)
            assertEquals("48", vm.uiState.focalLengthText)
        }
        text(t("48 mm", "48 мм")).assertIsDisplayed()
        text(t("Focal length", "Фокусное")).performClick()
        text(t("Show All Focal Lengths", "Показывать все фокусные")).assertIsDisplayed()
        text(t("48 mm", "48 мм")).assertExists()
        text(t("217 mm", "217 мм")).assertExists()
        compose.onAllNodesWithText(t("12 mm", "12 мм")).assertCountEquals(0)
        text(t("Show All Focal Lengths", "Показывать все фокусные")).performClick()
        text(t("12 mm", "12 мм")).assertExists()
        text(t("48 mm", "48 мм")).assertExists()
        text(t("Focal length, mm", "Фокусное расстояние, мм")).performTextReplacement("300")
        text(t("Close", "Закрыть")).performClick()
        text(t("Aperture", "Диафрагма")).performClick()
        text(t("Show All Apertures", "Показывать все диафрагмы")).assertIsDisplayed()
        text("f/" + number("1.7")).assertExists()
        text(t("Show All Apertures", "Показывать все диафрагмы")).performClick()
        text("f/1").assertExists()
        text(t("Aperture, f/", "Диафрагма, f/")).performTextReplacement("1.2")
        text(t("Close", "Закрыть")).performClick()
        compose.runOnIdle {
            assertEquals("300", vm.uiState.focalLengthText)
            assertEquals("1.2", vm.uiState.apertureText)
            assertNotNull(vm.uiState.result)
            assertEquals(2, vm.uiState.cameraLensKit.lenses.size)
        }
        compose.runOnIdle {
            vm.onCameraProfileSelected(BuiltInCameraProfiles.DEFAULT_ID)
            vm.onCameraProfileSelected(customId)
            // Show All includes 12 mm, but the actual kit minimum is still 48 mm.
            assertEquals("48", vm.uiState.focalLengthText)
            assertEquals("1.2", vm.uiState.apertureText)
        }
        compose.runOnIdle { vm.onEditCamera(customId) }
        compose.onAllNodesWithText(t("Delete Lens", "Удалить объектив")).onFirst()
            .performScrollTo().performClick()
        text(t("Delete", "Удалить")).performClick()
        text(t("Save", "Сохранить")).performClick()
        text(zoomSummary).assertIsDisplayed()
    }

    @Test fun englishPrimeZoomAndQuickInputs() = check(false)
    @Test fun russianPrimeZoomAndQuickInputsInDarkTheme() = check(true)
}
