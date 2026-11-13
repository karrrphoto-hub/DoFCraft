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
import com.example.dofcalculator.model.CloseFocusWarningState

@Composable
internal fun CloseFocusWarningCard(warning: CloseFocusWarningState) {
    if (warning !is CloseFocusWarningState.EstimatedMacro &&
        warning != CloseFocusWarningState.TooCloseToEstimate) return
    var expanded by rememberSaveable { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth().testTag("close-focus-warning"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("ⓘ", Modifier.clearAndSetSemantics {}, style = MaterialTheme.typography.titleMedium)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(if (warning is CloseFocusWarningState.EstimatedMacro)
                    R.string.close_focus_title else R.string.close_focus_close_title),
                    style = MaterialTheme.typography.titleSmall)
                if (warning is CloseFocusWarningState.EstimatedMacro) {
                    val denominator = checkNotNull(CloseFocusFormatter.denominator(warning.magnification,
                        LocalResources.current.configuration.locales[0]))
                    val ratio = stringResource(R.string.close_focus_approx_ratio, denominator)
                    Text(stringResource(R.string.close_focus_estimate, ratio),
                        style = MaterialTheme.typography.bodySmall)
                } else {
                    Text(stringResource(R.string.close_focus_close_body),
                        style = MaterialTheme.typography.bodySmall)
                }
                Text(stringResource(R.string.close_focus_summary), style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = { expanded = !expanded }) {
                    Text(stringResource(if (expanded) R.string.close_focus_hide_details else R.string.close_focus_learn_more))
                }
                if (expanded) {
                    Text(stringResource(R.string.close_focus_model_title), style = MaterialTheme.typography.titleSmall)
                    Text(stringResource(R.string.close_focus_model_help), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "Close focus English", widthDp = 360, locale = "en",
    showBackground = true)
@androidx.compose.ui.tooling.preview.Preview(name = "Close focus Russian large dark", widthDp = 320,
    locale = "ru", fontScale = 1.5f, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
    showBackground = true)
@Composable
private fun CloseFocusWarningPreview() {
    com.example.dofcalculator.ui.theme.DOFCalculatorTheme(dynamicColor = false) {
        Surface {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CloseFocusWarningCard(CloseFocusWarningState.EstimatedMacro(.2679491924311227))
            }
        }
    }
}
