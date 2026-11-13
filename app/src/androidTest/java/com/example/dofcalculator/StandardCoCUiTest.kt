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
import kotlinx.coroutines.*
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StandardCoCUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val half = CameraProfile("existing-half", "Half-frame film", FrameFormat.Custom,
        customFormatName = "Half-frame", widthMm = 17.0, heightMm = 24.0)

    @After fun cleanup() { scope.cancel() }

    private fun check(russian: Boolean) {
        fun t(en: String, ru: String) = if (russian) ru else en
        fun text(value: String) = compose.onAllNodesWithText(value).onLast()
        val fallback = t(
            "The sharpness setting for this format is calculated automatically from the frame dimensions.\n\nThe acceptable blur — the Circle of Confusion (CoC) — is determined from the frame diagonal. The reference is the 36×24 mm format with a value of 0.030 mm.",
            "Настройка резкости для этого формата рассчитана автоматически по размерам кадра.\n\nДопустимое размытие — кружок рассеяния (CoC, Circle of Confusion) — определяется по диагонали кадра. За основу взят формат 36×24 мм со значением 0,030 мм.")
        lateinit var vm: DofCalculatorViewModel
        compose.runOnUiThread {
            vm = DofCalculatorViewModel(InMemoryPresetRepository(PresetLibrary(
                cameras = BuiltInCameraProfiles.cameras + half, selectedCameraId = half.id)),
                InMemoryAppSettingsRepository(AppSettings(sharpnessMigrated = true)), scope)
        }
        compose.setContent {
            AppLocale(if (russian) AppLanguage.Russian else AppLanguage.English) {
                DOFCalculatorTheme(themeMode = if (russian) ThemeMode.Dark else ThemeMode.Light,
                    dynamicColor = false) {
                    DofCalculatorScreen(vm.uiState, onOpenSharpness = vm::onOpenSharpness)
                    if (vm.uiState.isSharpnessVisible) SharpnessCriterionSheet(
                        vm.uiState, vm::onCloseSharpness, vm::onSharpnessModeSelected,
                        vm::onSharpnessFieldChanged, vm::onPrintPlacementSelected, vm::onImportLegacyCoC)
                }
            }
        }
        text(t("CoC 0.02039 mm", "CoC 0,02039 мм")).assertDoesNotExist()
        text(t("Sharpness Criterion", "Критерий резкости")).performClick()
        text(t("Calculated CoC: 0.02039 mm", "Рассчитанный CoC: 0,02039 мм")).assertIsDisplayed()
        text(fallback).performScrollTo().assertIsDisplayed()
        compose.runOnIdle { vm.onCameraProfileSelected("builtin.6x6") }
        compose.onAllNodesWithText(fallback).assertCountEquals(0)
        text(t("Calculated CoC: 0.0530 mm", "Рассчитанный CoC: 0,0530 мм")).assertIsDisplayed()
        compose.runOnIdle { vm.onCameraProfileSelected(half.id) }
        text(fallback).performScrollTo().assertIsDisplayed()
        compose.runOnIdle { vm.onSharpnessModeSelected(SharpnessCriterionMode.CUSTOM) }
        compose.onAllNodesWithText(fallback).assertCountEquals(0)
        compose.runOnIdle { vm.onSharpnessModeSelected(SharpnessCriterionMode.STANDARD) }
        text(fallback).performScrollTo().assertIsDisplayed()
        compose.runOnIdle { vm.onCloseSharpness() }
        text(t("CoC 0.02039 mm", "CoC 0,02039 мм")).assertDoesNotExist()
    }

    @Test fun englishFallbackAndCataloguePriority() = check(false)
    @Test fun russianFallbackAndCataloguePriorityInDarkTheme() = check(true)
}
