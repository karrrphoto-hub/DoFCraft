package com.example.dofcalculator.data

import com.example.dofcalculator.model.*
import org.junit.Assert.*
import org.junit.Test

class CameraPresetCodecTest {
    @Test fun `legacy CoC is archived separately from geometry and obsolete lenses`() {
        val payload = "camera\tid\tYashica+Mat+124G\tMediumFormat6x6\t0.050\nlens\told\tYashinon\t80\t3.5"
        val decoded = requireNotNull(CameraPresetCodec.decode(payload))
        assertEquals("Yashica Mat 124G", decoded.name)
        assertEquals(FrameFormat.MediumFormat6x6, decoded.frameFormat)
        assertEquals(.050, CameraPresetCodec.legacyCoC(payload)!!, 0.0)
        assertEquals(.053, decoded.standardCoCmm!!, 0.0)
        assertEquals(56.0, decoded.widthMm!!, 0.0)
        assertEquals(decoded, CameraPresetCodec.decode(CameraPresetCodec.encode(decoded)))
    }
    @Test fun `invalid legacy CoC does not destroy camera identity or crash`() {
        for (value in listOf("NaN", "Infinity", "0", "-1")) {
            val payload = "camera\tid\tName\tFullFrame35Mm\t$value"
            assertNotNull(CameraPresetCodec.decode(payload))
            assertNull(CameraPresetCodec.legacyCoC(payload))
        }
        assertNull(CameraPresetCodec.decode("broken"))
    }
    @Test fun `custom geometry survives encoding without a CoC`() {
        val camera = CameraProfile("custom", "Panorama", FrameFormat.Custom,
            customFormatName = "Wide", widthMm = 56.0, heightMm = 112.0)
        assertEquals(camera, CameraPresetCodec.decode(CameraPresetCodec.encode(camera)))
    }
}
