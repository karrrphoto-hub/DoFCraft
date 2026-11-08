package com.example.dofcalculator.ui.calculator

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.dofcalculator.R
import com.example.dofcalculator.model.*
import java.math.BigDecimal
import java.text.DecimalFormatSymbols
import java.util.Locale

internal fun formatPresetNumber(value: Double, locale: Locale): String {
    val text = BigDecimal.valueOf(value).stripTrailingZeros().toPlainString()
    return if (DecimalFormatSymbols.getInstance(locale).decimalSeparator == ',') text.replace('.', ',') else text
}

@Composable
internal fun lensSummary(lens: Lens): String {
    val locale = LocalResources.current.configuration.locales[0]
    fun n(value: Double) = formatPresetNumber(value, locale)
    return when (lens) {
        is Lens.Prime -> stringResource(R.string.lens_prime_summary, n(lens.focalLengthMm), n(lens.maxAperture))
        is Lens.Zoom -> if (lens.maxApertureAtMinFocal == lens.maxApertureAtMaxFocal)
            stringResource(R.string.lens_zoom_summary, n(lens.minFocalLengthMm), n(lens.maxFocalLengthMm),
                n(lens.maxApertureAtMinFocal))
        else stringResource(R.string.lens_zoom_variable_summary, n(lens.minFocalLengthMm), n(lens.maxFocalLengthMm),
            n(lens.maxApertureAtMinFocal), n(lens.maxApertureAtMaxFocal))
    }
}

@Composable
internal fun PresetVisibilitySwitch(label: Int, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp)
        .toggleable(checked, role = Role.Switch, onValueChange = onChange),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(label), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
internal fun CameraLensDialogs(
    state: DofCalculatorUiState,
    onClose: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (String) -> Unit,
    onDelete: (String) -> Unit,
    onConfirmDelete: () -> Unit,
    onCancelDelete: () -> Unit,
    onName: (String) -> Unit,
    onZoom: (Boolean) -> Unit,
    onField: (LensField, String) -> Unit,
    onSave: () -> Unit,
    onCancelEditor: () -> Unit,
    onDismissError: () -> Unit,
) {
    val manager = state.lensManager
    val camera = state.presets.cameras.firstOrNull { it.id == manager.cameraId } ?: return
    val lenses = state.presets.lensKitsByCameraId[camera.id]?.lenses.orEmpty()
    val pending = lenses.firstOrNull { it.id == manager.pendingDeleteId }
    when {
        state.presets.storageError -> AlertDialog(
            onDismissRequest = onDismissError,
            title = { Text(stringResource(R.string.preset_storage_error_title)) },
            text = { Text(stringResource(R.string.preset_storage_error_message)) },
            confirmButton = { TextButton(onClick = onDismissError) { Text(stringResource(R.string.close)) } })
        pending != null -> AlertDialog(
            onDismissRequest = onCancelDelete,
            title = { Text(stringResource(R.string.delete_lens)) },
            text = { Text(stringResource(R.string.delete_lens_confirmation,
                pending.name.ifBlank { lensSummary(pending) })) },
            confirmButton = { TextButton(onClick = onConfirmDelete) { Text(stringResource(R.string.delete)) } },
            dismissButton = { TextButton(onClick = onCancelDelete) { Text(stringResource(R.string.cancel)) } })
        manager.editor != null -> LensEditorDialog(manager.editor, onName, onZoom, onField, onSave, onCancelEditor)
        else -> AlertDialog(
            onDismissRequest = onClose,
            title = { Column { Text(stringResource(R.string.lenses_title))
                Text(cameraNameLabel(camera), style = MaterialTheme.typography.bodyMedium) } },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.lens_kit_help), style = MaterialTheme.typography.bodySmall)
                    if (lenses.isEmpty()) Text(stringResource(R.string.no_lenses))
                    LazyColumn(Modifier.heightIn(max = 400.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(lenses, key = { it.id }) { lens ->
                            Column {
                                if (lens.name.isNotBlank()) Text(lens.name, style = MaterialTheme.typography.titleSmall,
                                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text(lensSummary(lens), style = MaterialTheme.typography.bodyMedium)
                                lens.minAperture?.let {
                                    Text(stringResource(R.string.lens_closed_value,
                                        formatPresetNumber(it, LocalResources.current.configuration.locales[0])),
                                        style = MaterialTheme.typography.bodySmall)
                                }
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TextButton(onClick = { onEdit(lens.id) }) { Text(stringResource(R.string.edit_lens)) }
                                    TextButton(onClick = { onDelete(lens.id) }) { Text(stringResource(R.string.delete_lens)) }
                                }
                                HorizontalDivider()
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = onAdd) { Text(stringResource(R.string.add_lens)) } },
            dismissButton = { TextButton(onClick = onClose) { Text(stringResource(R.string.close)) } })
    }
}

