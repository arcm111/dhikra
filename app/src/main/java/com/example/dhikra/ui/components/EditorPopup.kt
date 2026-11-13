package com.example.dhikra.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.dhikra.R

/**
 * The editors' popup: a card over the screen (not the whole screen) with a compact header — its
 * [title], [headerActions] and a close button — the [content] (which scrolls; see [PopupBody]) and a
 * footer with cancel and save. Closing it in any way (the close button, back, a tap outside it)
 * while [dirty] asks first whether to discard the changes; [footerStart] shows next to the buttons.
 */
@Composable
fun EditorPopup(
    title: @Composable () -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
    /** Whether something was changed: closing then asks first, and saving is offered. */
    dirty: Boolean = true,
    saveEnabled: Boolean = true,
    saveLabel: String = stringResource(R.string.save),
    /** The colour of the thin strip at the top of the card: what is edited (a collection's accent...). */
    accent: Color = MaterialTheme.colorScheme.primary,
    headerActions: @Composable RowScope.() -> Unit = {},
    footerStart: @Composable RowScope.() -> Unit = {},
    /** Something kept at the bottom, over the buttons, while the body scrolls (a search field...). */
    bottomBar: (@Composable ColumnScope.() -> Unit)? = null,
    /** A dot on the save button while [dirty]: there are unsaved changes. */
    unsavedDot: Boolean = false,
    /** The header's colour, when not the soft tint of [accent]. */
    barColor: Color? = null,
    /** More of the header, under the title row and as wide as the card (a section's time...). */
    headerExtra: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    // The header and the footer on a soft tint of what is edited, set apart from the white body.
    val scheme = MaterialTheme.colorScheme
    val bars = barColor ?: accent.copy(alpha = 0.08f).compositeOver(scheme.surfaceContainerLow)
    // The footer a light grey, a shade deeper than the body; the body a shade deeper than its white cards.
    val footer = scheme.surfaceContainerHigh
    val line = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
    var confirmDiscard by remember { mutableStateOf(false) }
    val close = { if (dirty) confirmDiscard = true else onDismiss() }
    // Edge to edge: the window is not resized for the keyboard; the card makes room for it (and the
    // system bars) itself, so it never sits under the keyboard.
    Dialog(
        onDismissRequest = close,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                // A tap around the card closes it (as for a standard dialog).
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = close)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 12.dp, vertical = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = scheme.surfaceContainerLow,
                // The program tab's section tiles' frame.
                border = BorderStroke(1.5.dp, scheme.primary.copy(alpha = 0.55f)),
                shadowElevation = 8.dp,
                modifier = modifier
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    // Taps on the card stay on it.
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {}),
            ) {
                Column {
                    Column(Modifier.fillMaxWidth().background(bars).padding(top = 14.dp, bottom = 12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(Modifier.weight(1f)) { title() }
                            headerActions()
                            IconButton(onClick = close) { Icon(Icons.Filled.Close, stringResource(R.string.close)) }
                        }
                        if (headerExtra != null) {
                            Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp), content = headerExtra)
                        }
                    }
                    HorizontalDivider(color = line)
                    content()
                    HorizontalDivider(color = line)
                    Column(Modifier.fillMaxWidth().background(footer)) {
                        if (bottomBar != null) {
                            Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp), content = bottomBar)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) { footerStart() }
                            TextButton(onClick = close) { Text(stringResource(R.string.cancel)) }
                            Spacer(Modifier.width(4.dp))
                            Box {
                                Button(onClick = onSave, enabled = saveEnabled && dirty) { Text(saveLabel) }
                                if (unsavedDot && dirty) {
                                    Box(
                                        Modifier
                                            .align(Alignment.TopEnd)
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(footer)
                                            .padding(2.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.tertiary),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text(stringResource(R.string.discard_title)) },
            text = { Text(stringResource(R.string.discard_message)) },
            confirmButton = {
                TextButton(onClick = { confirmDiscard = false; onDismiss() }) {
                    Text(stringResource(R.string.discard), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text(stringResource(R.string.keep_editing)) } },
        )
    }
}

/** An [EditorPopup]'s title: one line, with an optional smaller line under it. */
@Composable
fun EditorPopupTitle(title: String, subtitle: String? = null, subtitleColor: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Column {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (!subtitle.isNullOrBlank()) {
            Text(subtitle, style = MaterialTheme.typography.labelMedium, color = subtitleColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** The scrolling content of an [EditorPopup]: it takes the room left, no more than its own. */
@Composable
fun ColumnScope.PopupBody(
    modifier: Modifier = Modifier,
    spacing: Arrangement.Vertical = Arrangement.spacedBy(14.dp),
    padding: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(padding),
        verticalArrangement = spacing,
        content = content,
    )
}

/** A small heading inside a popup, with optional [trailing] content (an «add» button...). */
@Composable
fun PopupLabel(text: String, modifier: Modifier = Modifier, color: Color = Color.Unspecified, trailing: @Composable RowScope.() -> Unit = {}) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            color = if (color == Color.Unspecified) MaterialTheme.colorScheme.onSurfaceVariant else color,
            modifier = Modifier.weight(1f),
        )
        trailing()
    }
}

/** The footer's note that there are unsaved changes: a dot and the words, or nothing. */
@Composable
fun UnsavedNote(dirty: Boolean) {
    if (!dirty) return
    val tint = MaterialTheme.colorScheme.tertiary
    Box(Modifier.size(8.dp).clip(CircleShape).background(tint))
    Spacer(Modifier.width(6.dp))
    Text(
        stringResource(R.string.unsaved_changes),
        style = MaterialTheme.typography.labelMedium,
        color = tint,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

/** A soft banner in a popup: something to know about what is edited (shared with other steps...). */
@Composable
fun PopupNotice(text: String, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = scheme.secondaryContainer.copy(alpha = 0.7f),
        border = BorderStroke(1.dp, scheme.secondary.copy(alpha = 0.2f)),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Outlined.Info, null, tint = scheme.onSecondaryContainer, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Text(text, style = MaterialTheme.typography.bodySmall, color = scheme.onSecondaryContainer)
        }
    }
}

/** The «+ add» button at the end of a [PopupLabel]: as tall as the heading's line, not a full row. */
@Composable
fun PopupAddButton(text: String, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 10.dp),
        modifier = Modifier.height(34.dp),
    ) {
        Icon(Icons.Filled.Add, null, Modifier.size(18.dp))
        Spacer(Modifier.width(4.dp))
        Text(text)
    }
}
