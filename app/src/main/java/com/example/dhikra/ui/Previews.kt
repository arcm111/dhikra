package com.example.dhikra.ui

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
import com.example.dhikra.data.DefaultProgram
import com.example.dhikra.data.ItemType
import com.example.dhikra.ui.components.CounterScreen
import com.example.dhikra.ui.home.GroupMemberView
import com.example.dhikra.ui.home.HomeActions
import com.example.dhikra.ui.home.HomeScreen
import com.example.dhikra.ui.home.ItemActions
import com.example.dhikra.ui.home.ProgramItemView
import com.example.dhikra.ui.library.CollectionScreen
import com.example.dhikra.ui.library.GROUPS_TAB
import com.example.dhikra.ui.library.LibraryScreen
import com.example.dhikra.ui.library.SettingsActions
import com.example.dhikra.ui.library.SettingsScreen
import com.example.dhikra.ui.theme.DhikraTheme

private val sample = DefaultProgram.bundle()

private val previewState = UiState(
    loading = false,
    program = sample.program,
    library = sample.library,
    schedule = sample.program.resolvedWith(sample.library),
    day = "2026-11-04",
    progress = mapOf(
        "shurooq_adiya/dua_al_ahd" to 1,
        "shurooq_adiya/dua_baraka" to 1,
        "fajr_istighfar" to 3,
        "shurooq_muawwidhat/al_ikhlas" to 3,
        "shurooq_muawwidhat/al_falaq" to 1,
        "duha_adiya/dua_duha_tawba" to 100,
        "duha_adiya/dua_hasbi" to 1,
    ),
    selectedSectionId = "shurooq",
    nowSectionId = "shurooq",
)

private val previewItems = previewState.schedule.sections.flatMap { it.items }

private val noItemActions = ItemActions({}, {}, {}, {}, {})

private val noHomeActions = HomeActions(
    onSelectSection = {},
    onEdit = {},
    onOpenLibrary = {},
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
                    "shurooq_tasbeeh", "shurooq_tasbeeh_100", "qiyam_virtue", "qiyam_dua",
                    "open_ikhlas", "shurooq_kahf", "shukr_hamd", "fajr_tasbih", "salat_shukr",
                )
                ids.mapNotNull { id -> previewItems.firstOrNull { it.id == id } }.forEach { item ->
                    ProgramItemView(
                        item = item,
                        done = previewState.doneCount(item),
                        playing = false,
                        expanded = item.id == "open_ikhlas",
                        current = item.id == "open_ikhlas",
                        lettersPerSecond = 7f,
                        textScale = 1f,
                        actions = noItemActions,
                    )
                }
            }
        }
    }
}

/** An open group step: each of its texts on its own card, the first unfinished one current. */
@Preview(name = "Group card", heightDp = 2600, locale = "ar")
@Preview(
    name = "Group card (dark)",
    heightDp = 2600,
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
                listOf("shurooq_muawwidhat", "duha_adiya").forEach { id ->
                    val group = previewItems.first { it.id == id }
                    val next = group.members.firstOrNull { !previewState.isComplete(it) }?.id
                    ProgramItemView(
                        item = group,
                        done = previewState.doneCount(group),
                        playing = false,
                        expanded = true,
                        current = true,
                        lettersPerSecond = 7f,
                        textScale = 1f,
                        actions = noItemActions,
                        member = { text ->
                            GroupMemberView(
                                item = text,
                                done = previewState.doneCount(text),
                                playing = false,
                                lettersPerSecond = 7f,
                                textScale = 1f,
                                actions = noItemActions,
                                current = text.id == next,
                            )
                        },
                    )
                }
            }
        }
    }
}

@Preview(name = "Quran card", heightDp = 1100, locale = "ar")
@Preview(
    name = "Quran card (dark)",
    heightDp = 1100,
    locale = "ar",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun QuranCardPreview() {
    DhikraTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(Modifier.padding(16.dp)) {
                ProgramItemView(
                    item = previewItems.first { it.id == "shurooq_kahf" },
                    done = 0,
                    playing = false,
                    expanded = true,
                    current = true,
                    lettersPerSecond = 7f,
                    textScale = 1f,
                    actions = noItemActions,
                )
            }
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
            program = sample.program,
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
            program = sample.program,
            onBack = {},
            onSave = {},
            onDelete = {},
            onSaveGroup = {},
            onDeleteGroup = {},
            initialTab = GROUPS_TAB,
        )
    }
}

@Preview(name = "Counter", showSystemUi = true, locale = "ar")
@Composable
private fun CounterPreview() {
    DhikraTheme {
        CounterScreen(
            title = "سبحان الله وبحمده",
            text = "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ",
            opening = "",
            done = 37,
            target = 100,
            onCount = {},
            onUndo = {},
            onReset = {},
            onClose = {},
        )
    }
}
