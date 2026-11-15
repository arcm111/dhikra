package com.example.dhikra.ui.home

import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.dhikra.R
import com.example.dhikra.data.ArabicText
import com.example.dhikra.data.ItemType
import com.example.dhikra.data.ProgramSection
import com.example.dhikra.ui.UiState
import com.example.dhikra.ui.components.CounterDial
import com.example.dhikra.ui.components.GoldInk
import com.example.dhikra.ui.components.LightNavigationBarIcons
import com.example.dhikra.ui.components.NightButton
import com.example.dhikra.ui.components.NightInk
import com.example.dhikra.ui.components.NightTextColors
import com.example.dhikra.ui.components.NoteIcon
import com.example.dhikra.ui.components.ReaderText
import com.example.dhikra.ui.components.UndoIcon
import com.example.dhikra.ui.components.starLattice
import com.example.dhikra.ui.components.verticalScrollbar
import com.example.dhikra.ui.theme.MustardLight
import com.example.dhikra.ui.theme.LocalDhikraPalette
import com.example.dhikra.ui.theme.ReadingStyles
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.sign
import kotlin.math.roundToInt
import com.example.dhikra.ui.uiDigits
import androidx.compose.animation.core.Animatable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.gestures.TargetedFlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import com.example.dhikra.data.ProgramItem
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.compositeOver
import com.example.dhikra.ui.components.CounterBar
import com.example.dhikra.ui.components.PlayToggle
import com.example.dhikra.ui.components.PlayButtonSize
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.platform.LocalWindowInfo
import kotlin.math.ceil

/** Test tag of the full-screen reader. */
const val StepReaderTag = "reader"

/** Texts up to this long are centred, longer ones are justified like a book. */
private const val CENTERED_TEXT_LENGTH = 90
private const val RESET_CONFIRM_MILLIS = 3_000L

/** Below this height (a phone held sideways) the counter goes beside the text, not below it. */
private val CompactHeight = 480.dp
private val ReaderMaxWidth = 560.dp
private val SideDialMax = 260.dp
private val SideGap = 16.dp
private val TopBarHeight = 52.dp
private val ActionSize = 48.dp
private val FooterButtonHeight = 52.dp
/** The bead dial gives up room, down to this, for a counted text to fit on one page. */
private val MinDial = 180.dp
/** What a page after the first hides of the top of its first line (spare leading). */
private val PageTopTrim = 5.dp

/**
 * The full-screen reader of a section: one screen per step, under the section's steps as check
 * boxes and the step's title, on the night background of the counter. Swiping up
 * goes to the next step, down to the previous one, so the section can be read through without
 * going back to the list. A group step reads its texts on its one screen, swiped sideways, and a
 * long text is cut into pages, also swiped sideways. [startId] is the step (or text of a group)
 * tapped in the list; closing reports the step being read through [onClose], so the list can
 * scroll to it.
 */
