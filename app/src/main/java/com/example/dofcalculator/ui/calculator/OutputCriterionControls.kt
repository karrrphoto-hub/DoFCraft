package com.example.dofcalculator.ui.calculator

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.dofcalculator.R
import com.example.dofcalculator.calculation.ViewingDistanceScale
import com.example.dofcalculator.model.*
import java.math.BigDecimal
import java.math.MathContext
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.ceil

@Composable
internal fun PrintCriterionControls(
    state: DofCalculatorUiState,
    onField: (SharpnessField, String) -> Unit,
    onPlacement: (PrintPlacement) -> Unit,
    onSize: (PrintSizePreset) -> Unit,
    onAcknowledge: () -> Unit,
) {
    val s = state.sharpness
    var preparation by rememberSaveable { mutableStateOf(false) }
    if (s.printModelNoticePending) {
        Text(stringResource(R.string.print_model_changed), style = MaterialTheme.typography.bodyMedium)
        TextButton(onClick = onAcknowledge) { Text(stringResource(R.string.understood)) }
    }
    CriterionDropdown(
        stringResource(R.string.print_size), printSizeLabel(s.printSize),
        PrintSizePreset.entries.map { it to printSizeLabel(it) }, onSize)
    if (s.printSize == PrintSizePreset.CUSTOM) {
        SharpnessNumber(state, SharpnessField.PRINT_WIDTH, R.string.print_width, onField)
        SharpnessNumber(state, SharpnessField.PRINT_HEIGHT, R.string.print_height, onField)
    }
    ViewingDistanceControl(state, SharpnessField.PRINT_VIEWING, onField)
    AngleControl(state, SharpnessField.PRINT_ANGLE, onField)
    PlacementControl(s.printPlacement, onPlacement)
    TextButton(onClick = { preparation = !preparation }) {
        Text(stringResource(R.string.print_preparation))
    }
    if (preparation) {
        SharpnessNumber(state, SharpnessField.PRINT_PPI, R.string.optional_print_ppi, onField)
        ValueSuggestions(listOf(150, 240, 300, 360), R.string.ppi_suggestion) {
            onField(SharpnessField.PRINT_PPI, it)
        }
        SharpnessNumber(state, SharpnessField.PRINT_DPI, R.string.optional_print_dpi, onField)
        ValueSuggestions(listOf(300, 600, 1200, 2400), R.string.dpi_suggestion) {
            onField(SharpnessField.PRINT_DPI, it)
        }
        Text(stringResource(R.string.print_metadata_help), style = MaterialTheme.typography.bodySmall)
    }
    OutputReferenceInfo(state)
}

@Composable
internal fun ScreenCriterionControls(
    state: DofCalculatorUiState,
    onField: (SharpnessField, String) -> Unit,
    onDevice: (ScreenDevice) -> Unit,
    onCriterion: (ScreenCriterion) -> Unit,
    onPlacement: (PrintPlacement) -> Unit,
) {
    val s = state.sharpness
    var advanced by rememberSaveable { mutableStateOf(false) }
    CriterionDropdown(stringResource(R.string.screen_type), stringResource(s.screenDevice.labelResource()),
        ScreenDevice.entries.map { it to stringResource(it.labelResource()) }, onDevice)
    Text(stringResource(R.string.screen_presets_note), style = MaterialTheme.typography.bodySmall)
    SharpnessNumber(state, SharpnessField.SCREEN_DIAGONAL, R.string.screen_diagonal, onField)
    SharpnessNumber(state, SharpnessField.SCREEN_WIDTH, R.string.screen_width_pixels, onField)
    SharpnessNumber(state, SharpnessField.SCREEN_HEIGHT, R.string.screen_height_pixels, onField)
    ViewingDistanceControl(state, SharpnessField.SCREEN_VIEWING, onField)
    if (s.screenCriterion == ScreenCriterion.VISUAL) {
        AngleControl(state, SharpnessField.SCREEN_ANGLE, onField)
    } else {
        Text(stringResource(R.string.display_pixels_active), style = MaterialTheme.typography.labelLarge)
    }
    PlacementControl(s.screenPlacement, onPlacement)
    TextButton(onClick = { advanced = !advanced }) {
        Text(stringResource(R.string.sharpness_advanced))
    }
    if (advanced) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ScreenCriterion.entries.forEach { criterion ->
                FilterChip(selected = s.screenCriterion == criterion,
                    onClick = { onCriterion(criterion) },
                    label = { Text(stringResource(if (criterion == ScreenCriterion.VISUAL)
                        R.string.visual_criterion else R.string.display_pixels_criterion)) })
            }
        }
        if (s.screenCriterion == ScreenCriterion.DISPLAY_PIXELS) {
            SharpnessNumber(state, SharpnessField.SCREEN_BLUR, R.string.screen_blur_pixels, onField)
        }
        Text(stringResource(R.string.screen_methods_help), style = MaterialTheme.typography.bodySmall)
    }
    OutputReferenceInfo(state)
}

@Composable
private fun ViewingDistanceControl(
    state: DofCalculatorUiState, field: SharpnessField, onField: (SharpnessField, String) -> Unit,
) {
    SharpnessNumber(state, field, R.string.viewing_distance_cm, onField)
    val current = requireNotNull(field.value(state.sharpness))
    val scale = remember(current) { ViewingDistanceScale(current) }
    val description = stringResource(R.string.viewing_distance_slider)
    Slider(
        value = scale.positionOf(current).toFloat(),
        onValueChange = { onField(field, scale.distanceAt(it.toDouble()).toString()) },
        modifier = Modifier.fillMaxWidth().semantics { contentDescription = description },
    )
}

