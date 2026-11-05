package com.example.dhikra.data

/** Helpers for Arabic text: digits, recitation weight, ayah markers and count parsing. */
object ArabicText {

    /** Arabic "end of ayah" sign. Fonts such as Amiri Quran draw it around the following digits. */
    const val END_OF_AYAH = '۝'

    fun toArabicDigits(value: String): String = buildString(value.length) {
        value.forEach { c -> append(if (c in '0'..'9') ('٠' + (c - '0')) else c) }
    }

    fun toArabicDigits(value: Int): String = toArabicDigits(value.toString())

    fun toLatinDigits(value: String): String = buildString(value.length) {
        value.forEach { c ->
            append(
                when (c) {
                    in '٠'..'٩' -> '0' + (c - '٠')
                    in '۰'..'۹' -> '0' + (c - '۰')
                    else -> c
                }
            )
        }
    }

    /**
     * The number of "spoken" letters in [word], ignoring diacritics (tashkeel), Quranic marks and
     * tatweel. Used to pace the reader highlight proportionally to the length of each word.
     */
    fun letterWeight(word: String): Int =
        word.count { c -> Character.isLetterOrDigit(c) && c != 'ـ' }

    fun totalLetters(text: String): Int = letterWeight(text)

    /**
     * Normalized form of [text] for searching: diacritics, Quranic marks and tatweel removed,
     * alef / hamza / ta marbuta / alef maqsura variants unified, digits made Latin.
     */
    fun searchKey(text: String): String = buildString(text.length) {
        toLatinDigits(text).lowercase().forEach { c ->
            when {
                c == 'ـ' -> Unit
                Character.getType(c) == Character.NON_SPACING_MARK.toInt() -> Unit
                c in "أإآٱ" -> append('ا')
                c == 'ة' -> append('ه')
                c == 'ى' -> append('ي')
                c == 'ؤ' -> append('و')
                c == 'ئ' -> append('ي')
                else -> append(c)
            }
        }
    }.replace(Regex("""\s+"""), " ").trim()

    private val ayahNumber = Regex("""[(﴿]\s*([0-9٠-٩۰-۹]+)\s*[)﴾]""")

    /** Converts "(12)" / "(١٢)" ayah numbers into the ornamental end-of-ayah sign. */
    fun withAyahMarkers(text: String): String = ayahNumber.replace(text) { m ->
        " $END_OF_AYAH${toArabicDigits(toLatinDigits(m.groupValues[1]))} "
    }.replace(Regex("""[ \t]{2,}"""), " ").trim()

    fun isAyahMarker(token: String): Boolean = token.firstOrNull() == END_OF_AYAH

    private val trailingCount =
        Regex("""\(\s*(?:([0-9٠-٩۰-۹]+)\s*(?:مرة|مره|مرات|مرّات)?|(مرة واحدة|مره واحده))\s*\)\s*[.،]?\s*$""")

    /**
     * Detects a trailing repeat count such as "(٣ مرات)", "(100 مرة)" or "(مرة واحدة)" at the end of
     * [text]. Returns the text without it and the parsed count, or null when none is present.
     */
    fun extractTrailingCount(text: String): Pair<String, Int>? {
        val match = trailingCount.find(text) ?: return null
        val count = match.groupValues[1].takeIf { it.isNotEmpty() }
            ?.let { toLatinDigits(it).toIntOrNull() } ?: 1
        return text.substring(0, match.range.first).trimEnd() to count.coerceAtLeast(1)
    }
}