@Composable
fun StepReader(
    section: ProgramSection,
    startId: String,
    state: UiState,
    actions: HomeActions,
    onClose: (lastStepId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pages = remember(section) { readerPages(section) }
    if (pages.isEmpty()) {
        LaunchedEffect(Unit) { onClose(startId) }
        return
    }
    val initialPage = remember { pages.pageToOpen(startId) }
    // Moving on needs the step shown done: the pager ends at the first step not done yet from the
    // one settled on (going back is always possible), and grows as soon as it is done.
    val latestState by rememberUpdatedState(state)
    var settled by remember { mutableIntStateOf(initialPage) }
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { pages.reachableCount(settled, latestState::isComplete) },
    )
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { settled = it }
    }
    val scope = rememberCoroutineScope()

    // Reading along only lasts as long as its text is shown.
    var playingId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { playingId = null }
    }
    // The text each screen shows (a group's, as swiped to): reading along stops when it changes.
    val shownText = remember { mutableStateMapOf<String, Int>() }

    val shown = pagerState.currentPage.coerceIn(pages.indices)
    val close = { onClose(pages[shown].step.id) }
    BackHandler(onBack = close)
    LightNavigationBarIcons()

    // Moving on before the step is done: what completes it (the Done button, or the dial) shakes
    // "no" and the phone buzzes.
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val shake = remember { Animatable(0f) }
    val blocked = remember(pagerState) {
        BlockedSwipe(
            threshold = with(density) { 56.dp.toPx() },
            isBlocked = { pagerState.currentPage == pagerState.pageCount - 1 && pagerState.pageCount < pages.size },
            onBlocked = {
                haptic.performHapticFeedback(HapticFeedbackType.Reject)
                scope.launch {
                    shake.snapTo(0f)
                    for (dp in listOf(-14f, 12f, -9f, 6f, -3f, 0f)) {
                        shake.animateTo(with(density) { dp.dp.toPx() }, tween(55))
                    }
                }
            },
        )
    }

    val palette = LocalDhikraPalette.current
    // The counter's night, a little deeper.
    val nightTop = lerp(palette.nightStart, Color.Black, 0.2f)
    val nightBottom = lerp(palette.nightEnd, Color.Black, 0.2f)
    var headerHeight by remember { mutableIntStateOf(0) }
    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag(StepReaderTag)
            .nestedScroll(blocked)
            .background(Brush.verticalGradient(listOf(nightTop, nightBottom)))
            .starLattice(Color.White.copy(alpha = 0.035f), spacing = 58.dp, starRadius = 15.dp),
    ) {
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            key = { pages[it].id },
            // A page turns once dragged a little way, whether the finger flicks or lets go slowly.
            flingBehavior = deliberateFling(pagerState),
        ) { index ->
            val page = pages[index]
            key(page.texts.size) {
                ReaderStepScreen(
                    page = page,
                    startText = remember { page.textToOpen(if (index == initialPage) startId else "", latestState::isComplete) },
                    state = state,
                    actions = actions,
                    topInset = with(density) { headerHeight.toDp() },
                    isLast = index == pages.lastIndex,
                    sectionComplete = state.completedIn(section) >= section.trackableItems.size,
                    playingId = playingId,
                    onTogglePlay = { id -> playingId = if (playingId == id) null else id },
                    onTextShown = { text ->
                        if (shownText[page.id] != text) playingId = null
                        shownText[page.id] = text
                    },
                    onNext = {
                        if (index < pages.lastIndex) scope.launch { pagerState.animateScrollToPage(index + 1) }
                        else close()
                    },
                    nudge = { if (index == pagerState.currentPage) shake.value else 0f },
                    modifier = Modifier.graphicsLayer {
                        // The screens leaving or entering dim and shrink a little, like cards of a stack.
                        // A page past the pager's end — kept composed a moment after the step shown is
                        // reset (the pager then ends at it) — is simply out of view: asking its distance
                        // would throw.
                        val distance = if (index < pagerState.pageCount) {
                            pagerState.getOffsetDistanceInPages(index).absoluteValue.coerceIn(0f, 1f)
                        } else 1f
                        alpha = 1f - 0.5f * distance
                        scaleX = 1f - 0.06f * distance
                        scaleY = scaleX
                    },
                )
            }
        }

        // Fixed above the screens: the section's steps, and the step's title.
        val page = pages[shown]
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { headerHeight = it.height }
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
                .padding(bottom = 8.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = ReaderMaxWidth)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ReaderTopBar(
                    position = pagePosition(section, page),
                    marks = section.trackableItems.map { step ->
                        val current = step.id == page.step.id
                        StepMark(
                            done = state.isComplete(step),
                            current = current,
                            groupShare = if (current && step.isGroup && step.target > 0) {
                                state.doneCount(step).toFloat() / step.target
                            } else null,
                        )
                    },
                    groupComplete = page.step.isGroup && state.isComplete(page.step),
                    onSetGroupComplete = { actions.onSetComplete(page.step, it) },
                    onClose = close,
                )
                StepTitle(page.step)
            }
        }
    }
}

/** Where the reader is among the section's steps, as count/total: «٥/٩». */
private fun pagePosition(section: ProgramSection, page: ReaderPage): String {
    val steps = section.trackableItems
    return uiDigits("${steps.indexOfFirst { it.id == page.step.id } + 1}/${steps.size}")
}

/** The step's title under the check boxes, on a darker, translucent pill. */
@Composable
private fun StepTitle(step: ProgramItem, modifier: Modifier = Modifier) {
    if (step.title.isBlank()) return
    Text(
        text = step.title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = Color.White.copy(alpha = 0.92f),
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .padding(top = 2.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.Black.copy(alpha = 0.22f))
            .padding(horizontal = 16.dp, vertical = 6.dp),
    )
}

/**
 * What the footer offers for the text shown: counting it; turning to its next page (every page is
 * read before it can be done); marking it done — which finishes the step, every text of a group
 * included — or, in a group with texts still to read, marking it done and going on to the next
 * text; going on once it is done; and once the step is done, the next step.
 */
private enum class FooterMode { Counting, NextPage, MarkDone, MarkAndNext, NextText, Finished }

/**
 * One screen of the reader: a step, its texts side by side (one for a single step), each cut into
 * pages, with the dots of the group's texts under them. A counted text shows the bead dial and
 * counts a repetition on every tap anywhere on the screen; any other text is marked done with its
 * button, once all its pages are read.
 */
