package com.example.dhikra.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.example.dhikra.R
import com.example.dhikra.data.ArabicText
import com.example.dhikra.data.Program
import com.example.dhikra.data.ProgramSection
import com.example.dhikra.ui.UiState
import com.example.dhikra.ui.components.DhikraHeader
import com.example.dhikra.ui.components.GoldInk
import com.example.dhikra.ui.components.LibraryIcon
import com.example.dhikra.ui.components.PrayerIcon
import com.example.dhikra.ui.components.headerAppBarColors
import com.example.dhikra.ui.components.sectionIcon
import com.example.dhikra.ui.formatTimeArabic
import com.example.dhikra.ui.theme.EmphasizedDecelerate
import com.example.dhikra.ui.theme.LocalDhikraPalette
import com.example.dhikra.ui.theme.Mustard
import com.example.dhikra.ui.theme.MustardLight
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * How far the home header is pulled open: 0 shows the current section, 1 the list of every
 * section of the day. It follows the finger while dragged, then settles open or closed.
 */
@Stable
class HeaderExpansion internal constructor(private val scope: CoroutineScope) {
    /** 0 (closed) … 1 (open). */
    var fraction by mutableFloatStateOf(0f)
        private set

    /** Where the header is going: open or closed. */
    var expanded by mutableStateOf(false)
        private set

    private var animation: Job? = null

    fun open() = animateTo(true)
    fun close() = animateTo(false)
    fun toggle() = animateTo(!expanded)

    private fun animateTo(open: Boolean) {
        expanded = open
        animation?.cancel()
        animation = scope.launch {
            animate(fraction, if (open) 1f else 0f, animationSpec = tween(340, easing = EmphasizedDecelerate)) { value, _ ->
                fraction = value
            }
        }
    }

    /** Follows a drag of [delta] (in fractions of the full opening). */
    fun dragBy(delta: Float) {
        animation?.cancel()
        fraction = (fraction + delta).coerceIn(0f, 1f)
        expanded = fraction > 0f
    }

    /** Settles after a drag, flung by [velocity] (fractions per second) or past half way. */
    fun settle(velocity: Float) = animateTo(
        when {
            velocity > FLING -> true
            velocity < -FLING -> false
            else -> fraction > 0.5f
        }
    )

    private companion object {
        const val FLING = 1.5f
    }
}

@Composable
fun rememberHeaderExpansion(): HeaderExpansion {
    val scope = rememberCoroutineScope()
    return remember(scope) { HeaderExpansion(scope) }
}

/** Height of a section's row in the opened header; the list's height is worked out from it. */
private val SectionRowHeight = 60.dp
private val SectionRowGap = 2.dp
private val ListLabelHeight = 26.dp
private val ListVerticalPadding = 8.dp

