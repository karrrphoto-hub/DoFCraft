package com.example.dofcalculator

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.dofcalculator.model.SharpnessCriterionMode
import com.example.dofcalculator.settings.AppLanguage
import com.example.dofcalculator.ui.AppLocale
import com.example.dofcalculator.ui.calculator.*
import com.example.dofcalculator.ui.theme.DOFCalculatorTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SharpnessSheetScrollTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private fun checkFling(russian: Boolean) {
        val vm = DofCalculatorViewModel()
        vm.onSharpnessModeSelected(SharpnessCriterionMode.VIDEO_BASED)
        compose.setContent {
            AppLocale(if (russian) AppLanguage.Russian else AppLanguage.English) {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, if (russian) 1.3f else 1f)) {
                    DOFCalculatorTheme(dynamicColor = false) {
                        SharpnessCriterionSheet(vm.uiState, {}, vm::onSharpnessModeSelected,
                            vm::onSharpnessFieldChanged, vm::onPrintPlacementSelected, vm::onImportLegacyCoC,
                            onVideoResolution = vm::onVideoResolutionSelected,
                            onVideoField = vm::onVideoFieldChanged,
                            onVideoAreaMode = vm::onVideoAreaModeSelected)
                    }
                }
            }
        }
        compose.waitForIdle()
        val header = compose.onNodeWithText(if (russian) "Критерий резкости" else "Sharpness Criterion")
        val footer = compose.onNode(hasText(if (russian) "Рассчитанный CoC:" else "Calculated CoC:", substring = true))
        val scroll = compose.onNode(hasScrollAction())
        val headerNode = header.fetchSemanticsNode()
        val footerNode = footer.fetchSemanticsNode()
        val headerTop = compose.runOnUiThread { headerNode.boundsInRoot.top }
        val footerTop = compose.runOnUiThread { footerNode.boundsInRoot.top }

        compose.mainClock.autoAdvance = false
        try {
            // Read live coordinates on the UI thread without asking Espresso to idle a paused animation.
            repeat(3) {
                scroll.performTouchInput { swipeUp(durationMillis = 100) }
                repeat(60) {
                    compose.mainClock.advanceTimeByFrame()
                    compose.runOnUiThread {
                        assertEquals("The sheet header moved during an upward fling",
                            headerTop, headerNode.boundsInRoot.top, 4f)
                        assertEquals("The CoC footer moved during an upward fling",
                            footerTop, footerNode.boundsInRoot.top, 4f)
                    }
                }
            }
        } finally {
            compose.mainClock.autoAdvance = true
        }
        val range = scroll.fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange]
        assertTrue(range.value() > 0f)
        footer.assertIsDisplayed()
    }

    @Test fun englishUpwardFlingKeepsSheetAndFooterStill() = checkFling(false)
    @Test fun russianLargeFontUpwardFlingKeepsSheetAndFooterStill() = checkFling(true)
}
