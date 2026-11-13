package com.example.dhikra.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.dhikra.R
import com.example.dhikra.data.ItemType
import com.example.dhikra.data.ProgramItem
import com.example.dhikra.data.ProgramSection
import com.example.dhikra.ui.UiState
import com.example.dhikra.ui.components.DhikraLogo
import com.example.dhikra.ui.components.HeadingNode
import com.example.dhikra.ui.components.HeadingNodeSize
import com.example.dhikra.ui.components.StepNode
import com.example.dhikra.ui.components.SectionNoticeCard
import com.example.dhikra.ui.components.StepNodeSize
import com.example.dhikra.ui.components.StepState
import com.example.dhikra.ui.components.TimelineRow
import com.example.dhikra.ui.components.TimelineSegments
import com.example.dhikra.ui.components.TimelineSide
import com.example.dhikra.ui.components.addEightPointStar
import com.example.dhikra.ui.theme.EmphasizedAccelerate
import com.example.dhikra.ui.theme.EmphasizedDecelerate
import com.example.dhikra.ui.theme.LocalDhikraPalette
import kotlin.math.absoluteValue
import androidx.compose.runtime.CompositionLocalProvider
import com.example.dhikra.ui.components.LocalSectionComplete

/** Everything the home screen can ask for. */
class HomeActions(
    val onSelectSection: (String) -> Unit,
    val onSelectProgram: (String) -> Unit,
    val onCreateProgram: () -> Unit,
    val onEdit: () -> Unit,
    /** Opens one of the library's collections (the library is a tab of the day view). */
    val onOpenCollection: (ItemType) -> Unit,
    val onOpenSettings: () -> Unit,
    val onTextSize: () -> Unit,
    val onResetToday: () -> Unit,
    val onIncrement: (ProgramItem) -> Unit,
    val onDecrement: (ProgramItem) -> Unit,
    val onRepetitionRead: (ProgramItem, Long) -> Unit,
    val onSetComplete: (ProgramItem, Boolean) -> Unit,
    val onResetItem: (ProgramItem) -> Unit,
    val onOpenPrayerTimes: () -> Unit = {},
    /** Opens the editor on one section only. */
    val onEditSection: (String) -> Unit = {},
    /** The daily counter: open one dhikr's counter, add a dhikr (library entry id), take one off. */
    val onOpenCounter: (String) -> Unit = {},
    val onAddCounter: (String) -> Unit = {},
    val onRemoveCounter: (String) -> Unit = {},
    /** The favourites ([com.example.dhikra.data.Favourites] references): open one in the reader, mark or unmark one. */
    val onOpenFavourite: (String) -> Unit = {},
    val onToggleFavourite: (String) -> Unit = {},
    /** Editing from the section view (a step held down): the program, a library text or group. */
    val onEditProgram: ((com.example.dhikra.data.Program) -> com.example.dhikra.data.Program) -> Unit = {},
    /** The program as a whole, from the program tab: a copy of it, sharing or exporting it, deleting it (null when it is the only one). */
    val onDuplicateProgram: () -> Unit = {},
    val onShareProgram: () -> Unit = {},
    val onExportProgram: () -> Unit = {},
    val onDeleteProgram: (() -> Unit)? = null,
    val onSaveEntry: (com.example.dhikra.data.LibraryEntry) -> Unit = {},
    val onSaveGroup: (com.example.dhikra.data.LibraryGroup) -> Unit = {},
    /** Saves a section edited in the program tab: the section and the library texts and groups it changed, at once. */
    val onSaveSectionEdit: (com.example.dhikra.data.SectionEdit) -> Unit = {},
)

private const val KEY_NOTE = "note"
private const val KEY_EMPTY = "empty"
private const val KEY_COMPLETE = "complete"

/**
 * Task-then-step home: the branded header shows the task (section) being looked at — its
 * progress and a play button — and pulls down into the list of the day's tasks; each page shows
 * the task's steps on a vertical timeline. Tapping a step opens the full-screen reader at it,
 * which goes through the steps of the task with a swipe up.
 */