@Composable
private fun ReaderStepScreen(
    page: ReaderPage,
    startText: Int,
    state: UiState,
    actions: HomeActions,
    topInset: Dp,
    isLast: Boolean,
    sectionComplete: Boolean,
    playingId: String?,
    onTogglePlay: (String) -> Unit,
    onTextShown: (Int) -> Unit,
    onNext: () -> Unit,
    /** The sideways shake of what completes the step, when moving on is tried too early (px). */
    nudge: () -> Float,
    modifier: Modifier = Modifier,
) {
    val step = page.step
    val texts = page.texts
    val group = step.isGroup
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val textPager = rememberPagerState(initialPage = startText.coerceIn(texts.indices)) { texts.size }
    // Each text's pages: how many (once laid out), and the furthest one read.
    val pageCounts = remember { mutableStateListOf<Int>().apply { repeat(texts.size) { add(1) } } }
    val readTo = remember { mutableStateListOf<Int>().apply { repeat(texts.size) { add(-1) } } }
    val pageStates = texts.indices.map { i -> rememberPagerState { pageCounts.getOrElse(i) { 1 } } }
    LaunchedEffect(textPager) {
        snapshotFlow { textPager.settledPage.let { it to pageStates[it].settledPage } }
            .collect { (text, pageRead) -> if (pageRead > readTo[text]) readTo[text] = pageRead }
    }

    val current = textPager.currentPage.coerceIn(texts.indices)
    LaunchedEffect(current) { onTextShown(current) }
    val text = texts[current]
    val pages = pageStates[current]
    val pageCount = pageCounts[current]
    val counted = text.isRepeatable
    val target = text.target
    val done = state.doneCount(text)
    val textDone = state.isComplete(text)
    val stepDone = state.isComplete(step)
    val morePages = pages.currentPage < pageCount - 1

    // A text is read once done, or (a text not counted) once its last page has been shown.
    val isRead = { i: Int ->
        state.isComplete(texts[i]) || (!texts[i].isRepeatable && readTo[i] >= pageCounts[i] - 1)
    }
    val next = nextText(current, texts.size, { state.isComplete(texts[it]) }, isRead)
    // «Done» on this text finishes the step: a single step; the last text a choice group needs; in a
    // group, the last text to read — every text of the group is then marked done.
    val finishes = when {
        !group -> true
        step.isChoice -> state.doneCount(step) + 1 >= step.target
        else -> (1 until texts.size).all { isRead((current + it) % texts.size) }
    }
    val mode = when {
        counted && !textDone -> FooterMode.Counting
        stepDone -> FooterMode.Finished
        textDone -> FooterMode.NextText
        morePages -> FooterMode.NextPage
        finishes -> FooterMode.MarkDone
        else -> FooterMode.MarkAndNext
    }

    var cycle by remember(text.id) { mutableIntStateOf(0) }
    var cycleStart by remember(text.id) { mutableLongStateOf(0L) }
    var awaitingTap by remember(text.id) { mutableStateOf(false) }
    var pressed by remember { mutableStateOf(false) }
    var confirmReset by remember(text.id) { mutableStateOf(false) }
    // Reading along follows one page: a text of several pages is read without it.
    val playable = counted && pageCount == 1
    val playing = playable && playingId == text.id

    LaunchedEffect(textDone, playing) {
        if (textDone && playing) onTogglePlay(text.id)
    }
    LaunchedEffect(confirmReset) {
        if (confirmReset) {
            delay(RESET_CONFIRM_MILLIS)
            confirmReset = false
        }
    }

    // The dial's size for each counted text: smaller when the text would not fit on one page.
    val density = LocalDensity.current
    val windowHeight = with(density) { LocalWindowInfo.current.containerSize.height.toDp() }
    val fullDial = minOf(330.dp, windowHeight * 0.44f)
    val dialSizes = remember { mutableStateMapOf<String, Dp>() }
    val dialSize = dialSizes[text.id] ?: fullDial
    val fit: (ProgramItem, Int, Int) -> Unit = { fitted, room, needed ->
        if (fitted.isRepeatable && needed > room) {
            val size = dialSizes[fitted.id] ?: fullDial
            dialSizes[fitted.id] = (size - with(density) { (needed - room).toDp() }).coerceAtLeast(MinDial)
        }
    }

    val goToText: (Int) -> Unit = { scope.launch { textPager.animateScrollToPage(it) } }
    val turnPage: () -> Unit = { scope.launch { pages.animateScrollToPage(pages.currentPage + 1) } }
    val count: () -> Unit = {
        if (!textDone) {
            haptic.performHapticFeedback(
                if (done + 1 >= target) HapticFeedbackType.Confirm else HapticFeedbackType.SegmentTick
            )
            if (playing) {
                actions.onRepetitionRead(text, SystemClock.uptimeMillis() - cycleStart)
                cycle++
            } else {
                actions.onIncrement(text)
            }
            // A text of several pages starts over on its first page for the next repetition.
            if (pageCount > 1 && done + 1 < target) scope.launch { pages.animateScrollToPage(0) }
        }
    }
    // A tap on the screen reads a long counted text through its pages before counting it.
    val tap: () -> Unit = { if (morePages && !textDone) turnPage() else count() }
    val latestTap by rememberUpdatedState(tap)
    val undo: () -> Unit = {
        haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
        actions.onDecrement(text)
    }
    val reset: () -> Unit = {
        if (confirmReset) {
            confirmReset = false
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            actions.onResetItem(text)
        } else {
            confirmReset = true
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .then(
                if (counted) {
                    // Child buttons consume their own touches; a drag taken by a pager cancels the tap.
                    Modifier.pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                pressed = true
                                tryAwaitRelease()
                                pressed = false
                            },
                            onTap = { latestTap() },
                        )
                    }
                } else Modifier
            )
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
            .padding(top = topInset)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        val compact = maxHeight < CompactHeight
        val sideDial = counted && compact
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = if (sideDial) ReaderMaxWidth + SideGap + SideDialMax else ReaderMaxWidth)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(SideGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                    HorizontalPager(
                        state = textPager,
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        key = { texts[it].id },
                        pageSpacing = 24.dp,
                        verticalAlignment = Alignment.Top,
                    ) { i ->
                        TextSheet(
                            text = texts[i],
                            step = step,
                            pagerState = pageStates[i],
                            current = i == current,
                            onPageCount = { pageCounts[i] = it },
                            onFit = { room, needed -> fit(texts[i], room, needed) },
                            textScale = state.textScale,
                            lettersPerSecond = state.speedFor(texts[i]),
                            playing = playing && i == current,
                            cycle = cycle,
                            onCycleStart = { cycleStart = it },
                            onReachedEnd = { awaitingTap = it },
                        )
                    }
                    if (texts.size > 1) {
                        TextDots(
                            count = texts.size,
                            current = current,
                            isDone = { state.isComplete(texts[it]) },
                            onSelect = goToText,
                        )
                    }
                }
                if (sideDial) {
                    CounterDial(
                        done = done,
                        target = target,
                        complete = textDone,
                        pressed = pressed,
                        onCount = count,
                        modifier = Modifier.fillMaxHeight().graphicsLayer { translationX = nudge() },
                        maxDiameter = SideDialMax,
                    )
                }
            }
            if (counted && !compact) {
                Spacer(Modifier.height(8.dp))
                CounterDial(
                    done = done,
                    target = target,
                    complete = textDone,
                    pressed = pressed,
                    onCount = count,
                    modifier = Modifier.fillMaxWidth().graphicsLayer { translationX = nudge() },
                    maxDiameter = dialSize,
                )
                Spacer(Modifier.height(24.dp))
            } else {
                Spacer(Modifier.height(12.dp))
            }
            ReaderFooter(
                mode = mode,
                counted = counted,
                done = done,
                isLast = isLast,
                stepDone = stepDone,
                sectionComplete = sectionComplete,
                awaitingTap = playing && awaitingTap,
                confirmReset = confirmReset,
                playable = playable,
                playing = playing,
                onTogglePlay = { onTogglePlay(text.id) },
                onUndo = undo,
                onReset = reset,
                onMarkDone = {
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                    // The last text of a group to read finishes it: every one of its texts is done.
                    actions.onSetComplete(if (group && !step.isChoice) step else text, true)
                },
                onMarkAndNext = {
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                    actions.onSetComplete(text, true)
                    next?.let(goToText)
                },
                onUnmarkDone = {
                    haptic.performHapticFeedback(HapticFeedbackType.ToggleOff)
                    // A text passed by in a finished choice group: undoing undoes the group.
                    actions.onSetComplete(if (textDone) text else step, false)
                },
                onNextPage = turnPage,
                onNextText = { next?.let(goToText) ?: onNext() },
                onNext = onNext,
                nudge = nudge,
                modifier = Modifier.widthIn(max = ReaderMaxWidth),
            )
        }
    }
}

