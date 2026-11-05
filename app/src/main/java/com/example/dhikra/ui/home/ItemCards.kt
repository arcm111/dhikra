package com.example.dhikra.ui.home

import android.os.SystemClock
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.dhikra.R
import com.example.dhikra.data.ArabicText
import com.example.dhikra.data.ItemType
import com.example.dhikra.data.ProgramItem
import com.example.dhikra.ui.components.CounterBar
import com.example.dhikra.ui.components.DoneBar
import com.example.dhikra.ui.components.ExpandIcon
import com.example.dhikra.ui.components.NoteIcon
import com.example.dhikra.ui.components.QuranTitle
import com.example.dhikra.ui.components.ReaderText
import com.example.dhikra.ui.components.UndoIcon
import com.example.dhikra.ui.components.icon
import com.example.dhikra.ui.components.starLattice
import com.example.dhikra.ui.theme.ElementColors
import com.example.dhikra.ui.theme.LocalDhikraPalette
import com.example.dhikra.ui.theme.ReadingStyles

/** Callbacks of an item in the daily program. */
class ItemActions(
    val onTogglePlay: () -> Unit,
    val onIncrement: () -> Unit,
    val onRepetitionRead: (elapsedMs: Long) -> Unit,
    val onSetComplete: (Boolean) -> Unit,
    val onReset: () -> Unit,
    /** Expands the repetition counter to the immersive full-screen view. */
    val onExpandCounter: () -> Unit = {},
    /** Tapping the card header: focuses and opens a folded card, folds the open one. */
    val onToggleExpanded: () -> Unit = {},
)

/**
 * One step of the schedule. Cards are folded to their header (type and title) and only the
 * [expanded] one shows its text; [focused] lifts it. [current] marks the first unfinished step,
 * whose main action is emphasized. An open group step lists its texts, each drawn by [member]
 * (normally a [GroupMemberView]); [done] then counts the finished texts.
 */
@Composable
fun ProgramItemView(
    item: ProgramItem,
    done: Int,
    playing: Boolean,
    lettersPerSecond: Float,
    textScale: Float,
    actions: ItemActions,
    modifier: Modifier = Modifier,
    expanded: Boolean = false,
    focused: Boolean = expanded,
    current: Boolean = false,
    member: @Composable (ProgramItem) -> Unit = {},
) {
    when {
        item.type == ItemType.HEADING -> HeadingItem(item, modifier)
        item.type == ItemType.TASK -> TaskItem(item, done >= 1, textScale, focused, actions.onSetComplete, modifier)
        item.isGroup -> GroupCard(item, done, expanded, focused, textScale, actions, modifier, member)
        else -> ReadingCard(item, done, playing, lettersPerSecond, textScale, actions, expanded, focused, current, modifier)
    }
}

/**
 * One text inside an open group: its own card, styled for its type and always open, with its own
 * reader and counter (or done button).
 */
@Composable
fun GroupMemberView(
    item: ProgramItem,
    done: Int,
    playing: Boolean,
    lettersPerSecond: Float,
    textScale: Float,
    actions: ItemActions,
    modifier: Modifier = Modifier,
    current: Boolean = false,
) = ReadingCard(
    item, done, playing, lettersPerSecond, textScale, actions,
    expanded = true, focused = false, current = current, modifier = modifier, foldable = false,
)

/** Whether a step has text to unfold (tasks and headings are always fully visible). */
val ItemType.isExpandable: Boolean get() = isContent

/**
 * Vertical centre of each card's header, measured from its top: the timeline node is placed
 * there so it lines up with the card's title.
 */
