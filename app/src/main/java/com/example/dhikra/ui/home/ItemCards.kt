package com.example.dhikra.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.dhikra.R
import com.example.dhikra.data.ArabicText
import com.example.dhikra.data.ItemType
import com.example.dhikra.data.ProgramItem
import com.example.dhikra.ui.components.ExpandIcon
import com.example.dhikra.ui.components.NoteIcon
import com.example.dhikra.ui.components.Pill
import com.example.dhikra.ui.components.icon
import com.example.dhikra.ui.theme.ElementColors
import com.example.dhikra.ui.theme.LocalDhikraPalette
import com.example.dhikra.ui.theme.ReadingStyles
import com.example.dhikra.ui.uiDigits
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import com.example.dhikra.ui.components.TaskIcon
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import com.example.dhikra.ui.components.LocalSectionComplete
import com.example.dhikra.ui.components.GoldInk

/**
 * One step of the schedule, as a card with only its header (type and title). Tapping a text opens
 * it in the full-screen reader through [onOpen]; tapping a group unfolds the list of its texts,
 * each of which opens in the reader through [onOpenText] (with the text's id). A task is checked
 * off in place through [onSetComplete]. [done] counts the repetitions (for a group, the finished
 * texts), [textDone] the repetitions of each text of a group, and [current] marks the first
 * unfinished step, which is lifted.
 */
@Composable
fun ProgramItemView(
    item: ProgramItem,
    done: Int,
    textScale: Float,
    onOpen: () -> Unit,
    onSetComplete: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    current: Boolean = false,
    onOpenText: (String) -> Unit = {},
    textDone: (ProgramItem) -> Int = { 0 },
    /** Press and hold: the step (or, inside a group, the text) to edit or move. */
    onLongPress: (ProgramItem) -> Unit = {},
) {
    when {
        item.type == ItemType.HEADING -> HeadingItem(item, modifier, onLongPress)
        item.type == ItemType.TASK -> TaskItem(item, done >= 1, textScale, current, onSetComplete, modifier, onLongPress)
        item.isGroup -> GroupCard(item, done, current, onOpenText, textDone, modifier, onLongPress)
        else -> ReadingCard(item, done, current, onOpen, modifier, onLongPress)
    }
}

/**
 * Vertical centre of each card's header, measured from its top: the timeline node is placed
 * there so it lines up with the card's title.
 */
val ItemType.nodeCenter: Dp
    get() = when (this) {
        ItemType.HEADING -> 17.dp
        else -> 32.dp
    }

/** [ItemType.nodeCenter] of a step; every group, whatever its collection, has the same header. */
val ProgramItem.nodeCenter: Dp get() = if (isGroup) 32.dp else type.nodeCenter

@Composable
fun ItemType.colors(): ElementColors {
    val palette = LocalDhikraPalette.current
    return when (this) {
        ItemType.QURAN -> palette.quran
        ItemType.DUA -> palette.dua
        ItemType.HADITH -> palette.hadith
        ItemType.DHIKR -> palette.dhikr
        ItemType.TASK, ItemType.HEADING -> palette.task
    }
}

val ItemType.label: Int
    get() = when (this) {
        ItemType.TASK -> R.string.type_task
        ItemType.HEADING -> R.string.type_heading
        ItemType.DUA -> R.string.type_dua
        ItemType.DHIKR -> R.string.type_dhikr
        ItemType.QURAN -> R.string.type_quran
        ItemType.HADITH -> R.string.type_hadith
    }

/**
 * Opacity of the supporting text around the steps (headings, notes, descriptions), so it reads
 * as secondary to the steps and the progress. Kept at a level that still passes text contrast.
 */
internal const val QuietTextAlpha = 0.8f
internal const val FaintTextAlpha = 0.78f

