package com.example.dofcalculator.ui.calculator

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.CompositionLocalProvider
import com.example.dofcalculator.settings.DistanceDisplay
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.dofcalculator.R
import com.example.dofcalculator.calculation.DepthOfFieldCalculator
import com.example.dofcalculator.data.DataStorePresetRepository
import com.example.dofcalculator.data.cameraPresetsDataStore
import com.example.dofcalculator.model.DepthOfFieldInput
import com.example.dofcalculator.model.FrameFormat
import com.example.dofcalculator.settings.AppSettingsRepository
import com.example.dofcalculator.settings.CalculatorMode
import com.example.dofcalculator.ui.theme.DOFCalculatorTheme

@Composable
fun DofCalculatorRoute(
    appSettingsRepository: AppSettingsRepository,
) {
    val context = LocalContext.current.applicationContext
    val presetRepository = remember(context) {
        DataStorePresetRepository(context.cameraPresetsDataStore)
    }
    val versionName = remember(context) {
        @Suppress("DEPRECATION")
        context.packageManager
            .getPackageInfo(context.packageName, 0)
            .versionName
            .orEmpty()
    }
    val viewModel: DofCalculatorViewModel = viewModel(
        factory = remember(presetRepository, appSettingsRepository) {
            DofCalculatorViewModel.factory(
                presetRepository = presetRepository,
                appSettingsRepository = appSettingsRepository,
            )
        },
    )

    CompositionLocalProvider(
        LocalDistanceDisplay provides DistanceDisplay(
            viewModel.uiState.distanceUnit, viewModel.uiState.distanceRounding),
    ) {
    DofCalculatorScreen(
        uiState = viewModel.uiState,
        onFrameFormatSelected = viewModel::onFrameFormatSelected,
        onCameraProfileSelected = viewModel::onCameraProfileSelected,
        onCircleOfConfusionChanged = viewModel::onCircleOfConfusionChanged,
        onFocalLengthChanged = viewModel::onFocalLengthChanged,
        onStepFocalLength = viewModel::onStepFocalLength,
        onFocalLengthPresetSelected =
            viewModel::onFocalLengthPresetSelected,
        onApertureChanged = viewModel::onApertureChanged,
        onAperturePresetSelected = viewModel::onAperturePresetSelected,
        onPreviousAperture = viewModel::onPreviousAperture,
        onNextAperture = viewModel::onNextAperture,
        onOpenFocusDistance = viewModel::onOpenFocusDistance,
        onCalculatorModeSelected = viewModel::onCalculatorModeSelected,
        onOpenSettings = viewModel::onOpenSettings,
        presetState = viewModel.uiState.presets,
        onOpenPresetManager = viewModel::onOpenPresetManager,
        onOpenSharpness = viewModel::onOpenSharpness,
        onOpenDiffractionInfo = viewModel::onOpenDiffractionInfo,
        onOpenCameraLenses = viewModel::onOpenCameraLenses,
        onShowAllFocalLengths = viewModel::onShowAllFocalLengths,
        onShowAllApertures = viewModel::onShowAllApertures,
    )

    if (viewModel.uiState.isSharpnessVisible) {
        SharpnessCriterionSheet(
            state = viewModel.uiState,
            onDismiss = viewModel::onCloseSharpness,
            onMode = viewModel::onSharpnessModeSelected,
            onField = viewModel::onSharpnessFieldChanged,
            onPlacement = viewModel::onPrintPlacementSelected,
            onImport = viewModel::onImportLegacyCoC,
            onPrintSize = viewModel::onPrintSizeSelected,
            onScreenDevice = viewModel::onScreenDeviceSelected,
            onScreenCriterion = viewModel::onScreenCriterionSelected,
            onScreenPlacement = viewModel::onScreenPlacementSelected,
            onAcknowledgePrintModel = viewModel::onAcknowledgePrintModel,
            onVideoResolution = viewModel::onVideoResolutionSelected,
            onVideoField = viewModel::onVideoFieldChanged,
            onVideoAreaMode = viewModel::onVideoAreaModeSelected,
        )
    }

    FocusDistanceDialogs(
        state = viewModel.uiState,
        onDismiss = viewModel::onCloseFocusDistance,
        onText = viewModel::onFocusDistanceInputChanged,
        onSlider = viewModel::onDistanceSliderChanged,
        onStep = viewModel::onStepFocusDistance,
    )

    DiffractionDialogs(
        state = viewModel.uiState,
        onCloseInfo = viewModel::onCloseDiffractionInfo,
    )

    AppDialogs(
        uiState = viewModel.uiState,
        onDiffractionWarningsEnabled = viewModel::onDiffractionWarningsEnabled,
        versionName = versionName,
        onThemeModeSelected = viewModel::onThemeModeSelected,
        onLanguageSelected = viewModel::onLanguageSelected,
        onDistanceUnitSelected = viewModel::onDistanceUnitSelected,
        onDistanceRoundingSelected = viewModel::onDistanceRoundingSelected,
        onApertureIncrementsSelected = viewModel::onApertureIncrementsSelected,
        onOpenAbout = viewModel::onOpenAbout,
        onCloseSettings = viewModel::onCloseSettings,
        onCloseAbout = viewModel::onCloseAbout,
        onDismissStorageError =
            viewModel::onDismissSettingsStorageError,
    )

    if (viewModel.uiState.lensManager.cameraId != null) CameraLensDialogs(
        state = viewModel.uiState,
        onClose = viewModel::onCloseCameraLenses,
        onAdd = viewModel::onAddLens,
        onEdit = viewModel::onEditLens,
        onDelete = viewModel::onDeleteLens,
        onConfirmDelete = viewModel::onConfirmDeleteLens,
        onCancelDelete = viewModel::onCancelDeleteLens,
        onName = viewModel::onLensNameChanged,
        onZoom = viewModel::onLensZoomChanged,
        onField = viewModel::onLensFieldChanged,
        onSave = viewModel::onSaveLens,
        onCancelEditor = viewModel::onCancelLensEditor,
        onDismissError = viewModel::onDismissPresetStorageError,
    ) else PresetManagementDialogs(
        state = viewModel.uiState.presets,
        onOpenLenses = viewModel::onOpenCameraLenses,
        onCloseManager = viewModel::onClosePresetManager,
        onSelectCamera = viewModel::onCameraProfileSelected,
        onCustomFormatChanged = viewModel::onCameraCustomFormatNameChanged,
        onConfirmDelete = viewModel::onConfirmDeleteCamera,
        onCancelDelete = viewModel::onCancelDeleteCamera,
        onCreateCamera = viewModel::onCreateCamera,
        onEditCamera = viewModel::onEditCamera,
        onDeleteCamera = viewModel::onDeleteCamera,
        onCameraNameChanged = viewModel::onCameraEditorNameChanged,
        onCameraFrameFormatSelected =
            viewModel::onCameraEditorFrameFormatSelected,
        onCameraDimensionsChanged = viewModel::onCameraDimensionsChanged,
        onSaveCamera = viewModel::onSaveCameraEditor,
        onCancelCamera = viewModel::onCancelCameraEditor,
        onDismissStorageError = viewModel::onDismissPresetStorageError,
    )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DofCalculatorScreen(
    uiState: DofCalculatorUiState,
    modifier: Modifier = Modifier,
    onFrameFormatSelected: (FrameFormat) -> Unit = {},
    onCameraProfileSelected: (String) -> Unit = {},
    onCircleOfConfusionChanged: (String) -> Unit = {},
    onFocalLengthChanged: (String) -> Unit = {},
    onStepFocalLength: (Boolean) -> Unit = {},
    onFocalLengthPresetSelected: (Double) -> Unit = {},
    onApertureChanged: (String) -> Unit = {},
    onAperturePresetSelected: (Double) -> Unit = {},
    onPreviousAperture: () -> Unit = {},
    onNextAperture: () -> Unit = {},
    onOpenFocusDistance: () -> Unit = {},
    onCalculatorModeSelected: (CalculatorMode) -> Unit = {},
    onOpenSettings: () -> Unit = {},
    presetState: PresetManagerUiState = PresetManagerUiState(),
    onOpenPresetManager: () -> Unit = {},
    onOpenSharpness: () -> Unit = {},
    onOpenDiffractionInfo: () -> Unit = {},
    onOpenCameraLenses: (String) -> Unit = {},
    onShowAllFocalLengths: (Boolean) -> Unit = {},
    onShowAllApertures: (Boolean) -> Unit = {},
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(
                            when (uiState.calculatorMode) {
                                CalculatorMode.DepthOfField ->
                                    R.string.mode_dof
                                CalculatorMode.Hyperfocal ->
                                    R.string.mode_hyperfocal
                            },
                        ),
                        modifier = Modifier.padding(end = 8.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                actions = {
                    AppTopBarActions(
                        onOpenSettings = onOpenSettings,
                    )
                },
            )
        },
    ) { innerPadding ->
        CompactCalculatorContent(
            uiState = uiState,
            onFrameFormatSelected = onFrameFormatSelected,
            onCameraProfileSelected = onCameraProfileSelected,
            onCircleOfConfusionChanged = onCircleOfConfusionChanged,
            onFocalLengthChanged = onFocalLengthChanged,
            onStepFocalLength = onStepFocalLength,
            onFocalLengthPresetSelected = onFocalLengthPresetSelected,
            onApertureChanged = onApertureChanged,
            onAperturePresetSelected = onAperturePresetSelected,
            onPreviousAperture = onPreviousAperture,
            onNextAperture = onNextAperture,
            onOpenFocusDistance = onOpenFocusDistance,
            onCalculatorModeSelected = onCalculatorModeSelected,
            onOpenPresetManager = onOpenPresetManager,
            onOpenSharpness = onOpenSharpness,
            onOpenDiffractionInfo = onOpenDiffractionInfo,
            onOpenCameraLenses = onOpenCameraLenses,
            onShowAllFocalLengths = onShowAllFocalLengths,
            onShowAllApertures = onShowAllApertures,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding(),
        )
    }
}

