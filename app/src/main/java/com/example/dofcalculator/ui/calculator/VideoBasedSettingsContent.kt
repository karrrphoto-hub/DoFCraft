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

@Composable
internal fun videoResolutionName(resolution: VideoResolution): String = stringResource(when (resolution) {
    VideoResolution.HD -> R.string.video_format_hd
    VideoResolution.FHD -> R.string.video_format_fhd
    VideoResolution.DCI_2K -> R.string.video_format_dci_2k
    VideoResolution.QHD -> R.string.video_format_qhd
    VideoResolution.UHD_4K -> R.string.video_format_uhd_4k
    VideoResolution.DCI_4K -> R.string.video_format_dci_4k
    VideoResolution.UHD_8K -> R.string.video_format_uhd_8k
    VideoResolution.CUSTOM -> R.string.video_format_custom
})

@Composable
internal fun videoResolutionShort(s: VideoBasedSettings): String =
    if (s.resolution == VideoResolution.CUSTOM)
        stringResource(R.string.video_pixel_size, s.widthPx, s.heightPx)
    else videoResolutionName(s.resolution)

@Composable
private fun resolutionLabel(resolution: VideoResolution, settings: VideoBasedSettings): String =
    stringResource(R.string.video_resolution_value, videoResolutionName(resolution),
        resolution.widthPx ?: settings.customWidthPx, resolution.heightPx ?: settings.customHeightPx)

internal fun VideoError.labelResource(): Int = when (this) {
    VideoError.INVALID_RESOLUTION -> R.string.video_error_resolution
    VideoError.INVALID_BLUR -> R.string.video_error_blur
    VideoError.INVALID_CROP -> R.string.video_error_crop
    VideoError.INVALID_AREA -> R.string.video_error_area
    VideoError.AREA_EXCEEDS_SENSOR -> R.string.video_error_bounds
    VideoError.ASPECT_MISMATCH -> R.string.video_error_aspect
    VideoError.MISSING_SENSOR -> R.string.video_error_sensor
    VideoError.NUMERIC_RANGE -> R.string.video_error_range
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun VideoBasedSettingsContent(
    state: DofCalculatorUiState,
    onResolution: (VideoResolution) -> Unit,
    onField: (VideoField, String) -> Unit,
    onAreaMode: (VideoAreaMode) -> Unit,
) {
    val s = state.sharpness.video
    val locale = LocalResources.current.configuration.locales[0]
    var advanced by rememberSaveable { mutableStateOf(false) }
    var information by rememberSaveable { mutableStateOf(false) }
    var customCrop by rememberSaveable { mutableStateOf(s.cropFactor !in listOf(1.0, 1.5, 2.0)) }
    CriterionDropdown(
        stringResource(R.string.video_resolution), resolutionLabel(s.resolution, s),
        VideoResolution.entries.map { it to resolutionLabel(it, s) }, onResolution)
    if (s.resolution == VideoResolution.CUSTOM) {
        VideoNumber(state, VideoField.WIDTH, R.string.video_width, onField)
        VideoNumber(state, VideoField.HEIGHT, R.string.video_height, onField)
    }
    VideoNumber(state, VideoField.BLUR, R.string.blur_pixels_label, onField)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(1 to R.string.video_blur_strict, 2 to R.string.video_blur_moderate,
            3 to R.string.video_blur_relaxed).forEach { (value, label) ->
            FilterChip(selected = selectedVideoValue(state, VideoField.BLUR, value.toDouble()),
                onClick = { onField(VideoField.BLUR, value.toString()) },
                label = { Text(stringResource(label)) }, modifier = Modifier.heightIn(min = 48.dp))
        }
    }
    Text(stringResource(R.string.video_blur_help), style = MaterialTheme.typography.bodySmall)
    state.videoError?.let { error ->
        Text(stringResource(error.labelResource()), color = MaterialTheme.colorScheme.error)
        if (s.areaMode == VideoAreaMode.MANUAL) {
            TextButton(onClick = { onAreaMode(VideoAreaMode.AUTO) }) {
                Text(stringResource(R.string.video_switch_auto))
            }
        }
    }
    TextButton(onClick = { advanced = !advanced }) { Text(stringResource(R.string.sharpness_advanced)) }
    if (advanced) {
        Text(stringResource(R.string.video_area_mode), style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            VideoAreaMode.entries.forEach { mode ->
                FilterChip(selected = s.areaMode == mode, onClick = { onAreaMode(mode) },
                    label = { Text(stringResource(if (mode == VideoAreaMode.AUTO)
                        R.string.video_auto_area else R.string.video_manual_area)) },
                    modifier = Modifier.heightIn(min = 48.dp))
            }
        }
        if (s.areaMode == VideoAreaMode.AUTO) {
            Text(stringResource(R.string.video_crop), style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (crop in listOf(1.0, 1.5, 2.0)) {
                    FilterChip(selected = !customCrop && selectedVideoValue(state, VideoField.CROP, crop),
                        onClick = { customCrop = false; onField(VideoField.CROP, crop.toString()) },
                        label = { Text(stringResource(R.string.video_crop_value, referenceNumber(crop, locale))) },
                        modifier = Modifier.heightIn(min = 48.dp))
                }
                FilterChip(selected = customCrop || s.cropFactor !in listOf(1.0, 1.5, 2.0),
                    onClick = { customCrop = true },
                    label = { Text(stringResource(R.string.video_crop_custom)) },
                    modifier = Modifier.heightIn(min = 48.dp))
            }
            if (customCrop || s.cropFactor !in listOf(1.0, 1.5, 2.0) ||
                VideoField.CROP in state.videoInvalidFields) {
                VideoNumber(state, VideoField.CROP, R.string.video_crop, onField)
            }
            Text(stringResource(R.string.video_crop_help), style = MaterialTheme.typography.bodySmall)
        } else {
            VideoNumber(state, VideoField.ACTIVE_WIDTH, R.string.video_manual_width, onField)
            VideoNumber(state, VideoField.ACTIVE_HEIGHT, R.string.video_manual_height, onField)
            Text(stringResource(R.string.video_manual_help), style = MaterialTheme.typography.bodySmall)
        }
        TextButton(onClick = { information = !information }) {
            Text(stringResource(R.string.video_information))
        }
        if (information) VideoReferenceInfo(state)
    }
}

