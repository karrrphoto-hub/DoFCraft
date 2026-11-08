package com.example.dofcalculator.ui.calculator

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.dofcalculator.R
import com.example.dofcalculator.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DiffractionDialogs(
    state: DofCalculatorUiState,
    onCloseInfo: () -> Unit,
    onCloseMagnification: () -> Unit,
    onField: (MacroInputField, String) -> Unit,
    onPreset: (String) -> Unit,
    onApply: () -> Unit,
    onConfirm: () -> Unit,
    onCancelConfirmation: () -> Unit,
    onRestore: () -> Unit,
    onFocalLengthChanged: (String) -> Unit,
    onStepFocalLength: (Boolean) -> Unit,
    onFocalLengthPresetSelected: (Double) -> Unit,
    onShowAllFocalLengths: (Boolean) -> Unit = {},
) {
    if (state.isMagnificationSettingsVisible) {
        MagnificationSettingsSheet(state, onCloseMagnification, onField,
            onPreset, onApply, onConfirm, onCancelConfirmation, onRestore,
            onFocalLengthChanged, onStepFocalLength, onFocalLengthPresetSelected, onShowAllFocalLengths)
    } else if (state.isDiffractionInfoVisible) {
        ModalBottomSheet(onDismissRequest = onCloseInfo,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.diffraction_title), style = MaterialTheme.typography.headlineSmall)
                DiffractionDetails(state)
                Text(stringResource(R.string.diffraction_explanation), style = MaterialTheme.typography.bodySmall)
                ModelHelp()
                TextButton(onClick = onCloseInfo) { Text(stringResource(R.string.close)) }
            }
        }
    }
}

@Composable
private fun DiffractionDetails(state: DofCalculatorUiState) {
    val locale = LocalResources.current.configuration.locales[0]
    fun number(value: Double) = diffractionNumber(value, locale)
    val result = state.diffractionResult
    Text(stringResource(R.string.magnification_value, magnificationLabel(state.magnificationEstimate)))
    Text(stringResource(R.string.magnification_source, stringResource(
        if (state.magnificationEstimate.source == MagnificationMode.MANUAL)
            R.string.magnification_manual else R.string.magnification_auto)))
    MagnificationStatus(state.magnificationEstimate)
    if (result == null) {
        Text(stringResource(R.string.diffraction_no_result))
    } else {
        Text(stringResource(R.string.diffraction_nominal, number(result.nominalAperture)))
        Text(stringResource(R.string.diffraction_effective, number(result.effectiveAperture)))
        Text(stringResource(R.string.diffraction_airy, diffractionMicrons(result.airyDiameterMm, locale)))
        Text(stringResource(R.string.diffraction_coc, diffractionMicrons(result.effectiveCoCmm, locale)))
        Text(stringResource(R.string.diffraction_ratio, number(result.ratio)))
        Text(stringResource(R.string.diffraction_criterion, stringResource(state.sharpness.mode.labelResource())))
        if (result.level == DiffractionLevel.NONE) Text(stringResource(R.string.diffraction_no_warning))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MagnificationSettingsSheet(
    state: DofCalculatorUiState,
    onDismiss: () -> Unit,
    onField: (MacroInputField, String) -> Unit,
    onPreset: (String) -> Unit,
    onApply: () -> Unit,
    onConfirm: () -> Unit,
    onCancelConfirmation: () -> Unit,
    onRestore: () -> Unit,
    onFocalLengthChanged: (String) -> Unit,
    onStepFocalLength: (Boolean) -> Unit,
    onFocalLengthPresetSelected: (Double) -> Unit,
    onShowAllFocalLengths: (Boolean) -> Unit = {},
) {
    ModalBottomSheet(onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        MacroSettingsContent(state, onDismiss, onField, onPreset, onApply, onRestore,
            onFocalLengthChanged, onStepFocalLength, onFocalLengthPresetSelected, onShowAllFocalLengths)
    }
    state.macroForm.pendingConfirmation?.let { preview ->
        AlertDialog(onDismissRequest = onCancelConfirmation,
            title = { Text(stringResource(R.string.macro_check)) },
            text = { Text(unusualMacroText(preview), Modifier.verticalScroll(rememberScrollState())) },
            confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.macro_confirm)) } },
            dismissButton = { TextButton(onClick = onCancelConfirmation) { Text(stringResource(R.string.cancel)) } })
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MacroSettingsContent(
    state: DofCalculatorUiState, onDismiss: () -> Unit,
    onField: (MacroInputField, String) -> Unit, onPreset: (String) -> Unit,
    onApply: () -> Unit, onRestore: () -> Unit,
    onFocalLengthChanged: (String) -> Unit,
    onStepFocalLength: (Boolean) -> Unit,
    onFocalLengthPresetSelected: (Double) -> Unit,
    onShowAllFocalLengths: (Boolean) -> Unit = {},
) {
    val form = state.macroForm
    val locale = LocalResources.current.configuration.locales[0]
    Column(Modifier.fillMaxWidth().fillMaxHeight(.92f).imePadding().padding(horizontal = 20.dp)) {
        Text(stringResource(R.string.magnification_title), Modifier.padding(vertical = 12.dp),
            style = MaterialTheme.typography.headlineSmall)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            FocalLengthControls(state, onFocalLengthChanged, onStepFocalLength, onFocalLengthPresetSelected, onShowAllFocalLengths)
            Text(stringResource(R.string.macro_shared_focal), style = MaterialTheme.typography.bodySmall)
            HorizontalDivider()
            Text(stringResource(R.string.macro_actual), style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("1:10", "1:4", "1:2", "1:1", "2:1", "5:1").forEach { ratio ->
                    OutlinedButton(onClick = { onPreset(ratio) }, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(ratio)
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MacroNumber(form, MacroInputField.NUMERATOR, form.numerator, R.string.macro_a,
                    Modifier.weight(1f), onField)
                Text(":", Modifier.align(androidx.compose.ui.Alignment.CenterVertically))
                MacroNumber(form, MacroInputField.DENOMINATOR, form.denominator, R.string.macro_b,
                    Modifier.weight(1f), onField)
            }
            MacroNumber(form, MacroInputField.DISTANCE, form.distanceMeters,
                R.string.macro_distance_m, Modifier.fillMaxWidth(), onField)
            Text(stringResource(R.string.macro_sensor_plane), style = MaterialTheme.typography.bodySmall)
            if (form.validation.opticalInputError)
                Text(stringResource(R.string.macro_invalid_optics), color = MaterialTheme.colorScheme.error)
            form.validation.preview?.let { preview ->
                MacroInformationCard(preview)
                if (preview.unusual) {
                    Text(stringResource(R.string.macro_check), style = MaterialTheme.typography.titleSmall)
                    Text(unusualMacroText(preview), style = MaterialTheme.typography.bodySmall)
                }
            }
            state.diffractionSettings.manualDistanceMeters?.let { distance ->
                Text(stringResource(R.string.macro_saved_distance, referenceNumber(distance, locale)),
                    style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = onRestore) { Text(stringResource(R.string.macro_restore)) }
            }
            ModelHelp()
            Spacer(Modifier.height(12.dp))
        }
        HorizontalDivider()
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            FilledTonalButton(onClick = onApply, enabled = form.canApply) { Text(stringResource(R.string.macro_apply)) }
        }
    }
}

