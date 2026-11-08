package com.example.dofcalculator.ui.calculator

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.dofcalculator.R
import com.example.dofcalculator.model.DiffractionLevel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DiffractionDialogs(state: DofCalculatorUiState, onCloseInfo: () -> Unit) {
    if (state.isDiffractionInfoVisible) {
        ModalBottomSheet(onDismissRequest = onCloseInfo,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.diffraction_title), style = MaterialTheme.typography.headlineSmall)
                DiffractionDetails(state)
                Text(stringResource(R.string.diffraction_explanation), style = MaterialTheme.typography.bodySmall)
                Text(stringResource(R.string.diffraction_conventional_help), style = MaterialTheme.typography.bodySmall)
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
    if (result == null) {
        Text(stringResource(R.string.diffraction_no_result))
    } else {
        Text(stringResource(R.string.diffraction_nominal, number(result.nominalAperture)))
        Text(stringResource(R.string.diffraction_airy, diffractionMicrons(result.airyDiameterMm, locale)))
        Text(stringResource(R.string.diffraction_coc, diffractionMicrons(result.effectiveCoCmm, locale)))
        Text(stringResource(R.string.diffraction_ratio, number(result.ratio)))
        Text(stringResource(R.string.diffraction_criterion, stringResource(state.sharpness.mode.labelResource())))
        if (result.level == DiffractionLevel.NONE) Text(stringResource(R.string.diffraction_no_warning))
    }
}