val ItemType.nodeCenter: Dp
    get() = when (this) {
        ItemType.HEADING -> 17.dp
        ItemType.QURAN -> 30.dp
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
 * Folding and unfolding share one duration with the focus motion ([FOLD_MILLIS]), so the card
 * that takes the focus can be held steady for exactly as long as the sizes around it change.
 */
private val Unfold: EnterTransition =
    expandVertically(tween(FOLD_MILLIS, easing = FastOutSlowInEasing), expandFrom = Alignment.Top) +
        fadeIn(tween(220, delayMillis = 90))
private val Fold: ExitTransition =
    shrinkVertically(tween(FOLD_MILLIS, easing = FastOutSlowInEasing), shrinkTowards = Alignment.Top) +
        fadeOut(tween(140))

/** A group label on the timeline, e.g. "أذكار الصباح", followed by a fading gold rule. */
@Composable
private fun HeadingItem(item: ProgramItem, modifier: Modifier) {
    val palette = LocalDhikraPalette.current
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val rule = palette.ornament.copy(alpha = 0.55f)
    Column(modifier.fillMaxWidth().padding(top = 6.dp, bottom = 2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = item.title.ifBlank { item.text },
                style = MaterialTheme.typography.titleSmall,
                color = palette.ornamentText,
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
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

/** Card container shared by every step; the focused card is lifted (and outlined if bordered). */
@Composable
private fun StepSurface(
    colors: ElementColors,
    focused: Boolean,
    modifier: Modifier,
    shape: Shape = MaterialTheme.shapes.large,
    content: @Composable () -> Unit,
) {
    val bordered = colors.border.alpha > 0f
    val border by animateColorAsState(
        targetValue = if (focused) colors.accent.copy(alpha = 0.55f) else colors.border,
        label = "stepBorder",
    )
    val elevation by animateDpAsState(
        targetValue = when {
            focused && bordered -> 4.dp
            focused -> 8.dp
            bordered -> 0.dp
            else -> 2.dp
        },
        label = "stepElevation",
    )
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = colors.container,
        border = if (bordered) BorderStroke(if (focused) 1.5.dp else 1.dp, border) else null,
        shadowElevation = elevation,
        content = content,
    )
}

@Composable
private fun TaskItem(
    item: ProgramItem,
    complete: Boolean,
    textScale: Float,
    focused: Boolean,
    onSetComplete: (Boolean) -> Unit,
    modifier: Modifier,
) {
    val colors = ItemType.TASK.colors()
    val haptic = LocalHapticFeedback.current
    StepSurface(colors, focused, modifier) {
        Row(
            modifier = Modifier
                .toggleable(
                    value = complete,
                    role = Role.Checkbox,
                    onValueChange = { checked ->
                        haptic.performHapticFeedback(
                            if (checked) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff
                        )
                        onSetComplete(checked)
                    },
                )
                .padding(start = 16.dp, end = 14.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                if (item.title.isNotBlank()) {
                    Text(item.title, style = MaterialTheme.typography.titleSmall, color = colors.accent)
                }
                Text(
                    text = item.text,
                    style = ReadingStyles.task(textScale),
                    color = colors.ink.copy(alpha = if (complete) 0.6f else 1f),
                    textDecoration = if (complete) TextDecoration.LineThrough else null,
                )
                if (item.note.isNotBlank()) NoteLine(item.note, colors)
            }
            Spacer(Modifier.width(12.dp))
            CheckMark(checked = complete, colors = colors)
        }
    }
}

/** Round check box that fills with the accent color when checked. */
@Composable
private fun CheckMark(checked: Boolean, colors: ElementColors) {
    val fill by animateColorAsState(if (checked) colors.accent else Color.Transparent, label = "checkFill")
    val outline by animateColorAsState(
        targetValue = if (checked) colors.accent else MaterialTheme.colorScheme.outline,
        label = "checkOutline",
    )
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(fill)
            .border(2.dp, outline, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedVisibility(visible = checked, enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut()) {
            Icon(Icons.Filled.Check, null, tint = colors.onAccent, modifier = Modifier.size(18.dp))
        }
    }
}

/** A text's card. Not [foldable], it is always open and its header does nothing (texts of a group). */
@Composable
private fun ReadingCard(
    item: ProgramItem,
    done: Int,
    playing: Boolean,
    lettersPerSecond: Float,
    textScale: Float,
    actions: ItemActions,
    expanded: Boolean,
    focused: Boolean,
    current: Boolean,
    modifier: Modifier,
    foldable: Boolean = true,
) {
    val colors = item.type.colors()
    val haptic = LocalHapticFeedback.current
    val target = item.target
    val complete = done >= target

    var cycle by remember { mutableIntStateOf(0) }
    var cycleStart by remember { mutableLongStateOf(0L) }
    var awaitingTap by remember { mutableStateOf(false) }

    LaunchedEffect(complete, playing) {
        if (complete && playing) actions.onTogglePlay()
    }

    val countTap: () -> Unit = {
        if (done + 1 >= target) haptic.performHapticFeedback(HapticFeedbackType.Confirm)
        else haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
        if (playing) {
            actions.onRepetitionRead(SystemClock.uptimeMillis() - cycleStart)
            cycle++
        } else {
            actions.onIncrement()
        }
    }
    val reset: () -> Unit = {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        actions.onReset()
    }
    val isQuran = item.type == ItemType.QURAN
    val displayText = remember(item.text, isQuran) {
        if (isQuran) ArabicText.withAyahMarkers(item.text) else item.text
    }
    val footer: @Composable () -> Unit = {
        if (target > 1) {
            RepeatFooter(
                item = item,
                done = done,
                colors = colors,
                playing = playing,
                awaitingTap = playing && awaitingTap,
                onTogglePlay = actions.onTogglePlay,
                onCount = countTap,
                onReset = reset,
                onExpand = actions.onExpandCounter,
            )
        } else {
            SingleFooter(complete, colors, current) {
                haptic.performHapticFeedback(
                    if (it) HapticFeedbackType.Confirm else HapticFeedbackType.ToggleOff
                )
                actions.onSetComplete(it)
            }
        }
    }
    val body: @Composable () -> Unit = {
        if (item.opening.isNotBlank()) {
            Text(
                text = item.opening,
                style = if (isQuran) ReadingStyles.quranOpening(textScale)
                else ReadingStyles.dua(textScale).copy(textAlign = TextAlign.Center, fontWeight = FontWeight.Medium),
                color = if (isQuran) colors.ink.copy(alpha = 0.92f) else colors.accent,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            )
        }
        if (displayText.isNotBlank()) {
            ReaderText(
                text = displayText,
                style = when (item.type) {
                    ItemType.QURAN -> ReadingStyles.quran(textScale)
                    ItemType.HADITH -> ReadingStyles.hadith(textScale)
                    ItemType.DHIKR -> ReadingStyles.dhikr(textScale)
                    else -> ReadingStyles.dua(textScale)
                },
                colors = colors,
                playing = playing,
                cycle = cycle,
                lettersPerSecond = lettersPerSecond,
                ornamentalAyahMarkers = isQuran,
                onCycleStart = { cycleStart = it },
                onReachedEnd = { awaitingTap = it },
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            )
        }
        if (!isQuran && item.source.isNotBlank()) SourceLine(item.source, colors, textScale)
        if (item.note.isNotBlank()) NoteLine(item.note, colors)
        Spacer(Modifier.height(16.dp))
        footer()
    }

    StepSurface(colors = colors, focused = focused, modifier = modifier) {
        Column(
            modifier = Modifier
                .then(if (isQuran) Modifier.quranBackground(colors) else Modifier)
                .then(
                    if (playing) {
                        // While reading, the whole card is a big tap target to count one repetition.
                        Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(color = colors.accent),
                            onClick = countTap,
                        )
                    } else Modifier
                ),
        ) {
            val onHeaderClick = actions.onToggleExpanded.takeIf { foldable }
            if (isQuran) {
                QuranHeader(item, colors, complete, expanded, textScale, onHeaderClick)
            } else {
                StepHeader(item, colors, complete, expanded, onHeaderClick)
            }
            val content: @Composable () -> Unit = {
                Column(
                    Modifier.fillMaxWidth().padding(
                        start = if (isQuran) 22.dp else 16.dp,
                        end = if (isQuran) 22.dp else 16.dp,
                        bottom = 16.dp,
                    )
                ) { body() }
            }
            if (foldable) {
                AnimatedVisibility(visible = expanded, enter = Unfold, exit = Fold) { content() }
            } else {
                content()
            }
        }
    }
}

/** Colors of a group's own card: a light sheet that holds its texts' colored cards. */
@Composable
private fun ItemType.groupColors(): ElementColors {
    val colors = colors()
    val scheme = MaterialTheme.colorScheme
    // The Quran's gold is made for its deep green card: on the light sheet, use the brand green.
    val quran = this == ItemType.QURAN
    return colors.copy(
        container = scheme.surfaceContainerLowest,
        border = if (quran) scheme.outlineVariant else colors.border,
        accent = if (quran) scheme.primary else colors.accent,
        onAccent = if (quran) scheme.onPrimary else colors.onAccent,
        ink = scheme.onSurface,
    )
}

/**
 * A group step: several texts read one after the other (e.g. «أدعية الحمد والشكر»). Folded, it
 * shows its title and progress, over two sheets of its texts' color; open, it lists each text on
 * its own card ([member]), then offers to finish (or undo) all of them at once.
 */
@Composable
private fun GroupCard(
    item: ProgramItem,
    done: Int,
    expanded: Boolean,
    focused: Boolean,
    textScale: Float,
    actions: ItemActions,
    modifier: Modifier,
    member: @Composable (ProgramItem) -> Unit,
) {
    val colors = item.type.groupColors()
    val total = item.members.size
    val complete = total > 0 && done >= total
    val stack by animateDpAsState(if (expanded) 0.dp else 8.dp, label = "groupStack")
    Box(modifier.fillMaxWidth().padding(bottom = stack)) {
        if (stack > 0.dp) {
            StackSheet(item.type.colors(), inset = 18.dp, drop = stack)
            StackSheet(item.type.colors(), inset = 9.dp, drop = stack / 2)
        }
        StepSurface(colors = colors, focused = focused, modifier = Modifier) {
            Column {
                GroupHeader(item, colors, done, total, complete, expanded, actions.onToggleExpanded)
                AnimatedVisibility(visible = expanded, enter = Unfold, exit = Fold) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(start = 10.dp, end = 10.dp, bottom = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        if (item.source.isNotBlank() || item.note.isNotBlank()) {
                            Column(Modifier.padding(horizontal = 4.dp)) {
                                if (item.source.isNotBlank()) SourceLine(item.source, colors, textScale)
                                if (item.note.isNotBlank()) NoteLine(item.note, colors)
                            }
                        }
                        item.members.forEach { text -> key(text.id) { member(text) } }
                        GroupFooter(complete, colors, actions.onSetComplete)
                    }
                }
            }
        }
    }
}

/** A sheet peeking out [drop] below a folded group card, narrower by [inset] on each side. */
@Composable
private fun BoxScope.StackSheet(colors: ElementColors, inset: Dp, drop: Dp) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = colors.container,
        border = if (colors.border.alpha > 0f) BorderStroke(1.dp, colors.border) else null,
        modifier = Modifier.matchParentSize().padding(horizontal = inset).offset(y = drop),
    ) {}
}