/**
 * A text of the step (its own, or one of its group's), cut into pages that fit the screen, turned
 * sideways, with «١ / ٣» under them: its opening stays above the pages, its source and notes come
 * after its last line.
 */
@Composable
private fun TextSheet(
    text: ProgramItem,
    step: ProgramItem,
    pagerState: PagerState,
    /** Whether this is the text shown: only it reports how much room it needs. */
    current: Boolean,
    onPageCount: (Int) -> Unit,
    /** The room the text has and the room it needs (px), so a counted text can be given more. */
    onFit: (room: Int, needed: Int) -> Unit,
    textScale: Float,
    lettersPerSecond: Float,
    playing: Boolean,
    cycle: Int,
    onCycleStart: (Long) -> Unit,
    onReachedEnd: (Boolean) -> Unit,
) {
    // Where the text's lines (and the source and notes under it) lie, once laid out.
    var spans by remember(text.id, textScale) { mutableStateOf<List<Span>?>(null) }
    val latestPageCount by rememberUpdatedState(onPageCount)
    val latestFit by rememberUpdatedState(onFit)
    val density = LocalDensity.current
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        // The opening (أعوذ بالله…, بسم الله…) stays put above the text: only the text has pages.
        PageOpening(text, textScale, Modifier.padding(top = 8.dp))
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().padding(vertical = 8.dp)) {
            val height = constraints.maxHeight
            val cuts = remember(spans, height) { spans?.let { cutIntoPages(it, height) } }
            LaunchedEffect(cuts?.size) { cuts?.let { latestPageCount(it.size) } }
            LaunchedEffect(spans, height, current) {
                val needed = spans?.maxOfOrNull { it.bottom } ?: return@LaunchedEffect
                if (current) latestFit(height, needed)
            }
            val single = (cuts?.size ?: 1) <= 1
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                pageSpacing = 24.dp,
                verticalAlignment = Alignment.Top,
            ) { index ->
                val cut = cuts?.getOrNull(index)
                // A page after the first hides the top of its first line's leading, where the marks
                // under the line before it (on the page before) reach.
                val trim = if (index > 0) with(density) { PageTopTrim.roundToPx() } else 0
                val top = (cut?.top ?: 0) + trim
                val window = (cut?.let { it.bottom - top } ?: height).coerceIn(0, height)
                // A text that fits is centred; the pages of a long one start at the top.
                Box(Modifier.fillMaxSize(), contentAlignment = if (single) Alignment.Center else Alignment.TopCenter) {
                    Box(
                        Modifier
                            .padding(top = with(density) { trim.toDp() })
                            .fillMaxWidth()
                            .height(with(density) { window.toDp() })
                            .clipToBounds()
                            // Hidden until laid out, so it does not flash unpaged.
                            .graphicsLayer { alpha = if (cuts == null) 0f else 1f },
                    ) {
                        PageTexts(
                            item = text,
                            step = step,
                            textScale = textScale,
                            lettersPerSecond = lettersPerSecond,
                            playing = playing && index == pagerState.currentPage,
                            cycle = cycle,
                            onCycleStart = onCycleStart,
                            onReachedEnd = onReachedEnd,
                            onSpans = { spans = it },
                            modifier = Modifier
                                .wrapContentHeight(Alignment.Top, unbounded = true)
                                .offset { IntOffset(0, -top) },
                        )
                    }
                }
            }
        }
        if (pagerState.pageCount > 1) PageNumber(pagerState.currentPage, pagerState.pageCount)
    }
}