@Preview(name = "One lens English", widthDp = 360, heightDp = 720, locale = "en")
@Composable
private fun CameraSingleLensPreview() { CameraKitPreview(false) }

@Preview(name = "Multiple lenses English", widthDp = 360, heightDp = 720, locale = "en")
@Preview(name = "Multiple lenses Russian large", widthDp = 320, heightDp = 720, locale = "ru", fontScale = 1.5f)
@Composable
private fun CameraMultipleLensesPreview() { CameraKitPreview(true) }

@Composable
private fun CameraKitPreview(multiple: Boolean) {
    val lenses = listOf(
        com.example.dofcalculator.model.Lens.Zoom("zoom", "", 24.0, 4.0, 120.0, 4.0, 22.0),
        com.example.dofcalculator.model.Lens.Prime("prime50", "", 50.0, 1.4, 16.0),
        com.example.dofcalculator.model.Lens.Prime("prime200", "", 200.0, 4.0, 32.0),
    ).let { if (multiple) it else it.take(1) }
    val kit = com.example.dofcalculator.model.CameraLensKit(lenses)
    DOFCalculatorTheme(dynamicColor = false) {
        DofCalculatorScreen(DofCalculatorUiState(presets = PresetManagerUiState(
            lensKitsByCameraId = mapOf(com.example.dofcalculator.data.BuiltInCameraProfiles.DEFAULT_ID to kit))))
    }
}

@Preview(
    name = "English phone",
    widthDp = 360,
    heightDp = 720,
    locale = "en",
    showBackground = true,
)
@Preview(
    name = "Russian large text",
    widthDp = 360,
    heightDp = 720,
    locale = "ru",
    fontScale = 1.5f,
    showBackground = true,
)
@Preview(
    name = "Landscape",
    widthDp = 800,
    heightDp = 480,
    locale = "en",
    showBackground = true,
)
@Composable
private fun DofCalculatorScreenPreview() {
    val previewResult = remember {
        DepthOfFieldCalculator.calculate(
            DepthOfFieldInput(
                focalLengthMm = 50.0,
                aperture = 8.0,
                circleOfConfusionMm = 0.030,
                focusDistanceMeters = 5.0,
            ),
        )
    }
    DOFCalculatorTheme {
        DofCalculatorScreen(
            uiState = DofCalculatorUiState(result = previewResult),
        )
    }
}