/** The group's badge, title and progress through its texts. The whole row folds / unfolds it. */
@Composable
private fun GroupHeader(
    item: ProgramItem,
    colors: ElementColors,
    done: Int,
    total: Int,
    complete: Boolean,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
) {
    val progress = if (complete) stringResource(R.string.step_state_done)
    else stringResource(R.string.group_progress, ArabicText.toArabicDigits(done), ArabicText.toArabicDigits(total))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClickLabel = stringResource(if (expanded) R.string.collapse else R.string.expand),
                onClick = onToggleExpanded,
            )
            .padding(start = 14.dp, end = 12.dp, top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GroupBadge(item.type, total)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall,
                color = colors.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(R.string.group_label) + " • " + progress,
                style = MaterialTheme.typography.labelSmall,
                color = colors.accent,
            )
        }
        val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "groupChevron")
        Icon(
            imageVector = Icons.Filled.KeyboardArrowDown,
            contentDescription = null,
            tint = colors.accent.copy(alpha = 0.75f),
            modifier = Modifier.padding(start = 4.dp).size(22.dp).rotate(rotation),
        )
    }
}

/** "Finish all" once the texts are read (or, when the whole group is done, undo it). */
@Composable
private fun GroupFooter(complete: Boolean, colors: ElementColors, onSetComplete: (Boolean) -> Unit) {
    val haptic = LocalHapticFeedback.current
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        TextButton(
            onClick = {
                haptic.performHapticFeedback(if (complete) HapticFeedbackType.ToggleOff else HapticFeedbackType.Confirm)
                onSetComplete(!complete)
            },
        ) {
            Icon(if (complete) UndoIcon else Icons.Filled.Check, null, Modifier.size(18.dp), tint = colors.accent)
            Spacer(Modifier.width(6.dp))
            Text(
                text = stringResource(if (complete) R.string.undo_done else R.string.group_complete_all),
                color = colors.accent,
            )
        }
    }
}

