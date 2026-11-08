package com.example.dofcalculator.ui.calculator

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.text.rememberTextMeasurer
import com.example.dofcalculator.settings.DistanceDisplay
import com.example.dofcalculator.R
import com.example.dofcalculator.calculation.FocusDistanceScale
import com.example.dofcalculator.model.CalculatedDistance
import com.example.dofcalculator.settings.ThemeMode
import com.example.dofcalculator.ui.theme.DOFCalculatorTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FocusDistanceControl(
    state: DofCalculatorUiState,
    onText: (String) -> Unit,
    onSlider: (Double) -> Unit,
    onStep: (Boolean) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val haptics = LocalHapticFeedback.current
    var inputFocused by remember { mutableStateOf(false) }
    val scale = state.distanceScale
    val editable = state.isFocusDistanceEditable
    val atInfinity = state.focusDistance == CalculatedDistance.Infinity
    val fieldDescription = stringResource(R.string.focus_distance_title)
    val display = DistanceDisplay(state.distanceUnit, state.distanceRounding,
        LocalResources.current.configuration.locales[0])
    val inputText = if (state.calculatorMode == com.example.dofcalculator.settings.CalculatorMode.Hyperfocal)
        state.result?.hyperfocalDistanceMeters?.let(display::number).orEmpty()
        else if (atInfinity) "" else state.focusInputDraft
        ?: state.focusDistanceText.replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() }
            ?.let(display::number) ?: state.focusDistanceText
    val textStyle = MaterialTheme.typography.bodyLarge
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val unitLabel = distanceUnitLabel(state.distanceUnit)
    val desiredWidth = with(density) {
        val valueWidth = textMeasurer.measure(
            inputText.ifEmpty { "∞" }, style = textStyle, softWrap = false,
        ).size.width.toDp()
        val unitWidth = textMeasurer.measure(
            unitLabel, style = textStyle, softWrap = false,
        ).size.width.toDp()
        valueWidth + unitWidth
    } + 44.dp
    val previousA11y = stringResource(R.string.distance_decrease)
    val nextA11y = stringResource(R.string.distance_increase)
    val sliderA11y = stringResource(R.string.distance_slider)

    Card(
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.focus_distance_title),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            BoxWithConstraints(Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = inputText,
                onValueChange = onText,
                placeholder = { Text(if (editable) "∞" else "—") },
                suffix = { Text(unitLabel, style = textStyle) },
                modifier = Modifier
                    .width(desiredWidth.coerceAtLeast(88.dp).coerceAtMost(maxWidth))
                    .onFocusChanged { inputFocused = it.isFocused }
                    .semantics { contentDescription = fieldDescription },
                enabled = editable,
                singleLine = false,
                textStyle = textStyle,
                isError = state.focusDistanceHasError,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                    onDone = { focusManager.clearFocus() },
                ),
            )
            }
            if (state.focusDistanceHasError) {
                Text(
                    text = stringResource(R.string.input_number_error),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DistanceStepButton(
                    text = "−",
                    description = previousA11y,
                    enabled = editable && !atInfinity,
                    onClick = { onStep(false) },
                )
                // The marker overlays the track area, so it cannot offset the
                // slider vertically relative to the two step buttons.
                Box(
                    modifier = Modifier.weight(1f).height(64.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    val position = if (atInfinity) 1f else {
                        state.distanceSliderPosition.toFloat()
                            .coerceAtMost(Math.nextDown(1f))
                    }
                    Slider(
                        value = position,
                        onValueChange = { value ->
                            val previous = state.distanceSliderPosition
                            val next = value.toDouble()
                            val crossesH = scale?.hasHyperfocalMarker == true &&
                                ((previous < .75 && next >= .75) ||
                                    (previous > .75 && next <= .75))
                            if (crossesH || (next == 1.0 && !atInfinity)) {
                                haptics.performHapticFeedback(
                                    HapticFeedbackType.TextHandleMove,
                                )
                            }
                            onSlider(next)
                        },
                        enabled = editable && scale != null,
                        modifier = Modifier.fillMaxWidth().semantics {
                            contentDescription = sliderA11y
                        },
                        track = { sliderState ->
                            val markerColor = MaterialTheme.colorScheme.primary
                            SliderDefaults.Track(
                                sliderState = sliderState,
                                enabled = editable && scale != null,
                                modifier = Modifier.drawWithContent {
                                    drawContent()
                                    if (scale?.hasHyperfocalMarker == true) {
                                        // The track's measured bounds already account for
                                        // the thumb width and match its travel exactly.
                                        val fraction = if (layoutDirection == LayoutDirection.Rtl)
                                            0.25f else 0.75f
                                        val x = size.width * fraction
                                        drawLine(
                                            color = markerColor,
                                            start = Offset(x, center.y + 26.dp.toPx()),
                                            end = Offset(x, center.y + 32.dp.toPx()),
                                            strokeWidth = 2.dp.toPx(),
                                        )
                                    }
                                },
                            )
                        },
                    )
                }
                DistanceStepButton(
                    text = "+",
                    description = nextA11y,
                    enabled = editable && !atInfinity,
                    onClick = { onStep(true) },
                )
            }

            if (inputFocused) {
                TextButton(
                    onClick = focusManager::clearFocus,
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text(stringResource(R.string.hide_keyboard))
                }
            }
        }
    }
}

