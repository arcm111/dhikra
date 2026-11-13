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

/** What the reader can do on its screens; the screens themselves keep no progress of their own. */
internal class ReaderActions(
    val onCount: () -> Unit,
    val onRepetitionRead: (elapsedMs: Long) -> Unit,
    val onUndo: () -> Unit,
    val onReset: () -> Unit,
    val onSetComplete: (Boolean) -> Unit,
    val onSetGroupComplete: (Boolean) -> Unit,
    val onTogglePlay: () -> Unit,
    val onNext: () -> Unit,
    val onClose: () -> Unit,
)

/** Lets a page's leftover scroll and fling reach the pager, see [StepReader]. */
private val PassThroughNestedScroll = object : NestedScrollConnection {}

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
private val TopBarHeight = 56.dp
private val ActionSize = 48.dp
private val FooterButtonHeight = 52.dp

/**
 * The full-screen reader of a section: one screen per step (a group step has one per text), on the
 * night background of the counter. Swiping up goes to the next step, down to the previous one, so
 * the section can be read through without going back to the list. [startId] is the step (or text
 * of a group) tapped in the list; closing reports the step being read through [onClose], so the
 * list can scroll to it.
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
    val initialPage = remember { pages.pageToOpen(startId, state::isComplete) }
    // Moving on needs the screen shown done: the pager ends at the first screen not done yet from the
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

    // Reading along only lasts as long as its screen is shown.
    var playingId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { playingId = null }
    }

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
    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag(StepReaderTag)
            .nestedScroll(blocked)
            .background(Brush.verticalGradient(listOf(palette.nightStart, palette.nightEnd)))
            .starLattice(Color.White.copy(alpha = 0.035f), spacing = 58.dp, starRadius = 15.dp),
    ) {
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            key = { pages[it].id },
            // A swipe on a long text that is already at its end turns the page, fling included
            // (see [EdgeHandOff]). The default connection drops that fling, so a swipe starting
            // on the text only dragged the page a little and let it fall back.
            pageNestedScrollConnection = PassThroughNestedScroll,
            // A page turns only after a deliberate drag, not a brush against the screen.
            // A page turns once dragged a little way, whether the finger flicks or lets go slowly,
            // and the same when the drag reaches the pager from a long text at its end.
            flingBehavior = deliberateFling(pagerState),
        ) { index ->
            val page = pages[index]
            val item = page.item
            ReaderPageContent(
                page = page,
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
                done = state.doneCount(item),
                groupComplete = page.inGroup && state.isComplete(page.step),
                sectionComplete = state.completedIn(section) >= section.trackableItems.size,
                isLast = index == pages.lastIndex,
                textScale = state.textScale,
                lettersPerSecond = state.speedFor(item),
                playing = playingId == page.id,
                actions = ReaderActions(
                    onCount = { actions.onIncrement(item) },
                    onRepetitionRead = { elapsed -> actions.onRepetitionRead(item, elapsed) },
                    onUndo = { actions.onDecrement(item) },
                    onReset = { actions.onResetItem(item) },
                    onSetComplete = { actions.onSetComplete(item, it) },
                    onSetGroupComplete = { actions.onSetComplete(page.step, it) },
                    onTogglePlay = { playingId = if (playingId == page.id) null else page.id },
                    onNext = {
                        if (index < pages.lastIndex) scope.launch { pagerState.animateScrollToPage(index + 1) }
                        else close()
                    },
                    onClose = close,
                ),
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
                nudge = { if (index == pagerState.currentPage) shake.value else 0f },
            )
        }
    }
}

/**
 * Where the reader is, as count/total: the step among the section's, «٥/٩», and for a text of a
 * group its place in the group too, «٥/٩ · ٢/٤».
 */
private fun pagePosition(section: ProgramSection, page: ReaderPage): String {
    val steps = section.trackableItems
    val step = uiDigits("${steps.indexOfFirst { it.id == page.step.id } + 1}/${steps.size}")
    if (!page.inGroup) return step
    return step + "  ·  " + uiDigits("${page.indexInGroup + 1}/${page.step.members.size}")
}

private enum class FooterMode { Counting, MarkDone, Finished }

/**
 * One screen of the reader. A counted step ([ProgramItem.isRepeatable][com.example.dhikra.data.ProgramItem.isRepeatable])
 * shows the bead dial and counts a repetition on every tap anywhere on the screen; any other step
 * is marked done with its button. Once done, the screen invites to swipe up to the next step.
 */
