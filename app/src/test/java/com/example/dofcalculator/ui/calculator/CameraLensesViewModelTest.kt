package com.example.dofcalculator.ui.calculator

import com.example.dofcalculator.data.*
import com.example.dofcalculator.model.*
import com.example.dofcalculator.settings.*
import kotlinx.coroutines.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Test

class CameraLensesViewModelTest {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
    private val custom = CameraProfile("user-camera", "My camera", FrameFormat.FullFrame35Mm)
    private val cameras = InMemoryPresetRepository(PresetLibrary(
        cameras = BuiltInCameraProfiles.cameras + custom, selectedCameraId = custom.id))
    private val settings = InMemoryAppSettingsRepository(AppSettings(sharpnessMigrated = true))
    private fun vm() = DofCalculatorViewModel(cameras, settings, scope)
    @After fun cleanup() { scope.cancel() }

    private fun addPrime(vm: DofCalculatorViewModel, focal: String, aperture: String, closed: String = "") {
        vm.onOpenCameraLenses(custom.id)
        vm.onAddLens()
        vm.onLensFieldChanged(LensField.PRIME_FOCAL, focal)
        vm.onLensFieldChanged(LensField.PRIME_APERTURE, aperture)
        vm.onLensFieldChanged(LensField.MIN_APERTURE, closed)
        vm.onSaveLens()
        vm.onSaveLens() // a repeated click never creates a second record
    }

    @Test fun `selected zoom supplies both preset rows and Ignore Lens Profile bypasses the complete profile`() {
        val vm = vm()
        vm.onOpenCameraLenses(custom.id)
        vm.onAddLens()
        vm.onLensZoomChanged(true)
        for ((field, value) in mapOf(LensField.WIDE_FOCAL to "24", LensField.TELE_FOCAL to "120",
            LensField.WIDE_APERTURE to "4", LensField.TELE_APERTURE to "4", LensField.MIN_APERTURE to "22"))
            vm.onLensFieldChanged(field, value)
        vm.onSaveLens()
        addPrime(vm, "50", "1.4", "16")
        addPrime(vm, "200", "4", "32")
        assertEquals(3, vm.uiState.cameraLensKit.lenses.size)
        assertEquals(listOf(24.0, 28.0, 35.0, 50.0, 55.0, 58.0, 85.0, 90.0, 105.0, 120.0),
            vm.uiState.focalLengthPresets)
        assertEquals(listOf(4.0, 5.6, 8.0, 11.0, 16.0, 22.0), vm.uiState.aperturePresets)
        val limited = vm.uiState.focalLengthPresets
        vm.onShowAllFocalLengths(true)
        assertTrue(vm.uiState.focalLengthPresets.containsAll(CalculatorPresets.typicalFocalLengthsMm.map(Int::toDouble)))
        assertTrue(vm.uiState.aperturePresets.contains(1.0))
        vm.onShowAllApertures(true)
        assertTrue(vm.uiState.aperturePresets.contains(1.0))
        vm.onShowAllFocalLengths(false)
        assertEquals(limited, vm.uiState.focalLengthPresets)
        assertFalse(vm.uiState.cameraLensKit.ignoreLensProfile)
        vm.onCameraProfileSelected(BuiltInCameraProfiles.DEFAULT_ID)
        assertEquals(CalculatorPresets.typicalFocalLengthsMm.map(Int::toDouble), vm.uiState.focalLengthPresets)
        assertEquals(ApertureIncrements.FullStop.apertures, vm.uiState.aperturePresets)
        vm.onCameraProfileSelected(custom.id)
        assertEquals(limited, vm.uiState.focalLengthPresets)
        assertFalse(vm.uiState.cameraLensKit.ignoreLensProfile)
        val restarted = vm()
        assertEquals(vm.uiState.cameraLensKit, restarted.uiState.cameraLensKit)
        assertEquals(limited, restarted.uiState.focalLengthPresets)
    }

