package com.example.dofcalculator.settings

import androidx.datastore.preferences.core.*
import com.example.dofcalculator.data.InMemoryPresetRepository
import com.example.dofcalculator.ui.calculator.DofCalculatorViewModel
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test

class DiffractionPersistenceTest {
    @Test fun `ordinary diffraction preferences survive disk restart without changing other settings`() = runBlocking {
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
            val expected = DiffractionSettings(warningsEnabled = false)
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
        } finally { job.cancelAndJoin(); dir.deleteRecursively() }
    }

    @Test fun `legacy macro records remain verbatim and old active modes load safely`() = runBlocking {
        val dir = Files.createTempDirectory("legacy-optics").toFile()
        val job = SupervisorJob()
        val legacy = mapOf(
            "magnification_mode" to "MANUAL", "manual_magnification" to "2.3456789012345",
            "manual_pair_version" to "1", "manual_distance_m" to "0.3",
            "manual_focal_mm" to "100", "manual_confirmed" to "true",
            "legacy_manual_magnification" to "0.875", "focus_magnification" to "0.5",
            "macro_photography_warning" to "false")
        try {
            val data = PreferenceDataStoreFactory.create(scope = CoroutineScope(job + Dispatchers.IO),
                produceFile = { File(dir, "settings.preferences_pb") })
            val repo = DataStoreAppSettingsRepository(data)
            for ((storedMode, expected) in listOf(
                "DepthOfField" to CalculatorMode.DepthOfField,
                "Macro" to CalculatorMode.DepthOfField,
                "Hyperfocal" to CalculatorMode.Hyperfocal)) {
                data.edit { preferences ->
                    legacy.forEach { (key, value) -> preferences[stringPreferencesKey(key)] = value }
                    preferences[stringPreferencesKey("calculator_mode")] = storedMode
                    preferences[stringPreferencesKey("last_focal_length")] = "100"
                    preferences[stringPreferencesKey("last_aperture")] = "8"
                    preferences[stringPreferencesKey("last_focus_distance")] = "0.3"
                }
                val loaded = repo.settings.first()
                assertEquals(expected, loaded.calculator.mode)
                assertEquals("0.3", loaded.calculator.focusDistanceText)
                val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
                try {
                    val vm = DofCalculatorViewModel(InMemoryPresetRepository(),
                        InMemoryAppSettingsRepository(loaded), scope)
                    assertEquals(expected, vm.uiState.calculatorMode)
                    assertEquals("0.3", vm.uiState.focusDistanceText)
                    assertEquals(.010736, vm.uiState.diffractionResult!!.airyDiameterMm, 1e-15)
                } finally { scope.cancel() }
                repo.saveCalculatorSettings(loaded.calculator)
                repo.saveSharpnessSettings(loaded.sharpness)
                repo.saveDiffractionSettings(DiffractionSettings(false))
                repo.setThemeMode(ThemeMode.Dark)
                repo.setLanguage(AppLanguage.Russian)
                repo.setDistanceDisplay(DistanceUnit.Centimeters, DistanceRounding.None)
                val after = data.data.first()
                legacy.forEach { (key, value) -> assertEquals(key, value, after[stringPreferencesKey(key)]) }
                assertEquals(loaded.sharpness, repo.settings.first().sharpness)
                assertEquals("0.3", repo.settings.first().calculator.focusDistanceText)
            }
        } finally { job.cancelAndJoin(); dir.deleteRecursively() }
    }

    @Test fun `corrupt unused macro values never invalidate ordinary settings`() = runBlocking {
        val dir = Files.createTempDirectory("diffraction-corrupt").toFile()
        val job = SupervisorJob()
        try {
            val data = PreferenceDataStoreFactory.create(scope = CoroutineScope(job + Dispatchers.IO),
                produceFile = { File(dir, "settings.preferences_pb") })
            val repo = DataStoreAppSettingsRepository(data)
            for (bad in listOf("0", "-1", "NaN", "Infinity", "broken")) {
                data.edit {
                    it[stringPreferencesKey("manual_magnification")] = bad
                    it[stringPreferencesKey("manual_distance_m")] = bad
                    it[stringPreferencesKey("magnification_mode")] = bad
                    it[stringPreferencesKey("diffraction_warnings")] = bad
                }
                assertEquals(DiffractionSettings(), repo.settings.first().diffraction)
                repo.saveDiffractionSettings(DiffractionSettings(false))
                assertEquals(bad, data.data.first()[stringPreferencesKey("manual_magnification")])
            }
        } finally { job.cancelAndJoin(); dir.deleteRecursively() }
    }

    @Test fun `ordinary diffraction resources are localized and no macro controls remain`() {
        fun strings(locale: String): Map<String, String> {
            val relative = "src/main/res/$locale/strings.xml"
            val file = File(relative).takeIf { it.exists() } ?: File("app/$relative")
            val nodes = javax.xml.parsers.DocumentBuilderFactory.newInstance()
                .newDocumentBuilder().parse(file).getElementsByTagName("string")
            return (0 until nodes.length).map { nodes.item(it) }.associate {
                it.attributes.getNamedItem("name").nodeValue to it.textContent
            }
        }
        val allEn = strings("values")
        val allRu = strings("values-ru")
        assertTrue(allEn.keys.none { it.startsWith("macro_") || it.startsWith("magnification_") || it.startsWith("focus_macro_") })
        assertTrue(allRu.keys.none { it.startsWith("macro_") || it.startsWith("magnification_") || it.startsWith("focus_macro_") })
        val en = allEn.filterKeys { it.startsWith("diffraction_") }
        val ru = allRu.filterKeys { it.startsWith("diffraction_") }
        assertTrue(en.size >= 10)
        assertEquals(en.keys, ru.keys)
        assertTrue(en.containsKey("diffraction_conventional_help"))
        for (name in en.keys) {
            assertTrue(en.getValue(name).isNotBlank())
            assertTrue(ru.getValue(name).isNotBlank())
            val placeholders = Regex("%[0-9]+\\$[sdf]")
            assertEquals(placeholders.findAll(en.getValue(name)).map { it.value }.toList(),
                placeholders.findAll(ru.getValue(name)).map { it.value }.toList())
        }
    }
}