@Composable
fun HomeScreen(
    state: UiState,
    actions: HomeActions,
    snackbarHostState: SnackbarHostState,
    /** Back to the day view (the header's back button). */
    onBack: () -> Unit = {},
) {
    // Another program is another set of tasks: its pages, tabs and scroll positions start afresh.
    key(state.program.id) { ProgramHome(state, actions, snackbarHostState, onBack) }
}

@Composable
private fun ProgramHome(
    state: UiState,
    actions: HomeActions,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
) {
    val sections = state.schedule.sections
    val selectedIndex = sections.indexOfFirst { it.id == state.selectedSectionId }.coerceAtLeast(0)
    // Step the full-screen reader was opened from (null = reader closed).
    var readerStartId by rememberSaveable { mutableStateOf<String?>(null) }
    // The step the reader was closed on: the list scrolls to it so the user finds their place.
    var revealStepId by remember { mutableStateOf<String?>(null) }
    val readerSection = readerStartId?.let { id -> sectionOf(sections, id) }
    KeepScreenOn(readerSection != null)

    val pagerState = rememberPagerState(initialPage = selectedIndex) { sections.size }
    // Height of the closed header: the page keeps exactly that much room for it. Until the
    // header has been measured, a close estimate keeps the first frame from jumping.
    val density = LocalDensity.current
    val statusBar = WindowInsets.statusBars.getTop(density)
    var headerHeight by remember {
        mutableIntStateOf(statusBar + with(density) { 80.dp.roundToPx() })
    }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            topBar = { Spacer(Modifier.fillMaxWidth().height(with(density) { headerHeight.toDp() })) },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            // Reading the section shown: the screen's main action, floating in the bottom corner.
            floatingActionButton = {
                sections.getOrNull(pagerState.targetPage)?.let { section ->
                    SectionPlayButton(section, state, onPlay = { stepId -> readerStartId = stepId })
                }
            },
            containerColor = MaterialTheme.colorScheme.background,
        ) { padding ->
            if (sections.isEmpty()) {
                EmptyProgram(actions.onEdit, Modifier.padding(padding))
                return@Scaffold
            }

            val currentSections by rememberUpdatedState(sections)
            LaunchedEffect(selectedIndex) {
                if (pagerState.currentPage != selectedIndex) pagerState.animateScrollToPage(selectedIndex)
            }
            LaunchedEffect(pagerState) {
                // The first page is not a change: a reader restored after rotation must stay open.
                var initial = true
                snapshotFlow { pagerState.settledPage }.collect { page ->
                    val shown = currentSections.getOrNull(page)
                    shown?.let { actions.onSelectSection(it.id) }
                    // Leaving a task closes its reader; one opened on the task arrived at stays.
                    val reading = readerStartId
                    if (initial) {
                        initial = false
                    } else if (reading != null && (shown == null || sectionOf(listOf(shown), reading) == null)) {
                        readerStartId = null
                    }
                }
            }

            val layoutDirection = LocalLayoutDirection.current
            HorizontalPager(
                state = pagerState,
                key = { sections[it].id },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        top = padding.calculateTopPadding(),
                        start = padding.calculateStartPadding(layoutDirection),
                        end = padding.calculateEndPadding(layoutDirection),
                    )
                    .consumeWindowInsets(padding),
            ) { page ->
                SectionPage(
                    section = sections[page],
                    nextSection = nextUnfinishedSection(sections, page, state),
                    state = state,
                    // Room under the last step for the floating play button.
                    bottomInset = padding.calculateBottomPadding() + 80.dp,
                    actions = actions,
                    onOpenReader = { stepId -> readerStartId = stepId },
                    revealStepId = revealStepId?.takeIf { id -> sections[page].items.any { it.id == id } },
                    onRevealed = { revealStepId = null },
                    modifier = Modifier.graphicsLayer {
                        // Pages ease back and fade a little while swiping between tasks.
                        val offset = pagerState.getOffsetDistanceInPages(page).absoluteValue.coerceIn(0f, 1f)
                        alpha = 1f - 0.4f * offset
                        val scale = 1f - 0.05f * offset
                        scaleX = scale
                        scaleY = scale
                    },
                )
            }
        }

        HomeHeader(
            state = state,
            sections = sections,
            // The section the pager is on, or heading to: the header follows a swipe at once.
            shownIndex = if (sections.isEmpty()) 0 else pagerState.targetPage,
            onBack = onBack,
            onHeight = { headerHeight = it },
        )

        // Full-screen reader: grows out of the schedule and shrinks back into it.
        AnimatedContent(
            targetState = readerStartId?.takeIf { readerSection != null },
            transitionSpec = {
                val transform = if (targetState != null) {
                    (fadeIn(tween(220)) + scaleIn(tween(320, easing = EmphasizedDecelerate), initialScale = 0.9f))
                        .togetherWith(fadeOut(tween(90)))
                } else {
                    EnterTransition.None togetherWith
                        (fadeOut(tween(200)) + scaleOut(tween(220, easing = EmphasizedAccelerate), targetScale = 0.94f))
                }
                // Only fade and scale are animated; the container itself snaps to its new size.
                transform using SizeTransform(clip = false) { _, _ -> snap() }
            },
            modifier = Modifier.fillMaxSize(),
            label = "readerOverlay",
        ) { stepId ->
            val section = stepId?.let { id -> sectionOf(sections, id) }
            if (stepId != null && section != null) {
                StepReader(
                    section = section,
                    startId = stepId,
                    state = state,
                    actions = actions,
                    onClose = { lastStepId ->
                        readerStartId = null
                        revealStepId = lastStepId
                    },
                )
            }
        }
    }
}