    @Test fun `exact fractional optics and unrestricted manual input remain usable`() {
        val vm = vm()
        addPrime(vm, "135,5", "1,7")
        assertEquals(listOf(135.5), vm.uiState.focalLengthPresets)
        assertEquals(1.7, vm.uiState.aperturePresets.first(), 0.0)
        vm.onFocalLengthPresetSelected(135.5)
        assertEquals("135.5", vm.uiState.focalLengthText)
        assertEquals(135.5, vm.uiState.selectedFocalLengthPresetMm!!, 0.0)
        vm.onAperturePresetSelected(1.7)
        assertEquals("1.7", vm.uiState.apertureText)
        vm.onFocalLengthChanged("300")
        vm.onApertureChanged("1.2")
        assertFalse(vm.uiState.focalLengthHasError)
        assertFalse(vm.uiState.apertureHasError)
        assertNotNull(vm.uiState.result)
        val apertures = vm.uiState.aperturePresets
        vm.onFocalLengthChanged("24")
        assertEquals(apertures, vm.uiState.aperturePresets)
        vm.onApertureIncrementsSelected(ApertureIncrements.ThirdStop)
        assertTrue(vm.uiState.aperturePresets.contains(1.7))
        assertEquals("1.2", vm.uiState.apertureText)
    }

    @Test fun `lens changes and visibility do not alter any optical result in all six criteria`() {
        val vm = vm()
        vm.onFocalLengthChanged("100")
        vm.onApertureChanged("8")
        vm.onFocusDistanceChanged("0.3")
        for (mode in SharpnessCriterionMode.entries) {
            vm.onSharpnessModeSelected(mode)
            val before = vm.uiState
            addPrime(vm, "58", "1.7", "64")
            vm.onShowAllFocalLengths(!vm.uiState.cameraLensKit.showAllFocalLengths)
            vm.onShowAllApertures(!vm.uiState.cameraLensKit.showAllApertures)
            assertEquals(before.result, vm.uiState.result)
            assertEquals(before.effectiveCoCMm, vm.uiState.effectiveCoCMm)
            assertEquals(before.diffractionResult, vm.uiState.diffractionResult)
            assertEquals(before.diffractionSettings, vm.uiState.diffractionSettings)
            assertEquals(before.distanceSliderPosition, vm.uiState.distanceSliderPosition, 0.0)
            assertEquals(before.focalLengthText, vm.uiState.focalLengthText)
            assertEquals(before.apertureText, vm.uiState.apertureText)
            assertEquals(before.focusDistance, vm.uiState.focusDistance)
            assertEquals(before.sharpness, vm.uiState.sharpness)
            assertEquals(before.distanceScale!!.hyperfocalMeters, vm.uiState.distanceScale!!.hyperfocalMeters, 0.0)
        }
        vm.onFocusAtInfinity()
        addPrime(vm, "50", "1.4")
        assertSame(CalculatedDistance.Infinity, vm.uiState.focusDistance)
    }

    @Test fun `editing and deleting optics refresh shortcuts without touching camera data`() {
        val vm = vm()
        addPrime(vm, "58", "1.7")
        val id = vm.uiState.cameraLensKit.lenses.single().id
        vm.onEditLens(id)
        vm.onLensFieldChanged(LensField.PRIME_FOCAL, "105")
        vm.onLensFieldChanged(LensField.PRIME_APERTURE, "1.8")
        vm.onSaveLens()
        assertEquals(id, vm.uiState.cameraLensKit.lenses.single().id)
        assertEquals(listOf(105.0), vm.uiState.focalLengthPresets)
        assertFalse(vm.uiState.aperturePresets.contains(1.7))
        assertTrue(vm.uiState.aperturePresets.contains(1.8))
        assertEquals(custom, vm.uiState.presets.cameras.first { it.id == custom.id })
        vm.onDeleteLens(id)
        vm.onConfirmDeleteLens()
        assertEquals(CalculatorPresets.typicalFocalLengthsMm.map(Int::toDouble), vm.uiState.focalLengthPresets)
        assertEquals(ApertureIncrements.FullStop.apertures, vm.uiState.aperturePresets)
        vm.onCameraProfileSelected(BuiltInCameraProfiles.DEFAULT_ID)
        vm.onOpenCameraLenses(BuiltInCameraProfiles.DEFAULT_ID)
        vm.onAddLens()
        vm.onLensFieldChanged(LensField.PRIME_FOCAL, "50")
        vm.onLensFieldChanged(LensField.PRIME_APERTURE, "4")
        vm.onSaveLens()
        assertNull(vm.uiState.lensManager.editor)
        assertTrue(vm.uiState.cameraLensKit.lenses.isEmpty())
        assertEquals(CalculatorPresets.typicalFocalLengthsMm.map(Int::toDouble), vm.uiState.focalLengthPresets)
        assertEquals(BuiltInCameraProfiles.cameras, vm.uiState.presets.cameras.filter { it.isBuiltIn })
    }

