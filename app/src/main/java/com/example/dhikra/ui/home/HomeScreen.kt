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
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import com.example.dhikra.ui.components.FinishNode
import com.example.dhikra.ui.components.HeadingNode
import com.example.dhikra.ui.components.HeadingNodeSize
import com.example.dhikra.ui.components.StepNode
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
import kotlin.math.cos
import kotlin.math.sin

/** Everything the home screen can ask for. */
class HomeActions(
    val onSelectSection: (String) -> Unit,
    val onSelectProgram: (String) -> Unit,
    val onCreateProgram: () -> Unit,
    val onEdit: () -> Unit,
    val onOpenLibrary: () -> Unit,
    val onOpenSettings: () -> Unit,
    val onTextSize: () -> Unit,
    val onResetToday: () -> Unit,
    val onIncrement: (ProgramItem) -> Unit,
    val onDecrement: (ProgramItem) -> Unit,
    val onRepetitionRead: (ProgramItem, Long) -> Unit,
    val onSetComplete: (ProgramItem, Boolean) -> Unit,
    val onResetItem: (ProgramItem) -> Unit,
    val onOpenPrayerTimes: () -> Unit = {},
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
) {
    // Another program is another set of tasks: its pages, tabs and scroll positions start afresh.
    key(state.program.id) { ProgramHome(state, actions, snackbarHostState) }
}

@Composable
private fun ProgramHome(
    state: UiState,
    actions: HomeActions,
    snackbarHostState: SnackbarHostState,
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
    val expansion = rememberHeaderExpansion()
    // Height of the closed header: the page keeps exactly that much room for it. Until the
    // header has been measured, a close estimate keeps the first frame from jumping.
    val density = LocalDensity.current
    val statusBar = WindowInsets.statusBars.getTop(density)
    var headerHeight by remember {
        mutableIntStateOf(statusBar + with(density) { (if (sections.isEmpty()) 60.dp else 124.dp).roundToPx() })
    }
    BackHandler(enabled = expansion.expanded) { expansion.close() }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            topBar = { Spacer(Modifier.fillMaxWidth().height(with(density) { headerHeight.toDp() })) },
            snackbarHost = { SnackbarHost(snackbarHostState) },
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
                    bottomInset = padding.calculateBottomPadding(),
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

        // While the header is pulled open, the page dims behind it; a tap there closes it.
        val fraction = expansion.fraction
        if (fraction > 0f) {
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = fraction }
                    .background(Color.Black.copy(alpha = 0.38f))
                    .pointerInput(expansion) { detectTapGestures { expansion.close() } },
            )
        }

        HomeHeader(
            state = state,
            sections = sections,
            // The section the pager is on, or heading to: the header follows a swipe at once.
            shownIndex = if (sections.isEmpty()) 0 else pagerState.targetPage,
            expansion = expansion,
            actions = actions,
            onPlay = { stepId -> readerStartId = stepId },
            onClosedHeight = { headerHeight = it },
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

    // The current step is the first unfinished one; the timeline is "reached" up to it.
    val currentStepId = items.firstOrNull { it.type.isTrackable && !state.isComplete(it) }?.id
    val currentIndex = items.indexOfFirst { it.id == currentStepId }.let { if (it < 0) items.size else it }
    val stepNumbers = remember(items) {
        var number = 0
        items.map { if (it.type.isTrackable) ++number else 0 }
    }

    // Coming back from the reader, bring the step it was on into view, a quarter of the way down.
    val headerCount = (if (section.note.isNotBlank()) 1 else 0) + (if (items.isEmpty()) 1 else 0)
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
                hasBottom = index < items.lastIndex || allDone,
                topReached = index <= currentIndex,
                bottomReached = index + 1 <= currentIndex,
            )
            val heading = item.type == ItemType.HEADING
            val stepState = when {
                state.isComplete(item) -> StepState.Done
                item.id == currentStepId -> StepState.Current
                else -> StepState.Upcoming
            }
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
                )
            }
        }
        if (allDone) {
            item(key = KEY_COMPLETE) {
                TimelineRow(
                    segments = TimelineSegments(hasTop = true, hasBottom = false, topReached = true, bottomReached = true),
                    nodeCenter = 40.dp,
                    nodeSize = StepNodeSize,
                    node = { FinishNode() },
                    modifier = Modifier.animateItem(),
                    side = ScheduleTimelineSide,
                ) {
                    SectionComplete(section, nextSection, onGoTo = actions.onSelectSection)
                }
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

/** Celebration that closes a finished task, with a shortcut to the next unfinished one. */
@Composable
private fun SectionComplete(section: ProgramSection, next: ProgramSection?, onGoTo: (String) -> Unit) {
    val palette = LocalDhikraPalette.current
    val scheme = MaterialTheme.colorScheme
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) { appear.animateTo(1f, tween(700, easing = EmphasizedDecelerate)) }
    Surface(
        shape = MaterialTheme.shapes.large,
        color = scheme.tertiaryContainer.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, palette.ornament.copy(alpha = 0.45f)),
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = appear.value
                val scale = 0.92f + 0.08f * appear.value
                scaleX = scale
                scaleY = scale
            },
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CelebrationStar(progress = appear.value, color = palette.ornament)
            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.section_complete),
                style = MaterialTheme.typography.headlineSmall,
                color = palette.ornamentText,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.section_complete_message, section.title),
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            if (next != null) {
                Spacer(Modifier.height(16.dp))
                Button(onClick = { onGoTo(next.id) }, shape = CircleShape) {
                    Text(stringResource(R.string.next_section, next.title), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null, Modifier.size(18.dp))
                }
            }
        }
    }
}

/** Gold eight-pointed star with rays, turning into place as the celebration appears. */
@Composable
private fun CelebrationStar(progress: Float, color: Color) {
    Canvas(Modifier.size(64.dp)) {
        val radius = size.minDimension / 2f
        rotate(degrees = -45f * (1f - progress)) {
            for (i in 0 until 12) {
                val angle = Math.toRadians(i * 30.0)
                val inner = radius * 0.66f
                val outer = radius * (0.66f + 0.32f * progress)
                drawLine(
                    color = color.copy(alpha = 0.55f),
                    start = Offset(center.x + inner * cos(angle).toFloat(), center.y + inner * sin(angle).toFloat()),
                    end = Offset(center.x + outer * cos(angle).toFloat(), center.y + outer * sin(angle).toFloat()),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }
            drawPath(Path().apply { addEightPointStar(center, radius * 0.56f) }, color)
            drawCircle(Color.White.copy(alpha = 0.85f), radius = radius * 0.16f)
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
