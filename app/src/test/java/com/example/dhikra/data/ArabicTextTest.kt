package com.example.dhikra.data

import com.example.dhikra.reminder.ReminderScheduler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar

class ArabicTextTest {

    @Test
    fun digits_convertBothWays() {
        assertEquals("١٠٠", ArabicText.toArabicDigits(100))
        assertEquals("33", ArabicText.toLatinDigits("٣٣"))
        assertEquals("7", ArabicText.toLatinDigits("۷"))
    }

    @Test
    fun letterWeight_ignoresDiacritics() {
        assertEquals(5, ArabicText.letterWeight("سُبْحَانَ"))
        assertEquals(ArabicText.letterWeight("الرحمن"), ArabicText.letterWeight("ٱلرَّحۡمَٰنِ"))
    }

    @Test
    fun searchKey_ignoresMarksAndLetterVariants() {
        assertEquals(ArabicText.searchKey("الرحمن"), ArabicText.searchKey("ٱلرَّحۡمَٰنِ"))
        assertEquals(ArabicText.searchKey("الاخلاص"), ArabicText.searchKey("الإخلاص"))
        assertEquals(ArabicText.searchKey("سوره"), ArabicText.searchKey("سورة"))
        assertEquals("255", ArabicText.searchKey("٢٥٥"))
    }

    @Test
    fun ayahMarkers_replaceNumbersInParentheses() {
        val text = ArabicText.withAyahMarkers("أَبَدٗا (3) وَيُنذِرَ (١٠)")
        assertEquals("أَبَدٗا ${ArabicText.END_OF_AYAH}٣ وَيُنذِرَ ${ArabicText.END_OF_AYAH}١٠", text)
    }

    @Test
    fun trailingCount_isExtracted() {
        assertEquals("سبحان الله وبحمده" to 100, ArabicText.extractTrailingCount("سبحان الله وبحمده (١٠٠ مرة )"))
        assertEquals("قل هو الله أحد." to 3, ArabicText.extractTrailingCount("قل هو الله أحد. (٣مرات)"))
        assertEquals("آية" to 1, ArabicText.extractTrailingCount("آية (مرة واحدة)"))
        assertEquals("x" to 7, ArabicText.extractTrailingCount("x (7)"))
        assertNull(ArabicText.extractTrailingCount("لا يوجد عدد"))
    }

    @Test
    fun nextTrigger_isTodayWhenAheadOtherwiseTomorrow() {
        val now = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 4, 6, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val later = Calendar.getInstance().apply { timeInMillis = ReminderScheduler.nextTrigger(9 * 60, now) }
        assertEquals(4, later.get(Calendar.DAY_OF_MONTH))
        assertEquals(9, later.get(Calendar.HOUR_OF_DAY))

        val earlier = Calendar.getInstance().apply { timeInMillis = ReminderScheduler.nextTrigger(5 * 60, now) }
        assertEquals(5, earlier.get(Calendar.DAY_OF_MONTH))
        assertEquals(5, earlier.get(Calendar.HOUR_OF_DAY))
    }
}