/** The task holding the step (or the text of a group step) with this [id]. */
private fun sectionOf(sections: List<ProgramSection>, id: String): ProgramSection? =
    sections.firstOrNull { section ->
        section.items.any { step -> step.id == id || step.members.any { it.id == id } }
    }

/** The next task after [page] that still has unfinished steps (wrapping around), if any. */
private fun nextUnfinishedSection(sections: List<ProgramSection>, page: Int, state: UiState): ProgramSection? {
    val unfinished = { s: ProgramSection -> s.trackableItems.isNotEmpty() && state.completedIn(s) < s.trackableItems.size }
    return (sections.drop(page + 1) + sections.take(page)).firstOrNull(unfinished)
}

// ------------------------------------------------------------------------------ Page

@Composable
private fun SectionPage(
    section: ProgramSection,
    nextSection: ProgramSection?,
    state: UiState,
    bottomInset: Dp,
    actions: HomeActions,
    onOpenReader: (String) -> Unit,
    revealStepId: String?,
    onRevealed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val items = section.items
    val total = section.trackableItems.size
    val done = state.completedIn(section)
    val allDone = total > 0 && done == total
    val listState = rememberLazyListState()
    // A step held down: what can be done with it (edit it, move it to another section).
    var heldStep by remember { mutableStateOf<ProgramItem?>(null) }
    heldStep?.let { held -> StepActions(section, held, state, actions, onDone = { heldStep = null }) }

    // The current step is the first unfinished one; the timeline is "reached" up to it.
    val currentStepId = items.firstOrNull { it.type.isTrackable && !state.isComplete(it) }?.id
    val currentIndex = items.indexOfFirst { it.id == currentStepId }.let { if (it < 0) items.size else it }
    val stepNumbers = remember(items) {
        var number = 0
        items.map { if (it.type.isTrackable) ++number else 0 }
    }

    // Coming back from the reader, bring the step it was on into view, a quarter of the way down.
    val headerCount = (if (section.note.isNotBlank()) 1 else 0) + section.notices.size + (if (items.isEmpty()) 1 else 0)
    LaunchedEffect(revealStepId) {
        val index = items.indexOfFirst { it.id == revealStepId }
        if (index >= 0) {
            val layout = listState.layoutInfo
            val shown = layout.visibleItemsInfo.firstOrNull { it.index == headerCount + index }
            val fullyVisible = shown != null && shown.offset >= layout.viewportStartOffset &&
                shown.offset + shown.size <= layout.viewportEndOffset - layout.afterContentPadding
            if (!fullyVisible) {
                listState.animateScrollToItem(headerCount + index, scrollOffset = -layout.viewportSize.height / 4)
            }
        }
        if (revealStepId != null) onRevealed()
    }

    // On large screens the schedule keeps a comfortable reading width, centred on the page.
    val windowWidth = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.width.toDp() }
    val side = ((windowWidth - MaxContentWidth) / 2).coerceAtLeast(0.dp)
    val timelineOnStart = ScheduleTimelineSide == TimelineSide.Start
    // The summary and the note line up with the cards' edge on the timeline's side.
    val alongTimeline = if (timelineOnStart) PaddingValues(start = 8.dp) else PaddingValues(end = 8.dp)
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize().testTag(ScheduleListTag),
        contentPadding = PaddingValues(
            // The gutter brings its own room: the page margin is smaller on the timeline's side.
            start = side + if (timelineOnStart) 8.dp else 16.dp,
            end = side + if (timelineOnStart) 16.dp else 8.dp,
            top = 16.dp,
            bottom = bottomInset + 32.dp,
        ),
    ) {
        if (section.note.isNotBlank()) {
            item(key = KEY_NOTE) { SectionNote(section.note, Modifier.padding(alongTimeline).padding(bottom = 16.dp)) }
        }
        // The section's messages (information, warnings, what matters), above its steps.
        section.notices.forEachIndexed { index, notice ->
            item(key = "notice/$index") {
                SectionNoticeCard(notice, Modifier.padding(alongTimeline).padding(bottom = 10.dp))
            }
        }
        if (items.isEmpty()) {
            item(key = KEY_EMPTY) {
                Text(
                    text = stringResource(R.string.empty_section),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                )
            }
        }
        itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
            val segments = TimelineSegments(
                hasTop = index > 0,
                hasBottom = index < items.lastIndex,
                topReached = index <= currentIndex,
                bottomReached = index + 1 <= currentIndex,
            )
            val heading = item.type == ItemType.HEADING
            val stepState = when {
                state.isComplete(item) -> StepState.Done
                item.id == currentStepId -> StepState.Current
                else -> StepState.Upcoming
            }
            CompositionLocalProvider(LocalSectionComplete provides allDone) {
            TimelineRow(
                segments = segments,
                nodeCenter = item.nodeCenter,
                nodeSize = if (heading) HeadingNodeSize else StepNodeSize,
                node = { if (heading) HeadingNode() else StepNode(stepNumbers[index], stepState) },
                modifier = Modifier.animateItem(),
                side = ScheduleTimelineSide,
            ) {
                ProgramItemView(
                    item = item,
                    done = state.doneCount(item),
                    textScale = state.textScale,
                    onOpen = { onOpenReader(item.id) },
                    onSetComplete = { actions.onSetComplete(item, it) },
                    current = stepState == StepState.Current,
                    onOpenText = onOpenReader,
                    textDone = state::doneCount,
                    onLongPress = { heldStep = it },
                )
            }
            }
        }
        if (allDone) {
            item(key = KEY_COMPLETE) {
                SectionComplete(section, nextSection, onGoTo = actions.onSelectSection, modifier = Modifier.animateItem())
            }
        }
    }
}

