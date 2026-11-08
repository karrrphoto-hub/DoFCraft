package com.example.dofcalculator.ui.calculator

import androidx.compose.foundation.clickable

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.dofcalculator.R
import com.example.dofcalculator.model.CalculatedDistance
import com.example.dofcalculator.model.CameraProfile
import com.example.dofcalculator.model.DepthOfFieldResult
import com.example.dofcalculator.model.FrameFormat
import com.example.dofcalculator.model.SharpnessCriterionMode
import com.example.dofcalculator.settings.CalculatorMode
import com.example.dofcalculator.settings.DistanceDisplay
import kotlin.math.abs

private enum class ParameterEditor {
    Camera,
    FocalLength,
    Aperture,
}

@Composable
internal fun CompactCalculatorContent(
    uiState: DofCalculatorUiState,
    onFrameFormatSelected: (FrameFormat) -> Unit,
    onCameraProfileSelected: (String) -> Unit,
    onCircleOfConfusionChanged: (String) -> Unit,
    onFocalLengthChanged: (String) -> Unit,
    onStepFocalLength: (Boolean) -> Unit,
    onFocalLengthPresetSelected: (Double) -> Unit,
    onApertureChanged: (String) -> Unit,
    onAperturePresetSelected: (Double) -> Unit,
    onPreviousAperture: () -> Unit,
    onNextAperture: () -> Unit,
    onOpenFocusDistance: () -> Unit,
    onCalculatorModeSelected: (CalculatorMode) -> Unit,
    onOpenPresetManager: () -> Unit,
    onOpenSharpness: () -> Unit,
    onOpenDiffractionInfo: () -> Unit,
    onOpenCameraLenses: (String) -> Unit,
    onShowAllFocalLengths: (Boolean) -> Unit,
    onShowAllApertures: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var editorName by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedCamera = uiState.matchingCameraProfile()

    Box(
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = 720.dp)
                .fillMaxWidth()
                .heightIn(min = 0.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().height(androidx.compose.foundation.layout.IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ParameterTile(
                    label = stringResource(R.string.camera_parameter),
                    value = selectedCamera?.let { cameraNameLabel(it) }
                        ?: stringResource(uiState.selectedFrameFormat.labelResource()),
                    supportingValue = selectedCamera?.let { cameraFormatLabel(it) }
                        ?: stringResource(uiState.selectedFrameFormat.labelResource()),
                    onClick = onOpenPresetManager,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    multiline = true,
                    additionalValue = when (uiState.cameraLensKit.lenses.size) {
                        0 -> null
                        1 -> lensSummary(uiState.cameraLensKit.lenses.single())
                        else -> androidx.compose.ui.res.pluralStringResource(R.plurals.lens_count,
                            uiState.cameraLensKit.lenses.size, uiState.cameraLensKit.lenses.size)
                    },
                    onAdditionalClick = { selectedCamera?.id?.let(onOpenCameraLenses) },
                )
                ParameterTile(
                    label = stringResource(R.string.sharpness_title),
                    value = if (uiState.sharpness.mode == SharpnessCriterionMode.VIDEO_BASED)
                        stringResource(R.string.video_card_title, stringResource(uiState.sharpness.mode.labelResource()),
                            videoResolutionShort(uiState.sharpness.video))
                    else stringResource(uiState.sharpness.mode.labelResource()),
                    supportingValue = stringResource(R.string.sharpness_coc_value,
                        formatCoC(uiState.effectiveCoCMm, LocalResources.current.configuration.locales[0])),
                    onClick = onOpenSharpness,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    multiline = true,
                    compactValue = uiState.sharpness.mode == SharpnessCriterionMode.VIDEO_BASED ||
                        uiState.sharpness.mode == SharpnessCriterionMode.SCREEN_BASED,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().height(androidx.compose.foundation.layout.IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ParameterTile(
                    label = stringResource(R.string.focal_length_parameter),
                    value = stringResource(
                        R.string.focal_length_value,
                        uiState.focalLengthText,
                    ),
                    multiline = true,
                    onClick = { editorName = ParameterEditor.FocalLength.name },
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
                ParameterTile(
                    label = stringResource(R.string.aperture_parameter),
                    value = stringResource(
                        R.string.aperture_preset,
                        uiState.apertureText,
                    ),
                    onClick = {
                        editorName = ParameterEditor.Aperture.name
                    },
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
            }

            DiffractionWarning(
                state = uiState,
                onInformation = onOpenDiffractionInfo,
            )

            DistanceParameterTile(
                state = uiState,
                onDistance = onOpenFocusDistance,
                onMode = onCalculatorModeSelected,
            )


            ResultsInfographic(
                result = uiState.result,
                focusDistanceMeters = uiState.focusDistanceMeters,
                inputError = uiState.inputError,
                calculatorMode = uiState.calculatorMode,
                focusAtInfinity = uiState.calculatorMode == CalculatorMode.DepthOfField && uiState.focusAtInfinity,
            )

            val result = uiState.result
            val focusDistanceMeters = uiState.focusDistanceMeters
            if ((result != null) && (focusDistanceMeters != null || uiState.focusAtInfinity)) {
                DofVisualization(
                    nearLimitMeters = result.nearLimitMeters,
                    focusDistanceMeters = focusDistanceMeters ?: result.nearLimitMeters,
                    farLimit = result.farLimit,
                    focusAtInfinity = uiState.calculatorMode == CalculatorMode.DepthOfField && uiState.focusAtInfinity,
                )
            }
        }
    }

    when (editorName?.let(ParameterEditor::valueOf)) {
        ParameterEditor.Camera -> Unit
        ParameterEditor.FocalLength -> FocalLengthDialog(
            uiState = uiState,
            onFocalLengthChanged = onFocalLengthChanged,
            onStepFocalLength = onStepFocalLength,
            onFocalLengthPresetSelected = onFocalLengthPresetSelected,
            onShowAllFocalLengths = onShowAllFocalLengths,
            onDismiss = { editorName = null },
        )
        ParameterEditor.Aperture -> ApertureDialog(
            uiState = uiState,
            onApertureChanged = onApertureChanged,
            onAperturePresetSelected = onAperturePresetSelected,
            onPreviousAperture = onPreviousAperture,
            onNextAperture = onNextAperture,
            onShowAllApertures = onShowAllApertures,
            onDismiss = { editorName = null },
        )
        null -> Unit
    }
}

@Composable
private fun ParameterTile(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    supportingValue: String? = null,
    multiline: Boolean = false,
    compactValue: Boolean = false,
    additionalValue: String? = null,
    onAdditionalClick: (() -> Unit)? = null,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 72.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = if (multiline) 2 else 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = value,
                style = if (compactValue) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = if (multiline) 2 else 1,
                overflow = TextOverflow.Ellipsis,
            )
            supportingValue?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = if (multiline) 3 else 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            additionalValue?.let { lensText ->
                Text(lensText, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary, maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = if (onAdditionalClick != null) Modifier
                        .clickable(onClick = onAdditionalClick).padding(vertical = 6.dp) else Modifier)
            }
        }
    }
}