/**
 * A group's mark: its collection's icon in a circle, with the number of its texts in a small
 * bubble overlapping the corner (outside the circle's bounds, so the badge keeps its [size]).
 */
@Composable
fun GroupBadge(type: ItemType, count: Int, modifier: Modifier = Modifier, size: Dp = 36.dp) {
    val colors = type.colors()
    val quran = type == ItemType.QURAN
    Box(modifier.size(size)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(if (quran) colors.container else colors.accent.copy(alpha = 0.13f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(type.icon, contentDescription = null, tint = colors.accent, modifier = Modifier.size(size * 0.55f))
        }
        Surface(
            shape = CircleShape,
            color = if (quran) MaterialTheme.colorScheme.primary else colors.accent,
            contentColor = if (quran) MaterialTheme.colorScheme.onPrimary else colors.onAccent,
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.surfaceContainerLowest),
            modifier = Modifier.align(Alignment.BottomEnd).offset(x = 6.dp, y = 4.dp),
        ) {
            Text(
                text = ArabicText.toArabicDigits(count),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 5.dp),
            )
        }
    }
}

/** Deep green with a faint light from the top and a barely visible star texture. */
private fun Modifier.quranBackground(colors: ElementColors): Modifier = this
    .background(
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.05f).compositeOver(colors.container),
                colors.container,
                Color.Black.copy(alpha = 0.18f).compositeOver(colors.container),
            )
        )
    )
    .starLattice(colors.accent.copy(alpha = 0.045f), spacing = 54.dp, starRadius = 14.dp)

