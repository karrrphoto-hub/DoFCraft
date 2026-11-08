package com.example.dofcalculator.ui.calculator

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.example.dofcalculator.R
import com.example.dofcalculator.model.MacroDetectionState

@Composable
internal fun MacroWarningCard(detection: MacroDetectionState, onOpenMacro: () -> Unit) {
    if (detection !is MacroDetectionState.MacroEstimated &&
        detection != MacroDetectionState.MagnificationUnavailableAtCloseDistance) return
    val estimated = detection is MacroDetectionState.MacroEstimated
    var details by rememberSaveable(estimated) { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth().testTag("macro-warning-card"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("ⓘ", Modifier.clearAndSetSemantics {}, style = MaterialTheme.typography.titleMedium)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(if (estimated) R.string.macro_warning_title else R.string.macro_warning_close_title),
                    style = MaterialTheme.typography.titleSmall)
                if (detection is MacroDetectionState.MacroEstimated) {
                    val ratio = checkNotNull(MagnificationFormatter.ratio(detection.magnification,
                        LocalResources.current.configuration.locales[0]))
                    Text(stringResource(R.string.macro_warning_estimate, ratio),
                        style = MaterialTheme.typography.bodyMedium)
                } else Text(stringResource(R.string.macro_warning_close_body),
                    style = MaterialTheme.typography.bodySmall)
                Text(stringResource(R.string.macro_warning_summary), style = MaterialTheme.typography.bodySmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = onOpenMacro) {
                        Text(stringResource(if (estimated) R.string.macro_warning_open else R.string.macro_warning_close_open))
                    }
                    TextButton(onClick = { details = !details }) {
                        Text(stringResource(R.string.macro_warning_details))
                    }
                }
                if (details) {
                    Text(stringResource(R.string.macro_warning_details_body), style = MaterialTheme.typography.bodySmall)
                    Text(stringResource(R.string.macro_warning_capabilities), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "Macro warning English", widthDp = 360, locale = "en")
@androidx.compose.ui.tooling.preview.Preview(name = "Macro warning Russian large dark", widthDp = 320,
    locale = "ru", fontScale = 1.5f, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MacroWarningPreview() {
    com.example.dofcalculator.ui.theme.DOFCalculatorTheme(dynamicColor = false) {
        Surface {
            Column(Modifier.padding(16.dp)) {
                MacroWarningCard(MacroDetectionState.MacroEstimated(.2679491924311227), {})
            }
        }
    }
}