/** «٢ / ٣»: the page shown of a text's pages. */
@Composable
private fun PageNumber(page: Int, count: Int) {
    val spoken = stringResource(R.string.reader_page_position, uiDigits(page + 1), uiDigits(count))
    Text(
        text = uiDigits("${page + 1} / $count"),
        style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
        color = Color.White.copy(alpha = 0.75f),
        modifier = Modifier
            .clearAndSetSemantics { contentDescription = spoken }
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.08f))
            .padding(horizontal = 12.dp, vertical = 3.dp),
    )
}

/**
 * The dots of a group's texts, in order: the one shown wider, the done ones gold. A tap on one
 * goes to its text.
 */
@Composable
private fun TextDots(count: Int, current: Int, isDone: (Int) -> Boolean, onSelect: (Int) -> Unit) {
    val spoken = stringResource(R.string.reader_text_position, uiDigits(current + 1), uiDigits(count))
    Row(
        modifier = Modifier
            .padding(top = 6.dp)
            .clearAndSetSemantics { contentDescription = spoken },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count) { i ->
            val selected = i == current
            val width by animateDpAsState(if (selected) 22.dp else 8.dp, label = "textDot")
            Box(
                modifier = Modifier
                    .size(width = width + 8.dp, height = 24.dp)
                    .clickable(interactionSource = null, indication = null) { onSelect(i) },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .size(width = width, height = 8.dp)
                        .background(
                            when {
                                isDone(i) -> MustardLight
                                selected -> Color.White
                                else -> Color.White.copy(alpha = 0.28f)
                            },
                            CircleShape,
                        )
                )
            }
        }
    }
}

/** How far a page must be dragged (a share of the screen) to turn when let go. */
private const val MinDragToTurn = 0.12f

/**
 * The pager's fling: a page dragged at least [MinDragToTurn] of the screen turns when let go, even
 * slowly (it is given a turning speed in the drag's direction) — the pager alone would need half
 * the screen or a fast flick, and a drag handed over from a long text at its end rarely has that
 * speed, so it fell back. A shorter drag springs back; a flick the other way still wins.
 */
@Composable
private fun deliberateFling(state: PagerState): TargetedFlingBehavior {
    val base = PagerDefaults.flingBehavior(state)
    val turnSpeed = with(LocalDensity.current) { 700.dp.toPx() }
    return remember(base, state, turnSpeed) { DeliberateFling(base, state, turnSpeed) }
}

private class DeliberateFling(
    private val base: TargetedFlingBehavior,
    private val state: PagerState,
    private val turnSpeed: Float,
) : TargetedFlingBehavior {
    override suspend fun ScrollScope.performFling(initialVelocity: Float, onRemainingDistanceUpdated: (Float) -> Unit): Float {
        // Forward (towards the next page) is positive, for the offset as for the velocity.
        val offset = state.currentPageOffsetFraction
        val passedHalf = state.currentPage != state.settledPage
        val velocity = when {
            passedHalf -> initialVelocity
            offset.absoluteValue < MinDragToTurn -> 0f
            // A real flick decides, either way; let go slowly, the page turns the way it was dragged.
            initialVelocity.absoluteValue >= turnSpeed -> initialVelocity
            else -> offset.sign * turnSpeed
        }
        return with(base) { performFling(velocity, onRemainingDistanceUpdated) }
    }
}

/**
 * Notices a swipe to the next screen while the one shown is not done yet (the pager ends there):
 * what of the swipe is left past the pager's end, once more than [threshold] or a quick fling, calls
 * [onBlocked] — once per swipe.
 */
private class BlockedSwipe(
    private val threshold: Float,
    private val isBlocked: () -> Boolean,
    private val onBlocked: () -> Unit,
) : NestedScrollConnection {
    private var pulled = 0f
    private var fired = false

    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
        if (source == NestedScrollSource.UserInput && available.y < 0f && isBlocked()) {
            pulled -= available.y
            if (!fired && pulled > threshold) {
                fired = true
                onBlocked()
            }
        }
        return Offset.Zero
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        if (!fired && available.y < -1_500f && isBlocked()) onBlocked()
        pulled = 0f
        fired = false
        return Velocity.Zero
    }
}