/** Centred, reverent header of a Quran card; folded it also tells the type and the count. */
@Composable
private fun QuranHeader(
    item: ProgramItem,
    colors: ElementColors,
    complete: Boolean,
    expanded: Boolean,
    textScale: Float,
    onToggleExpanded: (() -> Unit)?,
) {
    // Always exactly one caption line, so the header keeps its height when the card unfolds.
    val summary = listOfNotNull(
        stringResource(R.string.type_quran),
        if (item.target > 1) stringResource(R.string.times_count, ArabicText.toArabicDigits(item.target)) else null,
        if (complete) stringResource(R.string.step_state_done) else null,
    ).joinToString(" • ")
    val caption = if (expanded && item.source.isNotBlank()) item.source else summary
    QuranTitle(
        title = item.title.ifBlank { stringResource(R.string.type_quran) },
        caption = caption,
        colors = colors,
        textScale = textScale,
        modifier = Modifier
            .then(
                if (onToggleExpanded == null) Modifier
                else Modifier.clickable(
                    onClickLabel = stringResource(if (expanded) R.string.collapse else R.string.expand),
                    onClick = onToggleExpanded,
                )
            )
            .padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 14.dp),
    )
}

/**
 * Type badge, title and repeat count. The whole row folds / unfolds the card; without
 * [onToggleExpanded] (a card that is always open) it has no chevron and does nothing.
 */
