package com.example.dofcalculator.ui.calculator

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.dofcalculator.R
import com.example.dofcalculator.model.LensDistanceUnit
import com.example.dofcalculator.settings.lensDistanceUnit
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.tooling.preview.Preview
import com.example.dofcalculator.model.CameraLensKit
import com.example.dofcalculator.model.Lens
import com.example.dofcalculator.ui.theme.DOFCalculatorTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LensProfileSelector(state: DofCalculatorUiState, onSelect: (String) -> Unit) {
    val kit = state.cameraLensKit
    if (kit.lenses.isEmpty()) return
    var expanded by remember { mutableStateOf(false) }
    val label = stringResource(R.string.choose_lens)
    val selected = state.selectedLens
    ExposedDropdownMenuBox(expanded = expanded && !kit.ignoreLensProfile,
        onExpandedChange = { if (!kit.ignoreLensProfile) expanded = it }) {
        OutlinedTextField(
            value = selected?.let { it.name.ifBlank { lensSummary(it) } }.orEmpty(),
            onValueChange = {}, readOnly = true, enabled = !kit.ignoreLensProfile,
            label = { Text(label) }, maxLines = 2,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            supportingText = {
                if (kit.ignoreLensProfile) Text(stringResource(R.string.lens_profile_ignored))
                else selected?.let {
                    val scale = it.distanceScaleUnit ?: state.distanceUnit.lensDistanceUnit()
                    Text(stringResource(R.string.lens_profile_details, lensSummary(it),
                        stringResource(if (scale == LensDistanceUnit.Feet)
                            R.string.lens_scale_feet else R.string.lens_scale_meters)))
                }
            },
            modifier = Modifier.fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .semantics { contentDescription = label },
        )
        ExposedDropdownMenu(expanded = expanded && !kit.ignoreLensProfile,
            onDismissRequest = { expanded = false }) {
            kit.lenses.forEach { lens ->
                DropdownMenuItem(text = {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        if (lens.name.isNotBlank()) Text(lens.name)
                        val scale = lens.distanceScaleUnit ?: state.distanceUnit.lensDistanceUnit()
                        Text(stringResource(R.string.lens_profile_details, lensSummary(lens),
                            stringResource(if (scale == LensDistanceUnit.Feet)
                                R.string.lens_scale_feet else R.string.lens_scale_meters)),
                            style = MaterialTheme.typography.bodySmall)
                    }
                }, onClick = { expanded = false; onSelect(lens.id) })
            }
        }
    }
}

@Preview(name = "Focal lens profile English", widthDp = 360, heightDp = 700, locale = "en")
@Preview(name = "Focal lens profile Russian", widthDp = 320, heightDp = 850,
    locale = "ru", fontScale = 1.3f)
@Composable
private fun FocalLensProfilePreview() {
    val lens = Lens.Zoom("zoom", "Nikkor 24–70 f/2.8", 24.0, 2.8, 70.0, 2.8,
        22.0, LensDistanceUnit.Feet)
    val presets = PresetManagerUiState(selectedCameraId = "preview-camera")
    val state = DofCalculatorUiState(
        focalLengthPresets = listOf(24.0, 28.0, 35.0, 50.0, 70.0),
        presets = presets.copy(lensKitsByCameraId = mapOf("preview-camera" to
            CameraLensKit(listOf(lens), selectedLensId = lens.id))))
    DOFCalculatorTheme(dynamicColor = false) {
        Surface {
            Column(Modifier.padding(24.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(stringResource(R.string.focal_length_dialog_title),
                    style = MaterialTheme.typography.headlineSmall)
                FocalLengthControls(state, {}, {}, {})
            }
        }
    }
}