/**
 * Scrolls the text so the highlighted word stays in view while reading along. The highlight
 * [fraction] of the way through the text is shown at the same fraction of the scroll; the user's
 * own scrolling takes over until they let go.
 */
internal fun followHighlight(scroll: ScrollState, fraction: Float) {
    val range = scroll.maxValue
    if (scroll.isScrollInProgress || range <= 0 || range == Int.MAX_VALUE) return
    scroll.dispatchRawDelta((fraction * range).roundToInt() - scroll.value.toFloat())
}

/**
 * One step of the section on the reader's top bar: [done] or not, the [current] one, and for a
 * current group the share of its texts done ([groupShare]).
 */
internal data class StepMark(val done: Boolean, val current: Boolean, val groupShare: Float? = null)

/**
 * The reader's top bar: the close button, and the section's steps as a row of check boxes — done
 * ones gold with a tick, the current one outlined brighter; a current group is a ring filling
 * with its texts, a tick once complete, and a long press on it finishes (or undoes) the whole
 * group. Too many steps to fit make small dots, then just «٥/٩».
 */
@Composable
private fun ReaderTopBar(
    position: String,
    marks: List<StepMark>,
    groupComplete: Boolean,
    onSetGroupComplete: (Boolean) -> Unit,
    onClose: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(TopBarHeight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(ActionSize), contentAlignment = Alignment.CenterStart) {
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, stringResource(R.string.close), tint = NightInk)
            }
        }
        StepMarks(
            marks = marks,
            position = position,
            groupComplete = groupComplete,
            onSetGroupComplete = onSetGroupComplete,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(ActionSize))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StepMarks(
    marks: List<StepMark>,
    position: String,
    groupComplete: Boolean,
    onSetGroupComplete: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val index = marks.indexOfFirst { it.current }
    val currentMark = marks.getOrNull(index)
    // The current step is a group: its ring (or its tick once complete) finishes or undoes it all.
    val isGroup = currentMark?.groupShare != null || (currentMark?.done == true && groupComplete)
    val spoken = stringResource(R.string.reader_position, uiDigits(index + 1), uiDigits(marks.size))
    val groupAction = stringResource(if (groupComplete) R.string.undo_done else R.string.group_complete_all)
    val toggleGroup = {
        haptic.performHapticFeedback(if (groupComplete) HapticFeedbackType.ToggleOff else HapticFeedbackType.Confirm)
        onSetGroupComplete(!groupComplete)
    }
    BoxWithConstraints(
        modifier = modifier.clearAndSetSemantics {
            contentDescription = spoken
            if (isGroup) customActions = listOf(CustomAccessibilityAction(groupAction) { toggleGroup(); true })
        },
        contentAlignment = Alignment.Center,
    ) {
        val box = 18.dp
        val gap = 6.dp
        when {
            (box + gap) * marks.size <= maxWidth -> Row(
                horizontalArrangement = Arrangement.spacedBy(gap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                marks.forEach { mark ->
                    val longPress = if (mark.current && isGroup) {
                        Modifier.combinedClickable(
                            interactionSource = null,
                            indication = null,
                            onClick = {},
                            onLongClick = toggleGroup,
                        )
                    } else Modifier
                    StepCheck(mark, box, longPress)
                }
            }
            (10.dp + 4.dp) * marks.size <= maxWidth -> Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                marks.forEach { mark ->
                    Box(
                        Modifier
                            .size(if (mark.current) 10.dp else 8.dp)
                            .background(
                                when {
                                    mark.done -> MustardLight
                                    mark.current -> NightInk
                                    else -> Color.White.copy(alpha = 0.25f)
                                },
                                CircleShape,
                            )
                    )
                }
            }
            else -> Text(
                text = position,
                style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                fontWeight = FontWeight.SemiBold,
                color = NightInk,
                maxLines = 1,
            )
        }
    }
}

/** One step's box: a gold tick when done, an outline while to do (brighter for the current one); a ring for a current group. */
@Composable
private fun StepCheck(mark: StepMark, size: Dp, modifier: Modifier = Modifier) {
    val share = mark.groupShare
    val shape = RoundedCornerShape(5.dp)
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        when {
            mark.done -> Box(
                Modifier
                    .fillMaxSize()
                    .background(MustardLight, shape)
                    .then(if (mark.current) Modifier.border(1.5.dp, Color.White, shape) else Modifier),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Check, null, tint = GoldInk, modifier = Modifier.size(size * 0.75f))
            }
            share != null -> {
                // A group under way: its texts done, round a ring.
                val progress by animateFloatAsState(share.coerceIn(0f, 1f), label = "groupMark")
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxSize(),
                    color = MustardLight,
                    // As bright as the current step's outline: this is where the reader is.
                    trackColor = Color.White.copy(alpha = 0.75f),
                    strokeWidth = 2.5.dp,
                    gapSize = 0.dp,
                )
            }
            else -> Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = if (mark.current) 0.14f else 0f), shape)
                    .border(
                        if (mark.current) 2.dp else 1.5.dp,
                        Color.White.copy(alpha = if (mark.current) 0.9f else 0.35f),
                        shape,
                    )
            )
        }
    }
}