/**
 * The home header: the app bar (program switcher and menu) over the current section — its icon,
 * title, time, play button and progress — with a handle underneath. Pulling the header down
 * (or tapping the section or the handle) opens it into the list of the day's sections; the play
 * button and the progress bar give way to that list as it opens.
 *
 * It is drawn over the page, which only keeps room for it closed ([onClosedHeight]), so opening
 * it never moves the schedule.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeHeader(
    state: UiState,
    sections: List<ProgramSection>,
    shownIndex: Int,
    expansion: HeaderExpansion,
    actions: HomeActions,
    onPlay: (stepId: String) -> Unit,
    onClosedHeight: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    DhikraHeader(
        modifier = modifier.onSizeChanged { if (expansion.fraction == 0f) onClosedHeight(it.height) },
        bottomPadding = if (sections.isEmpty()) 4.dp else 0.dp,
    ) {
        TopAppBar(
            title = {
                ProgramSwitcher(
                    programs = state.programs,
                    active = state.program,
                    onSelect = actions.onSelectProgram,
                    onCreate = actions.onCreateProgram,
                )
            },
            actions = { OverflowMenu(actions) },
            expandedHeight = 56.dp,
            windowInsets = WindowInsets(0, 0, 0, 0),
            colors = headerAppBarColors(),
        )
        if (sections.isNotEmpty()) {
            SectionPanel(
                state = state,
                sections = sections,
                shownIndex = shownIndex.coerceIn(sections.indices),
                expansion = expansion,
                onSelect = { id ->
                    actions.onSelectSection(id)
                    expansion.close()
                },
                onPlay = onPlay,
            )
        }
    }
}

/** The lower half of the header: the current section, or every section once pulled open. */
@Composable
private fun SectionPanel(
    state: UiState,
    sections: List<ProgramSection>,
    shownIndex: Int,
    expansion: HeaderExpansion,
    onSelect: (String) -> Unit,
    onPlay: (String) -> Unit,
) {
    val density = LocalDensity.current
    val windowHeight = with(density) { LocalWindowInfo.current.containerSize.height.toDp() }
    // The day in time order, as it is lived; the list never takes more than about half the screen.
    val ordered = remember(sections) {
        sections.withIndex()
            .sortedWith(compareBy({ it.value.reminderMinutes ?: Int.MAX_VALUE }, { it.index }))
            .map { it.value }
    }
    val listHeight = ListLabelHeight + ListVerticalPadding * 2 + (SectionRowHeight + SectionRowGap) * ordered.size
    val maxListHeight = windowHeight * 0.55f
    val fits = listHeight <= maxListHeight
    // A drag over this distance opens the header all the way.
    val dragRange = with(density) { (minOf(listHeight, maxListHeight) - ClosedPanelEstimate).coerceAtLeast(48.dp).toPx() }
    val drag = rememberDraggableState { delta -> expansion.dragBy(delta / dragRange) }
    val fraction = expansion.fraction

    Column(
        Modifier
            .fillMaxWidth()
            .testTag(HeaderPanelTag)
            .draggable(
                state = drag,
                orientation = Orientation.Vertical,
                onDragStopped = { velocity -> expansion.settle(velocity / dragRange) },
            ),
    ) {
        ExpandingLayout(
            fraction = fraction,
            closed = {
                // Gone for screen readers (and taps) once the list has fully taken its place.
                if (fraction < 1f) {
                    CurrentSection(
                        state = state,
                        sections = sections,
                        shownIndex = shownIndex,
                        onOpenList = expansion::open,
                        onPlay = onPlay,
                    )
                }
            },
            open = {
                if (fraction > 0f) {
                    SectionList(
                        state = state,
                        ordered = ordered,
                        selectedId = sections[shownIndex].id,
                        scrollable = !fits,
                        maxHeight = maxListHeight,
                        onSelect = onSelect,
                    )
                }
            },
        )
        Handle(expanded = expansion.expanded, fraction = fraction, onToggle = expansion::toggle)
    }
}

/** Test tag of the header's section panel, the part that is pulled open. */
const val HeaderPanelTag = "headerPanel"

/** Rough height of the closed panel, only used to scale a drag to the opening. */
private val ClosedPanelEstimate = 68.dp

/**
 * Lays [closed] and [open] over each other in a box whose height goes from the closed content's
 * to the open content's with [fraction]; the closed content fades out quickly, then the open one
 * fades in, so the two never read as one.
 */
@Composable
private fun ExpandingLayout(
    fraction: Float,
    closed: @Composable () -> Unit,
    open: @Composable () -> Unit,
) {
    Layout(contents = listOf(closed, open), modifier = Modifier.fillMaxWidth().clipToBounds()) { (closedM, openM), constraints ->
        val loose = constraints.copy(minHeight = 0)
        val closedP = closedM.map { it.measure(loose) }
        val openP = openM.map { it.measure(loose) }
        val closedHeight = closedP.maxOfOrNull { it.height } ?: 0
        val openHeight = openP.maxOfOrNull { it.height } ?: closedHeight
        val height = lerp(closedHeight.toFloat(), max(openHeight, closedHeight).toFloat(), fraction).roundToInt()
        layout(constraints.maxWidth, height) {
            val closedAlpha = (1f - fraction * 2.5f).coerceIn(0f, 1f)
            val openAlpha = ((fraction - 0.25f) / 0.75f).coerceIn(0f, 1f)
            closedP.forEach { it.placeRelativeWithLayer(0, 0) { alpha = closedAlpha } }
            openP.forEach { it.placeRelativeWithLayer(0, 0) { alpha = openAlpha } }
        }
    }
}