@Composable
private fun DistanceStepButton(
    text: String,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(48.dp).semantics {
            contentDescription = description
        },
        shape = MaterialTheme.shapes.medium,
        contentPadding = PaddingValues(0.dp),
    ) {
        Text(text = text, style = MaterialTheme.typography.titleLarge)
    }
}

@Preview(name = "Distance English", widthDp = 320, locale = "en", showBackground = true)
@Preview(name = "Distance Russian large text", widthDp = 320, locale = "ru",
    fontScale = 1.5f, showBackground = true)
@Composable
private fun FocusDistanceControlPreview() {
    DOFCalculatorTheme(themeMode = ThemeMode.Dark) {
        FocusDistanceControl(
            state = DofCalculatorUiState(
                focusDistanceText = "197.17",
                focusDistance = CalculatedDistance.Finite(197.17),
                distanceScale = FocusDistanceScale(0.8, 254.05),
                distanceSliderPosition = .72,
            ),
            onText = {},
            onSlider = {},
            onStep = {},
        )
    }
}

@Preview(name = "Full precision focus", widthDp = 320, locale = "en", showBackground = true)
@Preview(name = "Full precision large font", widthDp = 320, locale = "ru",
    fontScale = 1.5f, showBackground = true)
@Composable
private fun FullPrecisionFocusPreview() {
    DOFCalculatorTheme(themeMode = ThemeMode.Dark) {
        FocusDistanceControl(
            state = DofCalculatorUiState(
                focusDistanceText = "5.410835579514825",
                focusDistance = CalculatedDistance.Finite(5.410835579514825),
                distanceRounding = com.example.dofcalculator.settings.DistanceRounding.None,
                distanceScale = FocusDistanceScale(.3, 5.410835579514825),
                distanceSliderPosition = .75,
            ),
            onText = {}, onSlider = {}, onStep = {},
        )
    }
}

@Preview(name = "Focus targets light", widthDp = 320, locale = "ru", showBackground = true)
@Preview(name = "Focus targets dark", widthDp = 320, locale = "ru",
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun FocusTargetsPreview() {
    val hyperfocal = 6750.9
    val state = DofCalculatorUiState(
        focusDistanceText = hyperfocal.toString(),
        focusDistance = CalculatedDistance.Finite(hyperfocal),
        distanceScale = FocusDistanceScale.create(hyperfocal),
        distanceSliderPosition = .75,
    )
    DOFCalculatorTheme {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            for (targetState in listOf(state, state.copy(
                focusAtInfinity = true,
                focusDistance = CalculatedDistance.Infinity,
                distanceSliderPosition = 1.0,
            ))) {
                FocusDistanceControl(
                    state = targetState,
                    onText = {}, onSlider = {}, onStep = {},
                )
            }
        }
    }
}
