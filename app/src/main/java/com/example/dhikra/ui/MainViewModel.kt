package com.example.dhikra.ui

import android.app.Application
import android.net.Uri
import androidx.annotation.StringRes
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.dhikra.R
import com.example.dhikra.data.ArabicText
import com.example.dhikra.data.DefaultProgram
import com.example.dhikra.data.Library
import com.example.dhikra.data.LibraryEntry
import com.example.dhikra.data.LibraryGroup
import com.example.dhikra.data.Program
import com.example.dhikra.data.ProgramBundle
import com.example.dhikra.data.ProgramItem
import com.example.dhikra.data.ProgramJson
import com.example.dhikra.data.ProgramRepository
import com.example.dhikra.data.ProgramSection
import com.example.dhikra.reminder.ReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Calendar

data class UiState(
    val loading: Boolean = true,
    /** The program as stored: content steps only reference library entries. */
    val program: Program = Program(name = ""),
    val library: Library = Library(),
    /** The program as displayed, with every step's library text filled in. */
    val schedule: Program = Program(name = ""),
    val day: String = "",
    /** itemId -> repetitions completed today. */
    val progress: Map<String, Int> = emptyMap(),
    /** itemId -> learned reading speed in letters per second. */
    val speeds: Map<String, Float> = emptyMap(),
    val defaultSpeed: Float = ProgramRepository.DEFAULT_LETTERS_PER_SECOND,
    val textScale: Float = 1f,
    val selectedSectionId: String? = null,
    /** The section whose time of day it currently is. */
    val nowSectionId: String? = null,
    /** A program waiting for the user to confirm its import. */
    val pendingImport: ProgramBundle? = null,
) {
    /** Repetitions done today; for a group, how many of its texts are done. */
    fun doneCount(item: ProgramItem): Int =
        if (item.isGroup) item.members.count(::isComplete)
        else (progress[item.id] ?: 0).coerceAtMost(item.target)

    fun isComplete(item: ProgramItem): Boolean = item.target > 0 && doneCount(item) >= item.target
    fun speedFor(item: ProgramItem): Float = speeds[item.id] ?: defaultSpeed
    fun completedIn(section: ProgramSection): Int = section.trackableItems.count(::isComplete)
}

sealed interface UiEvent {
    data class Message(@StringRes val text: Int) : UiEvent
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ProgramRepository(application)
    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var saveJob: Job? = null
    private var dirty = false
    private var pendingSectionSelection: String? = null

    init {
        viewModelScope.launch {
            val today = ProgramRepository.today()
            val loaded = withContext(Dispatchers.IO) {
                val bundle = repository.load()
                UiState(
                    loading = false,
                    program = bundle.program,
                    library = bundle.library,
                    schedule = bundle.program.resolvedWith(bundle.library),
                    day = today,
                    progress = repository.loadProgress(today),
                    speeds = repository.loadSpeeds(),
                    defaultSpeed = repository.loadDefaultSpeed(),
                    textScale = repository.loadTextScale(),
                )
            }
            val now = currentSectionByTime(loaded.program)
            val selected = pendingSectionSelection?.takeIf { loaded.program.section(it) != null } ?: now
            _state.update {
                loaded.copy(selectedSectionId = selected, nowSectionId = now, pendingImport = it.pendingImport)
            }
            withContext(Dispatchers.IO) { ReminderScheduler.scheduleAll(application, loaded.program) }
        }
    }

    // ---------------------------------------------------------------- Navigation

    fun selectSection(sectionId: String) {
        if (_state.value.loading) pendingSectionSelection = sectionId
        else if (_state.value.selectedSectionId != sectionId) {
            _state.update { it.copy(selectedSectionId = sectionId) }
        }
    }

    /**
     * Called whenever the app comes back to the foreground. Resets the counters when a new day has
     * started, and opens the section of the current time of day when that time slot changed since
     * it was last shown, so a manual choice is kept while the slot lasts.
     */
    fun refreshDay() {
        val current = _state.value
        if (current.loading) return
        val today = ProgramRepository.today()
        val now = currentSectionByTime(current.program)
        val newDay = current.day != today
        if (!newDay && now == current.nowSectionId) return
        _state.update {
            it.copy(
                day = today,
                progress = if (newDay) emptyMap() else it.progress,
                nowSectionId = now,
                selectedSectionId = now ?: it.selectedSectionId,
            )
        }
    }

    // ---------------------------------------------------------------- Progress

    fun increment(item: ProgramItem) = updateProgress { progress ->
        progress[item.id] = ((progress[item.id] ?: 0) + 1).coerceAtMost(item.target)
    }

