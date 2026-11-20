package io.github.arcm111.dhikra.data

/**
 * The open-ended daily counter: each dhikr the user adds is counted without an end, towards
 * targets that keep rising — 100, 300, 500, 1 000, 3 000, 5 000, 10 000, then every 10 000. The
 * counts are part of the day's progress, so they start over every day; the list of adhkar stays.
 */
object DailyCounter {
    val TARGETS = listOf(100, 300, 500, 1_000, 3_000, 5_000, 10_000)
    private const val STEP_AFTER = 10_000

    /** Where today's count of [entryId] is kept, among the day's progress. */
    fun key(entryId: String) = "counter/$entryId"

    /** The target [count] is heading for: the first one above it. */
    fun target(count: Int): Int = TARGETS.firstOrNull { count < it } ?: ((count / STEP_AFTER) + 1) * STEP_AFTER

    /** The target reached before [count]'s (0 at first). */
    fun previous(count: Int): Int =
        if (count < TARGETS.last()) (listOf(0) + TARGETS).last { it <= count } else (count / STEP_AFTER) * STEP_AFTER

    /** How far [count] has come from the last target to the next, 0..1. */
    fun stage(count: Int): Float {
        val from = previous(count)
        return (count - from).toFloat() / (target(count) - from)
    }
}

/**
 * Favourites: library texts and groups the user marked, kept as references ("entry:<id>",
 * "group:<id>") in the order they were added.
 */
object Favourites {
    fun entry(entryId: String) = "entry:$entryId"
    fun group(groupId: String) = "group:$groupId"

    /** Prefix of the ids of the favourites' steps, so their progress stays apart from the programs'. */
    const val STEP_PREFIX = "fav:"

    /**
     * The favourites as a section the reader can go through: one step per text or group, with its
     * text(s) filled in. References to what the library no longer holds are left out.
     */
    fun section(refs: List<String>, library: Library, title: String): ProgramSection = ProgramSection(
        id = "favourites",
        title = title,
        items = refs.mapNotNull { ref ->
            val id = ref.substringAfter(':')
            val step = when {
                ref.startsWith("entry:") -> library.entry(id)?.let { ProgramItem(id = STEP_PREFIX + ref, type = it.type, entryId = id) }
                ref.startsWith("group:") -> library.group(id)?.let { ProgramItem(id = STEP_PREFIX + ref, type = it.type, groupId = id) }
                else -> null
            }
            step?.resolvedWith(library)
        },
    )
}