/** A group label on the timeline, e.g. "أذكار الصباح", followed by a fading rule. */
@Composable
private fun HeadingItem(item: ProgramItem, modifier: Modifier, onLongPress: (ProgramItem) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    // Quiet on purpose: a heading only groups the steps, it should not compete with them.
    val rule = scheme.outlineVariant
    Column(
        modifier
            .fillMaxWidth()
            .pointerInput(item.id) { detectTapGestures(onLongPress = { onLongPress(item) }) }
            .padding(top = 6.dp, bottom = 2.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = item.title.ifBlank { item.text },
                style = MaterialTheme.typography.titleSmall,
                color = scheme.onSurfaceVariant.copy(alpha = QuietTextAlpha),
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.width(12.dp))
            Box(
                Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(
                            if (rtl) listOf(Color.Transparent, rule) else listOf(rule, Color.Transparent)
                        )
                    )
            )
        }
        if (item.title.isNotBlank() && item.text.isNotBlank()) {
            Text(
                text = item.text,
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant.copy(alpha = FaintTextAlpha),
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

/**
 * The colors of every step card on the schedule, whatever its type: a white card outlined in the
 * theme's green, with the dhikr card's slate blue for the icons, labels and counts and its ink for
 * the text — the same whether the step is still to do or done (its check says which). The type
 * still shows in each card's icon and label.
 */
@Composable
private fun stepColors(): ElementColors {
    val palette = LocalDhikraPalette.current
    return palette.slate.copy(
        container = palette.task.container,
        border = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
    )
}

/**
 * Card container shared by every step: lifted with a shadow while still to do (the current one a
 * little more) with a firmer outline, flat and with the lighter outline once finished.
 */
@Composable
private fun StepSurface(
    colors: ElementColors,
    complete: Boolean,
    focused: Boolean,
    modifier: Modifier,
    shape: Shape = MaterialTheme.shapes.large,
    /** Whether a finished step takes the faint warm tint (a task keeps its own fill). */
    doneTint: Boolean = true,
    content: @Composable () -> Unit,
) {
    val elevation by animateDpAsState(
        targetValue = when {
            complete -> 0.dp
            focused -> 6.dp
            else -> 4.dp
        },
        animationSpec = tween(400),
        label = "stepElevation",
    )
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = stepContainer(colors, complete && doneTint),
        border = stepBorder(colors, complete),
        shadowElevation = elevation,
        content = content,
    )
}

/**
 * A step's outline: 2 dp in its colours while to do; a finished step turns golden — 1.5 dp in the
 * "done" amber. A group's stacked sheets take the same colour, each a little thinner than the
 * one in front ([widthScale]), so the stack recedes.
 */
@Composable
private fun stepBorder(colors: ElementColors, complete: Boolean, widthScale: Float = 1f): BorderStroke {
    val done = LocalDhikraPalette.current.done
    val color by animateColorAsState(if (complete) done else colors.border, tween(400), label = "stepBorder")
    return BorderStroke((if (complete) 1.5.dp else 2.dp) * widthScale, color)
}

/** A step's fill: its own, with a very light warm tint once finished. */
@Composable
private fun stepContainer(colors: ElementColors, complete: Boolean): Color {
    val done = LocalDhikraPalette.current.done
    return if (complete) done.copy(alpha = 0.035f).compositeOver(colors.container) else colors.container
}

@Composable
private fun TaskItem(
    item: ProgramItem,
    complete: Boolean,
    textScale: Float,
    current: Boolean,
    onSetComplete: (Boolean) -> Unit,
    modifier: Modifier,
    onLongPress: (ProgramItem) -> Unit,
) {
    val colors = taskColors()
    val haptic = LocalHapticFeedback.current
    StepSurface(colors, complete, current, modifier, doneTint = false) {
        Row(
            modifier = Modifier
                .combinedClickable(
                    role = Role.Checkbox,
                    onClick = {
                        haptic.performHapticFeedback(
                            if (!complete) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff
                        )
                        onSetComplete(!complete)
                    },
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLongPress(item)
                    },
                )
                .semantics { toggleableState = ToggleableState(complete) }
                .padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Something to do rather than to read: the to-do mark on its disc, in the task's blue — gold
            // once done, like every finished step.
            val palette = LocalDhikraPalette.current
            val markTone by animateColorAsState(if (complete) palette.done else colors.accent, tween(400), label = "taskMark")
            val markTint by animateColorAsState(if (complete) palette.doneText else colors.accent, tween(400), label = "taskMarkTint")
            Box(
                modifier = Modifier.size(36.dp).clip(CircleShape).background(markTone.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(TaskIcon, contentDescription = stringResource(R.string.type_task), tint = markTint, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                if (item.title.isNotBlank()) {
                    Text(item.title, style = MaterialTheme.typography.titleSmall, color = colors.accent)
                }
                Text(
                    text = item.text,
                    style = ReadingStyles.task(textScale),
                    color = colors.ink,
                    textDecoration = if (complete) TextDecoration.LineThrough else null,
                )
                if (item.note.isNotBlank()) NoteLine(item.note, colors)
            }
            Spacer(Modifier.width(12.dp))
            CheckMark(checked = complete)
        }
    }
}

/**
 * A task's colours: a shade of the blue accent — a soft blue card, blue outline, icon and title —
 * apart from the reading steps' white cards with their green outline. Done or not, the card keeps
 * its fill (only the outline turns gold and the check fills).
 */
@Composable
private fun taskColors(): ElementColors {
    val palette = LocalDhikraPalette.current
    val blue = palette.slate.accent
    return palette.task.copy(
        container = blue.copy(alpha = 0.06f).compositeOver(palette.task.container),
        border = blue.copy(alpha = 0.45f),
        accent = blue,
    )
}

/**
 * Round check box in the theme's green, like the timeline's circles: an empty ring, filled with a
 * check when [checked].
 */
@Composable
private fun CheckMark(checked: Boolean, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    // Gold once the whole section is done, like its step circles.
    val gold = LocalSectionComplete.current
    val tone = if (gold) LocalDhikraPalette.current.done else scheme.primary
    val fill by animateColorAsState(if (checked) tone else Color.Transparent, label = "checkFill")
    Box(
        modifier = modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(fill)
            .border(1.5.dp, if (checked) tone else scheme.primary, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedVisibility(visible = checked, enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut()) {
            Icon(Icons.Filled.Check, null, tint = if (gold) GoldInk else scheme.onPrimary, modifier = Modifier.size(13.dp))
        }
    }
}

/** A text's card: just its header, which opens the text in the reader. */
@Composable
private fun ReadingCard(
    item: ProgramItem,
    done: Int,
    current: Boolean,
    onOpen: () -> Unit,
    modifier: Modifier,
    onLongPress: (ProgramItem) -> Unit,
) {
    val complete = done >= item.target
    val colors = stepColors()
    StepSurface(colors = colors, complete = complete, focused = current, modifier = modifier) {
        Column(
            Modifier.combinedClickable(
                onClickLabel = stringResource(R.string.open_full_screen),
                onClick = onOpen,
                onLongClick = { onLongPress(item) },
            )
        ) {
            StepHeader(item, colors, done, complete)
        }
    }
}

/**
 * A group step: several texts read one after the other (e.g. «أدعية الحمد والشكر»). Folded, its
 * card shows the title and the progress through the texts, over two sheets of their color; a tap
 * unfolds the list of its texts, and a tap on a text opens the reader at it ([onOpenText]).
 */
@Composable
private fun GroupCard(
    item: ProgramItem,
    done: Int,
    current: Boolean,
    onOpenText: (String) -> Unit,
    textDone: (ProgramItem) -> Int,
    modifier: Modifier,
    onLongPress: (ProgramItem) -> Unit,
) {
    val total = item.members.size
    // Done once the texts needed are read: all of them, or the number of a choice group.
    val complete = item.target > 0 && done >= item.target
    val colors = stepColors()
    // Kept while the step scrolls out of view: the list items are keyed by step id.
    var expanded by rememberSaveable(item.id) { mutableStateOf(false) }
    // The sheets stand for the texts underneath: unfolded, the texts themselves are shown.
    val stack by animateDpAsState(if (expanded) 0.dp else 8.dp, label = "groupStack")
    Box(modifier.fillMaxWidth().padding(bottom = stack)) {
        if (stack > 0.dp) {
            // The sheets behind are a little thinner-edged than the card in front.
            StackSheet(colors, complete, inset = 18.dp, drop = stack, borderScale = 0.75f)
            StackSheet(colors, complete, inset = 9.dp, drop = stack / 2, borderScale = 0.75f)
        }
        StepSurface(colors = colors, complete = complete, focused = current, modifier = Modifier) {
            Column {
                GroupHeader(item, colors, done, total, complete, expanded, onToggle = { expanded = !expanded }, onLongPress = { onLongPress(item) })
                AnimatedVisibility(
                    visible = expanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    Column {
                        item.members.forEach { member ->
                            GroupText(
                                text = member,
                                done = textDone(member),
                                colors = colors,
                                onOpen = { onOpenText(member.id) },
                                onLongPress = { onLongPress(member) },
                            )
                        }
                    }
                }
            }
        }
    }
}

/** One text of an unfolded group: its check circle, title and count; opens the reader. */
@Composable
private fun GroupText(
    text: ProgramItem,
    done: Int,
    colors: ElementColors,
    onOpen: () -> Unit,
    onLongPress: () -> Unit,
) {
    val complete = text.target > 0 && done >= text.target
    Column {
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
            modifier = Modifier.padding(horizontal = 14.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClickLabel = stringResource(R.string.open_full_screen),
                    onClick = onOpen,
                    onLongClick = onLongPress,
                )
                .padding(start = 14.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val doneLabel = stringResource(R.string.step_state_done)
            CheckMark(
                checked = complete,
                modifier = Modifier.semantics { if (complete) contentDescription = doneLabel },
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = text.title.ifBlank { stringResource(text.type.label) },
                style = MaterialTheme.typography.bodyMedium,
                color = colors.ink.copy(alpha = if (complete) 0.65f else 1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (text.target > 1) {
                Pill(
                    text = repetitions(done, text.target),
                    color = colors.accent,
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
            OpenIcon(colors)
        }
    }
}

/** A sheet peeking out [drop] below a group card, narrower by [inset] on each side. */
@Composable
private fun BoxScope.StackSheet(colors: ElementColors, complete: Boolean, inset: Dp, drop: Dp, borderScale: Float) {
    // The card's own outline colour and fill, to do or finished; the outline [borderScale] as wide.
    Surface(
        shape = MaterialTheme.shapes.large,
        color = stepContainer(colors, complete),
        border = stepBorder(colors, complete, borderScale),
        modifier = Modifier.matchParentSize().padding(horizontal = inset).offset(y = drop),
    ) {}
}

/** The group's badge, title and progress through its texts. The whole row folds or unfolds the texts. */
@Composable
private fun GroupHeader(
    item: ProgramItem,
    colors: ElementColors,
    done: Int,
    total: Int,
    complete: Boolean,
    expanded: Boolean,
    onToggle: () -> Unit,
    onLongPress: () -> Unit,
) {
    // A choice group: the reader picks the texts; its progress counts toward the number needed.
    val needed = if (item.isChoice) item.choose else total
    val progress = if (complete) stringResource(R.string.step_state_done)
    else stringResource(R.string.group_progress, uiDigits(done), uiDigits(needed))
    val kind = if (item.isChoice) {
        stringResource(R.string.group_choose, uiDigits(item.choose), uiDigits(total))
    } else {
        stringResource(R.string.group_label)
    }
    val turn by animateFloatAsState(if (expanded) 180f else 0f, label = "groupChevron")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClickLabel = stringResource(if (expanded) R.string.group_hide_texts else R.string.group_show_texts),
                onClick = onToggle,
                onLongClick = onLongPress,
            )
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconPanel(item.type, complete) { tint ->
            // The number of texts sits on the disc's edge, as on the library's group badge.
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(item.type.icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
                Surface(
                    shape = CircleShape,
                    color = tint,
                    contentColor = LocalDhikraPalette.current.task.container,
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.surfaceContainerLowest),
                    modifier = Modifier.align(Alignment.BottomEnd).offset(x = 6.dp, y = 6.dp),
                ) {
                    Text(uiDigits(total), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 5.dp))
                }
            }
        }
        Row(
            modifier = Modifier.weight(1f).padding(start = 12.dp, end = 12.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall,
                color = colors.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "$kind • $progress",
                style = MaterialTheme.typography.labelSmall,
                color = colors.accent,
            )
        }
        Icon(
            imageVector = Icons.Filled.KeyboardArrowDown,
            contentDescription = null,
            tint = colors.accent.copy(alpha = 0.75f),
            modifier = Modifier.padding(start = 8.dp).size(24.dp).rotate(turn),
        )
        }
    }
}

/**
 * A group's mark: its collection's icon in a circle, with the number of its texts in a small
 * bubble overlapping the corner (outside the circle's bounds, so the badge keeps its [size]).
 */
@Composable
fun GroupBadge(
    type: ItemType,
    count: Int,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    colors: ElementColors = type.colors(),
) {
    Box(modifier.size(size)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(colors.accent.copy(alpha = 0.13f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(type.icon, contentDescription = null, tint = colors.accent, modifier = Modifier.size(size * 0.55f))
        }
        Surface(
            shape = CircleShape,
            color = colors.accent,
            contentColor = colors.onAccent,
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.surfaceContainerLowest),
            modifier = Modifier.align(Alignment.BottomEnd).offset(x = 6.dp, y = 4.dp),
        ) {
            Text(
                text = uiDigits(count),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 5.dp),
            )
        }
    }
}

/** What a card shows next to its count while it is being read: how far, or how many times. */
@Composable
private fun repetitions(done: Int, target: Int): String =
    if (done in 1 until target) {
        stringResource(R.string.group_progress, uiDigits(done), uiDigits(target))
    } else {
        stringResource(R.string.times_count, uiDigits(target))
    }

/** The hint at the end of a header that the card opens full screen. */
@Composable
private fun OpenIcon(colors: ElementColors) {
    Icon(
        imageVector = ExpandIcon,
        contentDescription = null,
        tint = colors.accent.copy(alpha = 0.75f),
        modifier = Modifier.padding(start = 8.dp).size(20.dp),
    )
}

/** Type badge, title and repeat count. The whole row opens the reader. */
@Composable
private fun StepHeader(
    item: ProgramItem,
    colors: ElementColors,
    done: Int,
    complete: Boolean,
) {
    val typeLabel = stringResource(item.type.label)
    val doneLabel = stringResource(R.string.step_state_done)
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), verticalAlignment = Alignment.CenterVertically) {
        IconPanel(item.type, complete) { tint ->
            Icon(item.type.icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        }
        Row(
            modifier = Modifier.weight(1f).padding(start = 12.dp, end = 12.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = item.title.ifBlank { typeLabel },
                style = MaterialTheme.typography.titleSmall,
                color = colors.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = if (complete) "$typeLabel • $doneLabel" else typeLabel,
                style = MaterialTheme.typography.labelSmall,
                color = colors.accent,
            )
        }
        if (item.target > 1) {
            Pill(
                text = repetitions(done, item.target),
                color = colors.accent,
                modifier = Modifier.padding(start = 6.dp),
            )
        }
        OpenIcon(colors)
        }
    }
}

