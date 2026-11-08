package com.example.dofcalculator.ui.calculator

import com.example.dofcalculator.data.BuiltInCameraProfiles
import com.example.dofcalculator.model.FrameFormat
import com.example.dofcalculator.settings.CalculatorMode
import org.junit.Assert.*
import org.junit.Test

class DofCalculatorPresetTest {
    @Test fun `all exact recommendations and IDs are stable and unique`() {
        val cameras = BuiltInCameraProfiles.cameras
        assertEquals(19, cameras.size)
        assertEquals(cameras.size, cameras.map { it.id }.distinct().size)
        assertEquals(listOf(.011,.015,.018,.018,.019,.023,.030,.047,.053,.059,.067,.083,.120,.110,.150,.220,
            .0365,.0365,.0445),
            cameras.map { it.standardCoCmm })
        assertTrue(cameras.all { it.isBuiltIn })
    }
    @Test fun `every built in selects CoC without changing independent optical inputs in both modes`() {
        val vm = DofCalculatorViewModel()
        vm.onFocalLengthChanged("105")
        vm.onApertureChanged("5.6")
        vm.onFocusDistanceChanged("7.5")
        for (mode in CalculatorMode.entries) {
            vm.onCalculatorModeSelected(mode)
            for (camera in BuiltInCameraProfiles.cameras) {
                vm.onCameraProfileSelected(camera.id)
                assertEquals(camera.standardCoCmm!!, vm.uiState.circleOfConfusionText.toDouble(), 0.0)
                assertEquals("105", vm.uiState.focalLengthText)
                assertEquals("5.6", vm.uiState.apertureText)
                assertEquals("7.5", vm.uiState.focusDistanceText)
                assertEquals(105.0 * 105.0 / (5.6 * camera.standardCoCmm!!) / 1000 + .105,
                    requireNotNull(vm.uiState.result).hyperfocalDistanceMeters, 1e-9)
            }
        }
    }
    private fun create(vm: DofCalculatorViewModel, coc: String = ""): String {
        vm.onCreateCamera()
        vm.onCameraEditorNameChanged("Yashica Mat 124G")
        vm.onCameraEditorFrameFormatSelected(FrameFormat.MediumFormat6x6)

        vm.onSaveCameraEditor()
        return vm.uiState.presets.cameras.single { !it.isBuiltIn }.id
    }
    @Test fun `default and custom CoC edit keeps ID and updates active profile`() {
        val vm = DofCalculatorViewModel()
        val id = create(vm)
        vm.onCameraProfileSelected(id)
        assertEquals(.053, vm.uiState.circleOfConfusionText.toDouble(), 0.0)
        vm.onEditCamera(id)
        vm.onCameraEditorFrameFormatSelected(FrameFormat.LargeFormat8x10)
        vm.onSaveCameraEditor()
        assertEquals(.220, vm.uiState.circleOfConfusionText.toDouble(), 0.0)
        vm.onEditCamera(id)
        vm.onCancelCameraEditor()
        vm.onCircleOfConfusionChanged("0,040")
        assertEquals(.040, vm.uiState.effectiveCoCMm!!, 0.0)
        assertEquals(id, vm.uiState.presets.cameras.single { !it.isBuiltIn }.id)
        vm.onDeleteCamera(id)
        assertTrue(vm.uiState.presets.cameras.any { it.id == id })
        vm.onConfirmDeleteCamera()
        assertFalse(vm.uiState.presets.cameras.any { it.id == id })
        assertEquals(BuiltInCameraProfiles.DEFAULT_ID, vm.uiState.presets.selectedCameraId)
        assertNotNull(vm.uiState.result)
    }
    @Test fun `invalid values and unnamed custom format keep editor open`() {
        for (value in listOf("NaN", "Infinity", "-1", "0", "0,")) {
            val vm = DofCalculatorViewModel()
            vm.onCreateCamera()
            vm.onCameraEditorNameChanged("Camera")
            vm.onCameraEditorFrameFormatSelected(FrameFormat.Custom)
            vm.onCameraCustomFormatNameChanged("Custom")
            vm.onCameraDimensionsChanged(true, value)
            vm.onCameraDimensionsChanged(false, "24")
            vm.onSaveCameraEditor()
            assertTrue(requireNotNull(vm.uiState.presets.cameraEditor).dimensionsHaveError)
        }
        val vm = DofCalculatorViewModel()
        vm.onCreateCamera()
        vm.onCameraEditorNameChanged(" ")
        vm.onCameraEditorFrameFormatSelected(FrameFormat.Custom)
        vm.onSaveCameraEditor()
        assertTrue(requireNotNull(vm.uiState.presets.cameraEditor).nameHasError)
        vm.onCameraEditorNameChanged("Panorama")
        vm.onCameraCustomFormatNameChanged("Experimental film")
        vm.onCameraDimensionsChanged(true, "36")
        vm.onCameraDimensionsChanged(false, "24")
        vm.onSaveCameraEditor()
        assertEquals(36.0, vm.uiState.presets.cameras.single { !it.isBuiltIn }.widthMm!!, 0.0)
    }
    @Test fun `built ins cannot be edited or queued for deletion`() {
        val vm = DofCalculatorViewModel()
        vm.onEditCamera(BuiltInCameraProfiles.DEFAULT_ID)
        assertNull(vm.uiState.presets.cameraEditor)
        vm.onDeleteCamera(BuiltInCameraProfiles.DEFAULT_ID)
        assertNull(vm.uiState.presets.pendingDeleteId)
    }
}
