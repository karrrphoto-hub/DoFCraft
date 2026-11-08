package com.example.dofcalculator.ui.calculator

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.dofcalculator.R
import com.example.dofcalculator.calculation.SharpnessCriterionCalculator
import com.example.dofcalculator.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SharpnessCriterionSheet(
    state: DofCalculatorUiState,
    onDismiss: () -> Unit,
    onMode: (SharpnessCriterionMode) -> Unit,
    onField: (SharpnessField, String) -> Unit,
    onPlacement: (PrintPlacement) -> Unit,
    onImport: (String?) -> Unit,
    onPrintSize: (PrintSizePreset) -> Unit = {},
    onScreenDevice: (ScreenDevice) -> Unit = {},
    onScreenCriterion: (ScreenCriterion) -> Unit = {},
    onScreenPlacement: (PrintPlacement) -> Unit = {},
    onAcknowledgePrintModel: () -> Unit = {},
    onVideoResolution: (VideoResolution) -> Unit = {},
    onVideoField: (VideoField, String) -> Unit = { _, _ -> },
    onVideoAreaMode: (VideoAreaMode) -> Unit = {},
) {
    ModalBottomSheet(onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        SharpnessCriterionContent(state, onMode, onField, onPlacement, onImport,
            onPrintSize, onScreenDevice, onScreenCriterion, onScreenPlacement, onAcknowledgePrintModel,
            onVideoResolution, onVideoField, onVideoAreaMode)
    }
}