/**
 * A card's start edge: a panel the card's full height in its type's accent colour (Quran green,
 * hadith sage, dua blue, dhikr purple), with the icon on a lighter disc of the same colour. Once the
 * step is [complete], panel, disc and icon all turn the "done" gold. [content] draws the icon in
 * the colour it is given.
 */
@Composable
private fun IconPanel(type: ItemType, complete: Boolean, content: @Composable (tint: Color) -> Unit) {
    val palette = LocalDhikraPalette.current
    val card = palette.task.container
    val tone by animateColorAsState(if (complete) palette.done else type.colors().accent, tween(400), label = "panelTone")
    val tint by animateColorAsState(if (complete) palette.doneText else type.colors().accent, tween(400), label = "panelTint")
    Box(
        modifier = Modifier.width(64.dp).fillMaxHeight().background(tone.copy(alpha = 0.22f).compositeOver(card)),
        contentAlignment = Alignment.Center,
    ) {
        // Drawn behind, not clipped: a group's count sits on the disc's edge.
        Box(
            modifier = Modifier.size(40.dp).background(tone.copy(alpha = 0.07f).compositeOver(card), CircleShape),
            contentAlignment = Alignment.Center,
        ) { content(tint) }
    }
}

@Composable
private fun NoteLine(note: String, colors: ElementColors) {
    Row(
        modifier = Modifier.padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = NoteIcon,
            contentDescription = stringResource(R.string.note_label),
            tint = colors.ink.copy(alpha = 0.4f),
            modifier = Modifier.size(14.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = note,
            style = MaterialTheme.typography.bodySmall,
            color = colors.ink.copy(alpha = 0.65f),
        )
    }
}
