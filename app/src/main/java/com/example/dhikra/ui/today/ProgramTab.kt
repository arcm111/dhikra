package com.example.dhikra.ui.today

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.dhikra.R
import com.example.dhikra.data.ItemType
import com.example.dhikra.data.Prayer
import com.example.dhikra.data.Program
import com.example.dhikra.data.ProgramSection
import com.example.dhikra.ui.UiState
import com.example.dhikra.ui.components.ProgramIcon
import com.example.dhikra.ui.components.sectionIcon
import com.example.dhikra.ui.editor.SectionEditPopup
import com.example.dhikra.ui.editor.newSection
import com.example.dhikra.ui.editor.sectionTone
import androidx.compose.runtime.LaunchedEffect
import com.example.dhikra.ui.formatClock
import com.example.dhikra.ui.home.HomeActions
import com.example.dhikra.ui.home.colors
import com.example.dhikra.ui.prayer.icon
import com.example.dhikra.ui.prayer.label
import com.example.dhikra.ui.theme.LocalDhikraPalette
import com.example.dhikra.ui.uiDigits
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.graphics.lerp

/** What the editor popup is open on: the section [sectionId], or a new one added to [prayer]'s time (or none). */
private data class EditTarget(val sectionId: String?, val prayer: Prayer?)

/** One group of the program tab: the sections of a prayer time, or those without a time. */
private data class SectionGroup(
    val key: String,
    val prayer: Prayer?,
    val start: Int?,
    val current: Boolean,
    val sections: List<ProgramSection>,
)

/** The day's prayer times as the program tab's groups, the sections without a time last (when any). */
private fun DayTimeline.sectionGroups(): List<SectionGroup> = buildList {
    periods.forEach { period ->
        add(
            SectionGroup(
                key = period.prayer?.name ?: "day",
                prayer = period.prayer,
                start = period.prayer?.let { period.start },
                current = period.current && period.prayer != null,
                sections = period.sections,
            )
        )
    }
    if (untimed.isNotEmpty()) add(SectionGroup("untimed", null, null, current = false, sections = untimed))
}

