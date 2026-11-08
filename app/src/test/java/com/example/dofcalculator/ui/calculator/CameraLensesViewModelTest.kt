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

    @Test fun `all lenses form one sorted kit and Show All switches stay independent per camera`() {
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
        assertEquals(listOf(24.0, 28.0, 35.0, 50.0, 55.0, 58.0, 85.0, 90.0, 105.0, 120.0, 200.0),
            vm.uiState.focalLengthPresets)
        assertEquals(listOf(1.4, 2.0, 2.8, 4.0, 5.6, 8.0, 11.0, 16.0, 22.0, 32.0), vm.uiState.aperturePresets)
        val limited = vm.uiState.focalLengthPresets
        vm.onShowAllFocalLengths(true)
        assertTrue(vm.uiState.focalLengthPresets.containsAll(CalculatorPresets.typicalFocalLengthsMm.map(Int::toDouble)))
        assertFalse(vm.uiState.aperturePresets.contains(1.0))
        vm.onShowAllApertures(true)
        assertTrue(vm.uiState.aperturePresets.contains(1.0))
        vm.onShowAllFocalLengths(false)
        assertEquals(limited, vm.uiState.focalLengthPresets)
        assertTrue(vm.uiState.cameraLensKit.showAllApertures)
        vm.onCameraProfileSelected(BuiltInCameraProfiles.DEFAULT_ID)
        assertEquals(CalculatorPresets.typicalFocalLengthsMm.map(Int::toDouble), vm.uiState.focalLengthPresets)
        assertEquals(ApertureIncrements.FullStop.apertures, vm.uiState.aperturePresets)
        vm.onCameraProfileSelected(custom.id)
        assertEquals(limited, vm.uiState.focalLengthPresets)
        assertTrue(vm.uiState.cameraLensKit.showAllApertures)
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
        vm.onOpenMagnificationSettings()
        vm.onMacroFieldChanged(MacroInputField.NUMERATOR, "1")
        vm.onMacroFieldChanged(MacroInputField.DENOMINATOR, "1")
        vm.onMacroFieldChanged(MacroInputField.DISTANCE, "0.3")
        vm.onApplyMacroSettings()
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
        assertEquals(listOf(50.0), vm.uiState.focalLengthPresets)
        assertEquals(BuiltInCameraProfiles.cameras, vm.uiState.presets.cameras.filter { it.isBuiltIn })
    }

    @Test fun `nonselected camera attachments stay separate and deleting a camera closes its kit`() {
        val vm = vm()
        val id = BuiltInCameraProfiles.DEFAULT_ID
        vm.onOpenCameraLenses(id)
        vm.onAddLens()
        vm.onLensFieldChanged(LensField.PRIME_FOCAL, "58")
        vm.onLensFieldChanged(LensField.PRIME_APERTURE, "1.7")
        vm.onSaveLens()
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