/**
 * The section shown in the pager: icon, title (with "now" when it is its time), reminder time
 * and steps done, the play button, and the progress through its steps. It slides along when the
 * user swipes to another section.
 */
@Composable
private fun CurrentSection(
    state: UiState,
    sections: List<ProgramSection>,
    shownIndex: Int,
    onOpenList: () -> Unit,
    onPlay: (String) -> Unit,
) {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    AnimatedContent(
        targetState = sections[shownIndex],
        contentKey = { it.id },
        transitionSpec = {
            // The next section comes in from the end side, as the pager's page does.
            val forward = sections.indexOfFirst { it.id == targetState.id } >
                sections.indexOfFirst { it.id == initialState.id }
            val sign = (if (forward) 1 else -1) * (if (rtl) -1 else 1)
            (slideInHorizontally(tween(320, easing = EmphasizedDecelerate)) { sign * it / 6 } + fadeIn(tween(220, 60))) togetherWith
                (slideOutHorizontally(tween(220)) { -sign * it / 6 } + fadeOut(tween(120)))
        },
        label = "currentSection",
    ) { section ->
        val total = section.trackableItems.size
        val done = state.completedIn(section)
        val nextStep = section.items.firstOrNull { it.type.isTrackable && !state.isComplete(it) }
        Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(MaterialTheme.shapes.medium)
                        .clickable(onClickLabel = stringResource(R.string.sections_show), onClick = onOpenList)
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SectionDisc(section, size = 32.dp)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = section.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Icon(
                        Icons.Filled.ArrowDropDown,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp),
                    )
                    section.reminderMinutes?.let { minutes ->
                        Icon(
                            Icons.Outlined.Notifications,
                            contentDescription = formatTimeArabic(minutes),
                            tint = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.padding(start = 4.dp).size(15.dp),
                        )
                    }
                    if (section.id == state.nowSectionId) NowChip()
                }
                Spacer(Modifier.width(8.dp))
                when {
                    nextStep != null -> PlayButton(started = done > 0, title = section.title) { onPlay(nextStep.id) }
                    total > 0 -> DoneMark()
                }
            }
            Spacer(Modifier.height(8.dp))
            StepProgressBar(done = done, total = total)
        }
    }
}

/**
 * The section's progress as one bar split into a segment per step: finished steps in mustard,
 * the rest as a faint track. A section with too many steps for the dividers to read gets one
 * continuous bar instead.
 */
@Composable
private fun StepProgressBar(done: Int, total: Int, modifier: Modifier = Modifier) {
    val target = if (total == 0) 0f else done.toFloat() / total
    val progress by animateFloatAsState(target, tween(500), label = "sectionProgress")
    val description = stringResource(R.string.steps_done, ArabicText.toArabicDigits(done), ArabicText.toArabicDigits(total))
    val fill = Mustard
    val track = Color.White.copy(alpha = 0.18f)
    Canvas(
        modifier
            .fillMaxWidth()
            .height(5.dp)
            .semantics {
                contentDescription = description
                progressBarRangeInfo = ProgressBarRangeInfo(done.toFloat(), 0f..total.coerceAtLeast(1).toFloat())
            },
    ) {
        val h = size.height
        val radius = CornerRadius(h / 2f)
        val gap = 2.dp.toPx()
        val rtl = layoutDirection == LayoutDirection.Rtl
        // Draws [from, to) along the bar, measured from its reading start.
        fun bar(from: Float, to: Float, color: Color) {
            if (to <= from) return
            val left = if (rtl) size.width - to else from
            drawRoundRect(color, Offset(left, 0f), Size(to - from, h), radius)
        }
        val segments = total.coerceAtLeast(1)
        val segment = (size.width - gap * (segments - 1)) / segments
        if (segments == 1 || segment < 6.dp.toPx()) {
            bar(0f, size.width, track)
            bar(0f, size.width * progress, fill)
            return@Canvas
        }
        val filled = progress * segments
        for (i in 0 until segments) {
            val start = i * (segment + gap)
            bar(start, start + segment, track)
            val part = (filled - i).coerceIn(0f, 1f)
            if (part > 0f) bar(start, start + segment * part, fill)
        }
    }
}

