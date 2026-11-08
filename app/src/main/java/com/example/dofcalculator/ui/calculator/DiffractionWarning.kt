package com.example.dofcalculator.ui.calculator

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.example.dofcalculator.R
import com.example.dofcalculator.calculation.MagnificationEstimator
import com.example.dofcalculator.model.*
import com.example.dofcalculator.settings.CalculatorMode
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
internal fun magnificationLabel(estimate: MagnificationEstimate): String {
    val m = estimate.value ?: return stringResource(if (estimate.source == MagnificationMode.MANUAL)
        R.string.magnification_input_error else R.string.magnification_unavailable)
    val locale = LocalResources.current.configuration.locales[0]
    val ratio = MagnificationFormatter.ratio(m, locale, fractionDigits = 3)
        ?: return stringResource(R.string.magnification_unavailable)
    return if (estimate.source == MagnificationMode.AUTO && m > 0.0)
        stringResource(R.string.magnification_approx, ratio) else ratio
}

@Composable
internal fun MagnificationStatus(estimate: MagnificationEstimate) {
    estimate.suspension?.let {
        Text(stringResource(when (it) {
            ManualMagnificationSuspension.INFINITY -> R.string.macro_suspended
            ManualMagnificationSuspension.HYPERFOCAL -> R.string.macro_unconfirmed
            ManualMagnificationSuspension.NEEDS_CONFIRMATION -> R.string.macro_unconfirmed
        }), style = MaterialTheme.typography.bodySmall)
    }
    if (estimate.value == null && estimate.source == MagnificationMode.AUTO)
        Text(stringResource(R.string.diffraction_uncorrected),
        style = MaterialTheme.typography.bodySmall)
}

@Composable
internal fun DiffractionWarning(
    state: DofCalculatorUiState,
    onInformation: () -> Unit,
    onMagnification: () -> Unit,
    onUseAuto: () -> Unit = {},
) {
    if (state.showDiffractionWarning) {
        val result = checkNotNull(state.diffractionResult)
        val macro = (result.magnification.value ?: 0.0) >= MagnificationEstimator.CLOSE_UP_THRESHOLD
        val locale = LocalResources.current.configuration.locales[0]
        val prominent = result.level == DiffractionLevel.NOTICEABLE
        Card(
            onClick = onInformation,
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (prominent) MaterialTheme.colorScheme.secondaryContainer
                    else MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = if (prominent) MaterialTheme.colorScheme.onSecondaryContainer
                    else MaterialTheme.colorScheme.onSurface),
        ) {
            Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("ⓘ", Modifier.clearAndSetSemantics {}, style = MaterialTheme.typography.titleMedium)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(when {
                        macro -> R.string.diffraction_macro
                        prominent -> R.string.diffraction_noticeable
                        else -> R.string.diffraction_possible
                    }), style = MaterialTheme.typography.titleSmall)
                    Text(if (macro) stringResource(R.string.diffraction_macro_body,
                        diffractionNumber(result.nominalAperture, locale),
                        magnificationLabel(result.magnification),
                        diffractionNumber(result.effectiveAperture, locale))
                    else stringResource(if (prominent) R.string.diffraction_noticeable_body
                        else R.string.diffraction_possible_body,
                        diffractionNumber(result.nominalAperture, locale)),
                        style = MaterialTheme.typography.bodySmall)
                    if (macro) Text(stringResource(R.string.diffraction_macro_impact),
                        style = MaterialTheme.typography.bodySmall)
                    if (macro) Text(stringResource(R.string.magnification_source,
                        stringResource(if (result.magnification.source == MagnificationMode.MANUAL)
                            R.string.magnification_manual else R.string.magnification_auto)),
                        style = MaterialTheme.typography.bodySmall)
                    if (state.calculatorMode == CalculatorMode.DepthOfField && !state.manualNeedsConfirmation)
                        MagnificationStatus(result.magnification)
                }
            }
        }
    }
    if (state.showManualMagnificationNotice) {
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.macro_needs_confirmation), style = MaterialTheme.typography.titleSmall)
                val changes = state.manualMagnificationAssessment.changes
                Text(stringResource(when {
                    state.diffractionSettings.manualDistanceMeters == null -> R.string.macro_unconfirmed
                    ManualMagnificationChange.DISTANCE in changes -> R.string.macro_distance_changed
                    ManualMagnificationChange.FOCAL_LENGTH in changes -> R.string.macro_focal_changed
                    else -> R.string.macro_unconfirmed
                }), style = MaterialTheme.typography.bodySmall)
                Text(stringResource(R.string.macro_auto_fallback, magnificationLabel(state.magnificationEstimate)),
                    style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = onMagnification) { Text(stringResource(R.string.macro_update)) }
                TextButton(onClick = onUseAuto) { Text(stringResource(R.string.macro_use_auto)) }
            }
        }
    } else if (state.showMacroIndicator && !state.showMacroWarning) {
        OutlinedButton(onClick = onMagnification, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text(if (state.magnificationEstimate.suspension != null)
                stringResource(R.string.magnification_paused)
            else stringResource(if (state.magnificationEstimate.source == MagnificationMode.MANUAL)
                R.string.magnification_macro_manual else R.string.magnification_macro_auto,
                magnificationLabel(state.magnificationEstimate)))
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "English diffraction", widthDp = 360,
    locale = "en", showBackground = true)
@androidx.compose.ui.tooling.preview.Preview(name = "Russian large font dark", widthDp = 320,
    locale = "ru", fontScale = 1.5f, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
    showBackground = true)
@Composable
private fun DiffractionWarningPreview() {
    val state = androidx.compose.runtime.remember {
        val estimate = MagnificationEstimate(1.0, MagnificationMode.MANUAL)
        DofCalculatorUiState(
            diffractionResult = com.example.dofcalculator.calculation.DiffractionCalculator
                .calculate(8.0, .013, estimate),
            magnificationEstimate = estimate,
        )
    }
    com.example.dofcalculator.ui.theme.DOFCalculatorTheme(dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DiffractionWarning(state, {}, {})
            }
        }
    }
}