/** The step's opening line (أعوذ بالله…, بسم الله…), in gold over its text; nothing when it has none. */
@Composable
private fun PageOpening(item: ProgramItem, textScale: Float, modifier: Modifier = Modifier) {
    if (item.opening.isBlank()) return
    val quran = item.type == ItemType.QURAN
    Text(
        text = item.opening,
        style = if (quran) ReadingStyles.quranOpening(textScale)
        else ReadingStyles.dua(textScale).copy(textAlign = TextAlign.Center, fontWeight = FontWeight.Medium),
        color = MustardLight,
        modifier = modifier.fillMaxWidth().padding(bottom = 4.dp),
    )
}

/**
 * The text's words, then its source and notes. Reports where its lines and the block under them
 * lie ([onSpans]), so it can be cut into pages.
 */
@Composable
private fun PageTexts(
    item: ProgramItem,
    step: ProgramItem,
    textScale: Float,
    lettersPerSecond: Float,
    playing: Boolean,
    cycle: Int,
    onCycleStart: (Long) -> Unit,
    onReachedEnd: (Boolean) -> Unit,
    onSpans: (List<Span>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val inGroup = step.isGroup
    val quran = item.type == ItemType.QURAN
    val text = remember(item.text, quran) { if (quran) ArabicText.withAyahMarkers(item.text) else item.text }
    // A text of a group also shows what its group says.
    val sources = listOf(item.source, if (inGroup) step.source else "").filter { it.isNotBlank() }
    val notes = listOf(item.note, if (inGroup) step.note else "").filter { it.isNotBlank() }

    // The block under the text (source, notes) starts where the text ends: sizes only, no
    // coordinates, which a page the pager has just recycled no longer has.
    var lines by remember { mutableStateOf<List<Span>>(emptyList()) }
    var textHeight by remember { mutableIntStateOf(0) }
    var afterHeight by remember { mutableIntStateOf(0) }
    val latestSpans by rememberUpdatedState(onSpans)
    LaunchedEffect(lines, textHeight, afterHeight) {
        val after = if (afterHeight > 0) Span(textHeight, textHeight + afterHeight) else null
        if (lines.isNotEmpty() || after != null) latestSpans(lines + listOfNotNull(after))
    }

    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        if (text.isNotBlank()) {
            val style = when (item.type) {
                ItemType.QURAN -> ReadingStyles.quran(textScale)
                ItemType.HADITH -> ReadingStyles.hadith(textScale)
                ItemType.DUA -> ReadingStyles.dua(textScale)
                ItemType.DHIKR, ItemType.TASK, ItemType.HEADING, ItemType.NOTICE -> ReadingStyles.dhikr(textScale)
            }
            ReaderText(
                text = text,
                style = if (item.text.length <= CENTERED_TEXT_LENGTH) style.copy(textAlign = TextAlign.Center) else style,
                colors = NightTextColors,
                playing = playing,
                cycle = cycle,
                lettersPerSecond = lettersPerSecond,
                ornamentalAyahMarkers = quran,
                onCycleStart = onCycleStart,
                onReachedEnd = onReachedEnd,
                onTextLayout = { layout ->
                    lines = (0 until layout.lineCount).map {
                        Span(layout.getLineTop(it).toInt(), ceil(layout.getLineBottom(it)).toInt())
                    }
                },
                modifier = Modifier.fillMaxWidth().onSizeChanged { textHeight = it.height },
            )
        }
        if (sources.isNotEmpty() || notes.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .onSizeChanged { afterHeight = it.height },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                sources.forEach { source ->
                    Text(
                        text = source,
                        style = ReadingStyles.hadithMeta(textScale).copy(textAlign = TextAlign.Center),
                        color = Color.White.copy(alpha = 0.75f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 14.dp)
                            .clip(MaterialTheme.shapes.small)
                            .background(Color.White.copy(alpha = 0.07f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
                notes.forEach { note ->
                    Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = NoteIcon,
                            contentDescription = stringResource(R.string.note_label),
                            tint = MustardLight.copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = note,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.75f),
                            modifier = Modifier.weight(1f, fill = false),
                        )
                    }
                }
            }
        }
    }
}

/**
 * A hint line and a row of buttons. Every mode takes the same height, so the screen does not jump
 * when a step is finished.
 */
