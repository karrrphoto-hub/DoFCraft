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
import com.example.dofcalculator.model.CalculatedDistance
import com.example.dofcalculator.model.FrameFormat
import com.example.dofcalculator.model.Lens
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
    onAddLens: () -> Unit = {},
    onEditLens: (String) -> Unit = {},
    onDeleteLens: (String) -> Unit = {},
    focusDistance: CalculatedDistance? = null,
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
            onSaveCamera, onCancelCamera,
            state.lensKitsByCameraId[state.cameraEditor.cameraId]?.lenses.orEmpty(),
            onAddLens, onEditLens, onDeleteLens,
            focusDistance.takeIf { state.cameraEditor.cameraId == state.selectedCameraId })
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
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
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
                                    val lenses = state.lensKitsByCameraId[camera.id]?.lenses.orEmpty()
                                    if (lenses.isNotEmpty() || !camera.isBuiltIn) {
                                        CameraLensDescriptions(lenses,
                                            Modifier.fillMaxWidth().padding(start = 48.dp, bottom = 8.dp),
                                            focusDistance.takeIf { camera.id == state.selectedCameraId })
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
    lenses: List<Lens>,
    onAddLens: () -> Unit,
    onEditLens: (String) -> Unit,
    onDeleteLens: (String) -> Unit,
    focusDistance: CalculatedDistance? = null,
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
                HorizontalDivider()
                CameraLensesSection(lenses, canAdd = editor.cameraId != null,
                    onAdd = onAddLens, onEdit = onEditLens, onDelete = onDeleteLens,
                    focusDistance = focusDistance)
                if (editor.cameraId == null)
                    Text(stringResource(R.string.lens_save_camera_first), style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = { TextButton(onClick = onSave) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) } },
    )
}

@androidx.compose.ui.tooling.preview.Preview(name = "Camera editor English", widthDp = 360, heightDp = 900, locale = "en")
@androidx.compose.ui.tooling.preview.Preview(name = "Camera editor Russian large dark", widthDp = 320, heightDp = 1000,
    locale = "ru", fontScale = 1.3f, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CameraEditorLensesPreview() {
    com.example.dofcalculator.ui.theme.DOFCalculatorTheme(dynamicColor = false) {
        CameraEditorDialog(
            editor = CameraEditorUiState(cameraId = "preview", nameText = "Yashica Mat 124G",
                frameFormat = FrameFormat.MediumFormat6x6),
            onName = {}, onFormat = {}, onDimensions = { _, _ -> }, onCustomFormat = {},
            onSave = {}, onCancel = {},
            lenses = listOf(Lens.Prime("preview-lens", "Yashinon", 80.0, 3.5, 22.0)),
            onAddLens = {}, onEditLens = {}, onDeleteLens = {},
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "Camera picker English", widthDp = 360, heightDp = 900, locale = "en")
@androidx.compose.ui.tooling.preview.Preview(name = "Camera picker Russian large dark", widthDp = 320, heightDp = 1000,
    locale = "ru", fontScale = 1.3f, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CameraPickerLensesPreview() {
    val camera = CameraProfile("preview", "Pentax 17", FrameFormat.Custom,
        customFormatName = "Half frame film", widthMm = 17.0, heightMm = 24.0)
    val kit = com.example.dofcalculator.model.CameraLensKit(listOf(
        Lens.Prime("prime", "HD Pentax", 25.0, 3.5),
        Lens.Zoom("zoom", minFocalLengthMm = 24.0, maxFocalLengthMm = 120.0,
            maxApertureAtMinFocal = 4.0, maxApertureAtMaxFocal = 4.0)))
    com.example.dofcalculator.ui.theme.DOFCalculatorTheme(dynamicColor = false) {
        PresetManagementDialogs(
            state = PresetManagerUiState(cameras = listOf(camera), selectedCameraId = camera.id,
                isManagerVisible = true, lensKitsByCameraId = mapOf(camera.id to kit)),
            onCloseManager = {}, onSelectCamera = {}, onCustomFormatChanged = {},
            onConfirmDelete = {}, onCancelDelete = {}, onCreateCamera = {}, onEditCamera = {},
            onDeleteCamera = {}, onCameraNameChanged = {}, onCameraFrameFormatSelected = {},
            onCameraDimensionsChanged = { _, _ -> }, onSaveCamera = {}, onCancelCamera = {},
            onDismissStorageError = {},
        )
    }
}
