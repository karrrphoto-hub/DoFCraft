package com.example.dofcalculator.ui.calculator

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.dofcalculator.R

@Composable
internal fun FocusDistanceDialogs(
    state: DofCalculatorUiState,
    onDismiss: () -> Unit,
    onText: (String) -> Unit,
    onSlider: (Double) -> Unit,
    onStep: (Boolean) -> Unit,
    onConfirmManualDistance: () -> Unit,
) {
    if (state.isExitHyperfocalConfirmationVisible) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.focus_distance_title)) },
            text = { Text(stringResource(R.string.exit_hyperfocal_confirmation)) },
            confirmButton = {
                TextButton(onClick = onConfirmManualDistance) { Text(stringResource(R.string.yes)) }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.no)) }
            },
        )
    } else if (state.isFocusDistanceVisible) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.focus_distance_title)) },
            text = {
                Column(Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState())) {
                    FocusDistanceControl(state, onText, onSlider, onStep)
                }
            },
            confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } },
        )
    }
}