@Composable
private fun ResultsInfographic(
    result: DepthOfFieldResult?,
    focusDistanceMeters: Double?,
    inputError: DofInputError?,
    calculatorMode: CalculatorMode,
    focusAtInfinity: Boolean,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if ((result == null) || (focusDistanceMeters == null && !focusAtInfinity)) {
                Text(
                    text = when (inputError) {
                        DofInputError.OutOfRange ->
                            stringResource(R.string.input_range_error)
                        DofInputError.InvalidNumber, null ->
                            stringResource(R.string.results_unavailable)
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (inputError == DofInputError.OutOfRange) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                return@Column
            }

            val formatter = currentDistanceDisplay()
            val metersUnit = distanceUnitLabel()
            val hyperfocal = formatMeters(
                result.hyperfocalDistanceMeters,
                formatter,
                metersUnit,
            )

            val isDofMode = calculatorMode == CalculatorMode.DepthOfField
            Text(
                text = stringResource(
                    if (isDofMode) R.string.total_depth_of_field_result
                    else R.string.hyperfocal_distance_result,
                ),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = if (isDofMode) {
                    formatDistance(result.totalDepthOfField, formatter, metersUnit)
                } else {
                    hyperfocal
                },
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            if (calculatorMode == CalculatorMode.Hyperfocal) {
                Text(
                    text = stringResource(R.string.hyperfocal_instruction),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            HorizontalDivider()

            if (calculatorMode == CalculatorMode.DepthOfField) {
                BoundaryResultMetrics(
                    values = listOf(
                        stringResource(R.string.near_limit_result) to
                            formatMeters(result.nearLimitMeters, formatter, metersUnit),
                        stringResource(R.string.dof_visualization_focus) to
                            if (focusAtInfinity) "∞" else formatMeters(
                                checkNotNull(focusDistanceMeters), formatter, metersUnit),
                        stringResource(R.string.far_limit_result) to
                            formatDistance(result.farLimit, formatter, metersUnit),
                    ),
                )
                HorizontalDivider()
                InfographicMetric(
                    label = stringResource(
                        R.string.hyperfocal_distance_result,
                    ),
                    value = hyperfocal,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                BoundaryResultMetrics(
                    values = listOf(
                        stringResource(R.string.near_limit_result) to
                            formatMeters(result.nearLimitMeters, formatter, metersUnit),
                        stringResource(R.string.far_limit_result) to
                            formatDistance(result.farLimit, formatter, metersUnit),
                    ),
                )
            }
        }
    }
}

@Composable
private fun BoundaryResultMetrics(values: List<Pair<String, String>>) {
    val useColumn = currentDistanceDisplay().rounding ==
        com.example.dofcalculator.settings.DistanceRounding.None ||
        LocalConfiguration.current.fontScale > 1.2f
    if (useColumn) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            values.forEach { (label, value) ->
                InfographicMetric(label, value, Modifier.fillMaxWidth())
            }
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            values.forEach { (label, value) ->
                InfographicMetric(label, value, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun InfographicMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun FocalLengthDialog(
    uiState: DofCalculatorUiState,
    onFocalLengthChanged: (String) -> Unit,
    onStepFocalLength: (Boolean) -> Unit,
    onFocalLengthPresetSelected: (Double) -> Unit,
    onShowAllFocalLengths: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.focal_length_dialog_title)) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                FocalLengthControls(uiState, onFocalLengthChanged, onStepFocalLength,
                    onFocalLengthPresetSelected, onShowAllFocalLengths)

            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        },
    )
}

@Composable
internal fun FocalLengthControls(
    uiState: DofCalculatorUiState,
    onFocalLengthChanged: (String) -> Unit,
    onStepFocalLength: (Boolean) -> Unit,
    onFocalLengthPresetSelected: (Double) -> Unit,
    onShowAllFocalLengths: (Boolean) -> Unit = {},
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val decreaseLabel = stringResource(R.string.decrease_focal_length)
            val increaseLabel = stringResource(R.string.increase_focal_length)
            ParameterStepButton(
                text = "−",
                enabled = uiState.canDecreaseFocalLength,
                onClick = { onStepFocalLength(false) },
                modifier = Modifier.semantics { contentDescription = decreaseLabel },
            )
            DialogNumberField(
                value = uiState.focalLengthText,
                onValueChange = onFocalLengthChanged,
                label = stringResource(R.string.focal_length_label),
                isError = uiState.focalLengthHasError,
                modifier = Modifier.weight(1f),
            )
            ParameterStepButton(
                text = "+",
                enabled = uiState.canIncreaseFocalLength,
                onClick = { onStepFocalLength(true) },
                modifier = Modifier.semantics { contentDescription = increaseLabel },
            )
        }
        if (uiState.cameraLensKit.lenses.isNotEmpty()) PresetVisibilitySwitch(
            R.string.show_all_focal_lengths, uiState.cameraLensKit.showAllFocalLengths, onShowAllFocalLengths)
        Text(
            text = stringResource(R.string.focal_length_presets),
            style = MaterialTheme.typography.titleSmall,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            uiState.focalLengthPresets.forEach { value ->
                FilterChip(
                    selected =
                        uiState.selectedFocalLengthPresetMm == value,
                    onClick = {
                        onFocalLengthPresetSelected(value)
                    },
                    label = {
                        Text(
                            stringResource(
                                R.string.focal_length_preset,
                                formatPresetNumber(value, LocalResources.current.configuration.locales[0]),
                            ),
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun ApertureDialog(
    uiState: DofCalculatorUiState,
    onApertureChanged: (String) -> Unit,
    onAperturePresetSelected: (Double) -> Unit,
    onPreviousAperture: () -> Unit,
    onNextAperture: () -> Unit,
    onShowAllApertures: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.aperture_dialog_title)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ParameterStepButton(
                        text = "−",
                        enabled = uiState.canSelectPreviousAperture,
                        onClick = onPreviousAperture,
                    )
                    DialogNumberField(
                        value = uiState.apertureText,
                        onValueChange = onApertureChanged,
                        label = stringResource(R.string.aperture_label),
                        isError = uiState.apertureHasError,
                        modifier = Modifier.weight(1f),
                    )
                    ParameterStepButton(
                        text = "+",
                        enabled = uiState.canSelectNextAperture,
                        onClick = onNextAperture,
                    )
                }
                if (uiState.cameraLensKit.lenses.isNotEmpty()) PresetVisibilitySwitch(
                    R.string.show_all_apertures, uiState.cameraLensKit.showAllApertures, onShowAllApertures)
                Text(
                    text = stringResource(R.string.aperture_presets),
                    style = MaterialTheme.typography.titleSmall,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    uiState.aperturePresets.forEach { value ->
                        FilterChip(
                            selected =
                                uiState.selectedStandardAperture == value,
                            onClick = {
                                onAperturePresetSelected(value)
                            },
                            label = {
                                Text(
                                    stringResource(
                                        R.string.aperture_preset,
                                        formatPresetNumber(value, LocalResources.current.configuration.locales[0]),
                                    ),
                                )
                            },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        },
    )
}

@Composable
private fun DialogNumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isError: Boolean,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        singleLine = true,
        isError = isError,
        supportingText = if (isError) {
            { Text(stringResource(R.string.input_number_error)) }
        } else {
            null
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Decimal,
            imeAction = ImeAction.Done,
        ),
    )
}

@Composable
private fun ParameterStepButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.size(52.dp),
        enabled = enabled,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleLarge,
        )
    }
}

private fun DofCalculatorUiState.matchingCameraProfile(): CameraProfile? =
    presets.cameras.firstOrNull { it.id == presets.selectedCameraId }



private fun formatDistance(
    distance: CalculatedDistance,
    formatter: DistanceDisplay,
    metersUnit: String,
): String = when (distance) {
    is CalculatedDistance.Finite ->
        formatMeters(distance.meters, formatter, metersUnit)
    CalculatedDistance.Infinity -> "∞"
}

private fun formatMeters(
    meters: Double,
    formatter: DistanceDisplay,
    metersUnit: String,
): String = "${formatter.number(meters)} $metersUnit"

@androidx.compose.ui.tooling.preview.Preview(
    name = "DOF result", widthDp = 320, locale = "en", showBackground = true,
)
@androidx.compose.ui.tooling.preview.Preview(
    name = "DOF Russian large text", widthDp = 320, locale = "ru",
    fontScale = 1.5f, showBackground = true,
)
@Composable
private fun DofResultsPreview() {
    com.example.dofcalculator.ui.theme.DOFCalculatorTheme {
        ResultsInfographic(
            result = previewDofResult(5.0),
            focusDistanceMeters = 5.0,
            inputError = null,
            calculatorMode = CalculatorMode.DepthOfField,
            focusAtInfinity = false,
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(
    name = "Hyperfocal result", widthDp = 320, locale = "en", showBackground = true,
)
@Composable
private fun HyperfocalResultsPreview() {
    val hyperfocalMeters = com.example.dofcalculator.calculation.DepthOfFieldCalculator
        .calculateHyperfocalDistanceMeters(50.0, 8.0, 0.030)
    com.example.dofcalculator.ui.theme.DOFCalculatorTheme {
        ResultsInfographic(
            result = previewDofResult(hyperfocalMeters),
            focusDistanceMeters = hyperfocalMeters,
            inputError = null,
            calculatorMode = CalculatorMode.Hyperfocal,
            focusAtInfinity = false,
        )
    }
}

private fun previewDofResult(focusDistanceMeters: Double): DepthOfFieldResult =
    com.example.dofcalculator.calculation.DepthOfFieldCalculator.calculate(
        com.example.dofcalculator.model.DepthOfFieldInput(
            focalLengthMm = 50.0,
            aperture = 8.0,
            circleOfConfusionMm = 0.030,
            focusDistanceMeters = focusDistanceMeters,
        ),
    )