/**
 * The program tab, where the program is edited: a heading saying so, the program itself (its name,
 * counts and actions — rename, duplicate, share, export, delete), then the sections of one prayer time
 * (picked in a bar at the bottom), each time a grid of small square tiles in the step
 * cards' style. A tile opens its section in the editor popup — details, steps and their texts, saved
 * together; its menu also duplicates or deletes it; a dashed tile adds one to that time.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ProgramTab(
    state: UiState,
    actions: HomeActions,
    timeline: DayTimeline,
    onEditProgram: ((Program) -> Program) -> Unit,
    listState: LazyListState,
    contentPadding: PaddingValues,
    /** The room the system bars take at the bottom (the bar of prayer times sits above it). */
    bottomInset: Dp,
) {
    val scope = rememberCoroutineScope()
    val resources = LocalResources.current
    val layoutDirection = LocalLayoutDirection.current
    val groups = remember(timeline) { timeline.sectionGroups() }
    val timed = groups.any { it.prayer != null }
    // The prayer time whose sections show (picked in the bar at the bottom): the one going on at first.
    var picked by rememberSaveable { mutableStateOf<String?>(null) }
    val selected = groups.firstOrNull { it.key == picked } ?: groups.firstOrNull { it.current } ?: groups.firstOrNull()
    val shown = if (timed) listOfNotNull(selected) else groups
    // The section open in the editor popup: one of the program's, or a new one.
    var editing by remember { mutableStateOf<EditTarget?>(null) }
    var deleting by remember { mutableStateOf<ProgramSection?>(null) }
    var renaming by rememberSaveable { mutableStateOf(false) }
    editing?.let { target ->
        val stored = target.sectionId?.let(state.program::section)
        val initial = stored ?: remember(target) { newSection(target.prayer, state.prayerTimes) }
        if (target.sectionId != null && stored == null) {
            // Deleted meanwhile.
            LaunchedEffect(target) { editing = null }
        } else {
            SectionEditPopup(
                initial = initial,
                isNew = stored == null,
                library = state.library,
                programs = state.programs.ifEmpty { listOf(state.program) },
                timetable = state.prayerTimes,
                onDismiss = { editing = null },
                onSave = { edit ->
                    actions.onSaveSectionEdit(edit)
                    editing = null
                },
                onDelete = stored?.let { section ->
                    {
                        onEditProgram { it.removeSection(section.id) }
                        editing = null
                    }
                },
            )
        }
    }
    deleting?.let { section ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            icon = { Icon(Icons.Filled.Delete, null) },
            text = { Text(stringResource(R.string.delete_section_message, section.title)) },
            confirmButton = {
                TextButton(onClick = {
                    onEditProgram { it.removeSection(section.id) }
                    deleting = null
                }) { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
    if (renaming) {
        RenameProgramDialog(
            name = state.program.name,
            onDismiss = { renaming = false },
            onSave = { name ->
                onEditProgram { it.copy(name = name) }
                renaming = false
            },
        )
    }
    val start = contentPadding.calculateStartPadding(layoutDirection)
    val end = contentPadding.calculateEndPadding(layoutDirection)
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(
                start = start,
                end = end,
                top = contentPadding.calculateTopPadding(),
                // Room under the last row for the bar of prayer times.
                bottom = bottomInset + if (timed) 84.dp else 24.dp,
            ),
            modifier = Modifier.fillMaxSize(),
        ) {
            item(key = "programHeading") {
                TabHeading(
                    title = stringResource(R.string.program_edit_title),
                    description = stringResource(R.string.program_edit_hint),
                    modifier = Modifier.padding(start = 8.dp, top = 4.dp),
                )
            }
            item(key = "programCard") {
                ProgramCard(
                    program = state.program,
                    onRename = { renaming = true },
                    actions = actions,
                    modifier = Modifier.padding(start = 8.dp, top = 14.dp, bottom = 8.dp),
                )
            }
            shown.forEach { group ->
                stickyHeader(key = "group/${group.key}") {
                    GroupHeader(group)
                }
                val cells: List<ProgramSection?> = group.sections + listOf(null)
                cells.chunked(TILES_PER_ROW).forEachIndexed { row, pair ->
                    item(key = "tiles/${group.key}/$row") {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(start = 8.dp, bottom = 10.dp),
                        ) {
                            pair.forEach { section ->
                                val cell = Modifier.weight(1f).aspectRatio(TILE_ASPECT)
                                if (section == null) {
                                    AddSectionTile(onClick = { editing = EditTarget(null, group.prayer) }, modifier = cell)
                                } else {
                                    SectionTile(
                                        section = section,
                                        onOpen = { editing = EditTarget(section.id, null) },
                                        onDuplicate = {
                                            onEditProgram { program ->
                                                program.duplicateSection(section.id) { title -> resources.getString(R.string.section_copy, title) }
                                            }
                                        },
                                        onDelete = { deleting = section },
                                        modifier = cell,
                                    )
                                }
                            }
                            repeat(TILES_PER_ROW - pair.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }
        if (timed) {
            GroupBar(
                groups = groups,
                selected = selected?.key,
                onSelect = { group ->
                    picked = group.key
                    // Back to the new time's header when the list was scrolled past it.
                    if (listState.firstVisibleItemIndex > 2) scope.launch { listState.scrollToItem(2) }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = start + 8.dp, end = end, bottom = bottomInset + 12.dp),
            )
        }
    }
}

/** Sections side by side in a prayer time's grid. */
private const val TILES_PER_ROW = 3

/** A tile's width to its height: square. */
private const val TILE_ASPECT = 1f

/**
 * The program being edited: its name with a small pencil (a tap renames it), its counts, and its
 * actions in a ⋮ menu — duplicate, share, export, delete.
 */
@Composable
private fun ProgramCard(program: Program, onRename: () -> Unit, actions: HomeActions, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val green = scheme.primary
    var menu by remember { mutableStateOf(false) }
    Surface(
        shape = MaterialTheme.shapes.large,
        color = green.copy(alpha = 0.06f).compositeOver(scheme.surfaceContainerLowest),
        border = BorderStroke(1.dp, green.copy(alpha = 0.22f)),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(start = 14.dp, end = 4.dp, top = 16.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(green.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) { Icon(ProgramIcon, null, tint = green, modifier = Modifier.size(22.dp)) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(
                    Modifier
                        .clip(MaterialTheme.shapes.small)
                        .clickable(onClickLabel = stringResource(R.string.program_rename), onClick = onRename),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        program.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.Filled.Edit, null, tint = green.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                }
                Text(
                    text = stringResource(R.string.program_summary, uiDigits(program.sections.size), uiDigits(program.itemCount)),
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.onSurfaceVariant,
                )
            }
            Box {
                IconButton(onClick = { menu = true }) {
                    Icon(Icons.Filled.MoreVert, stringResource(R.string.action_more), tint = scheme.onSurfaceVariant)
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    fun pick(action: () -> Unit): () -> Unit = { menu = false; action() }
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.program_duplicate)) },
                        leadingIcon = { Icon(Icons.Filled.Add, null) },
                        onClick = pick(actions.onDuplicateProgram),
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.share_short)) },
                        leadingIcon = { Icon(Icons.Filled.Share, null) },
                        onClick = pick(actions.onShareProgram),
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.export_short)) },
                        leadingIcon = { Icon(painterResource(R.drawable.ic_upload), null) },
                        onClick = pick(actions.onExportProgram),
                    )
                    actions.onDeleteProgram?.let { delete ->
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.delete_program), color = scheme.error) },
                            leadingIcon = { Icon(Icons.Filled.Delete, null, tint = scheme.error) },
                            onClick = pick(delete),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RenameProgramDialog(name: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var text by rememberSaveable { mutableStateOf(name) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.program_name)) },
        text = { OutlinedTextField(value = text, onValueChange = { text = it }, singleLine = true, modifier = Modifier.fillMaxWidth()) },
        confirmButton = {
            TextButton(onClick = { onSave(text.trim()) }, enabled = text.isNotBlank()) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

/**
 * The prayer times, as a bar at the bottom of the tab: a button each — its initial and how many
 * sections it has — showing that time's sections; the one showing is filled.
 */
@Composable
private fun GroupBar(groups: List<SectionGroup>, selected: String?, onSelect: (SectionGroup) -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        shape = MaterialTheme.shapes.large,
        color = scheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.7f)),
        shadowElevation = 6.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(Modifier.height(52.dp).padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
            groups.forEachIndexed { index, group ->
                val on = group.key == selected
                val nextOn = groups.getOrNull(index + 1)?.key == selected
                val label = group.prayer?.let { prayerInitial(it) } ?: stringResource(R.string.untimed_group)
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clip(MaterialTheme.shapes.medium)
                        .background(if (on) scheme.primary else Color.Transparent)
                        .selectable(selected = on, role = Role.Tab) { onSelect(group) },
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (on) scheme.onPrimary else scheme.primary,
                        maxLines = 1,
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        uiDigits(group.sections.size),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (on) scheme.onPrimary.copy(alpha = 0.8f) else scheme.onSurfaceVariant,
                    )
                }
                // Dividers between the buttons, none beside the filled one.
                if (index < groups.lastIndex && !on && !nextOn) {
                    Box(Modifier.width(1.dp).height(24.dp).background(scheme.outlineVariant.copy(alpha = 0.6f)))
                } else if (index < groups.lastIndex) {
                    Spacer(Modifier.width(1.dp))
                }
            }
        }
    }
}

