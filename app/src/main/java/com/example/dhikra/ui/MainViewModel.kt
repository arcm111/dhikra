package com.example.dhikra.ui

import android.app.Application
import android.net.Uri
import androidx.annotation.StringRes
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.dhikra.R
import com.example.dhikra.data.ArabicText
import com.example.dhikra.data.SectionEdit
import com.example.dhikra.data.Favourites
import com.example.dhikra.data.DailyCounter
import com.example.dhikra.data.DefaultProgram
import com.example.dhikra.data.Library
import com.example.dhikra.data.LibraryEntry
import com.example.dhikra.data.LibraryGroup
import com.example.dhikra.data.PrayerSettings
import com.example.dhikra.data.appPrayerTimes
import com.example.dhikra.data.Prayer
import com.example.dhikra.data.PrayerAlert
import com.example.dhikra.data.PrayerTimesSource
import com.example.dhikra.data.PrayerTimetable
import com.example.dhikra.data.Program
import com.example.dhikra.data.ProgramBundle
import com.example.dhikra.data.ProgramItem
import com.example.dhikra.data.ProgramJson
import com.example.dhikra.data.ProgramRepository
import com.example.dhikra.data.ProgramSection
import com.example.dhikra.data.ProgramStore
import com.example.dhikra.data.QuranDatabase
import com.example.dhikra.data.QuranLookup
import com.example.dhikra.data.ReminderSound
import com.example.dhikra.data.newId
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
import com.example.dhikra.widget.Widgets
import com.example.dhikra.data.CounterStore
import com.example.dhikra.overlay.CounterOverlayService
import com.example.dhikra.data.ReadingSpeeds
import com.example.dhikra.data.OverlayFade

data class UiState(
    val loading: Boolean = true,
    /** The program being followed, as stored: content steps only reference library entries. */
    val program: Program = Program(name = ""),
    /** All the user's programs, [program] included; they share the [library]. */
    val programs: List<Program> = emptyList(),
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
    /** Today's prayer times in the city the user picked; null until a city is picked. */
    val prayerTimes: PrayerTimetable? = null,
    /** How the prayer times are calculated (method, Asr school, minutes added). */
    val prayerSettings: PrayerSettings = PrayerSettings(),
    /** The sound of the sections' reminders. */
    val sectionSound: ReminderSound = ReminderSound.Default,
    /** The alert of each of the five prayers. */
    val prayerAlerts: Map<Prayer, PrayerAlert> = emptyMap(),
    /** A section to show in the section view at once (a reminder was tapped); cleared once shown. */
    val openSectionRequest: String? = null,
    /** The floating counter's size, a share of its default. */
    val overlayScale: Float = 1f,
    /** How the floating counter fades while left alone. */
    val overlayFade: OverlayFade = OverlayFade(),
    /** The first launch: no city yet, and the user was not asked for one. */
    val askCity: Boolean = false,
    /** A daily counter to show at once (a widget's counter was tapped); cleared once shown. */
    val openCounterRequest: String? = null,
    /** The adhkar of the daily counter (library entry ids), in the order they were added. */
    val counters: List<String> = emptyList(),
    /** The library texts and groups marked as favourites, see [Favourites]. */
    val favourites: List<String> = emptyList(),
) {
    /** Today's count of [entryId] on the daily counter. */
    fun counterCount(entryId: String): Int = progress[DailyCounter.key(entryId)] ?: 0

    /** What is stored: every program, the one being followed and the library. */
    val store: ProgramStore get() = ProgramStore(programs.ifEmpty { listOf(program) }, program.id, library)

    /** Repetitions done today; for a group, how many of its texts are done. */
    fun doneCount(item: ProgramItem): Int =
        if (item.isGroup) item.members.count(::isComplete).coerceAtMost(item.target)
        else (progress[item.id] ?: 0).coerceAtMost(item.target)

    fun isComplete(item: ProgramItem): Boolean = item.target > 0 && doneCount(item) >= item.target
    fun speedFor(item: ProgramItem): Float = speeds[item.id] ?: defaultSpeed
    /** The reading speed of the daily counter of [entryId]. */
    fun counterSpeed(entryId: String): Float = speeds[ReadingSpeeds.counterKey(entryId)] ?: defaultSpeed
    fun completedIn(section: ProgramSection): Int = section.trackableItems.count(::isComplete)
}

