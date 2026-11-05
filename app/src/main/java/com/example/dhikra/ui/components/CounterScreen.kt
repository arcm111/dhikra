package com.example.dhikra.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.core.view.WindowCompat
import com.example.dhikra.R
import com.example.dhikra.data.ArabicText
import com.example.dhikra.ui.theme.ElementColors
import com.example.dhikra.ui.theme.GoldLight
import com.example.dhikra.ui.theme.LocalDhikraPalette
import com.example.dhikra.ui.theme.Mint
import com.example.dhikra.ui.theme.ReadingStyles
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.time.Duration.Companion.seconds

/** Up to this many repetitions, every one is drawn as its own bead around the dial. */
private const val MAX_BEADS = 100

private val NightInk = Color.White.copy(alpha = 0.92f)
private val GoldInk = Color(0xFF3B2A00)

/**
 * Immersive counter, designed like a misbaha: a ring of beads that light up one by one around a
 * glowing central "pearl" holding the count. Tap anywhere to count; every tap sends a soft wave
 * out of the pearl, and completing the target bursts into gold sparkles. Undo and reset (with a
 * second tap to confirm) sit at the bottom, and back or the close button returns to the schedule.
 *
 * While [playing], each tap reports the elapsed time of the repetition through
 * [onRepetitionRead] (which also counts it), so the reader keeps adapting to the user's pace.
 */
@Composable
fun CounterScreen(
    title: String,
    text: String,
    opening: String,
    done: Int,
    target: Int,
    onCount: () -> Unit,
    onUndo: () -> Unit,
    onReset: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    quran: Boolean = false,
    playing: Boolean = false,
    onRepetitionRead: (elapsedMs: Long) -> Unit = {},
) {
    BackHandler(onBack = onClose)
    LightNavigationBarIcons()
    val haptic = LocalHapticFeedback.current
    val palette = LocalDhikraPalette.current

    // The count lives in the ViewModel; this screen only reports taps, so there is a single
    // source of truth and the number can never drift from the saved progress.
    var cycleStart by remember { mutableLongStateOf(SystemClock.uptimeMillis()) }
    LaunchedEffect(playing) { cycleStart = SystemClock.uptimeMillis() }

    val complete = target > 0 && done >= target
    var pressed by remember { mutableStateOf(false) }
    val count by rememberUpdatedState {
        if (!complete) {
            haptic.performHapticFeedback(
                if (done + 1 >= target) HapticFeedbackType.Confirm else HapticFeedbackType.SegmentTick
            )
            if (playing) {
                val now = SystemClock.uptimeMillis()
                onRepetitionRead(now - cycleStart)
                cycleStart = now
            } else {
                onCount()
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(palette.nightStart, palette.nightEnd)))
            .starLattice(Color.White.copy(alpha = 0.035f), spacing = 58.dp, starRadius = 15.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        tryAwaitRelease()
                        pressed = false
                    },
                    onTap = { count() },
                )
            },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CounterTopBar(title, target, onClose)
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CounterTexts(opening, text, quran)
            }
            CounterDial(
                done = done,
                target = target,
                complete = complete,
                pressed = pressed,
                onCount = { count() },
            )
            Spacer(Modifier.height(24.dp))
            CounterFooter(
                complete = complete,
                canUndo = done > 0,
                onUndo = {
                    haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                    onUndo()
                },
                onReset = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onReset()
                },
                onClose = onClose,
            )
        }
    }
}

@Composable
private fun CounterTopBar(title: String, target: Int, onClose: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(56.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onClose) {
            Icon(Icons.Filled.Close, stringResource(R.string.close), tint = NightInk)
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = NightInk,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
        )
        Box(Modifier.widthIn(min = 48.dp), contentAlignment = Alignment.Center) {
            if (target > 0) {
                Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.10f), contentColor = GoldLight) {
                    Text(
                        text = stringResource(R.string.times_count, ArabicText.toArabicDigits(target)),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    )
                }
            }
        }
    }
}