@Composable
private fun StepHeader(
    item: ProgramItem,
    colors: ElementColors,
    complete: Boolean,
    expanded: Boolean,
    onToggleExpanded: (() -> Unit)?,
) {
    val typeLabel = stringResource(item.type.label)
    val doneLabel = stringResource(R.string.step_state_done)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onToggleExpanded == null) Modifier
                else Modifier.clickable(
                    onClickLabel = stringResource(if (expanded) R.string.collapse else R.string.expand),
                    onClick = onToggleExpanded,
                )
            )
            .padding(start = 14.dp, end = 12.dp, top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(colors.accent.copy(alpha = 0.13f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(item.type.icon, contentDescription = null, tint = colors.accent, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
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
            com.example.dhikra.ui.components.Pill(
                text = stringResource(R.string.times_count, ArabicText.toArabicDigits(item.target)),
                color = colors.accent,
                modifier = Modifier.padding(start = 6.dp),
            )
        }
        if (onToggleExpanded != null) {
            val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "chevron")
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = colors.accent.copy(alpha = 0.75f),
                modifier = Modifier.padding(start = 4.dp).size(22.dp).rotate(rotation),
            )
        }
    }
}

/** The entry's description (book, reference, grade...) as a soft tinted panel. */
@Composable
private fun SourceLine(source: String, colors: ElementColors, textScale: Float) {
    Text(
        text = source,
        style = ReadingStyles.hadithMeta(textScale),
        color = colors.ink.copy(alpha = 0.75f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
            .clip(MaterialTheme.shapes.small)
            .background(colors.accent.copy(alpha = 0.07f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
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
            tint = colors.accent.copy(alpha = 0.8f),
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = note,
            style = MaterialTheme.typography.bodySmall,
            color = colors.ink.copy(alpha = 0.75f),
        )
    }
}

/** Counter bar (tap to count) plus the reader and full-screen controls. */
@Composable
private fun RepeatFooter(
    item: ProgramItem,
    done: Int,
    colors: ElementColors,
    playing: Boolean,
    awaitingTap: Boolean,
    onTogglePlay: () -> Unit,
    onCount: () -> Unit,
    onReset: () -> Unit,
    onExpand: () -> Unit,
) {
    val complete = done >= item.target
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CounterBar(
                done = done,
                target = item.target,
                colors = colors,
                onCount = onCount,
                onReset = onReset,
                pulse = awaitingTap,
                modifier = Modifier.weight(1f),
            )
            AnimatedVisibility(visible = !complete) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.width(8.dp))
                    FilledTonalIconButton(
                        onClick = onTogglePlay,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = colors.accent.copy(alpha = 0.14f),
                            contentColor = colors.accent,
                        ),
                        modifier = Modifier.size(48.dp),
                    ) {
                        if (playing) {
                            Icon(painterResource(R.drawable.ic_pause), stringResource(R.string.pause))
                        } else {
                            Icon(Icons.Filled.PlayArrow, stringResource(R.string.play))
                        }
                    }
                    IconButton(onClick = onExpand) {
                        Icon(
                            imageVector = ExpandIcon,
                            contentDescription = stringResource(R.string.expand_counter),
                            tint = colors.accent,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
        AnimatedVisibility(visible = playing) {
            Text(
                text = stringResource(if (awaitingTap) R.string.reader_waiting else R.string.reader_hint),
                style = MaterialTheme.typography.labelMedium,
                color = if (awaitingTap) colors.accent else colors.ink.copy(alpha = 0.65f),
                modifier = Modifier.padding(top = 8.dp, start = 4.dp),
            )
        }
    }
}

/** "Done" bar for single-pass steps; once done, an undo action. */
@Composable
private fun SingleFooter(
    complete: Boolean,
    colors: ElementColors,
    current: Boolean,
    onSetComplete: (Boolean) -> Unit,
) {
    AnimatedContent(targetState = complete, label = "singleFooter") { isComplete ->
        if (isComplete) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { onSetComplete(false) }) {
                    Icon(UndoIcon, null, Modifier.size(18.dp), tint = colors.accent)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.undo_done), color = colors.accent)
                }
            }
        } else {
            DoneBar(colors = colors, emphasized = current, onClick = { onSetComplete(true) })
        }
    }
}