sealed interface UiEvent {
    data class Message(@StringRes val text: Int) : UiEvent
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ProgramRepository(application)

    /** The bundled Quran text, for the editors that pick Quran passages. */
    val quran: QuranLookup = QuranDatabase.get(application).lookup

    /** Where prayer times come from: a mock timetable until the real source is chosen. */
    val prayerSource: PrayerTimesSource = appPrayerTimes(application)
    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var saveJob: Job? = null
    private var dirty = false
    private var pendingSectionSelection: String? = null

    init {
        // A count made outside the app's screen (the widget, the floating counter): show it.
        viewModelScope.launch {
            CounterStore.changes.collect { if (it == CounterStore.Source.OUTSIDE) reloadFromStorage() }
        }
        viewModelScope.launch {
            val today = ProgramRepository.today()
            val loaded = withContext(Dispatchers.IO) {
                val store = repository.load()
                val timetable = repository.loadPrayerCity()?.let { prayerSource.timetable(it, today) }
                UiState(
                    loading = false,
                    program = store.active,
                    programs = store.programs,
                    library = store.library,
                    schedule = store.active.resolvedWith(store.library).withPrayerTimes(timetable),
                    day = today,
                    progress = repository.loadProgress(today),
                    speeds = repository.loadSpeeds(),
                    defaultSpeed = repository.loadDefaultSpeed(),
                    textScale = repository.loadTextScale(),
                    overlayScale = repository.loadOverlayScale(),
                    overlayFade = repository.loadOverlayFade(),
                    prayerTimes = timetable,
                    sectionSound = repository.loadSectionSound(),
                    prayerAlerts = repository.loadPrayerAlerts(),
                    prayerSettings = repository.loadPrayerSettings(),
                    counters = repository.loadCounters(),
                    favourites = repository.loadFavourites(),
                    askCity = timetable == null && repository.loadPrayerCity() == null && !repository.loadCityAsked(),
                )
            }
            val now = currentSectionByTime(loaded.schedule)
            val selected = pendingSectionSelection?.takeIf { loaded.program.section(it) != null } ?: now
            _state.update {
                loaded.copy(
                    selectedSectionId = selected,
                    nowSectionId = now,
                    pendingImport = it.pendingImport,
                    openSectionRequest = it.openSectionRequest,
                    openCounterRequest = it.openCounterRequest,
                )
            }
            withContext(Dispatchers.IO) {
                ReminderScheduler.scheduleAll(application, loaded.program)
                ReminderScheduler.schedulePrayers(application)
            }
        }
    }

    // ---------------------------------------------------------------- Navigation

    fun selectSection(sectionId: String) {
        if (_state.value.loading) pendingSectionSelection = sectionId
        else if (_state.value.selectedSectionId != sectionId) {
            _state.update { it.copy(selectedSectionId = sectionId) }
        }
    }

    /** Selects [sectionId] and asks for the section view to show it (a reminder was tapped). */
    fun openSection(sectionId: String) {
        selectSection(sectionId)
        _state.update { it.copy(openSectionRequest = sectionId) }
    }

    /** Asks for the daily counter of [entryId] to be shown (a widget's counter was tapped). */
    fun openCounter(entryId: String) {
        _state.update { it.copy(openCounterRequest = entryId) }
    }

    /** The counter was opened for [UiState.openCounterRequest]. */
    fun consumeOpenCounter() {
        _state.update { it.copy(openCounterRequest = null) }
    }

