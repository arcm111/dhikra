package com.example.dhikra.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
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
import com.example.dhikra.ui.theme.MustardLight
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

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
 * title, reminder bell and play button — with a handle underneath. Pulling the header down
 * (or tapping the section or the handle) opens it into the list of the day's sections; the play
 * button gives way to that list as it opens.
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
            // The section view edits the section shown, not the whole program.
            actions = {
                OverflowMenu(
                    actions = actions,
                    editSection = sections.getOrNull(shownIndex)?.let { section -> { actions.onEditSection(section.id) } },
                )
            },
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
                        handle = { Handle(expanded = expansion.expanded, fraction = fraction, onToggle = expansion::toggle) },
                    )
                }
            },
            open = {
                if (fraction > 0f) {
                    Column {
                        SectionList(
                            state = state,
                            ordered = ordered,
                            selectedId = sections[shownIndex].id,
                            scrollable = !fits,
                            maxHeight = maxListHeight,
                            onSelect = onSelect,
                        )
                        Handle(expanded = expansion.expanded, fraction = fraction, onToggle = expansion::toggle)
                    }
                }
            },
        )
    }
}

/** Shape of the panel that holds the current section in the header. */
private val SectionPanelShape = RoundedCornerShape(26.dp)

/**
 * Makes a (clipped) panel look pressed into the header rather than raised from it: a slightly
 * darker well, a soft shadow falling in from its top edge, and a rim that is dark at the top and
 * catches the light at the bottom.
 */
private fun Modifier.recessed(shape: Shape): Modifier = this
    .background(Color.Black.copy(alpha = 0.14f))
    .drawBehind {
        drawRect(
            Brush.verticalGradient(
                listOf(Color.Black.copy(alpha = 0.22f), Color.Transparent),
                endY = 10.dp.toPx(),
            )
        )
    }
    .border(
        width = 1.dp,
        brush = Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.28f), Color.White.copy(alpha = 0.14f))),
        shape = shape,
    )

/** Test tag of the header's section panel, the part that is pulled open. */
const val HeaderPanelTag = "headerPanel"

/** Rough height of the closed panel, only used to scale a drag to the opening. */
private val ClosedPanelEstimate = 74.dp

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
 * The section shown in the pager: icon, title (with "now" when it is its time) and reminder bell,
 * and the play button (a check once every step is done). It slides along when the
 * user swipes to another section.
 */
@Composable
private fun CurrentSection(
    state: UiState,
    sections: List<ProgramSection>,
    shownIndex: Int,
    onOpenList: () -> Unit,
    onPlay: (String) -> Unit,
    handle: @Composable () -> Unit,
) {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    // The section sits in a soft translucent panel of the header, its handle at the bottom; tapping
    // the panel opens the day. The panel stays put while its content slides to another section.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, bottom = 10.dp)
            .clip(SectionPanelShape)
            .recessed(SectionPanelShape)
            .clickable(onClickLabel = stringResource(R.string.sections_show), onClick = onOpenList),
    ) {
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SectionDisc(section, size = 32.dp)
                Spacer(Modifier.width(10.dp))
                // Title, bell and "now" keep together; the play button stays at the end.
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = section.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    section.reminderMinutes?.let { minutes ->
                        Icon(
                            Icons.Outlined.Notifications,
                            contentDescription = formatTimeArabic(minutes),
                            tint = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.padding(start = 6.dp).size(15.dp),
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
        }
        handle()
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
internal fun NowChip() {
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
internal fun ProgramSwitcher(
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
internal fun OverflowMenu(actions: HomeActions, editSection: (() -> Unit)? = null) {
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
            if (editSection != null) {
                entry(R.string.edit_this_section, { Icon(Icons.Outlined.Edit, null) }, editSection)
            } else {
                entry(R.string.action_edit, { Icon(Icons.Outlined.Edit, null) }, actions.onEdit)
            }
            entry(R.string.prayer_times, { Icon(PrayerIcon, null) }, actions.onOpenPrayerTimes)
            entry(R.string.library, { Icon(LibraryIcon, null) }, actions.onOpenLibrary)
            entry(R.string.settings, { Icon(Icons.Outlined.Settings, null) }, actions.onOpenSettings)
            HorizontalDivider()
            entry(R.string.action_text_size, { Text("أ", style = MaterialTheme.typography.titleMedium) }, actions.onTextSize)
            entry(R.string.action_reset_today, { Icon(Icons.Outlined.Refresh, null) }, actions.onResetToday)
        }
    }
}
