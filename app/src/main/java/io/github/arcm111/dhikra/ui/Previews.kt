package io.github.arcm111.dhikra.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.arcm111.dhikra.data.DefaultProgram
import io.github.arcm111.dhikra.data.ItemType
import io.github.arcm111.dhikra.data.LibraryEntry
import io.github.arcm111.dhikra.data.MockPrayerTimes
import io.github.arcm111.dhikra.data.ProgramBundle
import io.github.arcm111.dhikra.data.ProgramItem
import io.github.arcm111.dhikra.data.ProgramSection
import io.github.arcm111.dhikra.data.Quran
import io.github.arcm111.dhikra.data.QuranRef
import io.github.arcm111.dhikra.data.QuranSource
import io.github.arcm111.dhikra.data.Surah
import io.github.arcm111.dhikra.data.withQuranText
import io.github.arcm111.dhikra.ui.home.HomeActions
import io.github.arcm111.dhikra.ui.home.HomeScreen
import io.github.arcm111.dhikra.ui.home.ProgramItemView
import io.github.arcm111.dhikra.ui.home.StepReader
import io.github.arcm111.dhikra.ui.library.CollectionScreen
import io.github.arcm111.dhikra.ui.library.GROUPS_TAB
import io.github.arcm111.dhikra.ui.library.LibraryScreen
import io.github.arcm111.dhikra.ui.library.QuranPassagePicker
import io.github.arcm111.dhikra.ui.library.SettingsActions
import io.github.arcm111.dhikra.ui.library.SettingsScreen
import io.github.arcm111.dhikra.ui.theme.DhikraTheme
import io.github.arcm111.dhikra.ui.today.TodayScreen

/**
 * The ayat the previews show, verbatim from the bundled Quran text (previews can't open its
 * database). As in the database, the first ayah of a surah starts with the basmala.
 */
private val previewAyat = mapOf(
    18 to listOf(
        "بِسْمِ ٱللَّهِ ٱلرَّحْمَـٰنِ ٱلرَّحِيمِ ٱلْحَمْدُ لِلَّهِ ٱلَّذِىٓ أَنزَلَ عَلَىٰ عَبْدِهِ ٱلْكِتَـٰبَ وَلَمْ يَجْعَل لَّهُۥ عِوَجَا ۜ",
        "قَيِّمًا لِّيُنذِرَ بَأْسًا شَدِيدًا مِّن لَّدُنْهُ وَيُبَشِّرَ ٱلْمُؤْمِنِينَ ٱلَّذِينَ يَعْمَلُونَ ٱلصَّـٰلِحَـٰتِ أَنَّ لَهُمْ أَجْرًا حَسَنًا",
        "مَّـٰكِثِينَ فِيهِ أَبَدًا",
        "وَيُنذِرَ ٱلَّذِينَ قَالُوا۟ ٱتَّخَذَ ٱللَّهُ وَلَدًا",
        "مَّا لَهُم بِهِۦ مِنْ عِلْمٍ وَلَا لِـَٔابَآئِهِمْ ۚ كَبُرَتْ كَلِمَةً تَخْرُجُ مِنْ أَفْوَٰهِهِمْ ۚ إِن يَقُولُونَ إِلَّا كَذِبًا",
        "فَلَعَلَّكَ بَـٰخِعٌ نَّفْسَكَ عَلَىٰٓ ءَاثَـٰرِهِمْ إِن لَّمْ يُؤْمِنُوا۟ بِهَـٰذَا ٱلْحَدِيثِ أَسَفًا",
        "إِنَّا جَعَلْنَا مَا عَلَى ٱلْأَرْضِ زِينَةً لَّهَا لِنَبْلُوَهُمْ أَيُّهُمْ أَحْسَنُ عَمَلًا",
        "وَإِنَّا لَجَـٰعِلُونَ مَا عَلَيْهَا صَعِيدًا جُرُزًا",
        "أَمْ حَسِبْتَ أَنَّ أَصْحَـٰبَ ٱلْكَهْفِ وَٱلرَّقِيمِ كَانُوا۟ مِنْ ءَايَـٰتِنَا عَجَبًا",
        "إِذْ أَوَى ٱلْفِتْيَةُ إِلَى ٱلْكَهْفِ فَقَالُوا۟ رَبَّنَآ ءَاتِنَا مِن لَّدُنكَ رَحْمَةً وَهَيِّئْ لَنَا مِنْ أَمْرِنَا رَشَدًا",
    ),
    112 to listOf(
        "بِسْمِ ٱللَّهِ ٱلرَّحْمَـٰنِ ٱلرَّحِيمِ قُلْ هُوَ ٱللَّهُ أَحَدٌ",
        "ٱللَّهُ ٱلصَّمَدُ",
        "لَمْ يَلِدْ وَلَمْ يُولَدْ",
        "وَلَمْ يَكُن لَّهُۥ كُفُوًا أَحَدٌۢ",
    ),
    113 to listOf(
        "بِسْمِ ٱللَّهِ ٱلرَّحْمَـٰنِ ٱلرَّحِيمِ قُلْ أَعُوذُ بِرَبِّ ٱلْفَلَقِ",
        "مِن شَرِّ مَا خَلَقَ",
        "وَمِن شَرِّ غَاسِقٍ إِذَا وَقَبَ",
        "وَمِن شَرِّ ٱلنَّفَّـٰثَـٰتِ فِى ٱلْعُقَدِ",
        "وَمِن شَرِّ حَاسِدٍ إِذَا حَسَدَ",
    ),
    114 to listOf(
        "بِسْمِ ٱللَّهِ ٱلرَّحْمَـٰنِ ٱلرَّحِيمِ قُلْ أَعُوذُ بِرَبِّ ٱلنَّاسِ",
        "مَلِكِ ٱلنَّاسِ",
        "إِلَـٰهِ ٱلنَّاسِ",
        "مِن شَرِّ ٱلْوَسْوَاسِ ٱلْخَنَّاسِ",
        "ٱلَّذِى يُوَسْوِسُ فِى صُدُورِ ٱلنَّاسِ",
        "مِنَ ٱلْجِنَّةِ وَٱلنَّاسِ",
    ),
)

