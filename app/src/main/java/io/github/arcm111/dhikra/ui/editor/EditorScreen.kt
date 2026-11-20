package io.github.arcm111.dhikra.ui.editor

import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.unit.IntOffset
import io.github.arcm111.dhikra.ui.components.reorderable
import io.github.arcm111.dhikra.ui.components.rememberReorderState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.arcm111.dhikra.R
import io.github.arcm111.dhikra.data.ArabicText
import io.github.arcm111.dhikra.data.Library
import io.github.arcm111.dhikra.data.ItemType
import io.github.arcm111.dhikra.data.LibraryEntry
import io.github.arcm111.dhikra.data.LibraryGroup
import io.github.arcm111.dhikra.data.Program
import io.github.arcm111.dhikra.data.ProgramItem
import io.github.arcm111.dhikra.data.ProgramSection
import io.github.arcm111.dhikra.data.PrayerTimetable
import io.github.arcm111.dhikra.ui.prayer.anchorLabel
import io.github.arcm111.dhikra.ui.formatClock
import io.github.arcm111.dhikra.ui.home.colors
import io.github.arcm111.dhikra.ui.components.DhikraHeader
import io.github.arcm111.dhikra.ui.components.headerAppBarColors
import io.github.arcm111.dhikra.ui.components.icon
import io.github.arcm111.dhikra.ui.components.sectionIcon
import io.github.arcm111.dhikra.ui.uiDigits
import io.github.arcm111.dhikra.ui.components.ProgramIcon
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import io.github.arcm111.dhikra.ui.components.clearButton
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.material3.OutlinedButton
import io.github.arcm111.dhikra.ui.components.reorderHandle
import androidx.compose.ui.res.painterResource
import io.github.arcm111.dhikra.ui.prayer.anchorShortLabel

private sealed interface EditorDialog {
    data class Section(val section: ProgramSection?) : EditorDialog
    data class Item(val sectionId: String, val item: ProgramItem?) : EditorDialog
    data class DeleteSection(val section: ProgramSection) : EditorDialog
    data class DeleteItem(val sectionId: String, val item: ProgramItem) : EditorDialog
    data class MoveItem(val sectionId: String, val item: ProgramItem) : EditorDialog
}

/** The editor's rows: the program's sections, one open at a time with its steps listed under it. */
private sealed interface EditorRow {
    val key: String

    data object Name : EditorRow {
        override val key = "name"
    }

    data object Hint : EditorRow {
        override val key = "hint"
    }

    data class Header(val section: ProgramSection, val index: Int, val count: Int, val open: Boolean) : EditorRow {
        override val key = sectionKey(section.id)
    }

    data class Step(val section: ProgramSection, val item: ProgramItem, val index: Int) : EditorRow {
        override val key = stepKey(section.id, item.id)
    }

    data class AddStep(val section: ProgramSection) : EditorRow {
        override val key = "add/" + section.id
    }
}

private const val SECTION_KEY = "s/"
private const val STEP_KEY = "i/"
private fun sectionKey(sectionId: String) = SECTION_KEY + sectionId
private fun stepKey(sectionId: String, itemId: String) = "$STEP_KEY$sectionId/$itemId"

/** The section and step of a step's key. */
private fun stepIds(key: String): Pair<String, String> =
    key.removePrefix(STEP_KEY).split('/', limit = 2).let { it[0] to it.getOrElse(1) { "" } }