/** The section's icon on a soft light disc, for the dark header. */
@Composable
private fun SectionDisc(section: ProgramSection, size: Dp) {
    Box(
        modifier = Modifier.size(size).clip(CircleShape).background(Color.White.copy(alpha = 0.13f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(sectionIcon(section.icon), contentDescription = null, tint = Color.White, modifier = Modifier.size(size * 0.54f))
    }
}

/** Reminder time and steps done, e.g. "٥:١٥ ص  •  ٣ من ٨ خطوات". */
@Composable
private fun SectionMeta(section: ProgramSection, done: Int, total: Int) {
    val color = Color.White.copy(alpha = 0.72f)
    Row(verticalAlignment = Alignment.CenterVertically) {
        section.reminderMinutes?.let { minutes ->
            Icon(Icons.Outlined.Notifications, contentDescription = null, tint = color, modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(4.dp))
            Text(formatTimeArabic(minutes), style = MaterialTheme.typography.labelMedium, color = color)
            Text("  •  ", style = MaterialTheme.typography.labelMedium, color = color.copy(alpha = 0.5f))
        }
        Text(
            text = stringResource(R.string.steps_done, ArabicText.toArabicDigits(done), ArabicText.toArabicDigits(total)),
            style = MaterialTheme.typography.labelMedium,
            color = color,
            maxLines = 1,
        )
    }
}

/** "الآن": the section whose time of day it is. */
@Composable
private fun NowChip() {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondary,
        contentColor = MaterialTheme.colorScheme.onSecondary,
        modifier = Modifier.padding(start = 6.dp),
    ) {
        Text(
            text = stringResource(R.string.now),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 1.dp),
        )
    }
}

/**
 * Reads the section in the full-screen reader, from its first unfinished step: a quiet tonal disc
 * on the header, so it does not compete with the schedule.
 */
@Composable
private fun PlayButton(started: Boolean, title: String, onClick: () -> Unit) {
    val description = stringResource(if (started) R.string.continue_now_section else R.string.start_now_section, title)
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Color.White.copy(alpha = 0.12f),
        contentColor = MustardLight,
        modifier = Modifier.size(32.dp).semantics { contentDescription = description },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
        }
    }
}

/** In place of the play button once every step of the section is done. */
@Composable
private fun DoneMark() {
    val description = stringResource(R.string.section_finished)
    Box(
        modifier = Modifier.size(32.dp).semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        SectionProgress(1f, Color.White)
    }
}

/** The day's sections, in time order, inside the opened header. Tapping one shows it. */
@Composable
private fun SectionList(
    state: UiState,
    ordered: List<ProgramSection>,
    selectedId: String,
    scrollable: Boolean,
    maxHeight: Dp,
    onSelect: (String) -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .heightIn(max = maxHeight)
            .verticalScroll(rememberScrollState(), enabled = scrollable)
            .padding(horizontal = 10.dp, vertical = ListVerticalPadding),
        verticalArrangement = Arrangement.spacedBy(SectionRowGap),
    ) {
        Text(
            text = stringResource(R.string.day_sections),
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.6f),
            modifier = Modifier.height(ListLabelHeight - SectionRowGap).padding(start = 8.dp, top = 2.dp),
        )
        ordered.forEach { section ->
            SectionRow(
                section = section,
                selected = section.id == selectedId,
                now = section.id == state.nowSectionId,
                done = state.completedIn(section),
                onClick = { onSelect(section.id) },
            )
        }
    }
}