    @Test fun `legacy built in attachments stay separate and deleting a camera closes its kit`() {
        val id = BuiltInCameraProfiles.DEFAULT_ID
        val legacy = CameraLensKit(listOf(Lens.Prime("legacy-lens", focalLengthMm = 58.0, maxAperture = 1.7)))
        val repository = InMemoryPresetRepository(PresetLibrary(
            cameras = BuiltInCameraProfiles.cameras + custom, selectedCameraId = custom.id,
            lensKitsByCameraId = mapOf(id to legacy)))
        val vm = DofCalculatorViewModel(repository, settings, scope)
        vm.onOpenCameraLenses(id)
        vm.onAddLens()
        assertNull(vm.uiState.lensManager.editor)
        assertTrue(vm.uiState.cameraLensKit.lenses.isEmpty())
        assertEquals(CalculatorPresets.typicalFocalLengthsMm.map(Int::toDouble), vm.uiState.focalLengthPresets)
        vm.onCameraProfileSelected(id)
        assertEquals(listOf(58.0), vm.uiState.focalLengthPresets)
        vm.onCameraProfileSelected(custom.id)
        addPrime(vm, "105", "2.8")
        vm.onDeleteCamera(custom.id)
        vm.onConfirmDeleteCamera()
        assertNull(vm.uiState.lensManager.cameraId)
        assertFalse(vm.uiState.presets.lensKitsByCameraId.containsKey(custom.id))
        assertEquals(id, vm.uiState.presets.selectedCameraId)
        assertEquals(listOf(58.0), vm.uiState.focalLengthPresets)
    }

    @Test fun `camera activation preserves focal length when lens profiles are ignored`() = runBlocking {
        val vm = vm()
        cameras.saveLens(custom.id, Lens.Prime("prime", focalLengthMm = 58.0, maxAperture = 1.7))
        cameras.saveLens(custom.id, Lens.Zoom("zoom", minFocalLengthMm = 48.25, maxFocalLengthMm = 217.0,
            maxApertureAtMinFocal = 3.5, maxApertureAtMaxFocal = 5.6))
        vm.onShowAllFocalLengths(true)
        vm.onFocalLengthChanged("300")
        vm.onApertureChanged("11")
        vm.onFocusDistanceChanged("0.6")
        vm.onSharpnessModeSelected(SharpnessCriterionMode.VIDEO_BASED)
        val before = vm.uiState
        vm.onCameraProfileSelected(BuiltInCameraProfiles.DEFAULT_ID)
        assertEquals("300", vm.uiState.focalLengthText)
        vm.onCameraProfileSelected(custom.id)
        assertEquals("300", vm.uiState.focalLengthText)
        assertEquals(300.0, vm.uiState.selectedFocalLengthPresetMm!!, 0.0)
        assertEquals(before.apertureText, vm.uiState.apertureText)
        assertEquals(before.focusDistance, vm.uiState.focusDistance)
        assertEquals(before.sharpness.copy(mode = SharpnessCriterionMode.STANDARD), vm.uiState.sharpness)
        assertEquals(before.diffractionSettings, vm.uiState.diffractionSettings)
        assertEquals(.030, vm.uiState.effectiveCoCMm!!, 0.0)
        vm.onFocusAtInfinity()
        vm.onFocalLengthChanged("500")
        vm.onCameraProfileSelected(BuiltInCameraProfiles.DEFAULT_ID)
        vm.onCameraProfileSelected(custom.id)
        assertEquals("500", vm.uiState.focalLengthText)
        assertSame(CalculatedDistance.Infinity, vm.uiState.focusDistance)
    }