/** Opening (Basmala…) and the recited text, centred above the dial; scrolls when long. */
@Composable
private fun CounterTexts(opening: String, text: String, quran: Boolean) {
    val nightColors = remember {
        ElementColors(
            container = Color.Transparent,
            border = GoldLight,
            accent = GoldLight,
            onAccent = GoldInk,
            ink = NightInk,
            highlight = GoldLight.copy(alpha = 0.25f),
        )
    }
    val displayText = remember(text, quran) { if (quran) ArabicText.withAyahMarkers(text) else text }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (opening.isNotBlank()) {
            Text(
                text = opening,
                style = if (quran) ReadingStyles.quranOpening(0.95f)
                else MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.Center),
                color = GoldLight,
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
            )
        }
        if (displayText.isNotBlank()) {
            ReaderText(
                text = displayText,
                style = (if (quran) ReadingStyles.quran(1f) else ReadingStyles.dhikr(1f))
                    .copy(textAlign = TextAlign.Center),
                colors = nightColors,
                ornamentalAyahMarkers = quran,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** The bead ring with the pearl in the middle. Mirrors its direction in right-to-left layouts. */
@Composable
private fun CounterDial(
    done: Int,
    target: Int,
    complete: Boolean,
    pressed: Boolean,
    onCount: () -> Unit,
) {
    val progress by animateFloatAsState(
        targetValue = if (target <= 0) 0f else (done.toFloat() / target).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing),
        label = "dialProgress",
    )
    val lit by animateColorAsState(if (complete) GoldLight else Mint, tween(600), label = "dialColor")
    val pearlScale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = 700f),
        label = "pearlPress",
    )

    // A wave leaves the pearl on every new count.
    val wave = remember { Animatable(1f) }
    var lastDone by remember { mutableIntStateOf(done) }
    LaunchedEffect(done) {
        val counted = done > lastDone
        lastDone = done
        if (counted) {
            wave.snapTo(0f)
            wave.animateTo(1f, tween(700, easing = LinearOutSlowInEasing))
        }
    }
    // Gold sparkles burst out once when the target is reached.
    val burst = remember { Animatable(if (complete) 1f else 0f) }
    LaunchedEffect(complete) {
        if (complete) burst.animateTo(1f, tween(1400, easing = LinearOutSlowInEasing))
        else burst.snapTo(0f)
    }

    val description = stringResource(
        R.string.counter_description,
        ArabicText.toArabicDigits(done),
        ArabicText.toArabicDigits(target),
    )
    val countLabel = stringResource(R.string.reader_waiting)
    val windowHeight = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.height.toDp() }

    BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val diameter = minOf(maxWidth, 330.dp, windowHeight * 0.44f)
        Box(
            modifier = Modifier
                .size(diameter)
                .semantics {
                    contentDescription = description
                    role = Role.Button
                    onClick(label = countLabel) { onCount(); true }
                },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val direction = if (layoutDirection == LayoutDirection.Rtl) -1f else 1f
                val outer = size.minDimension / 2f
                val beadMax = 7.dp.toPx()
                val ring = outer - beadMax - 3.dp.toPx()
                val pearl = size.minDimension * PEARL_RATIO / 2f

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(lit.copy(alpha = 0.22f), Color.Transparent),
                        center = center,
                        radius = outer,
                    ),
                    radius = outer,
                )
                // The misbaha's string, and the progress travelling along it.
                drawCircle(Color.White.copy(alpha = 0.10f), radius = ring, style = Stroke(1.5.dp.toPx()))
                drawArc(
                    color = lit.copy(alpha = 0.85f),
                    startAngle = -90f,
                    sweepAngle = 360f * progress * direction,
                    useCenter = false,
                    topLeft = Offset(center.x - ring, center.y - ring),
                    size = Size(ring * 2, ring * 2),
                    style = Stroke(3.dp.toPx(), cap = StrokeCap.Round),
                )
                if (target in 1..MAX_BEADS) {
                    val spacing = (2 * PI * ring / target).toFloat()
                    val radius = (spacing * 0.3f).coerceIn(2.2.dp.toPx(), beadMax)
                    for (i in 0 until target) {
                        val angle = -PI / 2 + direction * 2 * PI * i / target
                        val point = Offset(
                            center.x + ring * cos(angle).toFloat(),
                            center.y + ring * sin(angle).toFloat(),
                        )
                        val isLit = i < done
                        drawCircle(
                            color = if (isLit) lit else Color.White.copy(alpha = 0.18f),
                            radius = if (isLit) radius else radius * 0.78f,
                            center = point,
                        )
                        if (isLit && i == done - 1 && wave.value < 1f) {
                            drawCircle(
                                color = lit.copy(alpha = 0.45f * (1f - wave.value)),
                                radius = radius * (1.4f + 1.6f * wave.value),
                                center = point,
                            )
                        }
                    }
                }
                if (wave.value < 1f) {
                    drawCircle(
                        color = lit.copy(alpha = 0.5f * (1f - wave.value)),
                        radius = lerp(pearl, ring, wave.value),
                        style = Stroke(2.dp.toPx()),
                    )
                }
                if (burst.value > 0f && burst.value < 1f) {
                    val t = burst.value
                    val sparkles = Path()
                    for (k in 0 until 12) {
                        val angle = k * PI / 6 + PI / 12
                        val distance = lerp(pearl * 0.9f, outer + 18.dp.toPx(), t)
                        sparkles.addSparkle(
                            center = Offset(
                                center.x + distance * cos(angle).toFloat(),
                                center.y + distance * sin(angle).toFloat(),
                            ),
                            radius = (if (k % 2 == 0) 9.dp else 6.dp).toPx() * (1f - 0.5f * t),
                        )
                    }
                    drawPath(sparkles, GoldLight.copy(alpha = 1f - t))
                }
            }
            Pearl(
                done = done,
                target = target,
                complete = complete,
                modifier = Modifier
                    .size(diameter * PEARL_RATIO)
                    .graphicsLayer {
                        scaleX = pearlScale
                        scaleY = pearlScale
                    },
            )
        }
    }
}