private val previewQuran = QuranSource { ref ->
    previewAyat[ref.sura]?.takeIf { ref.to <= it.size }?.let { Quran.passage(ref, it.subList(ref.from - 1, ref.to)) }
}

/**
 * The built-in program, with a preview-only section for the kinds of step it doesn't have: a task,
 * and a Quran passage whose text the previews carry (the first ten ayat of al-Kahf).
 */
private val sample = DefaultProgram.bundle().let { bundle ->
    val kahf = LibraryEntry(id = "kahf_1_10", type = ItemType.QURAN, title = "سورة الكهف", text = "", quran = QuranRef(18, 1, 10))
    val extra = ProgramSection(
        id = "preview_extra",
        title = "صلاة الضحى",
        icon = "☀️",
        items = listOf(
            ProgramItem(id = "preview_kahf", type = ItemType.QURAN, entryId = kahf.id, note = "ورد تحصين: يقرأ يومياً"),
            ProgramItem(id = "preview_duha", type = ItemType.TASK, title = "صلاة الضحى", text = "صلِّ ركعتين أو أكثر"),
        ),
    )
    ProgramBundle(
        program = bundle.program.copy(sections = bundle.program.sections + extra),
        library = bundle.library.upsert(kahf).withQuranText(previewQuran),
    )
}

private val previewState = UiState(
    loading = false,
    program = sample.program,
    library = sample.library,
    schedule = sample.program.resolvedWith(sample.library),
    day = "2026-11-04",
    progress = mapOf(
        "st_sabah_1" to 1,
        "st_sabah_2" to 1,
        "st_sabah_4/al_ikhlas" to 3,
        "st_sabah_4/al_falaq" to 1,
        "st_sabah_7" to 1,
        "st_sabah_17" to 3,
        "st_nawm_tasbih/takbir" to 34,
    ),
    selectedSectionId = "st_sabah",
    nowSectionId = "st_sabah",
)

private val previewItems = previewState.schedule.sections.flatMap { it.items }

private val noHomeActions = HomeActions(
    onSelectSection = {},
    onSelectProgram = {},
    onCreateProgram = {},
    onEdit = {},
    onOpenCollection = {},
    onOpenSettings = {},
    onTextSize = {},
    onResetToday = {},
    onIncrement = {},
    onDecrement = {},
    onRepetitionRead = { _, _ -> },
    onSetComplete = { _, _ -> },
    onResetItem = {},
)