    @Test fun `lens edits preference emissions and restart retain manual focal until explicit camera selection`() = runBlocking {
        val vm = vm()
        addPrime(vm, "58", "1.7")
        vm.onFocalLengthChanged("300")
        addPrime(vm, "20", "4")
        vm.onShowAllFocalLengths(true)
        vm.onShowAllApertures(true)
        vm.onLanguageSelected(AppLanguage.Russian)
        cameras.saveCamera(custom.copy(name = "Renamed"))
        assertEquals("300", vm.uiState.focalLengthText)
        val restarted = vm()
        assertEquals("300", restarted.uiState.focalLengthText)
        restarted.onCameraProfileSelected(custom.id)
        assertEquals("300", restarted.uiState.focalLengthText)
        restarted.onIgnoreLensProfileChanged(false)
        restarted.onCameraProfileSelected(BuiltInCameraProfiles.DEFAULT_ID)
        restarted.onCameraProfileSelected(custom.id)
        assertEquals("58", restarted.uiState.focalLengthText)
        val wide = restarted.uiState.cameraLensKit.lenses.single { (it as Lens.Prime).focalLengthMm == 20.0 }
        restarted.onLensSelected(wide.id)
        restarted.onFocalLengthChanged("300")
        restarted.onCameraProfileSelected(BuiltInCameraProfiles.DEFAULT_ID)
        restarted.onCameraProfileSelected(custom.id)
        assertEquals("20", restarted.uiState.focalLengthText)
    }

    @Test fun `repository camera changes select the minimum but empty cameras preserve focal in both modes`() = runBlocking {
        val vm = vm()
        addPrime(vm, "135,5", "1.7")
        for (mode in CalculatorMode.entries) {
            vm.onCalculatorModeSelected(mode)
            vm.onFocalLengthChanged("300")
            val distance = vm.uiState.focusDistanceText
            cameras.selectCamera(BuiltInCameraProfiles.DEFAULT_ID)
            assertEquals("300", vm.uiState.focalLengthText)
            cameras.selectCamera(custom.id)
            assertEquals("135.5", vm.uiState.focalLengthText)
            assertEquals(distance, vm.uiState.focusDistanceText)
            assertEquals(mode, vm.uiState.calculatorMode)
        }
    }

    @Test fun `no built in profile can create a new lens but legacy records remain editable`() = runBlocking {
        val vm = vm()
        for (camera in BuiltInCameraProfiles.cameras) {
            vm.onOpenCameraLenses(camera.id)
            vm.onAddLens()
            assertNull(vm.uiState.lensManager.editor)
            assertTrue(runCatching { cameras.saveLens(camera.id,
                Lens.Prime("new", focalLengthMm = 50.0, maxAperture = 4.0)) }.isFailure)
        }
        val builtId = BuiltInCameraProfiles.DEFAULT_ID
        val old = Lens.Prime("legacy", focalLengthMm = 80.0, maxAperture = 2.8)
        val repository = InMemoryPresetRepository(PresetLibrary(
            lensKitsByCameraId = mapOf(builtId to CameraLensKit(listOf(old)))))
        val legacyVm = DofCalculatorViewModel(repository, settings, scope)
        legacyVm.onOpenCameraLenses(builtId)
        legacyVm.onEditLens(old.id)
        legacyVm.onLensFieldChanged(LensField.PRIME_FOCAL, "85")
        legacyVm.onSaveLens()
        assertEquals(85.0, (legacyVm.uiState.cameraLensKit.lenses.single() as Lens.Prime).focalLengthMm, 0.0)
        legacyVm.onAddLens()
        assertNull(legacyVm.uiState.lensManager.editor)
    }

    @Test fun `adding a lens from camera editing preserves the camera draft and optical inputs`() {
        val vm = vm()
        vm.onFocalLengthChanged("300")
        val before = vm.uiState
        vm.onEditCamera(custom.id)
        vm.onCameraEditorNameChanged("Unapplied camera name")
        val draft = vm.uiState.presets.cameraEditor
        vm.onAddCameraEditorLens()
        assertEquals(custom.id, vm.uiState.lensManager.cameraId)
        vm.onLensFieldChanged(LensField.PRIME_FOCAL, "58")
        vm.onLensFieldChanged(LensField.PRIME_APERTURE, "1.7")
        vm.onSaveLens()
        assertNull(vm.uiState.lensManager.editor)
        assertSame(draft, vm.uiState.presets.cameraEditor)
        assertEquals(custom.name, vm.uiState.presets.cameras.single { it.id == custom.id }.name)
        assertEquals(1, vm.uiState.cameraLensKit.lenses.size)
        assertEquals(before.result, vm.uiState.result)
        assertEquals(before.focalLengthText, vm.uiState.focalLengthText)
        assertEquals(before.focusDistance, vm.uiState.focusDistance)
        vm.onCancelCameraEditor()
        assertNull(vm.uiState.lensManager.cameraId)
        assertNull(vm.uiState.presets.cameraEditor)
        // Lens operations keep their pre-existing independent persistence semantics.
        assertEquals(1, vm.uiState.cameraLensKit.lenses.size)
        assertEquals(custom.name, vm.uiState.presets.cameras.single { it.id == custom.id }.name)
    }