@Composable
private fun MacroNumber(form: MacroFormUiState, field: MacroInputField, value: String, label: Int,
                        modifier: Modifier, onField: (MacroInputField, String) -> Unit) {
    val error = form.validation.errors[field]
    OutlinedTextField(value, { onField(field, it) }, modifier = modifier,
        label = { Text(stringResource(label), maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        isError = error != null, singleLine = true,
        supportingText = if (error == null) null else {{ Text(stringResource(
            if (error == MacroInputError.POSITIVE_NUMBER) R.string.macro_invalid_field
            else R.string.macro_numeric_range)) }})
}

@Composable
private fun MacroInformationCard(preview: ManualMacroPreview) {
    val locale = LocalResources.current.configuration.locales[0]
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.macro_nominal_focal, referenceNumber(preview.nominalFocalLengthMm, locale)))
            Text(stringResource(R.string.macro_equivalent_focal, referenceNumber(preview.equivalentFocalLengthMm, locale)))
            Text(stringResource(R.string.macro_decimal_value, referenceNumber(preview.magnification, locale)))
            Text(stringResource(R.string.diffraction_effective, referenceNumber(preview.effectiveAperture, locale)))
            Text(stringResource(R.string.magnification_source, stringResource(R.string.magnification_manual)))
            Text(stringResource(R.string.macro_equivalent_help), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun unusualMacroText(preview: ManualMacroPreview): String {
    val locale = LocalResources.current.configuration.locales[0]
    return stringResource(R.string.macro_unusual, referenceNumber(preview.equivalentFocalLengthMm, locale),
        referenceNumber(preview.nominalFocalLengthMm, locale), referenceNumber(preview.focalRatio, locale))
}

@Composable
private fun ModelHelp() {
    var expanded by rememberSaveable { mutableStateOf(false) }
    TextButton(onClick = { expanded = !expanded }) { Text(stringResource(R.string.magnification_help)) }
    if (expanded) {
        Text(stringResource(R.string.macro_model_help), style = MaterialTheme.typography.bodySmall)
        Text(stringResource(R.string.magnification_model_help), style = MaterialTheme.typography.bodySmall)
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "Manual macro English", widthDp = 360, heightDp = 740, locale = "en")
@androidx.compose.ui.tooling.preview.Preview(name = "Manual macro Russian large dark", widthDp = 320, heightDp = 740,
    locale = "ru", fontScale = 1.5f, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ManualMacroSettingsPreview() {
    val form = MacroFormUiState("1", "1", "0.3",
        MacroInputValidation(ManualMacroPreview(1.0, .3, 100.0, 75.0, .75, 16.0, false)))
    com.example.dofcalculator.ui.theme.DOFCalculatorTheme(dynamicColor = false) {
        Surface {
            MacroSettingsContent(
                state = DofCalculatorUiState(focalLengthText = "100",
                    selectedFocalLengthPresetMm = null, macroForm = form),
                onDismiss = {}, onField = { _, _ -> }, onPreset = {},
                onApply = {}, onRestore = {}, onFocalLengthChanged = {},
                onStepFocalLength = {}, onFocalLengthPresetSelected = {},
            )
        }
    }
}
