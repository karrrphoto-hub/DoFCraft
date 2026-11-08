package com.example.dofcalculator.ui.calculator

import com.example.dofcalculator.calculation.*
import com.example.dofcalculator.model.*
import com.example.dofcalculator.settings.*
import org.junit.Assert.*
import org.junit.Test

class VideoSharpnessViewModelTest {
    private fun video() = DofCalculatorViewModel().also {
        it.onSharpnessModeSelected(SharpnessCriterionMode.VIDEO_BASED)
    }

    @Test fun `reference hyperfocals and every consumer use one video CoC`() {
        val vm = video()
        vm.onFocalLengthChanged("24")
        vm.onApertureChanged("8")
        vm.onFocusDistanceChanged("3")
        for ((resolution, coc, h) in listOf(
            Triple(VideoResolution.FHD, .05625, 1.304),
            Triple(VideoResolution.UHD_4K, .028125, 2.584),
            Triple(VideoResolution.UHD_8K, .0140625, 5.144))) {
            vm.onVideoResolutionSelected(resolution)
            val s = vm.uiState
            assertEquals(coc, s.effectiveCoCMm!!, 1e-15)
            assertEquals(h, s.result!!.hyperfocalDistanceMeters, 1e-12)
            assertEquals(h, s.distanceScale!!.hyperfocalMeters, 1e-12)
            assertEquals(s.distanceScale.positionOf(CalculatedDistance.Finite(3.0)), s.distanceSliderPosition, 1e-15)
            assertEquals(DepthOfFieldCalculator.calculate(DepthOfFieldInput(24.0, 8.0, s.effectiveCoCMm, 3.0)), s.result)
            assertEquals(s.effectiveCoCMm, s.diffractionResult!!.effectiveCoCmm, 0.0)
            assertEquals(s.diffractionResult.airyDiameterMm / s.effectiveCoCMm, s.diffractionResult.ratio, 1e-15)
            vm.onCalculatorModeSelected(CalculatorMode.Hyperfocal)
            assertEquals(h, vm.uiState.focusDistanceMeters!!, 1e-12)
            assertEquals(coc, vm.uiState.diffractionResult!!.effectiveCoCmm, 1e-15)
            vm.onCalculatorModeSelected(CalculatorMode.DepthOfField)
            assertEquals(3.0, vm.uiState.focusDistanceMeters!!, 0.0)
        }
    }

    @Test fun `video edits preserve camera real optics infinity and warning settings`() {
        val vm = video()
        vm.onFocalLengthChanged("100")
        vm.onApertureChanged("11")
        vm.onFocusDistanceChanged("7.25")
        val camera = vm.uiState.presets.selectedCameraId
        val beforeH = vm.uiState.distanceScale!!.hyperfocalMeters
        vm.onVideoResolutionSelected(VideoResolution.FHD)
        vm.onVideoFieldChanged(VideoField.CROP, "1,5")
        vm.onVideoFieldChanged(VideoField.BLUR, "2.5")
        assertEquals(camera, vm.uiState.presets.selectedCameraId)
        assertEquals("100", vm.uiState.focalLengthText)
        assertEquals("11", vm.uiState.apertureText)
        assertEquals(7.25, vm.uiState.focusDistanceMeters!!, 0.0)
        assertNotEquals(beforeH, vm.uiState.distanceScale!!.hyperfocalMeters)
        vm.onFocusAtInfinity()
        vm.onVideoResolutionSelected(VideoResolution.UHD_8K)
        assertSame(CalculatedDistance.Infinity, vm.uiState.focusDistance)
        assertEquals(1.0, vm.uiState.distanceSliderPosition, 0.0)
        vm.onFocusDistanceChanged("0.45")
        val distance = vm.uiState.focusDistance
        val diffraction = vm.uiState.diffractionSettings
        vm.onVideoResolutionSelected(VideoResolution.UHD_4K)
        vm.onVideoFieldChanged(VideoField.CROP, "2")
        assertEquals(distance, vm.uiState.focusDistance)
        assertEquals(diffraction, vm.uiState.diffractionSettings)
        assertEquals(.014762, vm.uiState.diffractionResult!!.airyDiameterMm, 1e-15)
    }

