package com.example.dofcalculator.ui.calculator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Switch
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.dofcalculator.R
import com.example.dofcalculator.settings.ApertureIncrements
import com.example.dofcalculator.settings.AppLanguage
import com.example.dofcalculator.settings.ThemeMode
import com.example.dofcalculator.settings.DistanceUnit
import com.example.dofcalculator.settings.DistanceRounding

@Composable
internal fun AppTopBarActions(
    onOpenSettings: () -> Unit,
) {
    TextButton(
        onClick = onOpenSettings,
        modifier = Modifier.heightIn(min = 48.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_title),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun AppDialogs(
    uiState: DofCalculatorUiState,
    versionName: String,
    onThemeModeSelected: (ThemeMode) -> Unit,
    onLanguageSelected: (AppLanguage) -> Unit,
    onDistanceUnitSelected: (DistanceUnit) -> Unit,
    onDistanceRoundingSelected: (DistanceRounding) -> Unit,
    onOpenAbout: () -> Unit,
    onCloseSettings: () -> Unit,
    onCloseAbout: () -> Unit,
    onDismissStorageError: () -> Unit,
    onApertureIncrementsSelected: (ApertureIncrements) -> Unit = {},
    onDiffractionWarningsEnabled: (Boolean) -> Unit = {},
) {
    key(uiState.language) {
        when {
            uiState.settingsStorageError -> SettingsStorageErrorDialog(
                onDismiss = onDismissStorageError,
            )
            uiState.isSettingsVisible -> SettingsDialog(
                diffractionWarningsEnabled = uiState.diffractionSettings.warningsEnabled,
                onDiffractionWarningsEnabled = onDiffractionWarningsEnabled,
                selectedThemeMode = uiState.themeMode,
                selectedLanguage = uiState.language,
                onThemeModeSelected = onThemeModeSelected,
                onLanguageSelected = onLanguageSelected,
                onDistanceUnitSelected = onDistanceUnitSelected,
                onDistanceRoundingSelected = onDistanceRoundingSelected,
                selectedUnit = uiState.distanceUnit,
                selectedRounding = uiState.distanceRounding,
                selectedApertureIncrements = uiState.apertureIncrements,
                onApertureIncrementsSelected = onApertureIncrementsSelected,
                onOpenAbout = onOpenAbout,
                onDismiss = onCloseSettings,
            )
            uiState.isAboutVisible -> AboutDialog(
                versionName = versionName,
                onDismiss = onCloseAbout,
            )
        }
    }
}

@Composable
private fun SettingsDialog(
    diffractionWarningsEnabled: Boolean,
    onDiffractionWarningsEnabled: (Boolean) -> Unit,
    selectedThemeMode: ThemeMode,
    selectedLanguage: AppLanguage,
    selectedUnit: DistanceUnit,
    selectedRounding: DistanceRounding,
    selectedApertureIncrements: ApertureIncrements,
    onApertureIncrementsSelected: (ApertureIncrements) -> Unit,
    onDistanceUnitSelected: (DistanceUnit) -> Unit,
    onDistanceRoundingSelected: (DistanceRounding) -> Unit,
    onThemeModeSelected: (ThemeMode) -> Unit,
    onLanguageSelected: (AppLanguage) -> Unit,
    onOpenAbout: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_title)) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                SettingsSectionTitle(
                    text = stringResource(R.string.theme_mode_label),
                )
                ThemeMode.entries.forEach { themeMode ->
                    SettingsOption(
                        text = stringResource(themeMode.labelResource()),
                        selected = themeMode == selectedThemeMode,
                        onClick = { onThemeModeSelected(themeMode) },
                    )
                }

                SettingsSectionTitle(
                    text = stringResource(R.string.language_mode_label),
                    modifier = Modifier.padding(top = 12.dp),
                )
                AppLanguage.entries.forEach { language ->
                    SettingsOption(
                        text = stringResource(language.labelResource()),
                        selected = language == selectedLanguage,
                        onClick = { onLanguageSelected(language) },
                    )
                }

                SettingsSectionTitle(
                    text = stringResource(R.string.aperture_increments),
                    modifier = Modifier.padding(top = 12.dp),
                )
                ApertureIncrements.entries.forEach { increments ->
                    SettingsOption(
                        text = stringResource(increments.labelResource()),
                        selected = increments == selectedApertureIncrements,
                        onClick = { onApertureIncrementsSelected(increments) },
                    )
                }

                SettingsSectionTitle(
                    text = stringResource(R.string.distance_units),
                    modifier = Modifier.padding(top = 12.dp),
                )
                DistanceUnit.entries.forEach { unit ->
                    SettingsOption(
                        text = distanceUnitLabel(unit),
                        selected = unit == selectedUnit,
                        onClick = { onDistanceUnitSelected(unit) },
                    )
                }
                SettingsSectionTitle(
                    text = stringResource(R.string.distance_rounding),
                    modifier = Modifier.padding(top = 12.dp),
                )
                DistanceRounding.entries.forEach { rounding ->
                    SettingsOption(
                        text = stringResource(when (rounding) {
                            DistanceRounding.Two -> R.string.rounding_two
                            DistanceRounding.Three -> R.string.rounding_three
                            DistanceRounding.None -> R.string.rounding_none
                        }),
                        selected = rounding == selectedRounding,
                        onClick = { onDistanceRoundingSelected(rounding) },
                    )
                }
                SettingsSectionTitle(stringResource(R.string.diffraction_title),
                    modifier = Modifier.padding(top = 12.dp))
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 56.dp)
                        .toggleable(value = diffractionWarningsEnabled, role = Role.Switch,
                            onValueChange = onDiffractionWarningsEnabled),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(stringResource(R.string.diffraction_warnings), Modifier.weight(1f))
                    Switch(checked = diffractionWarningsEnabled, onCheckedChange = null)
                }

            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text(stringResource(R.string.close))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onOpenAbout,
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text(stringResource(R.string.about_menu))
            }
        },
    )
}

@Composable
private fun SettingsSectionTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        modifier = modifier.padding(bottom = 4.dp),
    )
}

@Composable
private fun SettingsOption(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton,
            )
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun AboutDialog(
    versionName: String,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.about_title)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.about_description),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = stringResource(
                        R.string.version_label,
                        versionName,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text(stringResource(R.string.close))
            }
        },
    )
}

@Composable
private fun SettingsStorageErrorDialog(
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.settings_storage_error_title))
        },
        text = {
            Text(stringResource(R.string.settings_storage_error_message))
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        },
    )
}

private fun ApertureIncrements.labelResource(): Int = when (this) {
    ApertureIncrements.FullStop -> R.string.aperture_full_stop
    ApertureIncrements.HalfStop -> R.string.aperture_half_stop
    ApertureIncrements.ThirdStop -> R.string.aperture_third_stop
}

private fun ThemeMode.labelResource(): Int = when (this) {
    ThemeMode.System -> R.string.theme_system
    ThemeMode.Light -> R.string.theme_light
    ThemeMode.Dark -> R.string.theme_dark
}

private fun AppLanguage.labelResource(): Int = when (this) {
    AppLanguage.System -> R.string.language_system
    AppLanguage.English -> R.string.language_english
    AppLanguage.Russian -> R.string.language_russian
}