@Composable
internal fun ReaderPageContent(
    page: ReaderPage,
    position: String,
    marks: List<StepMark>,
    done: Int,
    groupComplete: Boolean,
    sectionComplete: Boolean,
    isLast: Boolean,
    textScale: Float,
    lettersPerSecond: Float,
    playing: Boolean,
    actions: ReaderActions,
    modifier: Modifier = Modifier,
    /** The sideways shake of what completes the step, when moving on is tried too early (px). */
    nudge: () -> Float = { 0f },
) {
    val item = page.item
    val counted = item.isRepeatable
    val target = item.target
    val complete = done >= target
    val haptic = LocalHapticFeedback.current

    var cycle by remember { mutableIntStateOf(0) }
    var cycleStart by remember { mutableLongStateOf(0L) }
    var awaitingTap by remember { mutableStateOf(false) }
    var pressed by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }
    val scroll = rememberScrollState()

    LaunchedEffect(complete, playing) {
        if (complete && playing) actions.onTogglePlay()
    }
    LaunchedEffect(confirmReset) {
        if (confirmReset) {
            delay(RESET_CONFIRM_MILLIS)
            confirmReset = false
        }
    }

    val count: () -> Unit = {
        if (!complete) {
            haptic.performHapticFeedback(
                if (done + 1 >= target) HapticFeedbackType.Confirm else HapticFeedbackType.SegmentTick
            )
            if (playing) {
                actions.onRepetitionRead(SystemClock.uptimeMillis() - cycleStart)
                cycle++
            } else {
                actions.onCount()
            }
        }
    }
    val latestCount by rememberUpdatedState(count)
    val undo: () -> Unit = {
        haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
        actions.onUndo()
    }
    val reset: () -> Unit = {
        if (confirmReset) {
            confirmReset = false
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            actions.onReset()
        } else {
            confirmReset = true
        }
    }

    val mode = when {
        complete -> FooterMode.Finished
        counted -> FooterMode.Counting
        else -> FooterMode.MarkDone
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .then(
                if (counted) {
                    // Child buttons consume their own touches; a drag taken by the pager or the
                    // text's scroll cancels the tap.
                    Modifier.pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                pressed = true
                                tryAwaitRelease()
                                pressed = false
                            },
                            onTap = { latestCount() },
                        )
                    }
                } else Modifier
            )
            .safeDrawingPadding()
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
            ReaderTopBar(
                position = position,
                marks = marks,
                groupComplete = groupComplete,
                onSetGroupComplete = actions.onSetGroupComplete,
                onClose = actions.onClose,
            )
            Row(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(SideGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val handOff = remember(scroll) { EdgeHandOff(scroll) }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .then(handOff.modifier),
                    contentAlignment = Alignment.Center,
                ) {
                  Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    // The opening (أعوذ بالله…, بسم الله…) stays put above the text: only the text scrolls.
                    PageOpening(page.item, textScale, Modifier.padding(top = 8.dp))
                    Column(
                        // A text that fits does not scroll, so a swipe on it goes straight to the pager.
                        // A long text scrolls in its own space, its scroll bar showing; the screen around it
                        // (title, dial, buttons) stays in place.
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .fillMaxWidth()
                            .verticalScrollbar(scroll, thumb = Color.White.copy(alpha = 0.35f))
                            .verticalScroll(scroll, enabled = scroll.canScrollForward || scroll.canScrollBackward)
                            .padding(vertical = 8.dp)
                            .padding(end = if (scroll.maxValue > 0) 12.dp else 0.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        PageTexts(
                            page = page,
                            textScale = textScale,
                            lettersPerSecond = lettersPerSecond,
                            playing = playing,
                            cycle = cycle,
                            onCycleStart = { cycleStart = it },
                            onReachedEnd = { awaitingTap = it },
                            onProgress = { followHighlight(scroll, it) },
                        )
                    }
                  }
                }
                if (sideDial) {
                    CounterDial(
                        done = done,
                        target = target,
                        complete = complete,
                        pressed = pressed,
                        onCount = count,
                        modifier = Modifier.fillMaxHeight().graphicsLayer { translationX = nudge() },
                        maxDiameter = SideDialMax,
                    )
                }
            }
            if (counted && !compact) {
                CounterDial(
                    done = done,
                    target = target,
                    complete = complete,
                    pressed = pressed,
                    onCount = count,
                    modifier = Modifier.fillMaxWidth().graphicsLayer { translationX = nudge() },
                )
                Spacer(Modifier.height(24.dp))
            }
            ReaderFooter(
                mode = mode,
                counted = counted,
                done = done,
                isLast = isLast,
                sectionComplete = sectionComplete,
                awaitingTap = playing && awaitingTap,
                confirmReset = confirmReset,
                onUndo = undo,
                onReset = reset,
                onMarkDone = {
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                    actions.onSetComplete(true)
                },
                onUnmarkDone = {
                    haptic.performHapticFeedback(HapticFeedbackType.ToggleOff)
                    actions.onSetComplete(false)
                },
                onNext = actions.onNext,
                playing = playing,
                onTogglePlay = actions.onTogglePlay,
                nudge = nudge,
                modifier = Modifier.widthIn(max = ReaderMaxWidth),
            )
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
 * Between a long text's scroll and the pager: what is left of a swipe on the text (its drag and its
 * fling) goes on to the pager only when the text was already at its end in that direction when the
 * finger went down. A fling through the text stops at its edge; a new swipe from there turns the page.
 */