@Preview(name = "Home", showSystemUi = true, locale = "ar")
@Preview(
    name = "Home (dark)",
    showSystemUi = true,
    locale = "ar",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun HomePreview() {
    DhikraTheme {
        HomeScreen(
            state = previewState,
            snackbarHostState = SnackbarHostState(),
            actions = noHomeActions,
        )
    }
}

@Preview(name = "Today", showSystemUi = true, locale = "ar")
@Preview(
    name = "Today (dark)",
    showSystemUi = true,
    locale = "ar",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun TodayPreview() {
    DhikraTheme {
        TodayScreen(
            state = previewState.copy(prayerTimes = MockPrayerTimes.timetable("riyadh", previewState.day)),
            actions = noHomeActions,
            onOpenSection = {},
            nowMinutes = 7 * 60 + 40,
        )
    }
}

@Preview(name = "Today, no city", showSystemUi = true, locale = "ar")
@Composable
private fun TodayNoCityPreview() {
    DhikraTheme {
        TodayScreen(state = previewState, actions = noHomeActions, onOpenSection = {}, nowMinutes = 7 * 60 + 40)
    }
}

@Preview(name = "Cards", heightDp = 1800, locale = "ar")
@Preview(
    name = "Cards (dark)",
    heightDp = 1800,
    locale = "ar",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun CardsPreview() {
    DhikraTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                val ids = listOf(
                    "st_sabah_1", "st_sabah_2", "st_sabah_17", "st_sabah_34", "st_sabah_29",
                    "preview_kahf", "st_nawm_qini", "preview_duha",
                )
                ids.mapNotNull { id -> previewItems.firstOrNull { it.id == id } }.forEach { item ->
                    ProgramItemView(
                        item = item,
                        done = previewState.doneCount(item),
                        textScale = 1f,
                        onOpen = {},
                        onSetComplete = {},
                        current = item.id == "st_sabah_17",
                    )
                }
            }
        }
    }
}

/** Group steps: a stack of cards (one per text), opened full screen like any other step. */
@Preview(name = "Group card", heightDp = 420, locale = "ar")
@Preview(
    name = "Group card (dark)",
    heightDp = 420,
    locale = "ar",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun GroupCardPreview() {
    DhikraTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                listOf("st_sabah_4", "st_nawm_tasbih").forEach { id ->
                    val group = previewItems.first { it.id == id }
                    ProgramItemView(
                        item = group,
                        done = previewState.doneCount(group),
                        textScale = 1f,
                        onOpen = {},
                        onSetComplete = {},
                        current = true,
                    )
                }
            }
        }
    }
}

@Preview(name = "Quran card", heightDp = 240, locale = "ar")
@Preview(
    name = "Quran card (dark)",
    heightDp = 240,
    locale = "ar",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun QuranCardPreview() {
    DhikraTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(Modifier.padding(16.dp)) {
                ProgramItemView(
                    item = previewItems.first { it.id == "preview_kahf" },
                    done = 0,
                    textScale = 1f,
                    onOpen = {},
                    onSetComplete = {},
                    current = true,
                )
            }
        }
    }
}

