package io.github.arcm111.dhikra.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class QuranTest {

    @Test
    fun ayahCounts_coverTheWholeQuran() {
        assertEquals(Quran.AYAH_COUNT, (1..Quran.SURAH_COUNT).sumOf { Quran.ayahCount(it) })
        assertEquals(7, Quran.ayahCount(1))
        assertEquals(286, Quran.ayahCount(2))
        assertEquals(6, Quran.ayahCount(114))
        assertEquals(0, Quran.ayahCount(0))
        assertEquals(0, Quran.ayahCount(115))
    }

    @Test
    fun ref_parsesAndFormats() {
        assertEquals(QuranRef(2, 255), QuranRef.parse("2:255"))
        assertEquals(QuranRef(3, 190, 200), QuranRef.parse("3:190-200"))
        assertEquals(QuranRef(3, 190, 200), QuranRef.parse(" ٣ : ١٩٠ - ٢٠٠ "))
        assertEquals("2:255", QuranRef(2, 255).toString())
        assertEquals("3:190-200", QuranRef(3, 190, 200).toString())
        assertEquals(QuranRef(18, 1, 10), QuranRef.parse(QuranRef(18, 1, 10).toString()))
    }

    @Test
    fun ref_rejectsMalformedOrOutOfRange() {
        listOf("", "x", "2", "2:", ":5", "2:255-", "0:1", "1:0", "115:1", "2:287", "1:1-8", "3:200-190", "2:255:1")
            .forEach { assertNull(it, QuranRef.parse(it)) }
        assertFalse(QuranRef(9, 130).isValid)
        assertTrue(QuranRef(9, 129).isValid)
    }

    @Test
    fun ref_opensWithBasmala_exceptAlFatihaAndAtTawbah() {
        assertTrue(QuranRef(18, 1, 10).opensWithBasmala)
        assertTrue(QuranRef(112, 1, 4).opensWithBasmala)
        assertFalse(QuranRef(18, 2, 10).opensWithBasmala)
        assertFalse(QuranRef(1, 1, 7).opensWithBasmala)
        assertFalse(QuranRef(9, 1, 5).opensWithBasmala)
    }

    @Test
    fun passage_takesTheBasmalaOutOfTheFirstAyah() {
        val passage = Quran.passage(
            QuranRef(112, 1, 2),
            listOf("${Quran.BASMALA} قُلْ هُوَ ٱللَّهُ أَحَدٌ", "ٱللَّهُ ٱلصَّمَدُ"),
        )!!
        assertEquals(listOf(Ayah(1, "قُلْ هُوَ ٱللَّهُ أَحَدٌ"), Ayah(2, "ٱللَّهُ ٱلصَّمَدُ")), passage.ayat)
        assertEquals("قُلْ هُوَ ٱللَّهُ أَحَدٌ (1) ٱللَّهُ ٱلصَّمَدُ (2)", passage.text)
    }

    @Test
    fun basmala_isRecognisedWhateverItsMarks() {
        // 95:1 and 97:1 write it «بِّسْمِ», with a shadda.
        assertEquals("وَٱلتِّينِ", Quran.withoutBasmala("بِّسْمِ ٱللَّهِ ٱلرَّحْمَـٰنِ ٱلرَّحِيمِ وَٱلتِّينِ"))
        // Words that only start like it, or an ayah made of the basmala alone, are kept.
        assertEquals("بِسْمِ ٱللَّهِ مَجْر۪ىٰهَا وَمُرْسَىٰهَآ", Quran.withoutBasmala("بِسْمِ ٱللَّهِ مَجْر۪ىٰهَا وَمُرْسَىٰهَآ"))
        assertEquals(Quran.BASMALA, Quran.withoutBasmala(Quran.BASMALA))
    }

    @Test
    fun passage_keepsTheBasmalaWhereItIsAnAyahOrPartOfOne() {
        // Al-Fatiha: the basmala is its first ayah.
        assertEquals(Quran.BASMALA, Quran.passage(QuranRef(1, 1), listOf(Quran.BASMALA))!!.ayat.single().text)
        // 27:30 quotes it in the middle of the surah.
        val ayah = "إِنَّهُۥ مِن سُلَيْمَـٰنَ وَإِنَّهُۥ ${Quran.BASMALA}"
        assertEquals(ayah, Quran.passage(QuranRef(27, 30), listOf(ayah))!!.ayat.single().text)
    }

    @Test
    fun passage_needsOneTextPerAyah() {
        assertNull(Quran.passage(QuranRef(112, 1, 4), listOf("a", "b")))
        assertNull(Quran.passage(QuranRef(112, 4, 5), listOf("a", "b")))
    }

    @Test
    fun library_getsTheTextOfItsPassages() {
        val kahf = LibraryEntry(id = "k", type = ItemType.QURAN, title = "k", text = "", quran = QuranRef(18, 1, 2))
        val typed = LibraryEntry(id = "t", type = ItemType.QURAN, title = "t", text = "نص")
        val dua = LibraryEntry(id = "d", type = ItemType.DUA, title = "d", text = "دعاء")
        val unreadable = kahf.copy(id = "u", text = "kept", quran = QuranRef(2, 255))
        val source = QuranSource { ref ->
            if (ref.sura == 18) Quran.passage(ref, listOf("${Quran.BASMALA} أ", "ب")) else null
        }
        val library = Library(listOf(kahf, typed, dua, unreadable)).withQuranText(source)
        assertEquals("أ (1) ب (2)", library.entry("k")?.text)
        assertEquals("نص", library.entry("t")?.text)
        assertEquals("دعاء", library.entry("d")?.text)
        assertEquals("kept", library.entry("u")?.text)
        // Nothing to read: the same library.
        val plain = Library(listOf(typed, dua))
        assertSame(plain, plain.withQuranText(source))
    }

    @Test
    fun opening_isTheChosenOneElseTheBasmalaOfAPassageOpeningItsSurah() {
        val ikhlas = LibraryEntry(type = ItemType.QURAN, title = "", text = "", quran = QuranRef(112, 1, 4))
        assertEquals(Quran.BASMALA, ikhlas.displayOpening)
        assertEquals("أعوذ بالله", ikhlas.copy(opening = "أعوذ بالله").displayOpening)
        assertEquals("", ikhlas.copy(quran = QuranRef(2, 255)).displayOpening)
        assertEquals("", ikhlas.copy(quran = null).displayOpening)

        val step = ProgramItem(id = "s", type = ItemType.QURAN, entryId = ikhlas.id)
        assertEquals(Quran.BASMALA, step.resolvedWith(Library(listOf(ikhlas)))?.opening)
    }

    @Test
    fun quranicSigns_areTheMarksWrittenAsWords() {
        listOf("ۚ", "ۖ", "ۗ", "ۛ", "ۜ", "۩", "۞").forEach { assertTrue(it, ArabicText.isQuranicSign(it)) }
        listOf("", "قُلْ", "${ArabicText.END_OF_AYAH}٣", "،", "(1)").forEach { assertFalse(it, ArabicText.isQuranicSign(it)) }
    }
}
