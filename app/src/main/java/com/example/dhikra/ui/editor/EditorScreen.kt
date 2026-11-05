package com.example.dhikra.ui.editor

import androidx.compose.foundation.BorderStroke
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
import com.example.dhikra.R
import com.example.dhikra.data.ArabicText
import com.example.dhikra.data.Library
import com.example.dhikra.data.ItemType
import com.example.dhikra.data.LibraryEntry
import com.example.dhikra.data.LibraryGroup
import com.example.dhikra.data.Program
import com.example.dhikra.data.ProgramItem
import com.example.dhikra.data.ProgramSection
import com.example.dhikra.ui.formatTimeArabic
import com.example.dhikra.ui.home.colors
import com.example.dhikra.ui.components.DhikraHeader
import com.example.dhikra.ui.components.headerAppBarColors
import com.example.dhikra.ui.components.icon
import com.example.dhikra.ui.components.sectionIcon

private sealed interface EditorDialog {
    data class Section(val section: ProgramSection?) : EditorDialog
    data class Item(val sectionId: String, val item: ProgramItem?) : EditorDialog
    data class DeleteSection(val section: ProgramSection) : EditorDialog
    data class DeleteItem(val sectionId: String, val item: ProgramItem) : EditorDialog
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    program: Program,
    library: Library,
    onBack: () -> Unit,
    onEdit: ((Program) -> Program) -> Unit,
    onSaveEntry: (LibraryEntry) -> Unit,
    onSaveGroup: (LibraryGroup) -> Unit,
) {
    var dialog by remember { mutableStateOf<EditorDialog?>(null) }

    Scaffold(
        topBar = {
            DhikraHeader(bottomPadding = 4.dp) {
                TopAppBar(
                    title = { Text(stringResource(R.string.editor_title), style = MaterialTheme.typography.titleLarge) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                        }
                    },
                    expandedHeight = 56.dp,
                    windowInsets = WindowInsets(0, 0, 0, 0),
                    colors = headerAppBarColors(),
                )
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { dialog = EditorDialog.Section(null) },
                icon = { Icon(Icons.Filled.Add, null) },
                text = { Text(stringResource(R.string.add_section)) },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        val layoutDirection = LocalLayoutDirection.current
        LazyColumn(
            modifier = Modifier.fillMaxSize().consumeWindowInsets(padding),
            // Edge-to-edge: the list scrolls behind the navigation bar, its last card stays clear of it.
            contentPadding = PaddingValues(
                start = padding.calculateStartPadding(layoutDirection) + 16.dp,
                end = padding.calculateEndPadding(layoutDirection) + 16.dp,
                top = padding.calculateTopPadding() + 16.dp,
                bottom = padding.calculateBottomPadding() + 96.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "name") {
                OutlinedTextField(
                    value = program.name,
                    onValueChange = { name -> onEdit { it.copy(name = name) } },
                    label = { Text(stringResource(R.string.program_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            items(program.sections, key = { it.id }) { section ->
                SectionEditorCard(
                    section = section,
                    library = library,
                    isFirst = section.id == program.sections.first().id,
                    isLast = section.id == program.sections.last().id,
                    onEditSection = { dialog = EditorDialog.Section(section) },
                    onMoveSection = { delta -> onEdit { it.moveSection(section.id, delta) } },
                    onDeleteSection = { dialog = EditorDialog.DeleteSection(section) },
                    onAddItem = { dialog = EditorDialog.Item(section.id, null) },
                    onEditItem = { item -> dialog = EditorDialog.Item(section.id, item) },
                    onMoveItem = { item, delta -> onEdit { it.moveItem(section.id, item.id, delta) } },
                    onDuplicateItem = { item -> onEdit { it.duplicateItem(section.id, item.id) } },
                    onDeleteItem = { item -> dialog = EditorDialog.DeleteItem(section.id, item) },
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }

    when (val d = dialog) {
        null -> Unit
        is EditorDialog.Section -> SectionEditorDialog(
            initial = d.section,
            onDismiss = { dialog = null },
            onSave = { section ->
                onEdit { it.upsertSection(section) }
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

@Composable
private fun SectionEditorCard(
    section: ProgramSection,
    library: Library,
    isFirst: Boolean,
    isLast: Boolean,
    onEditSection: () -> Unit,
    onMoveSection: (Int) -> Unit,
    onDeleteSection: () -> Unit,
    onAddItem: () -> Unit,
    onEditItem: (ProgramItem) -> Unit,
    onMoveItem: (ProgramItem, Int) -> Unit,
    onDuplicateItem: (ProgramItem) -> Unit,
    onDeleteItem: (ProgramItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onEditSection)
                    .padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = sectionIcon(section.icon),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(section.title, style = MaterialTheme.typography.titleMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (section.reminderMinutes != null) {
                            Icon(
                                Icons.Filled.Notifications, null,
                                Modifier.size(13.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(Modifier.width(3.dp))
                        }
                        Text(
                            text = section.reminderMinutes?.let(::formatTimeArabic)
                                ?: stringResource(R.string.no_reminder),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                RowMenu(
                    editLabel = R.string.edit_section,
                    canMoveUp = !isFirst,
                    canMoveDown = !isLast,
                    onEdit = onEditSection,
                    onMove = onMoveSection,
                    onDelete = onDeleteSection,
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            section.items.forEachIndexed { index, item ->
                ItemRow(
                    // Library steps show their entry's title.
                    item = item.resolvedWith(library) ?: item,
                    canMoveUp = index > 0,
                    canMoveDown = index < section.items.lastIndex,
                    onClick = { onEditItem(item) },
                    onMove = { onMoveItem(item, it) },
                    onDuplicate = { onDuplicateItem(item) },
                    onDelete = { onDeleteItem(item) },
                )
            }
            TextButton(
                onClick = onAddItem,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            ) {
                Icon(Icons.Filled.Add, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.add_item))
            }
        }
    }
}

@Composable
private fun ItemRow(
    item: ProgramItem,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onClick: () -> Unit,
    onMove: (Int) -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = item.type.colors()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 12.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(shape = CircleShape, color = colors.container, modifier = Modifier.size(30.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = item.type.icon,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(16.dp),
                )
            }
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
            item.isGroup -> stringResource(R.string.group_count, ArabicText.toArabicDigits(item.members.size))
            item.target > 1 -> stringResource(R.string.times_count, ArabicText.toArabicDigits(item.target))
            else -> null
        }
        if (badge != null) {
            Text(
                text = badge,
                style = MaterialTheme.typography.labelMedium,
                color = colors.accent.takeIf { item.type != ItemType.QURAN } ?: MaterialTheme.colorScheme.primary,
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
        )
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
