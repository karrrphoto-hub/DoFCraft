package com.example.dofcalculator.ui.calculator

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.example.dofcalculator.R
import com.example.dofcalculator.model.*
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

internal fun diffractionNumber(value: Double, locale: Locale): String {
    if (!value.isFinite()) return "—"
    val pattern = if (value != 0.0 && (value < .001 || value >= 1e6)) "0.###E0" else "0.###"
    return DecimalFormat(pattern, DecimalFormatSymbols.getInstance(locale)).format(value)
}

internal fun diffractionMicrons(mm: Double, locale: Locale): String {
    val microns = mm * 1000.0
    return if (microns.isFinite()) diffractionNumber(microns, locale)
        else java.math.BigDecimal.valueOf(mm).movePointRight(3).toEngineeringString()
}

@Composable
internal fun DiffractionWarning(state: DofCalculatorUiState, onInformation: () -> Unit) {
    if (!state.showDiffractionWarning) return
    val result = checkNotNull(state.diffractionResult)
    val prominent = result.level == DiffractionLevel.NOTICEABLE
    Card(
        onClick = onInformation,
        modifier = Modifier.fillMaxWidth().testTag("diffraction-warning"),
        colors = CardDefaults.cardColors(
            containerColor = if (prominent) MaterialTheme.colorScheme.secondaryContainer
                else MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = if (prominent) MaterialTheme.colorScheme.onSecondaryContainer
                else MaterialTheme.colorScheme.onSurface),
    ) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("ⓘ", Modifier.clearAndSetSemantics {}, style = MaterialTheme.typography.titleMedium)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(if (prominent) R.string.diffraction_noticeable else R.string.diffraction_possible),
                    style = MaterialTheme.typography.titleSmall)
                Text(stringResource(if (prominent) R.string.diffraction_noticeable_body
                    else R.string.diffraction_possible_body),
                    style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "English diffraction", widthDp = 360,
    locale = "en", showBackground = true)
@androidx.compose.ui.tooling.preview.Preview(name = "Russian large font dark", widthDp = 320, heightDp = 1400,
    locale = "ru", fontScale = 1.5f, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
    showBackground = true)
@Composable
private fun DiffractionWarningPreview() {
    val states = androidx.compose.runtime.remember {
        listOf(22.0, 32.0, 45.0).map { aperture ->
            DofCalculatorUiState(diffractionResult = com.example.dofcalculator.calculation.DiffractionCalculator
                .calculate(aperture, .030))
        }
    }
    com.example.dofcalculator.ui.theme.DOFCalculatorTheme(dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                states.forEach { DiffractionWarning(it, {}) }
            }
        }
    }
}