/** A prayer time's header over its tiles (it stays while its tiles scroll): its name and time. */
@Composable
private fun GroupHeader(group: SectionGroup) {
    val scheme = MaterialTheme.colorScheme
    val slate = LocalDhikraPalette.current.slate.accent
    val ink = if (group.current) scheme.primary else slate
    Surface(color = scheme.background, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(start = 8.dp, top = 14.dp, bottom = 10.dp).semantics { heading() },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            group.prayer?.let { Icon(it.icon, null, tint = ink, modifier = Modifier.size(20.dp)) }
            if (group.prayer != null) Spacer(Modifier.width(8.dp))
            Text(
                text = group.prayer?.let { stringResource(it.label) }
                    ?: stringResource(if (group.key == "untimed") R.string.untimed_group else R.string.program_sections),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = ink,
            )
            group.start?.let {
                Spacer(Modifier.width(8.dp))
                Text(formatClock(it), style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
            }
        }
    }
}

/**
 * A section as a small square tile in the step cards' style — their frame (white, outlined in green,
 * raised) — its upper half tinted (a little muted) by how its time is set: green when it starts at a prayer's time,
 * purple on another city's clock (Madinah), blue otherwise; its start time and icon there, then its title. A tap opens
 * its steps, its menu the rest.
 */