    @Test fun `camera changes recalculate auto area and incompatible manual dimensions require correction`() {
        val vm = video()
        vm.onCameraProfileSelected("fujifilm_gfx")
        assertEquals(43.8, vm.uiState.videoResult!!.activeArea.widthMm, 1e-12)
        assertEquals(24.6375, vm.uiState.videoResult!!.activeArea.heightMm, 1e-12)
        vm.onVideoFieldChanged(VideoField.ACTIVE_WIDTH, "43.8")
        vm.onVideoFieldChanged(VideoField.ACTIVE_HEIGHT, "24.6375")
        vm.onVideoAreaModeSelected(VideoAreaMode.MANUAL)
        assertNotNull(vm.uiState.result)
        val fullFrame = vm.uiState.presets.cameras.first { it.frameFormat == FrameFormat.FullFrame35Mm }
        vm.onCameraProfileSelected(fullFrame.id)
        assertNull(vm.uiState.effectiveCoCMm)
        assertNull(vm.uiState.result)
        assertNull(vm.uiState.distanceScale)
        assertNull(vm.uiState.diffractionResult)
        assertEquals(VideoError.AREA_EXCEEDS_SENSOR, vm.uiState.videoError)
        assertTrue(VideoField.ACTIVE_WIDTH in vm.uiState.videoInvalidFields)
        vm.onVideoAreaModeSelected(VideoAreaMode.AUTO)
        assertEquals(VideoActiveArea(36.0, 20.25), vm.uiState.videoResult!!.activeArea)
        assertEquals(43.8, vm.uiState.sharpness.video.manualWidthMm, 0.0)
    }

    @Test fun `switching criteria presets and area modes retains independent settings`() {
        val vm = video()
        vm.onVideoResolutionSelected(VideoResolution.CUSTOM)
        vm.onVideoFieldChanged(VideoField.WIDTH, "2000")
        vm.onVideoFieldChanged(VideoField.HEIGHT, "1000")
        vm.onVideoFieldChanged(VideoField.BLUR, "1,5")
        vm.onVideoFieldChanged(VideoField.CROP, "1.7")
        vm.onVideoFieldChanged(VideoField.ACTIVE_WIDTH, "24")
        vm.onVideoFieldChanged(VideoField.ACTIVE_HEIGHT, "12")
        vm.onVideoAreaModeSelected(VideoAreaMode.MANUAL)
        val saved = vm.uiState.sharpness.video
        val coc = vm.uiState.effectiveCoCMm
        for (mode in SharpnessCriterionMode.entries) {
            vm.onSharpnessModeSelected(mode)
            assertEquals(saved, vm.uiState.sharpness.video)
        }
        vm.onSharpnessModeSelected(SharpnessCriterionMode.VIDEO_BASED)
        assertEquals(coc, vm.uiState.effectiveCoCMm)
        vm.onVideoAreaModeSelected(VideoAreaMode.AUTO)
        assertEquals(1.7, vm.uiState.videoResult!!.cropFactor!!, 0.0)
        vm.onVideoResolutionSelected(VideoResolution.UHD_4K)
        assertNotNull(vm.uiState.result)
        vm.onVideoResolutionSelected(VideoResolution.CUSTOM)
        assertEquals(2000, vm.uiState.sharpness.video.widthPx)
        assertEquals(1000, vm.uiState.sharpness.video.heightPx)
    }

    @Test fun `invalid drafts never feed optics and inactive fields do not invalidate other modes`() {
        val vm = video()
        for (text in listOf("", "0", "-1", "NaN", "Infinity", "1,", "1e999")) {
            vm.onVideoFieldChanged(VideoField.BLUR, text)
            assertNull(vm.uiState.effectiveCoCMm)
            assertNull(vm.uiState.result)
            assertNull(vm.uiState.diffractionResult)
            assertTrue(VideoField.BLUR in vm.uiState.videoInvalidFields)
        }
        vm.onSharpnessModeSelected(SharpnessCriterionMode.STANDARD)
        assertEquals(.03, vm.uiState.effectiveCoCMm!!, 0.0)
        vm.onSharpnessModeSelected(SharpnessCriterionMode.VIDEO_BASED)
        assertNull(vm.uiState.result)
        vm.onVideoFieldChanged(VideoField.BLUR, "2,5")
        assertNotNull(vm.uiState.result)
        vm.onVideoResolutionSelected(VideoResolution.CUSTOM)
        vm.onVideoFieldChanged(VideoField.WIDTH, "3840.5")
        assertNull(vm.uiState.result)
        vm.onVideoResolutionSelected(VideoResolution.UHD_4K)
        assertNotNull(vm.uiState.result)
        vm.onVideoFieldChanged(VideoField.ACTIVE_WIDTH, "")
        vm.onVideoAreaModeSelected(VideoAreaMode.MANUAL)
        assertNull(vm.uiState.result)
        vm.onVideoAreaModeSelected(VideoAreaMode.AUTO)
        assertNotNull(vm.uiState.result)
        vm.onVideoFieldChanged(VideoField.CROP, ".5")
        assertNull(vm.uiState.result)
        vm.onVideoFieldChanged(VideoField.CROP, "1")
        assertNotNull(vm.uiState.result)
    }
}
