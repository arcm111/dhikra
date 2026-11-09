package com.example.dhikra.ui.library

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.OutlinedButton
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Edit
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.LocationOn
import com.example.dhikra.data.ReminderSound
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.dhikra.R
import com.example.dhikra.data.ArabicText
import com.example.dhikra.data.GroupItem
import com.example.dhikra.data.ItemType
import com.example.dhikra.data.Library
import com.example.dhikra.data.LibraryEntry
import com.example.dhikra.data.LibraryGroup
import com.example.dhikra.data.Program
import com.example.dhikra.data.ProgramJson
import com.example.dhikra.data.QuranPassage
import com.example.dhikra.data.QuranRef
import com.example.dhikra.data.Surah
import com.example.dhikra.data.groupUsages
import com.example.dhikra.data.reads
import com.example.dhikra.data.usages
import com.example.dhikra.ui.components.DhikraHeader
import com.example.dhikra.ui.components.MosqueIcon
import com.example.dhikra.ui.components.PrayerIcon
import com.example.dhikra.ui.components.ReminderSoundDialog
import com.example.dhikra.ui.components.reminderSoundLabel
import com.example.dhikra.ui.components.HeaderTab
import com.example.dhikra.ui.components.Pill
import com.example.dhikra.ui.components.QuranIcon
import com.example.dhikra.ui.components.StarOrnamentIcon
import com.example.dhikra.ui.components.headerAppBarColors
import com.example.dhikra.ui.components.icon
import com.example.dhikra.ui.home.GroupBadge
import com.example.dhikra.ui.home.colors
import com.example.dhikra.ui.theme.ReadingStyles

/** Name of the library collection holding entries of this type. */
val ItemType.collectionName: Int
    get() = when (this) {
        ItemType.QURAN -> R.string.collection_quran
        ItemType.HADITH -> R.string.collection_hadith
        ItemType.DUA -> R.string.collection_dua
        else -> R.string.collection_dhikr
    }

/** What the settings screen can ask for. */
class SettingsActions(
    val onTextSize: () -> Unit,
    val onShare: () -> Unit,
    val onExport: () -> Unit,
    val onImport: () -> Unit,
    val onResetToday: () -> Unit,
    val onRestoreDefault: () -> Unit,
)