    /** The section view was opened for [UiState.openSectionRequest]. */
    fun consumeOpenSection() {
        _state.update { it.copy(openSectionRequest = null) }
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
        val newDay = current.day != today
        // A new day has its own prayer times, and the sections tied to them move with them.
        val prayerTimes = if (newDay) current.prayerTimes?.let { t -> prayerSource.timetable(t.city.id, today) } else current.prayerTimes
        val schedule = if (newDay) current.program.resolvedWith(current.library).withPrayerTimes(prayerTimes) else current.schedule
        val now = currentSectionByTime(schedule)
        if (!newDay && now == current.nowSectionId) return
        _state.update {
            it.copy(
                day = today,
                progress = if (newDay) emptyMap() else it.progress,
                prayerTimes = prayerTimes,
                schedule = schedule,
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

    /** Starts today over for the program being followed; the other programs keep their progress. */
    fun resetToday() {
        val stepIds = _state.value.program.sections.flatMapTo(mutableSetOf()) { s -> s.items.map { it.id } }
        // A group's texts are tracked as "<step id>/<entry id>".
        updateProgress { progress -> progress.keys.removeAll { it.substringBefore('/') in stepIds } }
    }

    /**
     * Called each time the user taps while the reader effect is running: one repetition has
     * been read in [elapsedMs]. The reading speed is adapted towards the user's real pace and
     * stored, then the counter is incremented.
     */
    fun onRepetitionRead(item: ProgramItem, elapsedMs: Long) {
        learnSpeed(item.id, item.text, elapsedMs)
        increment(item)
    }

    /** As [onRepetitionRead], for the daily counter of [entryId] (its own speed, then +1). */
    fun onCounterRepetitionRead(entryId: String, text: String, elapsedMs: Long) {
        learnSpeed(ReadingSpeeds.counterKey(entryId), text, elapsedMs)
        countDhikr(entryId)
    }

    private fun learnSpeed(key: String, text: String, elapsedMs: Long) {
        val current = _state.value
        val learned = ReadingSpeeds.learn(
            itemSpeed = current.speeds[key] ?: current.defaultSpeed,
            defaultSpeed = current.defaultSpeed,
            letters = ArabicText.totalLetters(text),
            elapsedMs = elapsedMs,
        ) ?: return
        val speeds = current.speeds + (key to learned.item)
        _state.update { it.copy(speeds = speeds, defaultSpeed = learned.default) }
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveSpeeds(speeds)
            repository.saveDefaultSpeed(learned.default)
        }
    }

    // ---------------------------------------------------------------- Daily counter

    /** Adds the dhikr [entryId] to the daily counter (at the end; once). */
    fun addCounter(entryId: String) {
        val counters = _state.value.counters
        if (entryId in counters) return
        saveCounters(counters + entryId)
    }

    /** Takes [entryId] off the daily counter, with today's count. */
    fun removeCounter(entryId: String) {
        if (CounterOverlayService.shownEntryId == entryId) CounterOverlayService.stop(getApplication())
        saveCounters(_state.value.counters - entryId)
        updateProgress { it.remove(DailyCounter.key(entryId)) }
    }

    private fun saveCounters(counters: List<String>) {
        _state.update { it.copy(counters = counters) }
        viewModelScope.launch(Dispatchers.IO) { repository.saveCounters(counters) }
        Widgets.refresh(getApplication())
    }

    /** One more on today's count of [entryId]: it has no end, only rising targets. */
    fun countDhikr(entryId: String) = updateProgress { progress ->
        val key = DailyCounter.key(entryId)
        progress[key] = (progress[key] ?: 0) + 1
    }

    fun undoDhikr(entryId: String) = updateProgress { progress ->
        val key = DailyCounter.key(entryId)
        val count = (progress[key] ?: 0) - 1
        if (count > 0) progress[key] = count else progress.remove(key)
    }

    fun resetDhikr(entryId: String) = updateProgress { it.remove(DailyCounter.key(entryId)) }

    // ---------------------------------------------------------------- Favourites

    /** Marks a library text or group ([Favourites.entry] / [Favourites.group]) as a favourite, or not. */
    fun toggleFavourite(ref: String) {
        val favourites = _state.value.favourites.let { if (ref in it) it - ref else it + ref }
        _state.update { it.copy(favourites = favourites) }
        viewModelScope.launch(Dispatchers.IO) { repository.saveFavourites(favourites) }
    }

    private fun updateProgress(block: (MutableMap<String, Int>) -> Unit) {
        refreshDay()
        val current = _state.value
        val progress = current.progress.toMutableMap().also(block)
        _state.update { it.copy(progress = progress) }
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveProgress(current.day, progress)
            CounterStore.appSaved()
            Widgets.refresh(getApplication())
        }
    }

    /**
     * Reads today's progress and counters again from storage: a home-screen widget may have
     * counted while the app was away. Called when the app comes back to the foreground.
     */
    fun reloadFromStorage() {
        if (_state.value.loading) return
        refreshDay()
        val day = _state.value.day
        viewModelScope.launch {
            val (progress, counters) = withContext(Dispatchers.IO) { repository.loadProgress(day) to repository.loadCounters() }
            // The floating counter learns reading speeds too.
            val (speeds, defaultSpeed) = withContext(Dispatchers.IO) { repository.loadSpeeds() to repository.loadDefaultSpeed() }
            _state.update {
                if (it.day == day) it.copy(progress = progress, counters = counters, speeds = speeds, defaultSpeed = defaultSpeed) else it
            }
        }
    }

    // ---------------------------------------------------------------- Preferences

    fun setOverlayFade(fade: OverlayFade) {
        _state.update { it.copy(overlayFade = fade) }
        viewModelScope.launch(Dispatchers.IO) { repository.saveOverlayFade(fade) }
    }

    fun setOverlayScale(scale: Float) {
        _state.update { it.copy(overlayScale = scale) }
        viewModelScope.launch(Dispatchers.IO) { repository.saveOverlayScale(scale) }
    }

    fun setTextScale(scale: Float) {
        _state.update { it.copy(textScale = scale) }
        viewModelScope.launch(Dispatchers.IO) { repository.saveTextScale(scale) }
    }

    /** The first launch's question is answered with [cityId]: it becomes the user's city. */
    fun chooseFirstCity(cityId: String) {
        setPrayerCity(cityId)
        dismissCityQuestion()
    }

    /** The first launch's question is put off ("later"): it is not asked again. */
    fun dismissCityQuestion() {
        _state.update { it.copy(askCity = false) }
        viewModelScope.launch(Dispatchers.IO) { repository.saveCityAsked() }
    }

    /** Shows the prayer times of [cityId] and remembers it as the user's city. */
    fun setPrayerCity(cityId: String) {
        val day = _state.value.day.ifEmpty { ProgramRepository.today() }
        viewModelScope.launch {
            // The city is read from the bundled database: off the main thread.
            val timetable = withContext(Dispatchers.IO) { prayerSource.timetable(cityId, day) } ?: return@launch
            withPrayerTimes(timetable)
            withContext(Dispatchers.IO) {
                repository.savePrayerCity(cityId)
                ReminderScheduler.schedulePrayers(getApplication())
                // Sections tied to a prayer follow the new city's times.
                ReminderScheduler.scheduleAll(getApplication(), _state.value.program)
            }
        }
    }

    /** Shows [timetable]'s prayer times, with the sections tied to a prayer at their times. */
    private fun withPrayerTimes(timetable: PrayerTimetable) {
        _state.update {
            val schedule = it.program.resolvedWith(it.library).withPrayerTimes(timetable)
            it.copy(prayerTimes = timetable, schedule = schedule, nowSectionId = currentSectionByTime(schedule))
        }
    }

    /** Changes how the prayer times are calculated; the day's times and the alerts follow. */
    fun setPrayerSettings(settings: PrayerSettings) {
        _state.update { it.copy(prayerSettings = settings) }
        val cityId = _state.value.prayerTimes?.city?.id
        val day = _state.value.day.ifEmpty { ProgramRepository.today() }
        viewModelScope.launch {
            val timetable = withContext(Dispatchers.IO) {
                repository.savePrayerSettings(settings)
                ReminderScheduler.schedulePrayers(getApplication())
                ReminderScheduler.scheduleAll(getApplication(), _state.value.program)
                cityId?.let { prayerSource.timetable(it, day) }
            }
            if (timetable != null) withPrayerTimes(timetable)
        }
    }

    // ---------------------------------------------------------------- Reminder sounds

    fun setSectionSound(sound: ReminderSound) {
        _state.update { it.copy(sectionSound = sound) }
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveSectionSound(sound)
            ReminderScheduler.removeUnusedChannels(getApplication())
        }
    }

