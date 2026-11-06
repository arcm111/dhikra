package com.example.dhikra.data

/**
 * A passage of the Quran: ayat [from] to [to] of surah [sura], numbered like the Madinah mushaf
 * (Hafs, Kufan count) of the bundled Tanzil text. A passage stays within one surah: texts spanning
 * several surahs are a [LibraryGroup] of passages.
 */
data class QuranRef(val sura: Int, val from: Int, val to: Int = from) {

    val isValid: Boolean
        get() = sura in 1..Quran.SURAH_COUNT && from >= 1 && to >= from && to <= Quran.ayahCount(sura)

    val ayahCount: Int get() = to - from + 1

    /**
     * Whether the passage starts its surah with the basmala: every surah but Al-Fatiha (where it is
     * the first ayah) and At-Tawbah (which has none).
     */
    val opensWithBasmala: Boolean get() = from == 1 && sura != 1 && sura != 9

    /** "2:255" or "3:190-200": the form stored in program files. */
    override fun toString(): String = if (from == to) "$sura:$from" else "$sura:$from-$to"

    companion object {
        private val pattern = Regex("""^\s*(\d{1,3})\s*:\s*(\d{1,3})\s*(?:-\s*(\d{1,3})\s*)?$""")

        /** Parses "2:255" or "3:190-200" (Latin or Arabic digits); null when malformed or out of range. */
        fun parse(value: String): QuranRef? {
            val match = pattern.find(ArabicText.toLatinDigits(value)) ?: return null
            val (sura, from, to) = match.destructured
            return QuranRef(sura.toInt(), from.toInt(), to.ifEmpty { from }.toInt()).takeIf { it.isValid }
        }
    }
}

data class Ayah(val number: Int, val text: String)

/**
 * The text of a [QuranRef], ayah by ayah, as written in the bundled Quran text. The basmala that
 * text writes at the start of the first ayah of a surah is left out: it is not part of the ayah,
 * and is shown as the passage's opening instead (see [LibraryEntry.displayOpening]).
 */
data class QuranPassage(val ref: QuranRef, val ayat: List<Ayah>) {
    /** The ayat as the program shows them, each followed by its number: "… (1) … (2)". */
    val text: String get() = ayat.joinToString(" ") { "${it.text} (${it.number})" }
}

/** Reads the text of Quran passages. Blocking: call it off the main thread. */
fun interface QuranSource {
    /** The passage [ref], or null when it is not a valid passage or can't be read. */
    fun passage(ref: QuranRef): QuranPassage?
}

/** A surah of the bundled Quran text: its [number] (1 to 114), [name] and number of ayat. */
data class Surah(val number: Int, val name: String, val ayahCount: Int)

/** What the screens read from the bundled Quran text, without blocking the caller. */
interface QuranLookup {
    /** The 114 surahs in order, or an empty list when they can't be read. */
    suspend fun surahs(): List<Surah>

    /** The passage [ref], or null when it is not a valid passage or can't be read. */
    suspend fun passage(ref: QuranRef): QuranPassage?
}

object Quran {
    const val SURAH_COUNT = 114
    const val AYAH_COUNT = 6236

    /** The basmala exactly as the bundled text writes it (Al-Fatiha 1:1). */
    const val BASMALA =
        "بِسْمِ ٱللَّهِ " +
            "ٱلرَّحْمَـٰنِ " +
            "ٱلرَّحِيمِ"

    /** Number of ayat of each surah, in order. Checked against the database by its tests. */
    private val AYAH_COUNTS = intArrayOf(
        7, 286, 200, 176, 120, 165, 206, 75, 129, 109, 123, 111, 43, 52, 99, 128, 111, 110, 98, 135,
        112, 78, 118, 64, 77, 227, 93, 88, 69, 60, 34, 30, 73, 54, 45, 83, 182, 88, 75, 85,
        54, 53, 89, 59, 37, 35, 38, 29, 18, 45, 60, 49, 62, 55, 78, 96, 29, 22, 24, 13,
        14, 11, 11, 18, 12, 12, 30, 52, 52, 44, 28, 28, 20, 56, 40, 31, 50, 40, 46, 42,
        29, 19, 36, 25, 22, 17, 19, 26, 30, 20, 15, 21, 11, 8, 8, 19, 5, 8, 8, 11,
        11, 8, 3, 9, 5, 4, 7, 3, 6, 3, 5, 4, 5, 6,
    )

    private val BASMALA_KEY = ArabicText.searchKey(BASMALA)

    /** Number of ayat of [sura], or 0 when there is no such surah. */
    fun ayahCount(sura: Int): Int = AYAH_COUNTS.getOrElse(sura - 1) { 0 }

    /**
     * The passage [ref] made of its ayat's [texts] as stored in the database, in order, or null
     * when [texts] doesn't hold exactly one text per ayah.
     */
    fun passage(ref: QuranRef, texts: List<String>): QuranPassage? {
        if (!ref.isValid || texts.size != ref.ayahCount) return null
        val ayat = texts.mapIndexed { index, text ->
            val number = ref.from + index
            Ayah(number, if (number == 1 && ref.opensWithBasmala) withoutBasmala(text) else text)
        }
        return QuranPassage(ref, ayat)
    }

    /**
     * [text] without its first four words when they are the basmala, whatever its marks: 95:1 and
     * 97:1 write it «بِّسْمِ», with a shadda.
     */
    fun withoutBasmala(text: String): String {
        val words = text.split(' ', limit = 5)
        if (words.size < 5) return text
        return if (ArabicText.searchKey(words.take(4).joinToString(" ")) == BASMALA_KEY) words[4] else text
    }
}

/**
 * This library with the text of its Quran passages read from [source]. An entry whose passage can't
 * be read keeps the text it had: its reference, the only thing stored, is never lost.
 */
fun Library.withQuranText(source: QuranSource): Library {
    if (entries.none { it.quran != null }) return this
    return copy(
        entries = entries.map { entry ->
            val ref = entry.quran ?: return@map entry
            source.passage(ref)?.let { entry.copy(text = it.text) } ?: entry
        }
    )
}
