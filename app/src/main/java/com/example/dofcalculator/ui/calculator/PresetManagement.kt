package com.example.dofcalculator.ui.calculator

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.dofcalculator.R
import com.example.dofcalculator.model.CameraProfile
import com.example.dofcalculator.model.FrameFormat
import java.util.Locale

@Composable
internal fun cameraFormatLabel(camera: CameraProfile): String =
    if (camera.frameFormat == FrameFormat.Custom) camera.customFormatName
    else stringResource(camera.frameFormat.labelResource())

@Composable
internal fun cameraNameLabel(camera: CameraProfile): String =
    if (camera.isBuiltIn && camera.id.startsWith("builtin."))
        stringResource(camera.frameFormat.labelResource())
    else camera.name

@Composable
internal fun PresetManagementDialogs(
    state: PresetManagerUiState,
    onCloseManager: () -> Unit,
    onSelectCamera: (String) -> Unit,
    onCustomFormatChanged: (String) -> Unit,
    onConfirmDelete: () -> Unit,
    onCancelDelete: () -> Unit,
    onCreateCamera: () -> Unit,
    onEditCamera: (String) -> Unit,
    onDeleteCamera: (String) -> Unit,
    onCameraNameChanged: (String) -> Unit,
    onCameraFrameFormatSelected: (FrameFormat) -> Unit,
    onCameraDimensionsChanged: (Boolean, String) -> Unit,
    onSaveCamera: () -> Unit,
    onCancelCamera: () -> Unit,
    onDismissStorageError: () -> Unit,
    onOpenLenses: (String) -> Unit = {},
) {
    when {
        state.storageError -> AlertDialog(
            onDismissRequest = onDismissStorageError,
            title = { Text(stringResource(R.string.preset_storage_error_title)) },
            text = { Text(stringResource(R.string.preset_storage_error_message)) },
            confirmButton = { TextButton(onClick = onDismissStorageError) {
                Text(stringResource(R.string.close))
            } },
        )
        state.pendingDeleteId != null -> AlertDialog(
            onDismissRequest = onCancelDelete,
            title = { Text(stringResource(R.string.delete_camera)) },
            text = { Text(stringResource(R.string.delete_camera_confirmation,
                state.cameras.firstOrNull { it.id == state.pendingDeleteId }?.name.orEmpty())) },
            confirmButton = { TextButton(onClick = onConfirmDelete) {
                Text(stringResource(R.string.delete))
            } },
            dismissButton = { TextButton(onClick = onCancelDelete) {
                Text(stringResource(R.string.cancel))
            } },
        )
        state.cameraEditor != null -> CameraEditorDialog(
            state.cameraEditor, onCameraNameChanged, onCameraFrameFormatSelected,
            onCameraDimensionsChanged, onCustomFormatChanged,
            onSaveCamera, onCancelCamera, onOpenLenses)
        state.isManagerVisible -> {
            var query by rememberSaveable { mutableStateOf("") }
            val names = state.cameras.map { camera ->
                Triple(camera, cameraNameLabel(camera), cameraFormatLabel(camera))
            }
            val filtered = names.filter { (_, name, format) ->
                name.contains(query, ignoreCase = true) || format.contains(query, ignoreCase = true)
            }
            AlertDialog(
                onDismissRequest = onCloseManager,
                title = { Text(stringResource(R.string.select_camera)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = query, onValueChange = { query = it },
                            label = { Text(stringResource(R.string.search_cameras)) },
                            singleLine = true, modifier = Modifier.fillMaxWidth())
                        LazyColumn(
                            state = rememberLazyListState(),
                            modifier = Modifier.heightIn(max = 400.dp),
                        ) {
                            items(filtered, key = { it.first.id }) { (camera, name, format) ->
                                Column(Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)
                                            .selectable(
                                                selected = state.selectedCameraId == camera.id,
                                                role = Role.RadioButton,
                                                onClick = { onSelectCamera(camera.id); onCloseManager() })
                                            .padding(vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        RadioButton(selected = state.selectedCameraId == camera.id, onClick = null)
                                        Column(Modifier.weight(1f)) {
                                            Text(name, style = MaterialTheme.typography.bodyMedium)
                                            Text(
                                                format,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    TextButton(onClick = { onOpenLenses(camera.id) },
                                        modifier = Modifier.padding(start = 24.dp)) {
                                        Text(stringResource(R.string.lenses_title))
                                    }
                                    if (!camera.isBuiltIn) {
                                        FlowRow(
                                            modifier = Modifier.fillMaxWidth().padding(start = 24.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        ) {
                                            TextButton(onClick = { onEditCamera(camera.id) }) {
                                                Text(stringResource(R.string.edit))
                                            }
                                            TextButton(onClick = { onDeleteCamera(camera.id) }) {
                                                Text(stringResource(R.string.delete))
                                            }
                                        }
                                    }
                                }
                                HorizontalDivider()
                            }
                        }
                    }
                },
                confirmButton = { TextButton(onClick = onCreateCamera) {
                    Text(stringResource(R.string.add_camera))
                } },
                dismissButton = { TextButton(onClick = onCloseManager) {
                    Text(stringResource(R.string.close))
                } },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CameraEditorDialog(
    editor: CameraEditorUiState,
    onName: (String) -> Unit,
    onFormat: (FrameFormat) -> Unit,
    onDimensions: (Boolean, String) -> Unit,
    onCustomFormat: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onOpenLenses: (String) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(if (editor.cameraId == null) R.string.add_camera else R.string.edit_camera)) },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (editor.cameraId != null) TextButton(onClick = { onOpenLenses(editor.cameraId) }) {
                    Text(stringResource(R.string.lenses_title))
                } else Text(stringResource(R.string.lens_save_camera_first), style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(value = editor.nameText, onValueChange = onName,
                    label = { Text(stringResource(R.string.camera_name)) },
                    isError = editor.nameHasError, modifier = Modifier.fillMaxWidth())
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = stringResource(editor.frameFormat.labelResource()),
                        onValueChange = {}, readOnly = true,
                        label = { Text(stringResource(R.string.frame_format_label)) },
                        modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) })
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        FrameFormat.entries.forEach { format ->
                            DropdownMenuItem(text = { Text(stringResource(format.labelResource())) },
                                onClick = { expanded = false; onFormat(format) })
                        }
                    }
                }
                if (editor.frameFormat == FrameFormat.Custom) {
                    OutlinedTextField(value = editor.customFormatName, onValueChange = onCustomFormat,
                        label = { Text(stringResource(R.string.custom_format_name)) },
                        isError = editor.customFormatHasError, modifier = Modifier.fillMaxWidth())
                }
                if (editor.frameFormat == FrameFormat.Custom) {
                    OutlinedTextField(value = editor.widthText,
                        onValueChange = { onDimensions(true, it) },
                        label = { Text(stringResource(R.string.sensor_width)) },
                        isError = editor.dimensionsHaveError,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = editor.heightText,
                        onValueChange = { onDimensions(false, it) },
                        label = { Text(stringResource(R.string.sensor_height)) },
                        isError = editor.dimensionsHaveError,
                        supportingText = { if (editor.dimensionsHaveError)
                            Text(stringResource(R.string.input_number_error)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth())
                }
            }
        },
        confirmButton = { TextButton(onClick = onSave) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) } },
    )
}