    /** Turns a prayer's alert on or off or changes its sound, and reschedules the alerts. */
    fun setPrayerAlert(prayer: Prayer, alert: PrayerAlert) {
        _state.update { it.copy(prayerAlerts = it.prayerAlerts + (prayer to alert)) }
        viewModelScope.launch(Dispatchers.IO) {
            repository.savePrayerAlert(prayer, alert)
            ReminderScheduler.schedulePrayers(getApplication())
            ReminderScheduler.removeUnusedChannels(getApplication())
        }
    }

    // ---------------------------------------------------------------- Editing

    /** Applies an edit to the program; it is persisted (debounced) and reminders rescheduled. */
    fun editProgram(transform: (Program) -> Program) = edit { it.copy(program = transform(it.program)) }

    /**
     * Saves a section edited in the program tab's popup — the section and the library texts and
     * groups changed with it — all at once.
     */
    fun saveSectionEdit(change: SectionEdit) = edit { change.applyTo(it) }

    /** Adds or updates an entry of the library collections. */
    fun saveEntry(entry: LibraryEntry) = edit { it.copy(library = it.library.upsert(entry)) }

    /** Deletes a library entry, together with the steps of every program that read it (it also leaves its groups). */
    fun deleteEntry(entryId: String) = editStore { it.withoutEntry(entryId) }