private fun selectedVideoValue(state: DofCalculatorUiState, field: VideoField, value: Double): Boolean =
    field.value(state.sharpness.video) == value && state.videoDrafts[field]?.let {
        com.example.dofcalculator.calculation.VideoCoCCalculator.parseField(field, it) == value
    } != false

@Composable
private fun VideoNumber(
    state: DofCalculatorUiState, field: VideoField, label: Int, onField: (VideoField, String) -> Unit,
) {
    val s = state.sharpness.video
    val locale = LocalResources.current.configuration.locales[0]
    val text = state.videoDrafts[field] ?: if (field.integer) field.value(s).toInt().toString()
        else referenceNumber(field.value(s), locale)
    val invalid = field in state.videoInvalidFields
    val fieldError = if (state.videoDrafts[field]?.let {
        com.example.dofcalculator.calculation.VideoCoCCalculator.parseField(field, it) == null
    } == true) com.example.dofcalculator.calculation.VideoCoCCalculator.fieldError(field)
        else state.videoError
    OutlinedTextField(
        value = text, onValueChange = { onField(field, it) },
        label = { Text(stringResource(label), maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) }, isError = invalid,
        supportingText = if (invalid) {{ Text(stringResource(
            fieldError?.labelResource() ?: R.string.video_error_range)) }} else null,
        keyboardOptions = KeyboardOptions(keyboardType = if (field.integer) KeyboardType.Number
            else KeyboardType.Decimal),
        singleLine = true, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun VideoReferenceInfo(state: DofCalculatorUiState) {
    val result = state.videoResult ?: return
    val locale = LocalResources.current.configuration.locales[0]
    fun n(value: Double) = referenceNumber(value, locale)
    Text(resolutionLabel(state.sharpness.video.resolution, state.sharpness.video))
    Text(stringResource(R.string.video_aspect, result.aspectRatioText))
    Text(stringResource(R.string.video_full_sensor, n(result.sensorArea.widthMm), n(result.sensorArea.heightMm)))
    Text(stringResource(R.string.video_base_area, n(result.baseActiveArea.widthMm), n(result.baseActiveArea.heightMm)))
    Text(stringResource(R.string.video_active_area, n(result.activeArea.widthMm), n(result.activeArea.heightMm)))
    Text(if (result.cropFactor == null) stringResource(R.string.video_crop_not_applied)
        else stringResource(R.string.video_crop_reference, n(result.cropFactor)))
    Text(stringResource(R.string.video_pixel_pitch, diffractionMicrons(result.pixelPitchXmm, locale)))
    Text(stringResource(R.string.video_blur_reference, n(result.blurPixels)))
    Text(stringResource(R.string.calculated_coc, n(result.effectiveCoCmm)))
}

@androidx.compose.ui.tooling.preview.Preview(name = "Video English", widthDp = 360, locale = "en",
    showBackground = true)
@androidx.compose.ui.tooling.preview.Preview(name = "Video Russian large dark", widthDp = 320,
    locale = "ru", fontScale = 1.5f, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
    showBackground = true)
@Composable
private fun VideoSettingsPreview() {
    com.example.dofcalculator.ui.theme.DOFCalculatorTheme(dynamicColor = false) {
        Surface {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                VideoBasedSettingsContent(DofCalculatorUiState(
                    sharpness = SharpnessSettings(mode = SharpnessCriterionMode.VIDEO_BASED)),
                    {}, { _, _ -> }, {})
            }
        }
    }
}