private val MaxContentWidth = 720.dp

/**
 * Side of the schedule the step timeline runs along: the start (the right, in Arabic). Switch to
 * [TimelineSide.End] to put it on the other side.
 */
private val ScheduleTimelineSide = TimelineSide.Start

/** Test tag of a schedule page's step list. */
const val ScheduleListTag = "schedule"

@Composable
private fun SectionNote(note: String, modifier: Modifier = Modifier) {
    // A plain, faded line rather than a coloured box: it explains the task without competing
    // with its steps.
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    Row(modifier = modifier.fillMaxWidth().padding(horizontal = 6.dp)) {
        Icon(
            imageVector = Icons.Outlined.Info,
            contentDescription = stringResource(R.string.note_label),
            tint = color.copy(alpha = 0.45f),
            modifier = Modifier.padding(top = 2.dp).size(15.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(note, style = MaterialTheme.typography.bodySmall, color = color.copy(alpha = FaintTextAlpha))
    }
}

/**
 * Closes a finished task under its last step: a mustard star with a check, the blessing and the
 * shortcut to the next unfinished task, centred on the page without a card around them, so it
 * reads as the schedule's quiet ending rather than one more step.
 */
@Composable
private fun SectionComplete(
    section: ProgramSection,
    next: ProgramSection?,
    onGoTo: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalDhikraPalette.current
    val scheme = MaterialTheme.colorScheme
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) { appear.animateTo(1f, tween(800, easing = EmphasizedDecelerate)) }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 28.dp, bottom = 8.dp)
            .graphicsLayer {
                alpha = (appear.value * 1.6f).coerceAtMost(1f)
                translationY = (1f - appear.value) * 12.dp.toPx()
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CompletionMark(progress = appear.value, color = palette.ornament, ink = palette.ornamentText)
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.section_complete),
            style = MaterialTheme.typography.titleMedium,
            color = palette.ornamentText,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.section_complete_message, section.title),
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant.copy(alpha = FaintTextAlpha),
            textAlign = TextAlign.Center,
        )
        if (next != null) {
            Spacer(Modifier.height(12.dp))
            FilledTonalButton(
                onClick = { onGoTo(next.id) },
                shape = CircleShape,
                contentPadding = PaddingValues(start = 18.dp, end = 14.dp),
                modifier = Modifier.height(38.dp),
            ) {
                Text(
                    text = stringResource(R.string.next_section, next.title),
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.width(6.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, Modifier.size(16.dp))
            }
        }
    }
}

