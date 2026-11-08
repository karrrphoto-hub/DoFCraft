package com.example.dofcalculator.ui.calculator

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.dofcalculator.R
import com.example.dofcalculator.model.*
import com.example.dofcalculator.settings.*
import com.example.dofcalculator.ui.theme.*

/** Independent click targets: distance editor on the left, mode buttons on the right. */
@Composable
internal fun DistanceParameterTile(
    state: DofCalculatorUiState,
    onDistance: () -> Unit,
    onMode: (CalculatorMode) -> Unit,
    onMacro: () -> Unit,
    onClearMacro: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hyperfocal = state.calculatorMode == CalculatorMode.Hyperfocal
    val macro = state.isMacroModeActive
    val hLabel = stringResource(R.string.focus_hyperfocal_button)
    val mLabel = stringResource(R.string.focus_macro_button)
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelLarge
    val density = LocalDensity.current
    val buttonsWidth = with(density) {
        maxOf(64.dp, textMeasurer.measure(hLabel, labelStyle, softWrap = false).size.width.toDp() + 24.dp) +
            // Leave room for pixel rounding when the button group is measured at another density.
            maxOf(64.dp, textMeasurer.measure(mLabel, labelStyle, softWrap = false).size.width.toDp() + 24.dp) + 12.dp
    }
    val onHyperfocal = { onMode(if (hyperfocal) CalculatorMode.DepthOfField else CalculatorMode.Hyperfocal) }
    val onMacroClick = if (macro) onClearMacro else onMacro
    Card(
        modifier = modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth().padding(8.dp)) {
            if (maxWidth >= buttonsWidth + 112.dp + 8.dp) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    DistanceSummary(state, onDistance, Modifier.weight(1f))
                    FocusModeButtons(hyperfocal, macro, onHyperfocal, onMacroClick, wrap = false)
                }
            } else {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DistanceSummary(state, onDistance, Modifier.fillMaxWidth())
                    FocusModeButtons(hyperfocal, macro, onHyperfocal, onMacroClick, Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun DistanceSummary(state: DofCalculatorUiState, onClick: () -> Unit, modifier: Modifier) {
    val display = DistanceDisplay(state.distanceUnit, state.distanceRounding,
        LocalResources.current.configuration.locales[0])
    val hyperfocal = state.calculatorMode == CalculatorMode.Hyperfocal
    val value = if (hyperfocal) stringResource(R.string.hyperfocal_mode_label)
    else when (val distance = state.focusDistance) {
        CalculatedDistance.Infinity -> "∞"
        is CalculatedDistance.Finite -> stringResource(R.string.focus_distance_with_unit,
            display.number(distance.meters), distanceUnitLabel(state.distanceUnit))
        null -> "—"
    }
    val supporting = when {
        hyperfocal -> null
        state.focusMagnification != null -> stringResource(R.string.focus_macro_active,
            magnificationLabel(MagnificationEstimate(state.focusMagnification, MagnificationMode.MANUAL)))
        else -> null
    }
    Column(modifier.heightIn(min = 64.dp).clickable(role = Role.Button, onClick = onClick).padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)) {
        Text(stringResource(R.string.distance_parameter), style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(value, style = if (hyperfocal) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold, maxLines = if (hyperfocal) 3 else 2, overflow = TextOverflow.Ellipsis)
        supporting?.let {
            Text(it, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FocusModeButtons(
    hyperfocal: Boolean, macro: Boolean, onHyperfocal: () -> Unit, onMacro: () -> Unit,
    modifier: Modifier = Modifier,
    wrap: Boolean = true,
) {
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val activeColors = ButtonDefaults.filledTonalButtonColors(
        containerColor = if (dark) FocusSelectedDark else FocusSelectedLight,
        contentColor = if (dark) OnFocusSelectedDark else OnFocusSelectedLight)
    val description = stringResource(R.string.focus_hyperfocal_description)
    val buttons: @Composable () -> Unit = {
        FilledTonalButton(onClick = onHyperfocal,
            modifier = Modifier.widthIn(min = 64.dp).heightIn(min = 48.dp).semantics {
                selected = hyperfocal
                contentDescription = description
            },
            colors = if (hyperfocal) activeColors else ButtonDefaults.filledTonalButtonColors(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)) {
            Text(stringResource(R.string.focus_hyperfocal_button), style = MaterialTheme.typography.labelLarge)
        }
        FilledTonalButton(onClick = onMacro,
            modifier = Modifier.widthIn(min = 64.dp).heightIn(min = 48.dp).semantics { selected = macro },
            colors = if (macro) activeColors else ButtonDefaults.filledTonalButtonColors(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)) {
            Text(stringResource(R.string.focus_macro_button), style = MaterialTheme.typography.labelLarge)
        }
    }
    if (wrap) {
        FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) { buttons() }
    } else {
        Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically) { buttons() }
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "Distance modes English", widthDp = 360, locale = "en")
@androidx.compose.ui.tooling.preview.Preview(name = "Distance modes Russian large", widthDp = 320,
    locale = "ru", fontScale = 1.5f, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DistanceParameterTilePreview() {
    val state = DofCalculatorUiState(focusDistance = CalculatedDistance.Finite(1.941))
    DOFCalculatorTheme(dynamicColor = false) {
        Surface {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DistanceParameterTile(state, {}, {}, {}, {})
                DistanceParameterTile(state.copy(calculatorMode = CalculatorMode.Hyperfocal), {}, {}, {}, {})
                DistanceParameterTile(state.copy(diffractionSettings =
                    DiffractionSettings(magnificationMode = MagnificationMode.MANUAL)), {}, {}, {}, {})
            }
        }
    }
}