@Composable
private fun SectionTile(
    section: ProgramSection,
    onOpen: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val tone = sectionTone(section)
    // The header: the colour a little muted and a shade deeper than the steps' light tiles, to tell them apart.
    val header = lerp(tone, HeaderShade, 0.35f).copy(alpha = 0.32f).compositeOver(scheme.surfaceContainerLowest)
    var menu by remember { mutableStateOf(false) }
    Surface(
        onClick = onOpen,
        shape = MaterialTheme.shapes.medium,
        color = scheme.surfaceContainerLowest,
        border = BorderStroke(1.5.dp, scheme.primary.copy(alpha = 0.55f)),
        shadowElevation = 3.dp,
        modifier = modifier,
    ) {
        Column {
            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(0.5f)
                    .background(header),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .align(Alignment.Center)
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(scheme.surfaceContainerLowest.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center,
                ) { Icon(sectionIcon(section.icon), null, tint = tone, modifier = Modifier.size(16.dp)) }
                // Its start time (today's, for a time tied to a prayer), in the colour of how it is set.
                section.reminderMinutes?.let { minutes ->
                    Text(
                        text = formatClock(minutes),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = tone,
                        maxLines = 1,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                    )
                }
                Box(Modifier.align(Alignment.TopStart)) {
                    IconButton(onClick = { menu = true }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.MoreVert, stringResource(R.string.action_more), tint = scheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.edit_section)) },
                            leadingIcon = { Icon(Icons.Filled.Edit, null) },
                            onClick = { menu = false; onOpen() },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.duplicate)) },
                            leadingIcon = { Icon(Icons.Filled.Add, null) },
                            onClick = { menu = false; onDuplicate() },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.delete), color = scheme.error) },
                            leadingIcon = { Icon(Icons.Filled.Delete, null, tint = scheme.error) },
                            onClick = { menu = false; onDelete() },
                        )
                    }
                }
            }
            Column(
                Modifier.fillMaxWidth().weight(0.5f).padding(horizontal = 7.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = section.title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** A grey the tiles' header colours are muted towards. */
private val HeaderShade = Color(0xFF6B7470)

/** The last tile of a prayer time: dashed, to add a section to it. */
@Composable
private fun AddSectionTile(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val green = MaterialTheme.colorScheme.primary
    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick)
            .drawBehind {
                val stroke = 1.5.dp.toPx()
                drawRoundRect(
                    color = green.copy(alpha = 0.5f),
                    topLeft = androidx.compose.ui.geometry.Offset(stroke / 2, stroke / 2),
                    size = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke),
                    cornerRadius = CornerRadius(12.dp.toPx()),
                    style = Stroke(width = stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10.dp.toPx(), 6.dp.toPx()))),
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Filled.Add, null, tint = green, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(2.dp))
            Text(stringResource(R.string.add_section), style = MaterialTheme.typography.labelMedium, color = green)
        }
    }
}
