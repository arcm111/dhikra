package com.example.dhikra.ui.library

import com.example.dhikra.data.QuranRef
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QuranPassagePickerTest {

    @Test
    fun fields_nameAPassage() {
        assertEquals(QuranRef(3, 190, 200), quranRefOf(3, "190", "200"))
        assertEquals(QuranRef(3, 190, 200), quranRefOf(3, "١٩٠", " ٢٠٠ "))
        assertEquals(QuranRef(2, 255), quranRefOf(2, "255", "255"))
    }

    @Test
    fun fields_nameNothingWhileIncompleteOrOutOfRange() {
        assertNull(quranRefOf(0, "1", "1")) // no surah picked yet
        assertNull(quranRefOf(3, "", "200"))
        assertNull(quranRefOf(3, "190", ""))
        assertNull(quranRefOf(3, "190", "2")) // still typing «200»
        assertNull(quranRefOf(3, "0", "5"))
        assertNull(quranRefOf(3, "190", "201"))
        assertNull(quranRefOf(115, "1", "1"))
    }
}
