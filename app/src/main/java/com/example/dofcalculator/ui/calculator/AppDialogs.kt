package com.example.dofcalculator.ui.calculator

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Switch
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
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
    onCloseFocusWarningsEnabled: (Boolean) -> Unit = {},
) {
    key(uiState.language) {
        when {
            uiState.settingsStorageError -> SettingsStorageErrorDialog(
                onDismiss = onDismissStorageError,
            )
            uiState.isSettingsVisible -> SettingsDialog(
                closeFocusWarningsEnabled = uiState.closeFocusWarningsEnabled,
                onCloseFocusWarningsEnabled = onCloseFocusWarningsEnabled,
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
    closeFocusWarningsEnabled: Boolean,
    onCloseFocusWarningsEnabled: (Boolean) -> Unit,
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
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                SettingsSection(stringResource(R.string.theme_mode_label)) {
                    ThemeMode.entries.forEach { themeMode ->
                        SettingsOption(
                            text = stringResource(themeMode.labelResource()),
                            selected = themeMode == selectedThemeMode,
                            onClick = { onThemeModeSelected(themeMode) },
                        )
                    }
                }
                SettingsSection(stringResource(R.string.language_mode_label)) {
                    AppLanguage.entries.forEach { language ->
                        SettingsOption(
                            text = stringResource(language.labelResource()),
                            selected = language == selectedLanguage,
                            onClick = { onLanguageSelected(language) },
                        )
                    }
                }
                SettingsSection(stringResource(R.string.aperture_increments)) {
                    ApertureIncrements.entries.forEach { increments ->
                        SettingsOption(
                            text = stringResource(increments.labelResource()),
                            selected = increments == selectedApertureIncrements,
                            onClick = { onApertureIncrementsSelected(increments) },
                        )
                    }
                }
                SettingsSection(stringResource(R.string.distance_units)) {
                    DistanceUnit.entries.forEach { unit ->
                        SettingsOption(
                            text = distanceUnitLabel(unit),
                            selected = unit == selectedUnit,
                            onClick = { onDistanceUnitSelected(unit) },
                        )
                    }
                }
                SettingsSection(stringResource(R.string.distance_rounding)) {
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
                }
                SettingsSection(stringResource(R.string.settings_warnings_section), radioGroup = false) {
                    SettingsToggle(
                        text = stringResource(R.string.diffraction_warnings),
                        checked = diffractionWarningsEnabled,
                        onCheckedChange = onDiffractionWarningsEnabled,
                    )
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    SettingsToggle(
                        text = stringResource(R.string.close_focus_warning_enabled),
                        checked = closeFocusWarningsEnabled,
                        onCheckedChange = onCloseFocusWarningsEnabled,
                    )
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
private fun SettingsSection(
    title: String,
    radioGroup: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics { heading() }.padding(bottom = 10.dp),
            )
            HorizontalDivider(Modifier.padding(bottom = 8.dp))
            Column(
                modifier = if (radioGroup) Modifier.selectableGroup() else Modifier,
                content = content,
            )
        }
    }
}

@Composable
private fun SettingsToggle(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = null)
    }
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
            modifier = Modifier.weight(1f),
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

@androidx.compose.ui.tooling.preview.Preview(name = "Settings English", widthDp = 360,
    heightDp = 900, locale = "en")
@androidx.compose.ui.tooling.preview.Preview(name = "Settings Russian dark", widthDp = 320,
    heightDp = 900, locale = "ru", fontScale = 1.3f,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SettingsDialogPreview() {
    com.example.dofcalculator.ui.theme.DOFCalculatorTheme(dynamicColor = false) {
        AppDialogs(
            uiState = DofCalculatorUiState(isSettingsVisible = true),
            versionName = "preview",
            onThemeModeSelected = {}, onLanguageSelected = {},
            onDistanceUnitSelected = {}, onDistanceRoundingSelected = {},
            onOpenAbout = {}, onCloseSettings = {}, onCloseAbout = {},
            onDismissStorageError = {},
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "Warning settings Russian large dark",
    widthDp = 320, heightDp = 600, locale = "ru", fontScale = 1.5f,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun WarningSettingsPreview() {
    com.example.dofcalculator.ui.theme.DOFCalculatorTheme(dynamicColor = false) {
        androidx.compose.material3.Surface {
            Column(Modifier.padding(16.dp)) {
                SettingsSection(stringResource(R.string.settings_warnings_section), radioGroup = false) {
                    SettingsToggle(stringResource(R.string.diffraction_warnings), true, {})
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    SettingsToggle(stringResource(R.string.close_focus_warning_enabled), true, {})
                }
            }
        }
    }
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
