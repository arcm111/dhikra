package io.github.arcm111.dhikra.data

import android.content.Context
import android.os.Handler
import android.os.Looper
import io.github.arcm111.dhikra.widget.Widgets
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import java.util.concurrent.Executors

/**
 * Today's daily-counter counts as stored, shared by the app, the home-screen widget and the
 * floating counter: each writes here and the others hear of it through [changes], so a count made
 * in one place shows in the others.
 */
object CounterStore {

    /** Who wrote: the app's screen, or something outside it (the widget, the floating counter). */
    enum class Source { APP, OUTSIDE }

    private val _changes = MutableSharedFlow<Source>(extraBufferCapacity = 64)

    /** A count was saved; the app reloads on [Source.OUTSIDE], the floating counter on [Source.APP]. */
    val changes: SharedFlow<Source> get() = _changes

    /** One writer, so quick taps are saved one after the other, none lost. */
    private val writer = Executors.newSingleThreadExecutor { Thread(it, "counter-store") }
    private val main by lazy { Handler(Looper.getMainLooper()) }

    /** Today's count of [entryId] (reads storage: off the main thread). */
    fun count(context: Context, entryId: String): Int =
        ProgramRepository(context).loadProgress(ProgramRepository.today())[DailyCounter.key(entryId)] ?: 0

    /**
     * Adds [delta] (1, or −1 to undo) to today's count of [entryId], in the background; [onDone]
     * gets the new count on the main thread.
     */
    fun add(context: Context, entryId: String, delta: Int, onDone: ((Int) -> Unit)? = null) {
        val app = context.applicationContext
        writer.execute {
            val count = addNow(app, entryId, delta)
            Widgets.refresh(app)
            onDone?.let { main.post { it(count) } }
        }
    }

    /** Sets today's count of [entryId] back to nothing, in the background. */
    fun reset(context: Context, entryId: String, onDone: (() -> Unit)? = null) {
        val app = context.applicationContext
        writer.execute {
            val repository = ProgramRepository(app)
            val day = ProgramRepository.today()
            repository.saveProgress(day, repository.loadProgress(day) - DailyCounter.key(entryId))
            _changes.tryEmit(Source.OUTSIDE)
            Widgets.refresh(app)
            onDone?.let { main.post { it() } }
        }
    }

    /** As [add], on the calling thread (a broadcast receiver's). */
    fun addNow(context: Context, entryId: String, delta: Int): Int {
        val repository = ProgramRepository(context)
        val day = ProgramRepository.today()
        val progress = withDelta(repository.loadProgress(day), entryId, delta)
        repository.saveProgress(day, progress)
        _changes.tryEmit(Source.OUTSIDE)
        return progress[DailyCounter.key(entryId)] ?: 0
    }

    /** The app saved today's progress. */
    fun appSaved() {
        _changes.tryEmit(Source.APP)
    }

    /** [progress] with [delta] added to [entryId]'s count; a count of 0 or less is dropped. */
    fun withDelta(progress: Map<String, Int>, entryId: String, delta: Int): Map<String, Int> {
        val key = DailyCounter.key(entryId)
        val count = (progress[key] ?: 0) + delta
        return if (count > 0) progress + (key to count) else progress - key
    }
}