@Composable
private fun SharpnessCriterionContent(
    state: DofCalculatorUiState,
    onMode: (SharpnessCriterionMode) -> Unit,
    onField: (SharpnessField, String) -> Unit,
    onPlacement: (PrintPlacement) -> Unit,
    onImport: (String?) -> Unit,
    onPrintSize: (PrintSizePreset) -> Unit = {},
    onScreenDevice: (ScreenDevice) -> Unit = {},
    onScreenCriterion: (ScreenCriterion) -> Unit = {},
    onScreenPlacement: (PrintPlacement) -> Unit = {},
    onAcknowledgePrintModel: () -> Unit = {},
    onVideoResolution: (VideoResolution) -> Unit = {},
    onVideoField: (VideoField, String) -> Unit = { _, _ -> },
    onVideoAreaMode: (VideoAreaMode) -> Unit = {},
) {
    var showInfo by rememberSaveable { mutableStateOf(false) }
    var pixelAdvanced by rememberSaveable { mutableStateOf(false) }
    val mode = state.sharpness.mode
    val infoDescription = stringResource(R.string.sharpness_info)
    val scrollBoundary = remember {
        object : NestedScrollConnection {
            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                // An upward fling at the end of the list must not restart the sheet spring.
                // Downward velocity still reaches Material 3 for swipe-to-dismiss.
                return Velocity(0f, available.y.coerceAtMost(0f))
            }
        }
    }
    // Fill the available sheet height; a fractional child height feeds changing insets back into anchors.
    Column(Modifier.fillMaxWidth().fillMaxHeight().nestedScroll(scrollBoundary).padding(horizontal = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.sharpness_title),
                modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
            IconButton(onClick = { showInfo = true },
                modifier = Modifier.semantics { contentDescription = infoDescription }) {
                Text("ⓘ", style = MaterialTheme.typography.titleLarge)
            }
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SharpnessCriterionMode.displayOrder.forEach { method ->
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 56.dp)
                        .selectable(selected = mode == method, role = Role.RadioButton,
                            onClick = { onMode(method) }).padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = mode == method, onClick = null)
                    Column(Modifier.weight(1f).padding(start = 8.dp)) {
                        Text(stringResource(method.labelResource()), style = MaterialTheme.typography.titleSmall)
                        Text(stringResource(method.descriptionResource()),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            HorizontalDivider()
            when (mode) {
                SharpnessCriterionMode.STANDARD -> {
                    Text(stringResource(R.string.standard_help), style = MaterialTheme.typography.bodyMedium)
                    if (state.standardCoC?.source == StandardCoCSource.DIAGONAL_FALLBACK) {
                        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
                            Text(stringResource(R.string.standard_diagonal_fallback), Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                SharpnessCriterionMode.PIXEL_BASED -> {
                    SharpnessNumber(state, SharpnessField.MEGAPIXELS, R.string.image_resolution, onField)
                    Text(stringResource(R.string.acceptable_blur), style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (k in 1..3) {
                            FilterChip(selected = state.sharpness.pixelBlur == k.toDouble() &&
                                state.sharpnessDrafts[SharpnessField.PIXEL_BLUR]?.let {
                                    SharpnessCriterionCalculator.positiveNumber(it) == k.toDouble()
                                } != false,
                                onClick = { onField(SharpnessField.PIXEL_BLUR, k.toString()) },
                                label = { Text(stringResource(R.string.blur_pixels, k.toString())) })
                        }
                    }
                    TextButton(onClick = { pixelAdvanced = !pixelAdvanced }) {
                        Text(stringResource(R.string.sharpness_advanced))
                    }
                    if (pixelAdvanced || state.sharpness.pixelBlur !in listOf(1.0, 2.0, 3.0)) {
                        SharpnessNumber(state, SharpnessField.PIXEL_BLUR, R.string.blur_pixels_label, onField)
                    }
                }
                SharpnessCriterionMode.VIDEO_BASED -> VideoBasedSettingsContent(
                    state, onVideoResolution, onVideoField, onVideoAreaMode)
                SharpnessCriterionMode.PRINT_BASED -> PrintCriterionControls(
                    state, onField, onPlacement, onPrintSize, onAcknowledgePrintModel)
                SharpnessCriterionMode.SCREEN_BASED -> ScreenCriterionControls(
                    state, onField, onScreenDevice, onScreenCriterion, onScreenPlacement)
                SharpnessCriterionMode.CUSTOM -> {
                    SharpnessNumber(state, SharpnessField.CUSTOM_COC, R.string.custom_coc_label, onField)
                    if (state.legacyCoCByCameraId.isNotEmpty() || state.legacyCalculatorCoCmm != null) {
                        Text(stringResource(R.string.legacy_coc_title), style = MaterialTheme.typography.labelLarge)
                        state.legacyCalculatorCoCmm?.let { value ->
                            TextButton(onClick = { onImport(null) }) {
                                Text(stringResource(R.string.legacy_calculator_coc_import,
                                    formatCoC(value, LocalResources.current.configuration.locales[0])))
                            }
                        }
                        state.legacyCoCByCameraId.forEach { (id, value) ->
                            val camera = state.presets.cameras.firstOrNull { it.id == id }
                            TextButton(onClick = { onImport(id) }) {
                                Text(stringResource(R.string.legacy_coc_import,
                                    camera?.let { cameraNameLabel(it) } ?: id,
                                    formatCoC(value, LocalResources.current.configuration.locales[0])))
                            }
                        }
                    }
                }
            }
            if (state.effectiveCoCMm == null) {
                Text(stringResource(R.string.sharpness_invalid),
                    color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(12.dp))
        }
        HorizontalDivider()
        Text(stringResource(R.string.calculated_coc,
            if (mode == SharpnessCriterionMode.VIDEO_BASED && state.effectiveCoCMm != null)
                referenceNumber(state.effectiveCoCMm, LocalResources.current.configuration.locales[0])
            else formatCoC(state.effectiveCoCMm, LocalResources.current.configuration.locales[0])),
            modifier = Modifier.padding(top = 12.dp), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.sharpness_stricter),
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
            style = MaterialTheme.typography.bodySmall)
    }
    if (showInfo) {
        AlertDialog(onDismissRequest = { showInfo = false },
            title = { Text(stringResource(mode.labelResource())) },
            text = { Text(stringResource(mode.helpResource()),
                modifier = Modifier.verticalScroll(rememberScrollState())) },
            confirmButton = { TextButton(onClick = { showInfo = false }) {
                Text(stringResource(R.string.close))
            } })
    }
}

@Composable
internal fun SharpnessNumber(
    state: DofCalculatorUiState, field: SharpnessField, label: Int,
    onField: (SharpnessField, String) -> Unit,
) {
    val text = state.sharpnessDrafts[field] ?: field.value(state.sharpness)?.toString().orEmpty()
    val number = SharpnessCriterionCalculator.positiveNumber(text)
    val referenceOverflow = field == SharpnessField.PRINT_PPI && number != null &&
        state.sharpness.mode == SharpnessCriterionMode.PRINT_BASED &&
        state.outputSharpness?.let { it.requiredWidthPx == null || it.requiredHeightPx == null } == true
    val invalidCombination = state.sharpnessCalculationFailed &&
        field in SharpnessField.active(state.sharpness)
    val invalid = !(field.optional && text.isBlank()) &&
        (!field.isValid(number) || referenceOverflow || invalidCombination)
    OutlinedTextField(
        value = text, onValueChange = { onField(field, it) },
        label = { Text(stringResource(label)) }, isError = invalid,
        supportingText = { if (invalid) Text(stringResource(
            if (number != null) R.string.output_range_error else R.string.input_number_error)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true, modifier = Modifier.fillMaxWidth(),
    )
}

private fun SharpnessCriterionMode.descriptionResource(): Int = when (this) {
    SharpnessCriterionMode.STANDARD -> R.string.standard_description
    SharpnessCriterionMode.PIXEL_BASED -> R.string.pixel_description
    SharpnessCriterionMode.VIDEO_BASED -> R.string.video_description
    SharpnessCriterionMode.PRINT_BASED -> R.string.print_description
    SharpnessCriterionMode.SCREEN_BASED -> R.string.screen_description
    SharpnessCriterionMode.CUSTOM -> R.string.custom_description
}
private fun SharpnessCriterionMode.helpResource(): Int = when (this) {
    SharpnessCriterionMode.STANDARD -> R.string.standard_help
    SharpnessCriterionMode.PIXEL_BASED -> R.string.pixel_help
    SharpnessCriterionMode.VIDEO_BASED -> R.string.video_help
    SharpnessCriterionMode.PRINT_BASED -> R.string.print_help
    SharpnessCriterionMode.SCREEN_BASED -> R.string.screen_help
    SharpnessCriterionMode.CUSTOM -> R.string.custom_help
}

@androidx.compose.ui.tooling.preview.Preview(name = "Sharpness English", widthDp = 360, heightDp = 800, locale = "en")
@androidx.compose.ui.tooling.preview.Preview(name = "Sharpness Russian large font", widthDp = 320, heightDp = 800, locale = "ru", fontScale = 1.3f)
@Composable
private fun SharpnessCriterionPreview() {
    OutputCriterionPreview(SharpnessCriterionMode.PRINT_BASED)
}

@androidx.compose.ui.tooling.preview.Preview(name = "Screen English", widthDp = 360, heightDp = 800, locale = "en")
@androidx.compose.ui.tooling.preview.Preview(name = "Screen Russian large font", widthDp = 320, heightDp = 800, locale = "ru", fontScale = 1.3f)
@Composable
private fun ScreenCriterionPreview() {
    OutputCriterionPreview(SharpnessCriterionMode.SCREEN_BASED)
}

@androidx.compose.ui.tooling.preview.Preview(
    name = "Standard fallback English", widthDp = 360, heightDp = 1000, locale = "en")
@androidx.compose.ui.tooling.preview.Preview(
    name = "Standard fallback Russian large font", widthDp = 320, heightDp = 1200,
    locale = "ru", fontScale = 1.3f)
@Composable
private fun StandardFallbackPreview() {
    val result = StandardCoCResult(.020392777756239697, StandardCoCSource.DIAGONAL_FALLBACK)
    com.example.dofcalculator.ui.theme.DOFCalculatorTheme {
        Surface {
            SharpnessCriterionContent(
                DofCalculatorUiState(standardCoC = result, effectiveCoCMm = result.coCmm),
                {}, { _, _ -> }, {}, {},
            )
        }
    }
}

@Composable
private fun OutputCriterionPreview(mode: SharpnessCriterionMode) {
    val settings = SharpnessSettings(mode = mode)
    val output = if (mode == SharpnessCriterionMode.PRINT_BASED)
        com.example.dofcalculator.calculation.OutputSharpnessCalculator.print(36.0, 24.0, settings)
    else com.example.dofcalculator.calculation.OutputSharpnessCalculator.screen(36.0, 24.0, settings)
    com.example.dofcalculator.ui.theme.DOFCalculatorTheme {
        Surface {
            SharpnessCriterionContent(
                DofCalculatorUiState(sharpness = settings, outputSharpness = output,
                    effectiveCoCMm = output.effectiveCoCmm),
                {}, { _, _ -> }, {}, {},
            )
        }
    }
}