    /** Takes back the last repetition (e.g. after an accidental tap). */
    fun decrement(item: ProgramItem) = updateProgress { progress ->
        val count = (progress[item.id] ?: 0) - 1
        if (count > 0) progress[item.id] = count else progress.remove(item.id)
    }

    /** Marks a step done (or not); for a group, every one of its texts. */
    fun setComplete(item: ProgramItem, complete: Boolean) = updateProgress { progress ->
        item.trackedParts().forEach { part ->
            if (complete) progress[part.id] = part.target else progress.remove(part.id)
        }
    }

    fun resetItem(item: ProgramItem) = updateProgress { progress ->
        item.trackedParts().forEach { progress.remove(it.id) }
    }

    /** The items whose progress is stored: a group's texts, or the step itself. */
    private fun ProgramItem.trackedParts(): List<ProgramItem> = if (isGroup) members else listOf(this)

    fun resetToday() = updateProgress { it.clear() }

    /**
     * Called each time the user taps while the reader effect is running: one repetition has
     * been read in [elapsedMs]. The reading speed is adapted towards the user's real pace and
     * stored, then the counter is incremented.
     */
    fun onRepetitionRead(item: ProgramItem, elapsedMs: Long) {
        val letters = ArabicText.totalLetters(item.text)
        if (elapsedMs >= MIN_MEASURABLE_MS && letters > 0) {
            val measured = (letters / (elapsedMs / 1000f)).coerceIn(
                ProgramRepository.MIN_LETTERS_PER_SECOND,
                ProgramRepository.MAX_LETTERS_PER_SECOND,
            )
            val current = _state.value
            val itemSpeed = current.speedFor(item) * (1 - ITEM_SMOOTHING) + measured * ITEM_SMOOTHING
            val defaultSpeed =
                current.defaultSpeed * (1 - DEFAULT_SMOOTHING) + measured * DEFAULT_SMOOTHING
            val speeds = current.speeds + (item.id to itemSpeed)
            _state.update { it.copy(speeds = speeds, defaultSpeed = defaultSpeed) }
            viewModelScope.launch(Dispatchers.IO) {
                repository.saveSpeeds(speeds)
                repository.saveDefaultSpeed(defaultSpeed)
            }
        }
        increment(item)
    }

    private fun updateProgress(block: (MutableMap<String, Int>) -> Unit) {
        refreshDay()
        val current = _state.value
        val progress = current.progress.toMutableMap().also(block)
        _state.update { it.copy(progress = progress) }
        viewModelScope.launch(Dispatchers.IO) { repository.saveProgress(current.day, progress) }
    }

    // ---------------------------------------------------------------- Preferences

    fun setTextScale(scale: Float) {
        _state.update { it.copy(textScale = scale) }
        viewModelScope.launch(Dispatchers.IO) { repository.saveTextScale(scale) }
    }

    // ---------------------------------------------------------------- Editing

    /** Applies an edit to the program; it is persisted (debounced) and reminders rescheduled. */
    fun editProgram(transform: (Program) -> Program) = edit { it.copy(program = transform(it.program)) }

    /** Adds or updates an entry of the library collections. */
    fun saveEntry(entry: LibraryEntry) = edit { it.copy(library = it.library.upsert(entry)) }

    /** Deletes a library entry, together with the program steps that read it (it also leaves its groups). */
    fun deleteEntry(entryId: String) = edit {
        ProgramBundle(program = it.program.withoutEntry(entryId), library = it.library.remove(entryId))
    }

    /** Adds or updates a group of library texts. */
    fun saveGroup(group: LibraryGroup) = edit { it.copy(library = it.library.upsertGroup(group)) }

    /** Deletes a group, together with the program steps that read it; its texts stay in the library. */
    fun deleteGroup(groupId: String) = edit {
        ProgramBundle(program = it.program.withoutGroup(groupId), library = it.library.removeGroup(groupId))
    }