    /** Adds or updates a group of library texts. */
    fun saveGroup(group: LibraryGroup) = edit { it.copy(library = it.library.upsertGroup(group)) }

    /** Saves [copy], a copy of the group [originalId], right after it in the library. */
    fun saveGroupCopy(copy: LibraryGroup, originalId: String) =
        edit { it.copy(library = it.library.upsertGroup(copy, after = originalId)) }

    /** Deletes a group, together with the steps of every program that read it; its texts stay in the library. */
    fun deleteGroup(groupId: String) = editStore { it.withoutGroup(groupId) }

    /** Edits the program being followed and the library. */
    private fun edit(transform: (ProgramBundle) -> ProgramBundle) =
        editStore { it.withBundle(transform(it.bundle)) }

    /** Applies an edit; it is persisted (debounced) and the reminders rescheduled. */
    private fun editStore(transform: (ProgramStore) -> ProgramStore) {
        val current = _state.value.store
        commit(transform(current), reselect = false)
    }

    /**
     * Shows [store] and saves it after a short delay. [reselect] opens the section of the current
     * time, for a program that has just been switched to.
     */
    private fun commit(store: ProgramStore, reselect: Boolean) {
        apply(store, reselect)
        dirty = true
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(SAVE_DEBOUNCE_MS)
            persist(store)
        }
    }

    private fun apply(store: ProgramStore, reselect: Boolean = false) {
        val program = store.active
        _state.update { state ->
            val schedule = program.resolvedWith(store.library).withPrayerTimes(state.prayerTimes)
            state.copy(
                program = program,
                programs = store.programs,
                library = store.library,
                schedule = schedule,
                selectedSectionId = if (reselect) currentSectionByTime(schedule)
                else state.selectedSectionId?.takeIf { program.section(it) != null }
                    ?: program.sections.firstOrNull()?.id,
                nowSectionId = currentSectionByTime(schedule),
            )
        }
    }

    private suspend fun persist(store: ProgramStore) = withContext(Dispatchers.IO) {
        repository.save(store)
        Widgets.refresh(getApplication())
        dirty = false
        // Only the program being followed has reminders.
        ReminderScheduler.scheduleAll(getApplication(), store.active)
    }

    // ---------------------------------------------------------------- Programs

    /** Switches to the program [programId]: its sections, progress and reminders. */
    fun selectProgram(programId: String) {
        val current = _state.value.store
        if (current.active.id == programId) return
        commit(current.select(programId), reselect = true)
    }

    /** Adds an empty program named [name] and switches to it. */
    fun createProgram(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        commit(_state.value.store.add(Program(name = trimmed)), reselect = true)
    }

    /** Adds a copy of the program followed, named [name] — its sections and steps with fresh ids — and switches to it. */
    fun duplicateProgram(name: String) {
        val program = _state.value.store.active
        val copy = Program(
            name = name,
            sections = program.sections.map { section ->
                section.copy(id = newId(), items = section.items.map { it.copy(id = newId()) })
            },
        )
        commit(_state.value.store.add(copy), reselect = true)
    }

    /** Deletes the program [programId], unless it is the last one. */
    fun deleteProgram(programId: String) {
        val current = _state.value.store
        commit(current.remove(programId), reselect = current.active.id == programId)
    }

    /**
     * Replaces the program being followed by the default one; its texts are added back to the
     * library collections.
     */
    fun restoreDefault() {
        val defaults = DefaultProgram.bundle()
        addPrograms { store ->
            val others = store.programs.filterNot { it.id == store.active.id }
            val program = defaults.program
                .withIdsOutside(others.flatMapTo(mutableSetOf()) { it.ids })
                .copy(id = store.active.id)
            store.copy(library = store.library.merge(defaults.library)).add(program)
        }
    }

    /**
     * Adds [bundle]'s program as a new program and switches to it, merging its texts into the
     * library. Its sections and steps get new ids where they clash with the other programs'.
     */
    private fun importProgram(bundle: ProgramBundle) = addPrograms { store ->
        val program = bundle.program.copy(id = newId()).withIdsOutside(store.ids)
        store.copy(library = store.library.merge(bundle.library)).add(program)
    }

    /**
     * Applies [transform], which brings in programs and texts from outside, once their Quran
     * passages (only references) have their text read from the bundled Quran text.
     */
    private fun addPrograms(transform: (ProgramStore) -> ProgramStore) {
        saveJob?.cancel()
        val changed = transform(_state.value.store)
        viewModelScope.launch {
            val withText = withContext(Dispatchers.IO) { repository.withQuranText(changed) }
            _state.update { it.copy(pendingImport = null) }
            commit(withText, reselect = true)
        }
    }

    // ---------------------------------------------------------------- Import / export

    private fun currentBundle() = _state.value.store.bundle

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

    /** Reads a program file and asks the user to confirm adding it as a new program. */
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
        // Dismissed right away: the replacement lands once the Quran texts are read.
        _state.update { it.copy(pendingImport = null) }
        importProgram(bundle)
        viewModelScope.launch { _events.send(UiEvent.Message(R.string.import_done)) }
    }

    fun dismissImport() = _state.update { it.copy(pendingImport = null) }

    override fun onCleared() {
        // Flush a debounced edit that has not been written yet.
        if (dirty) runCatching { repository.save(_state.value.store) }
    }

    private companion object {
        const val SAVE_DEBOUNCE_MS = 300L
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
