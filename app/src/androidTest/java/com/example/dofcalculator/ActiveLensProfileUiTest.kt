package com.example.dofcalculator

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.dofcalculator.data.*
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
class ActiveLensProfileUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private fun check(russian: Boolean) {
        fun t(en: String, ru: String) = if (russian) ru else en
        fun dialogText(value: String) = compose.onAllNodes(
            hasText(value) and hasAnyAncestor(isDialog())).onLast()
        val camera = CameraProfile("lens-ui-camera", "Test camera", FrameFormat.FullFrame35Mm)
        val prime = Lens.Prime("prime", "Prime 58.25", 58.25, 1.7, 16.0, LensDistanceUnit.Feet)
        val zoom = Lens.Zoom("zoom", "Zoom 24–70", 24.0, 2.8, 70.0, 2.8, 22.0, LensDistanceUnit.Meters)
        val repository = InMemoryPresetRepository(PresetLibrary(
            cameras = BuiltInCameraProfiles.cameras + camera, selectedCameraId = camera.id,
            lensKitsByCameraId = mapOf(camera.id to CameraLensKit(listOf(zoom, prime), selectedLensId = zoom.id))))
        val vm = DofCalculatorViewModel(repository, InMemoryAppSettingsRepository(AppSettings(sharpnessMigrated = true)))
        vm.onFocusDistanceChanged("12.192")
        compose.setContent {
            AppLocale(if (russian) AppLanguage.Russian else AppLanguage.English) {
                DOFCalculatorTheme(themeMode = if (russian) ThemeMode.Dark else ThemeMode.Light,
                    dynamicColor = false) {
                    DofCalculatorScreen(vm.uiState,
                        onFocalLengthChanged = vm::onFocalLengthChanged,
                        onFocalLengthPresetSelected = vm::onFocalLengthPresetSelected,
                        onApertureChanged = vm::onApertureChanged,
                        onAperturePresetSelected = vm::onAperturePresetSelected,
                        onShowAllFocalLengths = vm::onIgnoreLensProfileChanged,
                        onShowAllApertures = vm::onIgnoreLensProfileChanged,
                        onLensSelected = vm::onLensSelected)
                }
            }
        }
        compose.runOnIdle { vm.onFocusDistanceChanged("12.192") }
        compose.onAllNodesWithText(t("Focal length", "Фокусное")).onFirst().performClick()
        val selector = compose.onNodeWithContentDescription(t("Choose lens", "Выбрать объектив"))
        selector.performScrollTo().performClick()
        compose.onNodeWithText("Prime 58.25").performClick()
        compose.runOnIdle {
            assertEquals(prime.id, vm.uiState.activeLens!!.id)
            assertEquals("58.25", vm.uiState.focalLengthText)
            assertEquals(DistanceUnit.Feet, vm.uiState.effectiveFocusDistanceUnit)
            assertEquals(12.192, vm.uiState.focusDistanceMeters!!, 0.0)
        }
        dialogText(t("58.25 mm", "58,25 мм")).assertExists()
        val ignore = t("Ignore Lens Profile, Show All Focal Lengths",
            "Игнорировать профиль объектива, показать все фокусные")
        dialogText(ignore).performScrollTo().performClick()
        selector.assertIsNotEnabled()
        dialogText(t("12 mm", "12 мм")).assertExists()
        compose.onAllNodes(hasText(t("58.25 mm", "58,25 мм")) and hasAnyAncestor(isDialog()))
            .assertCountEquals(0)
        compose.runOnIdle {
            assertNull(vm.uiState.activeLens)
            assertEquals(DistanceUnit.Meters, vm.uiState.effectiveFocusDistanceUnit)
            assertEquals(12.192, vm.uiState.focusDistanceMeters!!, 0.0)
        }
        dialogText(ignore).performClick()
        selector.assertIsEnabled()
        dialogText(t("Close", "Закрыть")).performClick()
        compose.onAllNodesWithText(t("Aperture", "Диафрагма")).onFirst().performClick()
        dialogText(t("f/1.7", "f/1,7")).assertExists()
        dialogText("f/22").assertDoesNotExist()
        compose.runOnIdle {
            assertEquals(prime.id, vm.uiState.activeLens!!.id)
            assertNotNull(vm.uiState.result)
        }
    }

    @Test fun englishLensSelectionAndIgnore() = check(false)
    @Test fun russianLensSelectionAndIgnore() = check(true)
}
