package com.example.dhikra.ui

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class AppLanguageTest {

    @After
    fun backToArabic() {
        AppLanguage.current = AppLanguage.AR
    }

    @Test
    fun arabic_hasArabicDigitsAndSuffixes() {
        AppLanguage.current = AppLanguage.AR
        assertEquals("٥:٠٧ ص", formatTime(5 * 60 + 7))
        assertEquals("١٢:٠٠ م", formatTime(12 * 60))
        assertEquals("١٢:٣٠ ص", formatTime(24 * 60 + 30)) // past midnight wraps
        assertEquals("١٢٣", uiDigits(123))
    }

    @Test
    fun english_hasLatinDigitsAndAmPm() {
        AppLanguage.current = AppLanguage.EN
        assertEquals("5:07 AM", formatTime(5 * 60 + 7))
        assertEquals("12:00 PM", formatTime(12 * 60))
        assertEquals("11:59 PM", formatTime(23 * 60 + 59))
        assertEquals("123", uiDigits(123))
        assertEquals("12", uiDigits("١٢")) // typed in Arabic digits, shown in Latin
    }

    /** Every interface string has its English version, and no English string is left over. */
    @Test
    fun englishStrings_coverEveryKey() {
        fun keys(path: String): Set<String> {
            val file = File(path).takeIf { it.exists() } ?: File("app/$path")
            val root = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file).documentElement
            val nodes = root.childNodes
            return (0 until nodes.length).map { nodes.item(it) }
                .filter { it.nodeType == org.w3c.dom.Node.ELEMENT_NODE }
                .map { it as org.w3c.dom.Element }
                .filter { it.getAttribute("translatable") != "false" }
                .map { it.getAttribute("name") }
                .toSet()
        }
        val arabic = keys("src/main/res/values/strings.xml")
        val english = keys("src/main/res/values-en/strings.xml")
        assertTrue("missing in English: ${arabic - english}", (arabic - english).isEmpty())
        assertTrue("only in English: ${english - arabic}", (english - arabic).isEmpty())
    }
}