/** Picking the ayat of a Quran text: the first ten of Al-Kahf, with their preview. */
@Preview(name = "Quran passage picker", heightDp = 1100, locale = "ar")
@Composable
private fun QuranPassagePickerPreview() {
    DhikraTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            QuranPassagePicker(
                surahs = listOf(Surah(18, "الكهف", 110), Surah(112, "الإخلاص", 4)),
                sura = 18,
                from = "١",
                to = "١٠",
                onPick = { _, _, _ -> },
                passage = previewQuran.passage(QuranRef(18, 1, 10)),
                loading = false,
                showErrors = false,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@Preview(name = "Settings", showSystemUi = true, locale = "ar")
@Composable
private fun SettingsPreview() {
    DhikraTheme {
        SettingsScreen(
            onBack = {},
            actions = SettingsActions({}, {}, {}, {}, {}, {}),
        )
    }
}

@Preview(name = "Library", showSystemUi = true, locale = "ar")
@Composable
private fun LibraryPreview() {
    DhikraTheme {
        LibraryScreen(
            library = sample.library,
            onBack = {},
            onOpenCollection = {},
        )
    }
}

@Preview(name = "Collection", showSystemUi = true, locale = "ar")
@Composable
private fun CollectionPreview() {
    DhikraTheme {
        CollectionScreen(
            type = ItemType.DUA,
            library = sample.library,
            programs = listOf(sample.program),
            onBack = {},
            onSave = {},
            onDelete = {},
            onSaveGroup = {},
            onDeleteGroup = {},
        )
    }
}

@Preview(name = "Collection groups", showSystemUi = true, locale = "ar")
@Composable
private fun CollectionGroupsPreview() {
    DhikraTheme {
        CollectionScreen(
            type = ItemType.DUA,
            library = sample.library,
            programs = listOf(sample.program),
            onBack = {},
            onSave = {},
            onDelete = {},
            onSaveGroup = {},
            onDeleteGroup = {},
            initialTab = GROUPS_TAB,
        )
    }
}

/** Every step of [section] finished: the reader's last screen then closes the whole section. */
private fun allDoneState(section: ProgramSection): UiState = previewState.copy(
    progress = previewState.progress + section.trackableItems
        .flatMap { step -> if (step.isGroup) step.members else listOf(step) }
        .associate { it.id to it.target },
)

@Composable
private fun ReaderPreview(sectionId: String, startId: String, state: UiState = previewState) {
    DhikraTheme {
        StepReader(
            section = state.schedule.sections.first { it.id == sectionId },
            startId = startId,
            state = state,
            actions = noHomeActions,
            onClose = {},
        )
    }
}

/** A counted step: the bead dial, 37 of 100 so far. */
@Preview(name = "Reader: counted", showSystemUi = true, locale = "ar")
@Composable
private fun ReaderCountedPreview() {
    ReaderPreview(
        sectionId = "st_sabah",
        startId = "st_sabah_34",
        state = previewState.copy(progress = previewState.progress + ("st_sabah_34" to 37)),
    )
}

/** A counted step that is finished: the reader invites to swipe up. */
@Preview(name = "Reader: counted, finished", showSystemUi = true, locale = "ar")
@Composable
private fun ReaderCountedFinishedPreview() {
    ReaderPreview(
        sectionId = "st_sabah",
        startId = "st_sabah_34",
        state = previewState.copy(progress = previewState.progress + ("st_sabah_34" to 100)),
    )
}

/** A step read once: marked done with its button. */
@Preview(name = "Reader: Quran", showSystemUi = true, locale = "ar")
@Composable
private fun ReaderQuranPreview() {
    ReaderPreview(sectionId = "preview_extra", startId = "preview_kahf")
}

/** A group step opens on its first unfinished text. */
@Preview(name = "Reader: group", showSystemUi = true, locale = "ar")
@Composable
private fun ReaderGroupPreview() {
    ReaderPreview(sectionId = "st_sabah", startId = "st_sabah_4")
}

/** A task has no text: its screen only holds the title and the button. */
@Preview(name = "Reader: task", showSystemUi = true, locale = "ar")
@Composable
private fun ReaderTaskPreview() {
    ReaderPreview(sectionId = "preview_extra", startId = "preview_duha")
}

/** The last screen of a finished section. */
@Preview(name = "Reader: last", showSystemUi = true, locale = "ar")
@Composable
private fun ReaderLastPreview() {
    val section = previewState.schedule.sections.first { it.id == "st_nawm" }
    val last = section.trackableItems.last().let { if (it.isGroup) it.members.last() else it }
    ReaderPreview(
        sectionId = "st_nawm",
        startId = last.id,
        state = allDoneState(section),
    )
}

/** A phone held sideways: the dial goes beside the text. */
@Preview(name = "Reader: landscape", widthDp = 800, heightDp = 360, locale = "ar")
@Composable
private fun ReaderLandscapePreview() {
    ReaderPreview(
        sectionId = "st_sabah",
        startId = "st_sabah_34",
        state = previewState.copy(progress = previewState.progress + ("st_sabah_34" to 37)),
    )
}
