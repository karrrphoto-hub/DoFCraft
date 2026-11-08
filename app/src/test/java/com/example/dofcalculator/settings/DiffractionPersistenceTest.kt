package com.example.dofcalculator.settings

import androidx.datastore.preferences.core.*
import com.example.dofcalculator.model.MagnificationMode
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test

class DiffractionPersistenceTest {
    @Test fun `diffraction preferences survive disk restart without changing other settings`() = runBlocking {
        val dir = Files.createTempDirectory("diffraction-settings").toFile()
        val file = File(dir, "settings.preferences_pb")
        var job = SupervisorJob()
        fun store() = PreferenceDataStoreFactory.create(
            scope = CoroutineScope(job + Dispatchers.IO), produceFile = { file })
        try {
            var data = store()
            data.edit { it[stringPreferencesKey("unrelated")] = "keep" }
            var repo = DataStoreAppSettingsRepository(data)
            assertEquals(DiffractionSettings(), repo.settings.first().diffraction)
            repo.setThemeMode(ThemeMode.Dark)
            repo.setLanguage(AppLanguage.Russian)
            val optical = CalculatorSettings(focalLengthText = "100", focusDistanceText = "0.6")
            repo.saveCalculatorSettings(optical)
            val expected = DiffractionSettings(false, MagnificationMode.MANUAL, .123456789012345)
            repo.saveDiffractionSettings(expected)
            job.cancelAndJoin()
            job = SupervisorJob()
            data = store()
            repo = DataStoreAppSettingsRepository(data)
            val restored = repo.settings.first()
            assertEquals(expected, restored.diffraction)
            assertEquals(optical, restored.calculator)
            assertEquals(ThemeMode.Dark, restored.themeMode)
            assertEquals(AppLanguage.Russian, restored.language)
            assertEquals("keep", data.data.first()[stringPreferencesKey("unrelated")])
            repo.saveDiffractionSettings(expected.copy(magnificationMode = MagnificationMode.AUTO))
            assertEquals(expected.manualMagnification, repo.settings.first().diffraction.manualMagnification, 0.0)
        } finally { job.cancelAndJoin(); dir.deleteRecursively() }
    }

    @Test fun `invalid stored magnification falls back safely`() = runBlocking {
        val dir = Files.createTempDirectory("diffraction-corrupt").toFile()
        val job = SupervisorJob()
        try {
            val data = PreferenceDataStoreFactory.create(
                scope = CoroutineScope(job + Dispatchers.IO),
                produceFile = { File(dir, "settings.preferences_pb") })
            val repo = DataStoreAppSettingsRepository(data)
            for (bad in listOf("0", "-1", "NaN", "Infinity", "broken")) {
                data.edit {
                    it[stringPreferencesKey("manual_magnification")] = bad
                    it[stringPreferencesKey("magnification_mode")] = "broken"
                    it[stringPreferencesKey("diffraction_warnings")] = "broken"
                }
                assertEquals(DiffractionSettings(), repo.settings.first().diffraction)
            }
        } finally { job.cancelAndJoin(); dir.deleteRecursively() }
    }

    @Test fun `all diffraction and magnification resources have Russian and English translations`() {
        fun strings(locale: String): Map<String, String> {
            val relative = "src/main/res/$locale/strings.xml"
            val file = File(relative).takeIf { it.exists() } ?: File("app/$relative")
            val nodes = javax.xml.parsers.DocumentBuilderFactory.newInstance()
                .newDocumentBuilder().parse(file).getElementsByTagName("string")
            return (0 until nodes.length).map { nodes.item(it) }.associate {
                it.attributes.getNamedItem("name").nodeValue to it.textContent
            }.filterKeys { it.startsWith("diffraction_") || it.startsWith("magnification_") || it.startsWith("macro_") }
        }
        val en = strings("values")
        val ru = strings("values-ru")
        assertTrue(en.size >= 40)
        assertEquals(en.keys, ru.keys)
        for (name in en.keys) {
            assertTrue(en.getValue(name).isNotBlank())
            assertTrue(ru.getValue(name).isNotBlank())
            val placeholders = Regex("%[0-9]+\\$[sdf]")
            assertEquals(placeholders.findAll(en.getValue(name)).map { it.value }.toList(),
                placeholders.findAll(ru.getValue(name)).map { it.value }.toList())
        }
    }
}
