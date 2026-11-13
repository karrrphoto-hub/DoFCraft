package com.example.dofcalculator.ui.calculator

import java.io.File
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.*
import org.junit.Test

class CloseFocusFormatterTest {
    private fun resource(locale: String, name: String): String {
        val relative = "src/main/res/$locale/strings.xml"
        val file = File(relative).takeIf { it.exists() } ?: File("app/$relative")
        val nodes = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file).getElementsByTagName("string")
        return (0 until nodes.length).map { nodes.item(it) }.first {
            it.attributes.getNamedItem("name").nodeValue == name
        }.textContent
    }

    @Test fun `the localized resource and formatter produce approximate ratios without trailing zeros`() {
        for ((locale, folder) in listOf(Locale.US to "values", Locale.forLanguageTag("ru") to "values-ru")) {
            val template = resource(folder, "close_focus_approx_ratio")
            val decimal = if (folder == "values-ru") "3,7" else "3.7"
            for ((m, expected) in listOf(.2 to "5", .2679491924311227 to decimal, .5 to "2", 1.0 to "1")) {
                val denominator = CloseFocusFormatter.denominator(m, locale)
                assertEquals(expected, denominator)
                assertEquals("≈1:$expected", String.format(locale, template, denominator))
            }
        }
    }

    @Test fun `invalid and unrepresentable ratios never render zero Infinity or NaN`() {
        for (bad in listOf(0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY, 2.0, Double.MIN_VALUE))
            assertNull(CloseFocusFormatter.denominator(bad, Locale.US))
    }

    @Test fun `all close focus resources have matching translations and placeholders`() {
        fun names(folder: String): Map<String, String> {
            val relative = "src/main/res/$folder/strings.xml"
            val file = File(relative).takeIf { it.exists() } ?: File("app/$relative")
            val nodes = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file).getElementsByTagName("string")
            return (0 until nodes.length).map { nodes.item(it) }.associate {
                it.attributes.getNamedItem("name").nodeValue to it.textContent
            }.filterKeys { it.startsWith("close_focus_") }
        }
        val en = names("values")
        val ru = names("values-ru")
        assertEquals(12, en.size)
        assertEquals(en.keys, ru.keys)
        val placeholders = Regex("%[0-9]+\\$[sdf]")
        en.forEach { (key, value) ->
            assertTrue(value.isNotBlank())
            assertTrue(ru.getValue(key).isNotBlank())
            assertEquals(placeholders.findAll(value).map { it.value }.toList(),
                placeholders.findAll(ru.getValue(key)).map { it.value }.toList())
        }
    }
}