@Composable
private fun SectionRow(section: ProgramSection, selected: Boolean, now: Boolean, done: Int, onClick: () -> Unit) {
    val total = section.trackableItems.size
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(SectionRowHeight)
            .clip(MaterialTheme.shapes.large)
            .background(if (selected) Color.White.copy(alpha = 0.14f) else Color.Transparent)
            .semantics { this.selected = selected }
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SectionDisc(section, size = 38.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = section.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (now) NowChip()
            }
            SectionMeta(section, done, total)
        }
        Spacer(Modifier.width(10.dp))
        SectionProgress(if (total == 0) 0f else done.toFloat() / total, Color.White)
    }
}

/** A small progress ring, or a mustard check once the section is finished. */
@Composable
internal fun SectionProgress(fraction: Float, color: Color) {
    val palette = LocalDhikraPalette.current
    if (fraction >= 1f) {
        Box(
            modifier = Modifier.size(20.dp).clip(CircleShape).background(palette.ornament),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Check, null, tint = GoldInk, modifier = Modifier.size(13.dp))
        }
    } else {
        CircularProgressIndicator(
            progress = { fraction },
            modifier = Modifier.size(18.dp),
            color = color,
            trackColor = color.copy(alpha = 0.25f),
            strokeWidth = 2.5.dp,
            strokeCap = StrokeCap.Round,
            gapSize = 0.dp,
        )
    }
}

/**
 * The grab handle under the panel: pulling the header is its main gesture, tapping it does the
 * same for those who cannot drag (and for screen readers).
 */
@Composable
private fun Handle(expanded: Boolean, fraction: Float, onToggle: () -> Unit) {
    val label = stringResource(if (expanded) R.string.sections_hide else R.string.sections_show)
    val state = stringResource(if (expanded) R.string.sections_shown else R.string.sections_hidden)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(14.dp)
            .clickable(onClickLabel = label, role = Role.Button, onClick = onToggle)
            .semantics {
                contentDescription = label
                stateDescription = state
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(width = 32.dp, height = 3.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.32f + 0.2f * fraction)),
        )
    }
}

/**
 * The program's name in the app bar: it drops down the user's programs, to switch to another one,
 * and the action that adds a new program.
 */
@Composable
private fun ProgramSwitcher(
    programs: List<Program>,
    active: Program,
    onSelect: (String) -> Unit,
    onCreate: () -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    val defaultName = stringResource(R.string.default_program_name)
    Box {
        Row(
            modifier = Modifier
                .clip(MaterialTheme.shapes.small)
                .clickable(onClickLabel = stringResource(R.string.programs)) { open = true }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = active.name.ifBlank { defaultName },
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            programs.forEach { program ->
                val current = program.id == active.id
                DropdownMenuItem(
                    text = { Text(program.name.ifBlank { defaultName }, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    leadingIcon = {
                        if (current) Icon(Icons.Filled.Check, contentDescription = null)
                        else Spacer(Modifier.size(24.dp))
                    },
                    onClick = {
                        open = false
                        onSelect(program.id)
                    },
                    modifier = Modifier.semantics { selected = current },
                )
            }
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text(stringResource(R.string.new_program)) },
                leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null) },
                onClick = {
                    open = false
                    onCreate()
                },
            )
        }
    }
}

@Composable
private fun OverflowMenu(actions: HomeActions) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Filled.MoreVert, stringResource(R.string.action_more))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            @Composable
            fun entry(text: Int, icon: @Composable () -> Unit, onClick: () -> Unit) =
                DropdownMenuItem(
                    text = { Text(stringResource(text)) },
                    leadingIcon = icon,
                    onClick = { open = false; onClick() },
                )
            entry(R.string.action_edit, { Icon(Icons.Outlined.Edit, null) }, actions.onEdit)
            entry(R.string.prayer_times, { Icon(PrayerIcon, null) }, actions.onOpenPrayerTimes)
            entry(R.string.library, { Icon(LibraryIcon, null) }, actions.onOpenLibrary)
            entry(R.string.settings, { Icon(Icons.Outlined.Settings, null) }, actions.onOpenSettings)
            HorizontalDivider()
            entry(R.string.action_text_size, { Text("أ", style = MaterialTheme.typography.titleMedium) }, actions.onTextSize)
            entry(R.string.action_reset_today, { Icon(Icons.Outlined.Refresh, null) }, actions.onResetToday)
        }
    }
}
