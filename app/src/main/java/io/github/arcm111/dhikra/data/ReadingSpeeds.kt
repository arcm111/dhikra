package io.github.arcm111.dhikra.data

import android.content.Context
import java.util.concurrent.Executors

/**
 * The reading speed (letters per second) the reader's play follows, learned from the user's taps:
 * each repetition read while playing moves the text's own speed, and a little the default one,
 * towards the pace measured. The section reader keeps it per step; a daily counter per dhikr
 * ([counterKey]), in the app as in the floating counter.
 */
object ReadingSpeeds {

    /** A repetition faster than this is a slip of the finger, not a reading. */
    const val MIN_MEASURABLE_MS = 400L
    private const val ITEM_SMOOTHING = 0.5f
    private const val DEFAULT_SMOOTHING = 0.25f

    /** The speed key of the daily counter of [entryId]. */
    fun counterKey(entryId: String) = "counter/$entryId"

    /** A speed learned: the text's own, and the new default. */
    data class Learned(val item: Float, val default: Float)

    /**
     * The speeds after [letters] were read in [elapsedMs], from the text's [itemSpeed] and the
     * [defaultSpeed]; null when the repetition cannot be measured.
     */
    fun learn(itemSpeed: Float, defaultSpeed: Float, letters: Int, elapsedMs: Long): Learned? {
        if (elapsedMs < MIN_MEASURABLE_MS || letters <= 0) return null
        val measured = (letters / (elapsedMs / 1000f)).coerceIn(
            ProgramRepository.MIN_LETTERS_PER_SECOND,
            ProgramRepository.MAX_LETTERS_PER_SECOND,
        )
        return Learned(
            item = itemSpeed * (1 - ITEM_SMOOTHING) + measured * ITEM_SMOOTHING,
            default = defaultSpeed * (1 - DEFAULT_SMOOTHING) + measured * DEFAULT_SMOOTHING,
        )
    }

    /** The stored speed of [key] (its own, else the default). Reads storage: off the main thread. */
    fun load(context: Context, key: String): Float {
        val repository = ProgramRepository(context)
        return repository.loadSpeeds()[key] ?: repository.loadDefaultSpeed()
    }

    private val writer = Executors.newSingleThreadExecutor { Thread(it, "reading-speeds") }

    /**
     * Learns from [text] read in [elapsedMs] under [key] and stores it, in the background (the
     * floating counter, which has no view model). Returns at once.
     */
    fun record(context: Context, key: String, text: String, elapsedMs: Long) {
        val app = context.applicationContext
        writer.execute {
            val repository = ProgramRepository(app)
            val speeds = repository.loadSpeeds()
            val defaultSpeed = repository.loadDefaultSpeed()
            val learned = learn(speeds[key] ?: defaultSpeed, defaultSpeed, ArabicText.totalLetters(text), elapsedMs)
                ?: return@execute
            repository.saveSpeeds(speeds + (key to learned.item))
            repository.saveDefaultSpeed(learned.default)
        }
    }
}