/** Screen top bar inside the branded header, with a back arrow and optional [tabs] below it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScreenHeader(title: String, onBack: () -> Unit, tabs: (@Composable () -> Unit)? = null) {
    DhikraHeader(bottomPadding = if (tabs == null) 4.dp else 14.dp) {
        TopAppBar(
            title = { Text(title, style = MaterialTheme.typography.titleLarge) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                }
            },
            expandedHeight = 56.dp,
            windowInsets = WindowInsets(0, 0, 0, 0),
            colors = headerAppBarColors(),
        )
        tabs?.invoke()
    }
}

/** The library, opened from the main menu: its four collections and how many texts each holds. */
@Composable
fun LibraryScreen(library: Library, onBack: () -> Unit, onOpenCollection: (ItemType) -> Unit) {
    Scaffold(
        topBar = { ScreenHeader(stringResource(R.string.library), onBack) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        val layoutDirection = LocalLayoutDirection.current
        LazyColumn(
            modifier = Modifier.fillMaxSize().consumeWindowInsets(padding),
            contentPadding = PaddingValues(
                start = padding.calculateStartPadding(layoutDirection) + 16.dp,
                end = padding.calculateEndPadding(layoutDirection) + 16.dp,
                top = padding.calculateTopPadding() + 16.dp,
                bottom = padding.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Text(
                    text = stringResource(R.string.library_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 4.dp),
                )
            }
            item {
                SettingsGroup {
                    ItemType.CONTENT.forEachIndexed { index, type ->
                        if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        val colors = type.colors()
                        SettingsRow(
                            title = stringResource(type.collectionName),
                            subtitle = stringResource(
                                R.string.collection_counts,
                                ArabicText.toArabicDigits(library.ofType(type).size),
                                ArabicText.toArabicDigits(library.groupsOfType(type).size),
                            ),
                            leading = {
                                Box(
                                    Modifier.size(40.dp).clip(CircleShape).background(
                                        colors.accent.copy(alpha = 0.13f)
                                    ),
                                    contentAlignment = Alignment.Center,
                                ) { Icon(type.icon, null, tint = colors.accent, modifier = Modifier.size(22.dp)) }
                            },
                            chevron = true,
                            onClick = { onOpenCollection(type) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    actions: SettingsActions,
    sectionSound: ReminderSound = ReminderSound.Default,
    onSectionSound: (ReminderSound) -> Unit = {},
) {
    var choosingSound by rememberSaveable { mutableStateOf(false) }
    if (choosingSound) {
        ReminderSoundDialog(
            current = sectionSound,
            allowAzan = false,
            title = stringResource(R.string.section_sound),
            onSelect = { onSectionSound(it); choosingSound = false },
            onDismiss = { choosingSound = false },
        )
    }
    Scaffold(
        topBar = { ScreenHeader(stringResource(R.string.settings), onBack) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        val layoutDirection = LocalLayoutDirection.current
        LazyColumn(
            modifier = Modifier.fillMaxSize().consumeWindowInsets(padding),
            contentPadding = PaddingValues(
                start = padding.calculateStartPadding(layoutDirection) + 16.dp,
                end = padding.calculateEndPadding(layoutDirection) + 16.dp,
                top = padding.calculateTopPadding() + 16.dp,
                bottom = padding.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { GroupTitle(R.string.settings_display) }
            item {
                SettingsGroup {
                    SettingsRow(
                        title = stringResource(R.string.action_text_size),
                        leading = { RowIcon { Text("أ", style = MaterialTheme.typography.titleMedium) } },
                        onClick = actions.onTextSize,
                    )
                }
            }
            item { GroupTitle(R.string.settings_reminders) }
            item {
                SettingsGroup {
                    SettingsRow(
                        title = stringResource(R.string.section_sound),
                        subtitle = reminderSoundLabel(sectionSound),
                        leading = { RowIcon(Icons.Outlined.Notifications) },
                        onClick = { choosingSound = true },
                        chevron = true,
                    )
                }
            }
            item { GroupTitle(R.string.settings_data) }
            item {
                SettingsGroup {
                    SettingsRow(stringResource(R.string.action_share), leading = { RowIcon(Icons.Outlined.Share) }, onClick = actions.onShare)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsRow(
                        stringResource(R.string.action_export),
                        leading = { RowIcon { Icon(painterResource(R.drawable.ic_upload), null) } },
                        onClick = actions.onExport,
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsRow(
                        stringResource(R.string.action_import),
                        leading = { RowIcon { Icon(painterResource(R.drawable.ic_download), null) } },
                        onClick = actions.onImport,
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsRow(stringResource(R.string.action_reset_today), leading = { RowIcon(Icons.Outlined.Refresh) }, onClick = actions.onResetToday)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsRow(stringResource(R.string.action_restore_default), leading = { RowIcon(StarOrnamentIcon) }, onClick = actions.onRestoreDefault)
                }
            }
            item { GroupTitle(R.string.settings_about) }
            item {
                // Tanzil's terms: the source of the Quran text is named, with a link to tanzil.net.
                val uriHandler = LocalUriHandler.current
                val url = stringResource(R.string.quran_source_url)
                SettingsGroup {
                    SettingsRow(
                        title = stringResource(R.string.quran_source),
                        subtitle = stringResource(R.string.quran_source_details),
                        leading = { RowIcon(QuranIcon) },
                        onClick = { uriHandler.openUri(url) },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    val azanUrl = stringResource(R.string.azan_source_url)
                    SettingsRow(
                        title = stringResource(R.string.azan_source),
                        subtitle = stringResource(R.string.azan_source_details),
                        leading = { RowIcon(MosqueIcon) },
                        onClick = { uriHandler.openUri(azanUrl) },
                    )
                    // GeoNames' licence (CC BY 4.0) asks for this credit.
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    val geonamesUrl = stringResource(R.string.geonames_source_url)
                    SettingsRow(
                        title = stringResource(R.string.geonames_source),
                        subtitle = stringResource(R.string.geonames_source_details),
                        leading = { RowIcon(Icons.Outlined.LocationOn) },
                        onClick = { uriHandler.openUri(geonamesUrl) },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    val adhanUrl = stringResource(R.string.adhan_source_url)
                    SettingsRow(
                        title = stringResource(R.string.adhan_source),
                        subtitle = stringResource(R.string.adhan_source_details),
                        leading = { RowIcon(PrayerIcon) },
                        onClick = { uriHandler.openUri(adhanUrl) },
                    )
                }
            }
        }
    }
}

@Composable
private fun GroupTitle(title: Int) {
    Text(
        text = stringResource(title),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 8.dp, top = 12.dp, bottom = 4.dp),
    )
}

@Composable
private fun SettingsGroup(content: @Composable () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = Modifier.fillMaxWidth(),
    ) { Column { content() } }
}

@Composable
private fun RowIcon(icon: ImageVector) = RowIcon { Icon(icon, null) }

@Composable
private fun RowIcon(content: @Composable () -> Unit) {
    Box(
        Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides MaterialTheme.colorScheme.onSecondaryContainer,
            content = content,
        )
    }
}

@Composable
private fun SettingsRow(
    title: String,
    leading: @Composable () -> Unit,
    onClick: () -> Unit,
    subtitle: String? = null,
    chevron: Boolean = false,
) {
    Surface(onClick = onClick, color = androidx.compose.ui.graphics.Color.Transparent) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            leading()
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                if (subtitle != null) {
                    Text(subtitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (chevron) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ------------------------------------------------------------------------------ Collection

/** Index of the tab showing the groups of a collection (the first one shows its single texts). */
const val GROUPS_TAB = 1

/**
 * One library collection, in two tabs: its single texts, and its groups of texts (read one after
 * the other). Both are searchable, with add / edit / delete.
 */
@Composable
fun CollectionScreen(
    type: ItemType,
    library: Library,
    /** All the user's programs: a text is "in the program" when one of them reads it. */
    programs: List<Program>,
    onBack: () -> Unit,
    onSave: (LibraryEntry) -> Unit,
    onDelete: (String) -> Unit,
    onSaveGroup: (LibraryGroup) -> Unit,
    onDeleteGroup: (String) -> Unit,
    initialTab: Int = 0,
    /** Saves a copy of a group (the copy, the original's id), placed right after the original. */
    onSaveGroupCopy: (LibraryGroup, String) -> Unit = { copy, _ -> onSaveGroup(copy) },
) {
    var tab by rememberSaveable { mutableIntStateOf(initialTab) }
    var query by rememberSaveable { mutableStateOf("") }
    val resources = LocalResources.current
    var editing by remember { mutableStateOf<LibraryEntry?>(null) }
    var editingGroup by remember { mutableStateOf<LibraryGroup?>(null) }
    var adding by rememberSaveable { mutableStateOf(false) }
    val groupsTab = tab == GROUPS_TAB
    val entries = library.ofType(type)
    val groups = library.groupsOfType(type)
    val key = ArabicText.searchKey(query)
    val shownEntries = remember(entries, key) { if (key.isEmpty()) entries else entries.filter { it.matches(key) } }
    val shownGroups = remember(groups, library, key) {
        if (key.isEmpty()) groups
        else groups.filter { group ->
            ArabicText.searchKey(group.title + " " + group.description).contains(key) ||
                group.items.any { library.entry(it.entryId)?.matches(key) == true }
        }
    }

    Scaffold(
        topBar = {
            ScreenHeader(stringResource(type.collectionName), onBack) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp).selectableGroup(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CollectionTab(R.string.library_tab_singles, entries.size, selected = !groupsTab) { tab = 0 }
                    CollectionTab(R.string.library_tab_groups, groups.size, selected = groupsTab) { tab = GROUPS_TAB }
                }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { adding = true },
                icon = { Icon(Icons.Filled.Add, null) },
                text = { Text(stringResource(if (groupsTab) R.string.add_group else R.string.add_entry)) },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        val layoutDirection = LocalLayoutDirection.current
        LazyColumn(
            modifier = Modifier.fillMaxSize().consumeWindowInsets(padding),
            contentPadding = PaddingValues(
                start = padding.calculateStartPadding(layoutDirection) + 16.dp,
                end = padding.calculateEndPadding(layoutDirection) + 16.dp,
                top = padding.calculateTopPadding() + 16.dp,
                bottom = padding.calculateBottomPadding() + 96.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (type == ItemType.QURAN) {
                item(key = "source") { QuranSourceNote() }
            }
            if (entries.isNotEmpty() || groups.isNotEmpty()) {
                item(key = "search") { SearchField(query, onQueryChange = { query = it }) }
            }
            if (groupsTab) {
                if (shownGroups.isEmpty()) {
                    item(key = "empty") { EmptyText(if (groups.isEmpty()) R.string.groups_empty else R.string.no_results) }
                }
                items(shownGroups, key = { "group:" + it.id }) { group ->
                    GroupEntryCard(
                        group = group,
                        library = library,
                        inProgram = programs.groupUsages(group.id) > 0,
                        onClick = { editingGroup = group },
                        onDuplicate = {
                            // The copy is saved at once, right after the original, and opened to rename it.
                            library.duplicateGroup(group.id) { resources.getString(R.string.section_copy, it) }?.let { (_, copy) ->
                                onSaveGroupCopy(copy, group.id)
                                editingGroup = copy
                            }
                        },
                        modifier = Modifier.animateItem(),
                    )
                }
            } else {
                if (shownEntries.isEmpty()) {
                    item(key = "empty") { EmptyText(if (entries.isEmpty()) R.string.collection_empty else R.string.no_results) }
                }
                items(shownEntries, key = { "entry:" + it.id }) { entry ->
                    EntryCard(
                        entry = entry,
                        inProgram = programs.reads(entry.id, library),
                        onClick = { editing = entry },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }

    if (adding) {
        if (groupsTab) {
            GroupEditorDialog(
                type = type,
                initial = null,
                library = library,
                onDismiss = { adding = false },
                onSave = { onSaveGroup(it); adding = false },
                onSaveEntry = onSave,
            )
        } else {
            EntryEditorDialog(
                type = type,
                initial = null,
                onDismiss = { adding = false },
                onSave = { onSave(it); adding = false },
            )
        }
    }
    editing?.let { entry ->
        EntryEditorDialog(
            type = type,
            initial = entry,
            usages = programs.usages(entry.id),
            groups = library.groupsWith(entry.id).size,
            onDismiss = { editing = null },
            onSave = { onSave(it); editing = null },
            onDelete = { onDelete(entry.id); editing = null },
        )
    }
    editingGroup?.let { group ->
        GroupEditorDialog(
            type = type,
            initial = group,
            library = library,
            usages = programs.groupUsages(group.id),
            onDismiss = { editingGroup = null },
            onSave = { onSaveGroup(it); editingGroup = null },
            onSaveEntry = onSave,
            onDelete = { onDeleteGroup(group.id); editingGroup = null },
        )
    }
}

/** A tab of a collection: its name and how many items it holds. */
@Composable
private fun CollectionTab(label: Int, count: Int, selected: Boolean, onClick: () -> Unit) {
    HeaderTab(selected = selected, onClick = onClick) {
        Text(stringResource(label), style = MaterialTheme.typography.labelLarge, maxLines = 1)
        Spacer(Modifier.width(8.dp))
        Text(ArabicText.toArabicDigits(count), style = MaterialTheme.typography.labelMedium, maxLines = 1)
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(stringResource(R.string.search)) },
        leadingIcon = { Icon(Icons.Filled.Search, null) },
        trailingIcon = if (query.isNotEmpty()) {
            { IconButton(onClick = { onQueryChange("") }) { Icon(Icons.Filled.Close, stringResource(R.string.close)) } }
        } else null,
        singleLine = true,
        shape = CircleShape,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun EmptyText(text: Int) {
    Text(
        text = stringResource(text),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(32.dp),
    )
}

/** The entry's text as previewed in the lists: a Quran text's ayah numbers in Arabic digits. */
private val LibraryEntry.previewText: String
    get() = if (type == ItemType.QURAN) ArabicText.toArabicDigits(text) else text

/** Whether the entry's title, description or text contains the search [key] (see [ArabicText.searchKey]). */
private fun LibraryEntry.matches(key: String): Boolean =
    ArabicText.searchKey("$title $description $text").contains(key)

/** A library text, tinted with its collection's colors (the Quran on its dark green). */
@Composable
fun EntryCard(
    entry: LibraryEntry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    inProgram: Boolean = false,
    selected: Boolean = false,
) {
    val colors = entry.type.colors()
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = colors.container,
        border = when {
            selected -> androidx.compose.foundation.BorderStroke(2.dp, colors.accent)
            colors.border.alpha > 0f -> androidx.compose.foundation.BorderStroke(1.dp, colors.border)
            else -> null
        },
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = entry.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (entry.type == ItemType.QURAN) colors.accent else colors.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (inProgram) {
                    com.example.dhikra.ui.components.Pill(stringResource(R.string.entry_in_program), colors.accent)
                }
            }
            if (entry.description.isNotBlank()) {
                Text(
                    text = entry.description,
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.ink.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = entry.previewText,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.ink.copy(alpha = 0.85f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

private const val BASMALA = "بِسۡمِ ٱللَّهِ ٱلرَّحۡمَٰنِ ٱلرَّحِيمِ"
private const val ISTIADHA = "أَعُوذُ بِاللَّهِ مِنَ الشَّيْطَانِ الرَّجِيمِ"

/**
 * Full-screen editor of a library text: title, optional description, optional opening (Quran
 * and dua) and the text itself. A Quran text is not typed in: its surah and ayat are picked, and
 * its text read from the bundled Quran text. [onDelete] adds a delete action (with confirmation,
 * which tells how many program steps and [groups] read the text).
 */
@Composable
fun EntryEditorDialog(
    type: ItemType,
    initial: LibraryEntry?,
    onDismiss: () -> Unit,
    onSave: (LibraryEntry) -> Unit,
    usages: Int = 0,
    groups: Int = 0,
    onDelete: (() -> Unit)? = null,
) {
    var title by rememberSaveable { mutableStateOf(initial?.title.orEmpty()) }
    var description by rememberSaveable { mutableStateOf(initial?.description.orEmpty()) }
    var opening by rememberSaveable { mutableStateOf(initial?.opening.orEmpty()) }
    var text by rememberSaveable { mutableStateOf(initial?.text.orEmpty()) }
    var showErrors by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val hasOpening = type == ItemType.QURAN || type == ItemType.DUA

    // A Quran text: the surah and the two ayah fields as typed, and the passage they name.
    val isQuran = type == ItemType.QURAN
    val resources = LocalResources.current
    val quranLookup = LocalQuranLookup.current
    val surahs by produceState(emptyList<Surah>(), quranLookup, isQuran) {
        if (isQuran) value = quranLookup?.surahs().orEmpty()
    }
    var sura by rememberSaveable { mutableIntStateOf(initial?.quran?.sura ?: 0) }
    var fromAya by rememberSaveable { mutableStateOf(initial?.quran?.from?.let(ArabicText::toArabicDigits).orEmpty()) }
    var toAya by rememberSaveable { mutableStateOf(initial?.quran?.to?.let(ArabicText::toArabicDigits).orEmpty()) }
    val ref = if (isQuran) quranRefOf(sura, fromAya, toAya) else null
    var loaded by remember { mutableStateOf<Pair<QuranRef, QuranPassage?>?>(null) }
    LaunchedEffect(ref, quranLookup) {
        if (ref != null && loaded?.first != ref) loaded = ref to quranLookup?.passage(ref)
    }
    val passage = loaded?.takeIf { it.first == ref }?.second
    // A Quran text typed in before the Quran text was bundled, kept until a passage is picked.
    val legacyText = isQuran && initial != null && initial.quran == null && sura == 0 && initial.text.isNotBlank()

    // The title and description last filled in from the passage: they follow it until edited.
    var autoTitle by rememberSaveable { mutableStateOf("") }
    var autoDescription by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(surahs) {
        val initialRef = initial?.quran ?: return@LaunchedEffect
        if (autoTitle.isEmpty()) surahs.firstOrNull { it.number == initialRef.sura }?.let { autoTitle = quranTitle(resources, it) }
        if (autoDescription.isEmpty()) autoDescription = quranRangeDescription(resources, initialRef)
    }

    fun pick(newSura: Int, newFrom: String, newTo: String) {
        if (newSura != sura) {
            surahs.firstOrNull { it.number == newSura }?.let { surah ->
                val auto = quranTitle(resources, surah)
                if (title.isBlank() || title == autoTitle) title = auto
                autoTitle = auto
            }
        }
        quranRefOf(newSura, newFrom, newTo)?.let { newRef ->
            val auto = quranRangeDescription(resources, newRef)
            if (description.isBlank() || description == autoDescription) description = auto
            autoDescription = auto
        }
        sura = newSura
        fromAya = newFrom
        toAya = newTo
    }

    fun save() {
        val hasText = if (isQuran) passage != null || legacyText else text.isNotBlank()
        if (title.isBlank() || !hasText) {
            showErrors = true
            return
        }
        onSave(
            (initial ?: LibraryEntry(type = type, title = "", text = "")).copy(
                title = title.trim(),
                description = description.trim(),
                opening = if (hasOpening) opening.trim() else "",
                text = passage?.text ?: text.trim(),
                quran = passage?.ref,
            )
        )
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.imePadding()) {
                EditorTopBar(
                    title = stringResource(if (initial == null) R.string.new_entry else R.string.edit_entry),
                    onClose = onDismiss,
                    onSave = ::save,
                    onDelete = if (onDelete != null) ({ confirmDelete = true }) else null,
                )
                HorizontalDivider()
                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(stringResource(type.collectionName), style = MaterialTheme.typography.labelLarge, color = type.colors().accent)
                    // A Quran text starts with its ayat: they fill in the title and description below.
                    if (isQuran) {
                        QuranPassagePicker(
                            surahs = surahs,
                            sura = sura,
                            from = fromAya,
                            to = toAya,
                            onPick = ::pick,
                            passage = passage,
                            loading = ref != null && loaded?.first != ref,
                            showErrors = showErrors && !legacyText,
                        )
                    }
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text(stringResource(R.string.entry_title)) },
                        isError = showErrors && title.isBlank(),
                        supportingText = if (showErrors && title.isBlank()) {
                            { Text(stringResource(R.string.entry_title_error)) }
                        } else null,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text(stringResource(R.string.entry_description)) },
                        placeholder = { Text(stringResource(R.string.entry_description_hint)) },
                        minLines = 1,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (hasOpening) {
                        OutlinedTextField(
                            value = opening,
                            onValueChange = { opening = it },
                            label = { Text(stringResource(R.string.item_opening)) },
                            supportingText = if (opening.isBlank() && ref?.opensWithBasmala == true) {
                                { Text(stringResource(R.string.quran_basmala_auto)) }
                            } else null,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = opening == BASMALA,
                                onClick = { opening = if (opening == BASMALA) "" else BASMALA },
                                label = { Text(stringResource(R.string.opening_basmala)) },
                            )
                            FilterChip(
                                selected = opening == ISTIADHA,
                                onClick = { opening = if (opening == ISTIADHA) "" else ISTIADHA },
                                label = { Text(stringResource(R.string.opening_istiadha)) },
                            )
                        }
                    }
                    if (!isQuran || legacyText) {
                        OutlinedTextField(
                            value = text,
                            onValueChange = { text = it },
                            label = { Text(stringResource(R.string.entry_text)) },
                            readOnly = legacyText,
                            isError = !isQuran && showErrors && text.isBlank(),
                            supportingText = when {
                                legacyText -> {
                                    { Text(stringResource(R.string.quran_legacy_text)) }
                                }
                                showErrors && text.isBlank() -> {
                                    { Text(stringResource(R.string.entry_text_error)) }
                                }
                                else -> null
                            },
                            textStyle = (if (isQuran) ReadingStyles.quran(0.85f) else ReadingStyles.dua(0.9f))
                                .copy(textAlign = TextAlign.Start, color = MaterialTheme.colorScheme.onSurface),
                            minLines = 5,
                            maxLines = 16,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }

    if (confirmDelete && onDelete != null && initial != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            icon = { Icon(Icons.Filled.Delete, null) },
            text = {
                val message = if (usages > 0) {
                    stringResource(R.string.delete_entry_used_message, initial.title, ArabicText.toArabicDigits(usages))
                } else {
                    stringResource(R.string.delete_entry_message, initial.title)
                }
                val groupsNote = if (groups > 0) {
                    " " + stringResource(R.string.delete_entry_groups_note, ArabicText.toArabicDigits(groups))
                } else ""
                Text(message + groupsNote)
            },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete() }) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

/** Top row of the full-screen editors: close, title, an optional delete and the save button. */
@Composable
private fun EditorTopBar(title: String, onClose: () -> Unit, onSave: () -> Unit, onDelete: (() -> Unit)?) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onClose) { Icon(Icons.Filled.Close, stringResource(R.string.close)) }
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        if (onDelete != null) {
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, stringResource(R.string.delete), tint = MaterialTheme.colorScheme.error)
            }
        }
        Button(onClick = onSave, modifier = Modifier.padding(end = 8.dp)) { Text(stringResource(R.string.save)) }
    }
}

// ------------------------------------------------------------------------------ Groups

/** Accent of a collection on a light surface. */
@Composable
private fun ItemType.surfaceAccent(): Color = colors().accent

/** A group of the library: its badge, title, description and the titles of its texts in order. */
@Composable
fun GroupEntryCard(
    group: LibraryGroup,
    library: Library,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    inProgram: Boolean = false,
    selected: Boolean = false,
    /** Adds a menu to the card with «تعديل» ([onClick]) and «تكرار» (this). */
    onDuplicate: (() -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val accent = group.type.surfaceAccent()
    val border = group.type.colors().border
    val titles = group.items.mapNotNull { item ->
        library.entry(item.entryId)?.let { entry ->
            if (item.count > 1) entry.title + " " + stringResource(R.string.times_count, ArabicText.toArabicDigits(item.count))
            else entry.title
        }
    }
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = scheme.surfaceContainerLowest,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) accent else border),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 14.dp)) {
            GroupBadge(group.type, group.items.size, modifier = Modifier.padding(top = 2.dp), size = 40.dp)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = group.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = scheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (inProgram) Pill(stringResource(R.string.entry_in_program), accent)
                    if (onDuplicate != null) GroupCardMenu(onEdit = onClick, onDuplicate = onDuplicate)
                }
                Text(
                    text = listOf(
                        stringResource(R.string.group_texts_count, ArabicText.toArabicDigits(titles.size)),
                        if (group.isChoice) {
                            stringResource(R.string.group_required_short, ArabicText.toArabicDigits(group.required))
                        } else "",
                        group.description,
                    ).filter { it.isNotBlank() }.joinToString(" • "),
                    style = MaterialTheme.typography.labelMedium,
                    color = accent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = titles.joinToString("، "),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}

/** A library group's menu: edit it, or make a copy of it. */
@Composable
private fun GroupCardMenu(onEdit: () -> Unit, onDuplicate: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Filled.MoreVert, stringResource(R.string.action_more))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.edit_group)) },
                leadingIcon = { Icon(Icons.Filled.Edit, null) },
                onClick = { open = false; onEdit() },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.duplicate)) },
                leadingIcon = { Icon(Icons.Filled.Add, null) },
                onClick = { open = false; onDuplicate() },
            )
        }
    }
}

/** Keeps a group's texts across recreation, as "count:entryId" strings. */
private val GroupItemsSaver = listSaver<List<GroupItem>, String>(
    save = { items -> items.map { "${it.count}:${it.entryId}" } },
    restore = { saved ->
        saved.map { GroupItem(entryId = it.substringAfter(':'), count = it.substringBefore(':').toInt()) }
    },
)

/**
 * Full-screen editor of a group: title, optional description and its texts in reading order, each
 * with its repeat count. Texts are picked from the collection, or written here (which adds them to
 * the library too). [onDelete] adds a delete action (with confirmation, which tells how many
 * program steps read the group).
 */
@Composable
fun GroupEditorDialog(
    type: ItemType,
    initial: LibraryGroup?,
    library: Library,
    onDismiss: () -> Unit,
    onSave: (LibraryGroup) -> Unit,
    onSaveEntry: (LibraryEntry) -> Unit,
    usages: Int = 0,
    onDelete: (() -> Unit)? = null,
) {
    var title by rememberSaveable { mutableStateOf(initial?.title.orEmpty()) }
    var description by rememberSaveable { mutableStateOf(initial?.description.orEmpty()) }
    var items by rememberSaveable(stateSaver = GroupItemsSaver) { mutableStateOf(initial?.items.orEmpty()) }
    // All the texts required (0), or a minimum the reader chooses them to.
    var required by rememberSaveable { mutableIntStateOf(initial?.required ?: 0) }
    var query by rememberSaveable { mutableStateOf("") }
    var showErrors by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var writingEntry by remember { mutableStateOf(false) }
    val accent = type.surfaceAccent()
    val available = library.ofType(type).filter { entry -> items.none { it.entryId == entry.id } }
    val key = ArabicText.searchKey(query)
    val shown = if (key.isEmpty()) available else available.filter { it.matches(key) }

    fun save() {
        if (title.isBlank() || items.isEmpty()) {
            showErrors = true
            return
        }
        onSave(
            (initial ?: LibraryGroup(type = type, title = "")).copy(
                title = title.trim(),
                description = description.trim(),
                items = items,
                required = required.takeIf { it in 1 until items.size } ?: 0,
            )
        )
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.imePadding()) {
                EditorTopBar(
                    title = stringResource(if (initial == null) R.string.new_group else R.string.edit_group),
                    onClose = onDismiss,
                    onSave = ::save,
                    onDelete = if (onDelete != null) ({ confirmDelete = true }) else null,
                )
                HorizontalDivider()
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item(key = "collection") {
                        Text(stringResource(type.collectionName), style = MaterialTheme.typography.labelLarge, color = accent)
                    }
                    item(key = "title") {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text(stringResource(R.string.group_title)) },
                            isError = showErrors && title.isBlank(),
                            supportingText = if (showErrors && title.isBlank()) {
                                { Text(stringResource(R.string.group_title_error)) }
                            } else null,
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    item(key = "description") {
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text(stringResource(R.string.entry_description)) },
                            placeholder = { Text(stringResource(R.string.entry_description_hint)) },
                            maxLines = 4,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    item(key = "required") {
                        RequiredEditor(required = required, size = items.size, accent = accent, onChange = { required = it })
                    }
                    item(key = "items") {
                        val missing = showErrors && items.isEmpty()
                        Column(Modifier.padding(top = 6.dp)) {
                            Text(
                                text = stringResource(R.string.group_items) + " (" + ArabicText.toArabicDigits(items.size) + ")",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (missing) MaterialTheme.colorScheme.error else Color.Unspecified,
                            )
                            if (items.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.group_items_error),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (missing) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    itemsIndexed(items, key = { _, item -> "item:" + item.entryId }) { index, item ->
                        GroupItemRow(
                            number = index + 1,
                            entry = library.entry(item.entryId),
                            type = type,
                            count = item.count,
                            canMoveUp = index > 0,
                            canMoveDown = index < items.lastIndex,
                            onCountChange = { count ->
                                items = items.map { if (it.entryId == item.entryId) it.copy(count = count) else it }
                            },
                            onMove = { delta -> items = items.moved(index, delta) },
                            onRemove = { items = items.filterNot { it.entryId == item.entryId } },
                            modifier = Modifier.animateItem(),
                        )
                    }
                    item(key = "add") {
                        Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(R.string.group_add_items),
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(onClick = { writingEntry = true }) {
                                Icon(Icons.Filled.Add, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.add_entry))
                            }
                        }
                    }
                    if (available.size > 4) {
                        item(key = "search") { SearchField(query, onQueryChange = { query = it }) }
                    }
                    if (available.isEmpty()) {
                        item(key = "all_added") {
                            Text(
                                text = stringResource(R.string.group_all_added),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    items(shown, key = { "available:" + it.id }) { entry ->
                        AvailableEntryRow(
                            entry = entry,
                            accent = accent,
                            onAdd = { items = items + GroupItem(entry.id); showErrors = false },
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
            }
        }
    }

    if (writingEntry) {
        EntryEditorDialog(
            type = type,
            initial = null,
            onDismiss = { writingEntry = false },
            onSave = { entry ->
                onSaveEntry(entry)
                items = items + GroupItem(entry.id)
                showErrors = false
                writingEntry = false
            },
        )
    }

    if (confirmDelete && onDelete != null && initial != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            icon = { Icon(Icons.Filled.Delete, null) },
            text = {
                Text(
                    if (usages > 0) stringResource(R.string.delete_group_used_message, initial.title, ArabicText.toArabicDigits(usages))
                    else stringResource(R.string.delete_group_message, initial.title)
                )
            },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete() }) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

/**
 * How many of the group's texts are required: all of them, or a custom minimum (1 up to one less
 * than the texts), the reader choosing which.
 */
@Composable
private fun RequiredEditor(required: Int, size: Int, accent: Color, onChange: (Int) -> Unit) {
    // A minimum needs at least two texts; one at or over the texts' number is "all".
    val custom = required in 1 until size
    Column(Modifier.fillMaxWidth().padding(top = 6.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.choose_title), style = MaterialTheme.typography.labelLarge)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = !custom,
                onClick = { onChange(0) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
            ) { Text(stringResource(R.string.required_all)) }
            SegmentedButton(
                selected = custom,
                enabled = size > 1,
                onClick = { if (!custom) onChange((size - 1).coerceAtLeast(1)) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
            ) { Text(stringResource(R.string.required_custom)) }
        }
        if (custom) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StepButton("−", stringResource(R.string.decrease), enabled = required > 1) { onChange(required - 1) }
                Text(
                    text = stringResource(R.string.choose_of, ArabicText.toArabicDigits(required), ArabicText.toArabicDigits(size)),
                    style = MaterialTheme.typography.titleMedium,
                    color = accent,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
                StepButton("+", stringResource(R.string.increase), enabled = required < size - 1) { onChange(required + 1) }
            }
        }
        Text(
            text = when {
                size < 2 -> stringResource(R.string.required_needs_two)
                custom -> stringResource(R.string.required_custom_hint, ArabicText.toArabicDigits(required))
                else -> stringResource(R.string.choose_all, ArabicText.toArabicDigits(size))
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StepButton(symbol: String, description: String, enabled: Boolean, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.size(48.dp).semantics { contentDescription = description },
        contentPadding = PaddingValues(0.dp),
    ) {
        Text(symbol, fontSize = 22.sp)
    }
}

/** A text of the group being edited: its place, title, repeat count and a menu to move or remove it. */
@Composable
private fun GroupItemRow(
    number: Int,
    entry: LibraryEntry?,
    type: ItemType,
    count: Int,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onCountChange: (Int) -> Unit,
    onMove: (Int) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val colors = type.colors()
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = colors.container,
        border = BorderStroke(1.dp, colors.border),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(start = 12.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = ArabicText.toArabicDigits(number),
                style = MaterialTheme.typography.titleSmall,
                color = type.surfaceAccent(),
                modifier = Modifier.width(28.dp),
            )
            Column(Modifier.weight(1f)) {
                Text(
                    text = entry?.title ?: stringResource(R.string.untitled),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (entry != null) {
                    Text(
                        text = entry.previewText,
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            CountButton(count, type.surfaceAccent(), onCountChange)
            GroupItemMenu(canMoveUp, canMoveDown, onMove, onRemove)
        }
    }
}

private val CountPresets = listOf(1, 3, 7, 10, 33, 34, 100)

/** The repeat count of a text of the group; opens a menu to step it or pick a usual count. */
@Composable
private fun CountButton(count: Int, accent: Color, onCountChange: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { open = true }) {
            Text(stringResource(R.string.times_count, ArabicText.toArabicDigits(count)), color = accent)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            val decrease = stringResource(R.string.decrease)
            val increase = stringResource(R.string.increase)
            Row(Modifier.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    onClick = { onCountChange((count - 1).coerceAtLeast(1)) },
                    enabled = count > 1,
                    modifier = Modifier.semantics { contentDescription = decrease },
                ) { Text("−", fontSize = 20.sp) }
                Text(
                    text = ArabicText.toArabicDigits(count),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(min = 40.dp),
                )
                TextButton(
                    onClick = { onCountChange((count + 1).coerceAtMost(ProgramJson.MAX_COUNT)) },
                    modifier = Modifier.semantics { contentDescription = increase },
                ) { Text("+", fontSize = 20.sp) }
            }
            HorizontalDivider()
            CountPresets.forEach { preset ->
                DropdownMenuItem(
                    text = { Text("×" + ArabicText.toArabicDigits(preset)) },
                    trailingIcon = if (preset == count) {
                        { Icon(Icons.Filled.Check, null, tint = accent) }
                    } else null,
                    onClick = { onCountChange(preset); open = false },
                )
            }
        }
    }
}

@Composable
private fun GroupItemMenu(canMoveUp: Boolean, canMoveDown: Boolean, onMove: (Int) -> Unit, onRemove: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Filled.MoreVert, stringResource(R.string.action_more))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            if (canMoveUp) DropdownMenuItem(
                text = { Text(stringResource(R.string.move_up)) },
                leadingIcon = { Icon(Icons.Filled.KeyboardArrowUp, null) },
                onClick = { open = false; onMove(-1) },
            )
            if (canMoveDown) DropdownMenuItem(
                text = { Text(stringResource(R.string.move_down)) },
                leadingIcon = { Icon(Icons.Filled.KeyboardArrowDown, null) },
                onClick = { open = false; onMove(1) },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.remove_from_group), color = MaterialTheme.colorScheme.error) },
                leadingIcon = { Icon(Icons.Filled.Close, null, tint = MaterialTheme.colorScheme.error) },
                onClick = { open = false; onRemove() },
            )
        }
    }
}

/** A text of the collection that is not in the group yet: tapping it adds it at the end. */
@Composable
private fun AvailableEntryRow(entry: LibraryEntry, accent: Color, onAdd: () -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        onClick = onAdd,
        shape = MaterialTheme.shapes.medium,
        color = scheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, scheme.outlineVariant),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(entry.title, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    text = entry.previewText,
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_to_group), tint = accent)
        }
    }
}

private fun <T> List<T>.moved(index: Int, delta: Int): List<T> {
    val target = index + delta
    if (index !in indices || target !in indices) return this
    return toMutableList().also { it.add(target, it.removeAt(index)) }
}