    private fun edit(transform: (ProgramBundle) -> ProgramBundle) {
        val current = _state.value
        val bundle = transform(ProgramBundle(current.program, current.library))
        apply(bundle)
        dirty = true
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(SAVE_DEBOUNCE_MS)
            persist(bundle)
        }
    }

    private fun apply(bundle: ProgramBundle, reselect: Boolean = false) {
        val program = bundle.program
        _state.update { state ->
            state.copy(
                program = program,
                library = bundle.library,
                schedule = program.resolvedWith(bundle.library),
                pendingImport = if (reselect) null else state.pendingImport,
                selectedSectionId = if (reselect) currentSectionByTime(program)
                else state.selectedSectionId?.takeIf { program.section(it) != null }
                    ?: program.sections.firstOrNull()?.id,
                nowSectionId = currentSectionByTime(program),
            )
        }
    }

    private suspend fun persist(bundle: ProgramBundle) = withContext(Dispatchers.IO) {
        repository.save(bundle)
        dirty = false
        ReminderScheduler.scheduleAll(getApplication(), bundle.program)
    }

    /** Restores the default program; its texts are added back to the library collections. */
    fun restoreDefault() = replaceWith(DefaultProgram.bundle())

    /** Replaces the program, merging [bundle]'s texts into the existing library. */
    private fun replaceWith(bundle: ProgramBundle) {
        saveJob?.cancel()
        val merged = ProgramBundle(bundle.program, _state.value.library.merge(bundle.library))
        apply(merged, reselect = true)
        viewModelScope.launch { persist(merged) }
    }

    // ---------------------------------------------------------------- Import / export

    private fun currentBundle() = _state.value.let { ProgramBundle(it.program, it.library) }

    fun exportTo(uri: Uri) {
        val bundle = currentBundle()
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    getApplication<Application>().contentResolver.openOutputStream(uri, "wt")
                        ?.use { it.write(ProgramJson.encode(bundle).encodeToByteArray()) }
                        ?: error("No output stream")
                }.isSuccess
            }
            _events.send(UiEvent.Message(if (ok) R.string.export_done else R.string.export_failed))
        }
    }

    /** Writes the program to a cache file and returns a shareable content Uri, or null. */
    suspend fun prepareShareFile(): Uri? = withContext(Dispatchers.IO) {
        val app = getApplication<Application>()
        runCatching {
            val bundle = currentBundle()
            val dir = File(app.cacheDir, "shared").apply { mkdirs() }
            dir.listFiles()?.forEach { it.delete() }
            val file = File(dir, exportFileName(bundle.program))
            file.writeText(ProgramJson.encode(bundle))
            FileProvider.getUriForFile(app, "${app.packageName}.fileprovider", file)
        }.getOrNull()
    }.also { if (it == null) _events.send(UiEvent.Message(R.string.share_failed)) }

    fun exportFileName(program: Program = _state.value.program): String {
        val base = program.name.ifBlank { "dhikra" }
            .replace(Regex("""[\\/:*?"<>|\s]+"""), "_").take(40)
        return base + ProgramJson.FILE_EXTENSION
    }

    /** Reads a program file and asks the user to confirm replacing the current program. */
    fun loadImport(uri: Uri) {
        viewModelScope.launch {
            val bundle = withContext(Dispatchers.IO) {
                runCatching {
                    getApplication<Application>().contentResolver.openInputStream(uri)?.use { input ->
                        val bytes = input.readBytes()
                        require(bytes.size <= MAX_IMPORT_BYTES)
                        ProgramJson.decode(bytes.decodeToString())
                    }
                }.getOrNull()
            }
            if (bundle == null || bundle.program.sections.isEmpty()) {
                _events.send(UiEvent.Message(R.string.import_failed))
            } else {
                _state.update { it.copy(pendingImport = bundle) }
            }
        }
    }

    fun confirmImport() {
        val bundle = _state.value.pendingImport ?: return
        replaceWith(bundle)
        viewModelScope.launch { _events.send(UiEvent.Message(R.string.import_done)) }
    }

    fun dismissImport() = _state.update { it.copy(pendingImport = null) }

    override fun onCleared() {
        // Flush a debounced edit that has not been written yet.
        if (dirty) runCatching { repository.save(currentBundle()) }
    }

    private companion object {
        const val SAVE_DEBOUNCE_MS = 300L
        const val MIN_MEASURABLE_MS = 400L
        const val ITEM_SMOOTHING = 0.5f
        const val DEFAULT_SMOOTHING = 0.25f
        const val MAX_IMPORT_BYTES = 4 * 1024 * 1024

        /** The section whose reminder time most recently passed, or the first section. */
        fun currentSectionByTime(program: Program): String? {
            val now = Calendar.getInstance().let { it.get(Calendar.HOUR_OF_DAY) * 60 + it.get(Calendar.MINUTE) }
            val timed = program.sections.filter { it.reminderMinutes != null }
            return timed.filter { it.reminderMinutes!! <= now }.maxByOrNull { it.reminderMinutes!! }?.id
                // Before the first section of the day: still the last one of the night.
                ?: timed.maxByOrNull { it.reminderMinutes!! }?.id
                ?: program.sections.firstOrNull()?.id
        }
    }
}
