package com.example.dhikra.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** The bundled Quran database (assets/quran.db) as the app reads it. */
@RunWith(AndroidJUnit4::class)
class QuranDatabaseTest {

    private val quran = QuranDatabase.get(InstrumentationRegistry.getInstrumentation().targetContext)

    private fun key(ref: QuranRef, ayah: Int = 0): String =
        ArabicText.searchKey(quran.passage(ref)!!.ayat[ayah].text)

    @Test
    fun everySurahHoldsTheAyatOfTheStaticCounts() {
        var total = 0
        for (sura in 1..Quran.SURAH_COUNT) {
            val passage = quran.passage(QuranRef(sura, 1, Quran.ayahCount(sura)))
            assertNotNull("surah $sura", passage)
            assertEquals((1..Quran.ayahCount(sura)).toList(), passage!!.ayat.map { it.number })
            assertTrue("surah $sura", passage.ayat.all { it.text.isNotBlank() })
            total += passage.ayat.size
        }
        assertEquals(Quran.AYAH_COUNT, total)
    }

    @Test
    fun surahs_matchTheStaticCounts() {
        val surahs = quran.surahs()
        assertEquals((1..Quran.SURAH_COUNT).toList(), surahs.map { it.number })
        assertEquals((1..Quran.SURAH_COUNT).map(Quran::ayahCount), surahs.map { it.ayahCount })
        assertEquals("الفاتحة", surahs.first().name)
        assertEquals("الناس", surahs.last().name)
        assertTrue(surahs.all { it.name.isNotBlank() })
    }

    @Test
    fun basmalaConstant_isAlFatihasFirstAyah() {
        assertEquals(Quran.BASMALA, quran.passage(QuranRef(1, 1))!!.ayat.single().text)
    }

    @Test
    fun basmala_isTakenOutOfTheFirstAyahOfTheSurahs() {
        assertEquals("قل هو الله احد", key(QuranRef(112, 1)))
        // Written «بِّسْمِ» in these two.
        assertTrue(key(QuranRef(95, 1)).startsWith("والتين"))
        assertTrue(key(QuranRef(97, 1)).startsWith("انا انزلنه"))
        // At-Tawbah has none; 27:30 quotes it within the ayah.
        assertFalse(key(QuranRef(9, 1)).startsWith("بسم"))
        assertTrue(key(QuranRef(27, 30)).endsWith("بسم الله الرحمن الرحيم"))
        // Only the first ayah of a passage starting its surah is concerned.
        assertTrue(key(QuranRef(18, 1, 10)).startsWith("الحمد لله"))
        assertEquals(10, quran.passage(QuranRef(18, 1, 10))!!.ayat.size)
    }

    @Test
    fun ayatAlKursi() {
        val passage = quran.passage(QuranRef(2, 255))!!
        val text = ArabicText.searchKey(passage.ayat.single().text)
        assertTrue(text.startsWith("الله لا اله"))
        assertTrue(text.endsWith("وهو العلي العظيم"))
        assertTrue(passage.text.endsWith("(255)"))
    }

    @Test
    fun invalidPassages_areNotRead() {
        assertNull(quran.passage(QuranRef(2, 287)))
        assertNull(quran.passage(QuranRef(115, 1)))
        assertNull(quran.passage(QuranRef(3, 200, 190)))
    }
}