/**
 * An eight-pointed star outlined in mustard with a soft fill, turning into place while its check
 * is drawn in, stroke by stroke, as the ending appears.
 */
@Composable
private fun CompletionMark(progress: Float, color: Color, ink: Color) {
    Canvas(Modifier.size(44.dp)) {
        val radius = size.minDimension / 2f
        val star = Path().apply { addEightPointStar(center, radius * 0.96f, innerRatio = 0.8f) }
        rotate(degrees = -45f * (1f - progress)) {
            drawPath(star, color.copy(alpha = 0.16f))
            drawPath(star, color, style = Stroke(width = 1.5.dp.toPx(), join = StrokeJoin.Round))
        }
        // The check, drawn from its short stroke to the end of its long one.
        val a = Offset(center.x - radius * 0.30f, center.y + radius * 0.02f)
        val b = Offset(center.x - radius * 0.08f, center.y + radius * 0.24f)
        val c = Offset(center.x + radius * 0.32f, center.y - radius * 0.22f)
        val first = (a - b).getDistance()
        val second = (c - b).getDistance()
        val drawn = ((progress - 0.35f) / 0.65f).coerceIn(0f, 1f) * (first + second)
        val check = Path().apply {
            moveTo(a.x, a.y)
            if (drawn <= first) {
                val t = drawn / first
                lineTo(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t)
            } else {
                lineTo(b.x, b.y)
                val t = (drawn - first) / second
                lineTo(b.x + (c.x - b.x) * t, b.y + (c.y - b.y) * t)
            }
        }
        if (drawn > 0f) {
            drawPath(check, ink, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

@Composable
private fun EmptyProgram(onEdit: () -> Unit, modifier: Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        DhikraLogo(size = 88.dp)
        Spacer(Modifier.height(20.dp))
        Text(stringResource(R.string.empty_program), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.empty_program_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 320.dp),
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onEdit, shape = CircleShape) {
            Icon(Icons.Filled.Add, null)
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.add_section))
        }
    }
}

/** Prevents the screen from sleeping while the reader effect or the counter is running. */
@Composable
private fun KeepScreenOn(enabled: Boolean) {
    val view = LocalView.current
    DisposableEffect(view, enabled) {
        view.keepScreenOn = enabled
        onDispose { view.keepScreenOn = false }
    }
}