@Composable
private fun LensEditorDialog(
    editor: LensEditorUiState,
    onName: (String) -> Unit,
    onZoom: (Boolean) -> Unit,
    onField: (LensField, String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(if (editor.isNew) R.string.add_lens else R.string.edit_lens)) },
        text = { Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState())) {
            LensEditorContent(editor, onName, onZoom, onField)
        } },
        confirmButton = { TextButton(onClick = onSave, enabled = !editor.isSaving) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton(onClick = onCancel, enabled = !editor.isSaving) { Text(stringResource(R.string.cancel)) } })
}

@Composable
internal fun LensEditorContent(
    editor: LensEditorUiState,
    onName: (String) -> Unit,
    onZoom: (Boolean) -> Unit,
    onField: (LensField, String) -> Unit,
) {
    val zoom = editor.draft.type == LensType.ZOOM
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(editor.draft.name, onName, enabled = !editor.isSaving,
            label = { Text(stringResource(R.string.lens_name)) }, modifier = Modifier.fillMaxWidth())
        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(
            zoom, enabled = !editor.isSaving, role = Role.Checkbox, onValueChange = onZoom),
            verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = zoom, onCheckedChange = null)
            Text(stringResource(R.string.zoom_lens), Modifier.weight(1f))
        }
        if (zoom) {
            LensFieldPair(editor, LensField.WIDE_FOCAL, LensField.WIDE_APERTURE, onField)
            LensFieldPair(editor, LensField.TELE_FOCAL, LensField.TELE_APERTURE, onField)
        } else LensFieldPair(editor, LensField.PRIME_FOCAL, LensField.PRIME_APERTURE, onField)
        LensNumber(editor, LensField.MIN_APERTURE, onField)
        editor.validation.lens?.let { lens ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.lens_preview), style = MaterialTheme.typography.labelMedium)
                    Text(lensSummary(lens), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun LensFieldPair(
    editor: LensEditorUiState, first: LensField, second: LensField,
    onField: (LensField, String) -> Unit,
) {
    val fontScale = LocalConfiguration.current.fontScale
    BoxWithConstraints {
        if (maxWidth >= 420.dp && fontScale <= 1.2f) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LensNumber(editor, first, onField, Modifier.weight(1f))
                LensNumber(editor, second, onField, Modifier.weight(1f))
            }
        } else Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            LensNumber(editor, first, onField)
            LensNumber(editor, second, onField)
        }
    }
}

@Composable
private fun LensNumber(
    editor: LensEditorUiState, field: LensField, onField: (LensField, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val raw = editor.draft.fields[field].orEmpty()
    val error = editor.validation.errors[field]?.takeIf { editor.showErrors || raw.isNotBlank() }
    val label = when (field) {
        LensField.PRIME_FOCAL -> R.string.lens_focal
        LensField.PRIME_APERTURE -> R.string.lens_aperture
        LensField.WIDE_FOCAL -> R.string.lens_wide_focal
        LensField.WIDE_APERTURE -> R.string.lens_wide_aperture
        LensField.TELE_FOCAL -> R.string.lens_tele_focal
        LensField.TELE_APERTURE -> R.string.lens_tele_aperture
        LensField.MIN_APERTURE -> R.string.lens_min_aperture
    }
    val labelText = stringResource(label)
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(labelText, style = MaterialTheme.typography.bodySmall)
        OutlinedTextField(value = raw, onValueChange = { onField(field, it) }, enabled = !editor.isSaving,
            isError = error != null, singleLine = true,
            supportingText = { error?.let {
                Text(stringResource(when (it) {
                    LensInputError.POSITIVE_NUMBER -> R.string.input_number_error
                    LensInputError.ZOOM_RANGE -> R.string.lens_zoom_range_error
                    LensInputError.APERTURE_RANGE -> R.string.lens_aperture_range_error
                }))
            } }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = labelText })
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "Lens editor English", widthDp = 360, locale = "en")
@androidx.compose.ui.tooling.preview.Preview(name = "Lens editor Russian large", widthDp = 320, heightDp = 1050, locale = "ru", fontScale = 1.3f)
@Composable
private fun LensEditorPreview() {
    val draft = LensDraft.from(Lens.Zoom("preview", "Nikkor 18–55", 18.0, 3.5, 55.0, 5.6, 22.0))
    com.example.dofcalculator.ui.theme.DOFCalculatorTheme(dynamicColor = false) {
        Surface {
            Box(Modifier.padding(16.dp)) {
                LensEditorContent(LensEditorUiState(draft,
                    com.example.dofcalculator.calculation.LensInputValidator.validate(draft)), {}, {}, { _, _ -> })
            }
        }
    }
}