@Composable
private fun ReaderFooter(
    mode: FooterMode,
    counted: Boolean,
    done: Int,
    isLast: Boolean,
    stepDone: Boolean,
    sectionComplete: Boolean,
    awaitingTap: Boolean,
    confirmReset: Boolean,
    playable: Boolean,
    playing: Boolean,
    onTogglePlay: () -> Unit,
    onUndo: () -> Unit,
    onReset: () -> Unit,
    onMarkDone: () -> Unit,
    onMarkAndNext: () -> Unit,
    onUnmarkDone: () -> Unit,
    onNextPage: () -> Unit,
    onNextText: () -> Unit,
    onNext: () -> Unit,
    nudge: () -> Float,
    modifier: Modifier = Modifier,
) {
    AnimatedContent(
        targetState = mode,
        modifier = modifier,
        transitionSpec = {
            fadeIn(tween(220)) togetherWith fadeOut(tween(120)) using SizeTransform(clip = false) { _, _ -> snap() }
        },
        label = "readerFooter",
    ) { current ->
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.heightIn(min = 22.dp), contentAlignment = Alignment.Center) {
                when (current) {
                    FooterMode.Counting -> when {
                        stepDone && !isLast -> SwipeHint(stringResource(R.string.reader_swipe_hint))
                        else -> Hint(stringResource(if (awaitingTap) R.string.reader_waiting else R.string.counter_screen_hint))
                    }
                    FooterMode.NextPage -> Hint(stringResource(R.string.reader_pages_hint))
                    FooterMode.MarkDone -> Hint(stringResource(R.string.reader_done_hint))
                    FooterMode.MarkAndNext, FooterMode.NextText -> Hint(stringResource(R.string.reader_group_hint))
                    FooterMode.Finished -> when {
                        !isLast -> SwipeHint(stringResource(R.string.reader_swipe_hint))
                        sectionComplete -> Hint(stringResource(R.string.section_complete), MustardLight)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth().height(FooterButtonHeight),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                val side = Modifier.weight(1f).fillMaxHeight()
                // What completes the text shakes "no" when the next step is swiped to too early.
                val shaken = side.graphicsLayer { translationX = nudge() }
                when (current) {
                    FooterMode.Counting -> CountingBar(
                        done = done,
                        confirmReset = confirmReset,
                        playable = playable,
                        playing = playing,
                        onUndo = onUndo,
                        onReset = onReset,
                        onTogglePlay = onTogglePlay,
                    )
                    FooterMode.NextPage -> GoldButton(
                        icon = Icons.AutoMirrored.Filled.ArrowForward,
                        label = stringResource(R.string.reader_next_page),
                        onClick = onNextPage,
                        modifier = shaken,
                    )
                    FooterMode.MarkDone -> GoldButton(
                        icon = Icons.Filled.Check,
                        label = stringResource(R.string.mark_done),
                        onClick = onMarkDone,
                        modifier = shaken,
                    )
                    FooterMode.MarkAndNext -> GoldButton(
                        icon = Icons.AutoMirrored.Filled.ArrowForward,
                        label = stringResource(R.string.reader_next_text),
                        onClick = onMarkAndNext,
                        modifier = shaken,
                    )
                    FooterMode.NextText, FooterMode.Finished -> {
                        if (counted) ResetButton(confirmReset, onReset, side, enabled = true)
                        else NightButton(UndoIcon, stringResource(R.string.undo_done), onUnmarkDone, side)
                        if (current == FooterMode.NextText) {
                            GoldButton(
                                icon = Icons.AutoMirrored.Filled.ArrowForward,
                                label = stringResource(R.string.reader_next_text),
                                onClick = onNextText,
                                modifier = shaken,
                            )
                        } else {
                            GoldButton(
                                icon = if (isLast) Icons.Filled.Close else Icons.Filled.KeyboardArrowUp,
                                label = stringResource(if (isLast) R.string.reader_back_to_list else R.string.reader_next_step),
                                onClick = onNext,
                                modifier = side,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * A counted text's controls: the counters' bar, undo on one side and reset on the other, with
 * reading along — play, or pause while playing — as a quiet gold button in its middle when the
 * text is [playable] (it fits on one page).
 */
@Composable
private fun RowScope.CountingBar(
    done: Int,
    confirmReset: Boolean,
    playable: Boolean,
    playing: Boolean,
    onUndo: () -> Unit,
    onReset: () -> Unit,
    onTogglePlay: () -> Unit,
) {
    CounterBar(
        undoEnabled = done > 0,
        resetEnabled = done > 0,
        confirmReset = confirmReset,
        onUndo = onUndo,
        onReset = onReset,
        centerSize = PlayButtonSize,
        modifier = Modifier.weight(1f).fillMaxHeight(),
        center = if (playable) {
            { PlayToggle(playing = playing, onToggle = onTogglePlay, size = PlayButtonSize) }
        } else null,
    )
}

@Composable
private fun ResetButton(confirm: Boolean, onClick: () -> Unit, modifier: Modifier, enabled: Boolean) {
    NightButton(
        icon = Icons.Filled.Refresh,
        label = stringResource(if (confirm) R.string.counter_reset_confirm else R.string.counter_reset),
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
    )
}

@Composable
private fun GoldButton(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier) {
    Button(
        onClick = onClick,
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(containerColor = MustardLight, contentColor = GoldInk),
        modifier = modifier,
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun Hint(text: String, color: Color = Color.White.copy(alpha = 0.6f)) {
    Text(text = text, style = MaterialTheme.typography.bodySmall, color = color, textAlign = TextAlign.Center)
}

/** Gold hint with a chevron that keeps lifting, showing the way the next step comes from. */
@Composable
private fun SwipeHint(text: String) {
    val lift by rememberInfiniteTransition(label = "swipeHint").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "swipeHintLift",
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Filled.KeyboardArrowUp,
            contentDescription = null,
            tint = MustardLight,
            modifier = Modifier.size(20.dp).graphicsLayer {
                translationY = -4.dp.toPx() * lift
                alpha = 0.55f + 0.45f * lift
            },
        )
        Spacer(Modifier.width(4.dp))
        Text(text = text, style = MaterialTheme.typography.labelLarge, color = MustardLight)
    }
}