    @Test fun `inline lens edit deletion and cancellation return to the same camera form`() {
        val vm = vm()
        addPrime(vm, "58", "1.7")
        vm.onCloseCameraLenses()
        val id = vm.uiState.cameraLensKit.lenses.single().id
        vm.onEditCamera(custom.id)
        vm.onCameraEditorNameChanged("Saved camera name")
        vm.onEditCameraEditorLens(id)
        vm.onLensFieldChanged(LensField.PRIME_FOCAL, "105")
        vm.onSaveLens()
        assertEquals("Saved camera name", vm.uiState.presets.cameraEditor!!.nameText)
        assertEquals(105.0, (vm.uiState.cameraLensKit.lenses.single() as Lens.Prime).focalLengthMm, 0.0)
        vm.onDeleteCameraEditorLens(id)
        assertEquals(id, vm.uiState.lensManager.pendingDeleteId)
        vm.onCancelDeleteLens()
        assertEquals(1, vm.uiState.cameraLensKit.lenses.size)
        vm.onDeleteCameraEditorLens(id)
        vm.onConfirmDeleteLens()
        assertTrue(vm.uiState.cameraLensKit.lenses.isEmpty())
        assertNotNull(vm.uiState.presets.cameraEditor)
        vm.onAddCameraEditorLens()
        vm.onCancelLensEditor()
        assertNotNull(vm.uiState.presets.cameraEditor)
        vm.onSaveCameraEditor()
        assertNull(vm.uiState.lensManager.cameraId)
        assertNull(vm.uiState.presets.cameraEditor)
        assertEquals("Saved camera name", vm.uiState.presets.cameras.single { it.id == custom.id }.name)
    }

    @Test fun `unsaved camera has no lens mutations before its profile exists`() {
        val vm = vm()
        vm.onCreateCamera()
        vm.onCameraEditorNameChanged("New camera draft")
        vm.onAddCameraEditorLens()
        vm.onEditCameraEditorLens("missing")
        vm.onDeleteCameraEditorLens("missing")
        assertNull(vm.uiState.lensManager.cameraId)
        assertEquals("New camera draft", vm.uiState.presets.cameraEditor!!.nameText)
        assertTrue(vm.uiState.presets.lensKitsByCameraId.isEmpty())
    }

    @Test fun `type toggle retains both drafts and invalid or incomplete forms cannot save`() {
        val vm = vm()
        vm.onOpenCameraLenses(custom.id)
        vm.onAddLens()
        vm.onLensFieldChanged(LensField.PRIME_FOCAL, "58")
        vm.onLensFieldChanged(LensField.PRIME_APERTURE, "1.7")
        vm.onLensZoomChanged(true)
        vm.onLensFieldChanged(LensField.WIDE_FOCAL, "48")
        vm.onLensZoomChanged(false)
        assertEquals("58", vm.uiState.lensManager.editor!!.draft.fields[LensField.PRIME_FOCAL])
        vm.onLensZoomChanged(true)
        assertEquals("48", vm.uiState.lensManager.editor!!.draft.fields[LensField.WIDE_FOCAL])
        vm.onSaveLens()
        assertTrue(vm.uiState.lensManager.editor!!.showErrors)
        assertTrue(vm.uiState.cameraLensKit.lenses.isEmpty())
        for (bad in listOf("", "0", "-1", "NaN", "Infinity", ".")) {
            vm.onLensFieldChanged(LensField.WIDE_FOCAL, bad)
            vm.onSaveLens()
            assertNotNull(vm.uiState.lensManager.editor)
            assertTrue(vm.uiState.cameraLensKit.lenses.isEmpty())
        }
        vm.onCancelLensEditor()
        assertTrue(vm.uiState.cameraLensKit.lenses.isEmpty())
    }
}