private const val PEARL_RATIO = 0.58f

/** The glowing central bead: emerald while counting, gold once the target is reached. */
@Composable
private fun Pearl(done: Int, target: Int, complete: Boolean, modifier: Modifier = Modifier) {
    val light by animateColorAsState(if (complete) Color(0xFFF3D697) else Color(0xFF1F9A7E), tween(600), label = "pearlLight")
    val deep by animateColorAsState(if (complete) Color(0xFFA6772A) else Color(0xFF0A4A3D), tween(600), label = "pearlDeep")
    val ink by animateColorAsState(if (complete) GoldInk else Color.White, tween(600), label = "pearlInk")
    Box(
        modifier = modifier
            .shadow(elevation = 24.dp, shape = CircleShape, ambientColor = light, spotColor = light)
            .drawWithCache {
                val body = Brush.radialGradient(
                    colors = listOf(light, deep),
                    center = Offset(size.width * 0.36f, size.height * 0.28f),
                    radius = size.width * 0.9f,
                )
                val rim = Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.45f), Color.White.copy(alpha = 0.04f))
                )
                val shineCenter = Offset(size.width * 0.36f, size.height * 0.26f)
                val shine = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.22f), Color.Transparent),
                    center = shineCenter,
                    radius = size.width * 0.3f,
                )
                val rimStroke = Stroke(1.5.dp.toPx())
                onDrawBehind {
                    drawCircle(body)
                    drawCircle(shine, radius = size.width * 0.3f, center = shineCenter)
                    drawCircle(rim, radius = size.minDimension / 2f - rimStroke.width / 2f, style = rimStroke)
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AnimatedContent(
                targetState = done,
                transitionSpec = {
                    (slideInVertically { it / 3 } + fadeIn(tween(220))) togetherWith
                        (slideOutVertically { -it / 3 } + fadeOut(tween(120))) using
                        SizeTransform(clip = false)
                },
                label = "pearlCount",
            ) { value ->
                Text(
                    text = ArabicText.toArabicDigits(value),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = ink,
                    maxLines = 1,
                )
            }
            AnimatedContent(targetState = complete, label = "pearlCaption") { finished ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (finished) {
                        Icon(Icons.Filled.Check, null, tint = ink, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(
                        text = if (finished) stringResource(R.string.counter_done)
                        else stringResource(R.string.counter_of, ArabicText.toArabicDigits(target)),
                        style = MaterialTheme.typography.titleSmall,
                        color = ink.copy(alpha = 0.75f),
                    )
                }
            }
        }
    }
}

/** Hint plus the undo / reset actions, or "continue" once the target is reached. */
@Composable
private fun CounterFooter(
    complete: Boolean,
    canUndo: Boolean,
    onUndo: () -> Unit,
    onReset: () -> Unit,
    onClose: () -> Unit,
) {
    var confirmReset by remember { mutableStateOf(false) }
    LaunchedEffect(confirmReset) {
        if (confirmReset) {
            delay(3.seconds)
            confirmReset = false
        }
    }
    val reset = {
        if (confirmReset) {
            confirmReset = false
            onReset()
        } else {
            confirmReset = true
        }
    }
    val resetLabel = stringResource(if (confirmReset) R.string.counter_reset_confirm else R.string.counter_reset)

    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(if (complete) R.string.counter_complete_hint else R.string.counter_screen_hint),
            style = MaterialTheme.typography.labelLarge,
            color = if (complete) GoldLight else Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(14.dp))
        if (complete) {
            Button(
                onClick = onClose,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = GoldLight, contentColor = GoldInk),
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Icon(Icons.Filled.Check, null, Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.counter_continue), style = MaterialTheme.typography.labelLarge)
            }
            Spacer(Modifier.height(8.dp))
            NightButton(Icons.Filled.Refresh, resetLabel, onClick = reset, modifier = Modifier.fillMaxWidth())
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                NightButton(UndoIcon, stringResource(R.string.counter_undo), onClick = onUndo, enabled = canUndo, modifier = Modifier.weight(1f))
                NightButton(Icons.Filled.Refresh, resetLabel, onClick = reset, enabled = canUndo, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun NightButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        border = BorderStroke(1.dp, Color.White.copy(alpha = if (enabled) 0.28f else 0.12f)),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = NightInk,
            disabledContentColor = Color.White.copy(alpha = 0.35f),
        ),
        modifier = modifier.height(48.dp),
    ) {
        Icon(icon, null, Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** The counter is always dark: use light navigation bar icons while it is shown. */
@Composable
private fun LightNavigationBarIcons() {
    val view = LocalView.current
    if (view.isInEditMode) return
    DisposableEffect(view) {
        val window = view.context.findActivity()?.window
        if (window == null) {
            onDispose { }
        } else {
            val controller = WindowCompat.getInsetsController(window, view)
            val previous = controller.isAppearanceLightNavigationBars
            controller.isAppearanceLightNavigationBars = false
            onDispose { controller.isAppearanceLightNavigationBars = previous }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