private class EdgeHandOff(private val scroll: ScrollState) : NestedScrollConnection {
    private var startedAtTop = true
    private var startedAtEnd = true

    // Up the screen (negative) moves on to the next page; down goes back.
    private fun passes(dy: Float) = if (dy < 0) startedAtEnd else startedAtTop

    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset =
        if (available.y == 0f || passes(available.y)) Offset.Zero else available

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity =
        if (available.y == 0f || passes(available.y)) Velocity.Zero else available

    val modifier: Modifier = Modifier
        .pointerInput(Unit) {
            // Notes where the text stands when the finger goes down, without taking the touch.
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                startedAtTop = !scroll.canScrollBackward
                startedAtEnd = !scroll.canScrollForward
            }
        }
        .nestedScroll(this)
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

/** The step's text, source and note (its opening is above, out of the scroll). */
@Composable
private fun PageTexts(
    page: ReaderPage,
    textScale: Float,
    lettersPerSecond: Float,
    playing: Boolean,
    cycle: Int,
    onCycleStart: (Long) -> Unit,
    onReachedEnd: (Boolean) -> Unit,
    onProgress: (Float) -> Unit,
) {
    val item = page.item
    val quran = item.type == ItemType.QURAN
    val text = remember(item.text, quran) { if (quran) ArabicText.withAyahMarkers(item.text) else item.text }
    // A text of a group also shows what its group says.
    val sources = listOf(item.source, if (page.inGroup) page.step.source else "").filter { it.isNotBlank() }
    val notes = listOf(item.note, if (page.inGroup) page.step.note else "").filter { it.isNotBlank() }

    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
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
                onProgress = onProgress,
                modifier = Modifier.fillMaxWidth(),
            )
        }
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
    sectionComplete: Boolean,
    awaitingTap: Boolean,
    confirmReset: Boolean,
    onUndo: () -> Unit,
    onReset: () -> Unit,
    onMarkDone: () -> Unit,
    onUnmarkDone: () -> Unit,
    onNext: () -> Unit,
    playing: Boolean,
    onTogglePlay: () -> Unit,
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
                    FooterMode.Counting -> Hint(
                        stringResource(if (awaitingTap) R.string.reader_waiting else R.string.counter_screen_hint)
                    )
                    FooterMode.MarkDone -> Hint(stringResource(R.string.reader_done_hint))
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
                when (current) {
                    FooterMode.Counting -> CountingBar(
                        done = done,
                        confirmReset = confirmReset,
                        playing = playing,
                        onUndo = onUndo,
                        onReset = onReset,
                        onTogglePlay = onTogglePlay,
                    )
                    FooterMode.MarkDone -> GoldButton(
                        icon = Icons.Filled.Check,
                        label = stringResource(R.string.mark_done),
                        onClick = onMarkDone,
                        // Shakes "no" when the next step is swiped to before this one is done.
                        modifier = side.graphicsLayer { translationX = nudge() },
                    )
                    FooterMode.Finished -> {
                        if (counted) ResetButton(confirmReset, onReset, side, enabled = true)
                        else NightButton(UndoIcon, stringResource(R.string.undo_done), onUnmarkDone, side)
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

/**
 * A counted step's controls: the counters' bar, undo on one side and reset on the other, with
 * reading along — play, or pause while playing — as a quiet gold button in its middle.
 */
@Composable
private fun RowScope.CountingBar(
    done: Int,
    confirmReset: Boolean,
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
    ) {
        PlayToggle(playing = playing, onToggle = onTogglePlay, size = PlayButtonSize)
    }
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