@Composable
private fun AngleControl(
    state: DofCalculatorUiState, field: SharpnessField, onField: (SharpnessField, String) -> Unit,
) {
    val value = field.value(state.sharpness)
    var custom by rememberSaveable { mutableStateOf(false) }
    Text(stringResource(R.string.visual_criterion), style = MaterialTheme.typography.labelLarge)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for ((angle, label) in listOf(1 to R.string.angle_strict, 2 to R.string.angle_normal,
            3 to R.string.angle_relaxed)) {
            FilterChip(selected = value == angle.toDouble() &&
                state.sharpnessDrafts[field]?.let {
                    com.example.dofcalculator.calculation.SharpnessCriterionCalculator.positiveNumber(it) == value
                } != false,
                onClick = { onField(field, angle.toString()) },
                label = { Text(stringResource(label)) })
        }
    }
    Text(stringResource(R.string.angle_help), style = MaterialTheme.typography.bodySmall)
    TextButton(onClick = { custom = !custom }) { Text(stringResource(R.string.custom_viewing_angle)) }
    if (custom || value !in listOf(1.0, 2.0, 3.0)) {
        SharpnessNumber(state, field, R.string.angle_minutes, onField)
    }
}

@Composable
private fun PlacementControl(placement: PrintPlacement, onPlacement: (PrintPlacement) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PrintPlacement.entries.forEach {
            FilterChip(selected = placement == it, onClick = { onPlacement(it) },
                label = { Text(stringResource(if (it == PrintPlacement.FIT)
                    R.string.print_fit else R.string.print_fill)) })
        }
    }
    Text(stringResource(R.string.print_placement_help), style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun ValueSuggestions(values: List<Int>, label: Int, onValue: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        values.forEach { value ->
            SuggestionChip(onClick = { onValue(value.toString()) },
                label = { Text(stringResource(label, value)) })
        }
    }
}

@Composable
private fun OutputReferenceInfo(state: DofCalculatorUiState) {
    val output = state.outputSharpness ?: return
    var expanded by rememberSaveable { mutableStateOf(false) }
    TextButton(onClick = { expanded = !expanded }) { Text(stringResource(R.string.output_information)) }
    if (!expanded) return
    val locale = LocalResources.current.configuration.locales[0]
    val n: (Double) -> String = { referenceNumber(it, locale) }
    val screen = state.sharpness.mode == SharpnessCriterionMode.SCREEN_BASED
    if (screen) {
        Text(stringResource(R.string.computed_ppi, n(requireNotNull(output.screenPpi))))
        Text(stringResource(R.string.pixel_pitch_mm, n(requireNotNull(output.pixelPitchMm))))
        Text(stringResource(R.string.screen_physical_size, n(output.outputWidthMm), n(output.outputHeightMm)))
    } else {
        Text(stringResource(R.string.paper_physical_size, n(output.outputWidthMm), n(output.outputHeightMm)))
    }
    Text(stringResource(R.string.displayed_photo_size,
        n(output.geometry.visibleWidthMm), n(output.geometry.visibleHeightMm)))
    if ((if (screen) state.sharpness.screenPlacement else state.sharpness.printPlacement) == PrintPlacement.FILL) {
        Text(stringResource(R.string.uncropped_photo_size,
            n(output.geometry.imageWidthMm), n(output.geometry.imageHeightMm)))
    }
    Text(stringResource(R.string.output_blur_mm, n(output.outputBlurMm)))
    output.equivalentBlurPx?.let { Text(stringResource(R.string.equivalent_screen_blur, n(it))) }
    if (output.requiredWidthPx != null && output.requiredHeightPx != null) {
        Text(stringResource(R.string.required_file_pixels,
            n(ceil(output.requiredWidthPx)), n(ceil(output.requiredHeightPx))))
        Text(stringResource(R.string.required_file_help), style = MaterialTheme.typography.bodySmall)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun <T> CriterionDropdown(label: String, value: String, items: List<Pair<T, String>>,
                                 onSelected: (T) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(value = value, onValueChange = {}, readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth())
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            items.forEach { (item, title) ->
                DropdownMenuItem(text = { Text(title) }, onClick = { expanded = false; onSelected(item) })
            }
        }
    }
}

@Composable
private fun printSizeLabel(size: PrintSizePreset): String =
    if (size == PrintSizePreset.CUSTOM) stringResource(R.string.custom_print_size)
    else stringResource(R.string.print_size_cm, referenceNumber(requireNotNull(size.widthCm), Locale.ROOT),
        referenceNumber(requireNotNull(size.heightCm), Locale.ROOT))

private fun ScreenDevice.labelResource(): Int = when (this) {
    ScreenDevice.SMARTPHONE -> R.string.screen_smartphone
    ScreenDevice.TABLET -> R.string.screen_tablet
    ScreenDevice.MONITOR -> R.string.screen_monitor
    ScreenDevice.TV -> R.string.screen_tv
    ScreenDevice.CUSTOM -> R.string.screen_custom
}

internal fun referenceNumber(value: Double, locale: Locale): String {
    val decimal = BigDecimal.valueOf(value).round(MathContext(7)).stripTrailingZeros()
    val text = if (value != 0.0 && (value < 0.00001 || value >= 1e10)) decimal.toString()
        else decimal.toPlainString()
    return if (DecimalFormatSymbols.getInstance(locale).decimalSeparator == ',') text.replace('.', ',') else text
}