/**
 * The program editor: the program's name, then its sections — one open at a time, with its steps
 * under it. A section, or a step within its section, is moved by pressing and holding it, then
 * dragging it to its place. With [sectionId] it edits that one section only (from the section view
 * or the day view's list), already open.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    program: Program,
    library: Library,
    onBack: () -> Unit,
    onEdit: ((Program) -> Program) -> Unit,
    onSaveEntry: (LibraryEntry) -> Unit,
    onSaveGroup: (LibraryGroup) -> Unit,
    /** Deletes the program (after confirmation, by the caller); null when it is the only one. */
    onDelete: (() -> Unit)? = null,
    sectionId: String? = null,
    /** Today's prayer times, for the sections whose time follows a prayer. */
    prayerTimes: PrayerTimetable? = null,
) {
    var dialog by remember { mutableStateOf<EditorDialog?>(null) }
    val single = sectionId != null
    val focused = sectionId?.let(program::section)
    if (single && focused == null) {
        // The section edited on its own was deleted: back to where it was opened from.
        LaunchedEffect(Unit) { onBack() }
        return
    }
    var expandedId by rememberSaveable { mutableStateOf(sectionId) }
    val sections = focused?.let(::listOf) ?: program.sections
    val rows = buildList {
        if (!single) add(EditorRow.Name)
        add(EditorRow.Hint)
        sections.forEachIndexed { index, section ->
            val open = single || section.id == expandedId
            // A section edited on its own has its header in the top bar.
            if (!single) add(EditorRow.Header(section, index, sections.size, open))
            if (open) {
                section.items.forEachIndexed { at, item -> add(EditorRow.Step(section, item, at)) }
                add(EditorRow.AddStep(section))
            }
        }
    }
    val resources = LocalResources.current
    val copyTitle = { title: String -> resources.getString(R.string.section_copy, title) }

    val listState = rememberLazyListState()
    val haptics = LocalHapticFeedback.current
    val reorder = rememberReorderState(
        listState = listState,
        // Sections move among sections; a step moves within its own section.
        canMove = { from, to ->
            val f = from as String
            val t = to as String
            when {
                f.startsWith(SECTION_KEY) -> t.startsWith(SECTION_KEY)
                f.startsWith(STEP_KEY) -> t.startsWith(STEP_KEY) && stepIds(t).first == stepIds(f).first
                else -> false
            }
        },
        onMove = { from, to ->
            val f = from as String
            val t = to as String
            if (f.startsWith(SECTION_KEY)) {
                val moved = f.removePrefix(SECTION_KEY)
                val target = t.removePrefix(SECTION_KEY)
                onEdit { p ->
                    val a = p.sections.indexOfFirst { it.id == moved }
                    val b = p.sections.indexOfFirst { it.id == target }
                    if (a < 0 || b < 0) p else p.moveSection(moved, b - a)
                }
            } else {
                val (section, moved) = stepIds(f)
                val target = stepIds(t).second
                onEdit { p ->
                    val items = p.section(section)?.items.orEmpty()
                    val a = items.indexOfFirst { it.id == moved }
                    val b = items.indexOfFirst { it.id == target }
                    if (a < 0 || b < 0) p else p.moveItem(section, moved, b - a)
                }
            }
        },
    )

    Scaffold(
        topBar = {
            DhikraHeader(bottomPadding = 4.dp) {
                TopAppBar(
                    title = {
                        if (focused != null) {
                            SectionTitle(focused)
                        } else {
                            Text(stringResource(R.string.editor_title), style = MaterialTheme.typography.titleLarge)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                        }
                    },
                    actions = {
                        if (focused != null) {
                            // The section's own menu: its details, or deleting it.
                            RowMenu(
                                editLabel = R.string.edit_details,
                                canMoveUp = false,
                                canMoveDown = false,
                                onEdit = { dialog = EditorDialog.Section(focused) },
                                onMove = {},
                                onDelete = { dialog = EditorDialog.DeleteSection(focused) },
                            )
                        }
                        if (onDelete != null && !single) {
                            IconButton(onClick = onDelete) {
                                Icon(Icons.Outlined.Delete, stringResource(R.string.delete_program))
                            }
                        }
                    },
                    expandedHeight = if (single) 64.dp else 56.dp,
                    windowInsets = WindowInsets(0, 0, 0, 0),
                    colors = headerAppBarColors(),
                )
            }
        },
        floatingActionButton = {
            if (!single) {
                ExtendedFloatingActionButton(
                    onClick = { dialog = EditorDialog.Section(null) },
                    icon = { Icon(Icons.Filled.Add, null) },
                    text = { Text(stringResource(R.string.add_section)) },
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        val layoutDirection = LocalLayoutDirection.current
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().consumeWindowInsets(padding),
            // Edge-to-edge: the list scrolls behind the navigation bar, its last card stays clear of it.
            contentPadding = PaddingValues(
                start = padding.calculateStartPadding(layoutDirection) + 16.dp,
                end = padding.calculateEndPadding(layoutDirection) + 16.dp,
                top = padding.calculateTopPadding() + 16.dp,
                bottom = padding.calculateBottomPadding() + 96.dp,
            ),
        ) {
            items(rows, key = { it.key }) { row ->
                // The row being dragged follows the finger, not the reordering animation.
                val itemModifier = Modifier.animateItem(
                    placementSpec = if (reorder.isDragging(row.key)) null else spring(stiffness = Spring.StiffnessMediumLow, visibilityThreshold = IntOffset.VisibilityThreshold),
                )
                when (row) {
                    EditorRow.Name -> OutlinedTextField(
                        value = program.name,
                        onValueChange = { name -> onEdit { it.copy(name = name) } },
                        trailingIcon = clearButton(program.name) { onEdit { it.copy(name = "") } },
                        label = { Text(stringResource(R.string.program_name)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    )
                    EditorRow.Hint -> Text(
                        text = stringResource(if (single) R.string.section_edit_hint else R.string.reorder_hint),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
                    )
                    is EditorRow.Header -> {
                        val section = row.section
                        SectionHeaderRow(
                            section = section,
                            open = row.open,
                            single = single,
                            canMoveUp = !single && row.index > 0,
                            canMoveDown = !single && row.index < row.count - 1,
                            onToggle = { if (!single) expandedId = if (row.open) null else section.id },
                            onEditSection = { dialog = EditorDialog.Section(section) },
                            onMoveSection = { delta -> onEdit { it.moveSection(section.id, delta) } },
                            onDuplicateSection = if (single) null else ({ onEdit { it.duplicateSection(section.id, copyTitle) } }),
                            onDeleteSection = { dialog = EditorDialog.DeleteSection(section) },
                            modifier = itemModifier
                                .padding(top = 10.dp)
                                .then(
                                    if (single) Modifier
                                    else Modifier.reorderable(reorder, row.key) {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        // An open section is folded to be moved, its steps stay with it.
                                        if (expandedId == section.id) expandedId = null
                                    }
                                ),
                        )
                    }
                    is EditorRow.Step -> {
                        val item = row.item
                        Surface(
                            // Under its section's header card in the full editor; on the page on its own.
                            color = if (single) Color.Transparent else MaterialTheme.colorScheme.surfaceContainerLowest,
                            shape = RectangleShape,
                            modifier = itemModifier.fillMaxWidth().reorderable(reorder, row.key) {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                        ) {
                            ItemRow(
                                // Library steps show their entry's title.
                                item = item.resolvedWith(library) ?: item,
                                handle = Modifier.reorderHandle(reorder, row.key) {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                canMoveUp = row.index > 0,
                                canMoveDown = row.index < row.section.items.lastIndex,
                                onClick = { dialog = EditorDialog.Item(row.section.id, item) },
                                onMove = { delta -> onEdit { it.moveItem(row.section.id, item.id, delta) } },
                                onDuplicate = { onEdit { it.duplicateItem(row.section.id, item.id) } },
                                onDelete = { dialog = EditorDialog.DeleteItem(row.section.id, item) },
                                onMoveToSection = if (program.sections.size > 1) {
                                    { dialog = EditorDialog.MoveItem(row.section.id, item.resolvedWith(library) ?: item) }
                                } else null,
                            )
                        }
                    }
                    is EditorRow.AddStep -> Surface(
                        color = if (single) Color.Transparent else MaterialTheme.colorScheme.surfaceContainerLowest,
                        shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
                        modifier = itemModifier.fillMaxWidth(),
                    ) {
                        // Adding a step: a clear button under the steps.
                        OutlinedButton(
                            onClick = { dialog = EditorDialog.Item(row.section.id, null) },
                            shape = CircleShape,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                        ) {
                            Icon(Icons.Filled.Add, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.add_step))
                        }
                    }
                }
            }
        }
    }

    when (val d = dialog) {
        null -> Unit
        is EditorDialog.Section -> SectionEditorDialog(
            initial = d.section,
            timetable = prayerTimes,
            onDismiss = { dialog = null },
            onSave = { section ->
                onEdit { it.upsertSection(section) }
                // A new section opens, ready for its steps.
                if (d.section == null) expandedId = section.id
                dialog = null
            },
        )
        is EditorDialog.Item -> ItemEditorDialog(
            initial = d.item,
            library = library,
            onSaveEntry = onSaveEntry,
            onSaveGroup = onSaveGroup,
            onDismiss = { dialog = null },
            onSave = { item ->
                onEdit { it.upsertItem(d.sectionId, item) }
                dialog = null
            },
        )
        is EditorDialog.DeleteSection -> ConfirmDeleteDialog(
            message = stringResource(R.string.delete_section_message, d.section.title),
            onDismiss = { dialog = null },
            onConfirm = {
                onEdit { it.removeSection(d.section.id) }
                dialog = null
            },
        )
        is EditorDialog.MoveItem -> AlertDialog(
            onDismissRequest = { dialog = null },
            title = {
                Text(
                    stringResource(R.string.move_to_section_title, d.item.title.ifBlank { d.item.text }),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            text = {
                // A long program scrolls within the dialog.
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    program.sections.filter { it.id != d.sectionId }.forEach { target ->
                        Surface(
                            onClick = {
                                onEdit { it.moveItemToSection(d.sectionId, d.item.id, target.id) }
                                dialog = null
                            },
                            shape = MaterialTheme.shapes.small,
                            color = Color.Transparent,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(Modifier.padding(vertical = 12.dp, horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(sectionIcon(target.icon), null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                                Spacer(Modifier.width(14.dp))
                                Text(target.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { dialog = null }) { Text(stringResource(R.string.cancel)) } },
        )

        is EditorDialog.DeleteItem -> ConfirmDeleteDialog(
            message = stringResource(R.string.delete_item_message),
            onDismiss = { dialog = null },
            onConfirm = {
                onEdit { it.removeItem(d.sectionId, d.item.id) }
                dialog = null
            },
        )
    }
}

/**
 * A section's header in the editor: its icon, title, time and number of steps, and its menu.
 * Tapping it opens it (closing the one open before) or folds it; open, it is the top of a card
 * whose steps follow.
 */
/**
 * A section edited on its own, as the top bar's title: its icon and title, and below them that it is
 * being edited, its time and its steps («تعديل القسم · ٢٢:٣٠ · ١٠ خطوات»).
 */
@Composable
private fun SectionTitle(section: ProgramSection) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) { Icon(sectionIcon(section.icon), null, tint = Color.White, modifier = Modifier.size(20.dp)) }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                section.title,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val time = section.anchor?.let { anchorShortLabel(it) } ?: section.reminderMinutes?.let(::formatClock)
            Text(
                text = listOfNotNull(
                    stringResource(R.string.edit_section),
                    time,
                    stringResource(R.string.section_steps_count, uiDigits(section.trackableItems.size)),
                ).joinToString("  ·  "),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.75f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SectionHeaderRow(
    section: ProgramSection,
    open: Boolean,
    single: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onToggle: () -> Unit,
    onEditSection: () -> Unit,
    onMoveSection: (Int) -> Unit,
    onDuplicateSection: (() -> Unit)?,
    onDeleteSection: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val shape = if (open) RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp) else MaterialTheme.shapes.large
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = if (open) scheme.primaryContainer.copy(alpha = 0.45f) else scheme.surfaceContainerLowest,
        border = if (open) null else BorderStroke(1.dp, scheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = !single, onClick = onToggle)
                .padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(scheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(sectionIcon(section.icon), null, tint = scheme.onPrimaryContainer, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(section.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (section.reminderMinutes != null) {
                        Icon(Icons.Filled.Notifications, null, Modifier.size(13.dp), tint = scheme.primary)
                        Spacer(Modifier.width(3.dp))
                    }
                    Text(
                        text = (section.anchor?.let { anchorLabel(it) }
                            ?: section.reminderMinutes?.let(::formatClock)
                            ?: stringResource(R.string.no_reminder)) +
                            "  ·  " + stringResource(R.string.section_steps_count, uiDigits(section.trackableItems.size)),
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant,
                    )
                }
            }
            if (!single) {
                Icon(
                    imageVector = if (open) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = scheme.onSurfaceVariant,
                )
            }
            RowMenu(
                editLabel = R.string.edit_section,
                canMoveUp = canMoveUp,
                canMoveDown = canMoveDown,
                onEdit = onEditSection,
                onMove = onMoveSection,
                onDelete = onDeleteSection,
                onDuplicate = onDuplicateSection,
            )
        }
    }
}

@Composable
private fun ItemRow(
    item: ProgramItem,
    /** The drag handle's gesture: the step moves as soon as the handle is dragged. */
    handle: Modifier,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onClick: () -> Unit,
    onMove: (Int) -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onMoveToSection: (() -> Unit)? = null,
) {
    val colors = item.type.colors()
    val scheme = MaterialTheme.colorScheme
    // A small card in the step cards' style: white, outlined in green, the tile of its kind first.
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = scheme.surfaceContainerLowest,
        border = BorderStroke(1.5.dp, scheme.primary.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 0.dp, end = 2.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // The standard drag handle on the leading edge: drag it to move the step.
        Box(
            modifier = Modifier.size(width = 32.dp, height = 40.dp).then(handle),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_drag_handle),
                contentDescription = stringResource(R.string.reorder_handle),
                tint = scheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp),
            )
        }
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(MaterialTheme.shapes.small)
                .background(colors.accent.copy(alpha = 0.22f).compositeOver(scheme.surfaceContainerLowest)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = item.type.icon,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = item.title.ifBlank { item.text }.ifBlank { stringResource(R.string.untitled) },
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        val badge = when {
            item.isChoice -> stringResource(R.string.group_choose, uiDigits(item.choose), uiDigits(item.members.size))
            item.isGroup -> stringResource(R.string.group_count, uiDigits(item.members.size))
            item.target > 1 -> stringResource(R.string.times_count, uiDigits(item.target))
            else -> null
        }
        if (badge != null) {
            Text(
                text = badge,
                style = MaterialTheme.typography.labelMedium,
                color = colors.accent,
                modifier = Modifier.padding(horizontal = 6.dp),
            )
        }
        RowMenu(
            editLabel = R.string.edit_item,
            canMoveUp = canMoveUp,
            canMoveDown = canMoveDown,
            onEdit = onClick,
            onMove = onMove,
            onDuplicate = onDuplicate,
            onDelete = onDelete,
            onMoveToSection = onMoveToSection,
        )
    }
    }
}

@Composable
private fun RowMenu(
    editLabel: Int,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onEdit: () -> Unit,
    onMove: (Int) -> Unit,
    onDelete: () -> Unit,
    onDuplicate: (() -> Unit)? = null,
    onMoveToSection: (() -> Unit)? = null,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Filled.MoreVert, stringResource(R.string.action_more))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(editLabel)) },
                leadingIcon = { Icon(Icons.Filled.Edit, null) },
                onClick = { open = false; onEdit() },
            )
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
            if (onMoveToSection != null) DropdownMenuItem(
                text = { Text(stringResource(R.string.move_to_section)) },
                leadingIcon = { Icon(ProgramIcon, null) },
                onClick = { open = false; onMoveToSection() },
            )
            if (onDuplicate != null) DropdownMenuItem(
                text = { Text(stringResource(R.string.duplicate)) },
                leadingIcon = { Icon(Icons.Filled.Add, null) },
                onClick = { open = false; onDuplicate() },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) },
                leadingIcon = { Icon(Icons.Filled.Delete, null, tint = MaterialTheme.colorScheme.error) },
                onClick = { open = false; onDelete() },
            )
        }
    }
}

@Composable
private fun ConfirmDeleteDialog(message: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.Delete, null) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
