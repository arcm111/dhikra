package com.example.dhikra.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.util.lerp
import androidx.core.view.WindowCompat
import com.example.dhikra.R
import com.example.dhikra.data.ArabicText
import com.example.dhikra.ui.theme.ElementColors
import com.example.dhikra.ui.theme.MustardLight
import com.example.dhikra.ui.theme.ForestLight
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import com.example.dhikra.ui.uiDigits

// The pieces of the night-time look shared by the full-screen reader: a misbaha-like counter dial,
// the buttons and the dark palette of the texts.

/** Up to this many repetitions, every one is drawn as its own bead around the dial. */
private const val MAX_BEADS = 100

private const val PEARL_RATIO = 0.58f

internal val NightInk = Color.White.copy(alpha = 0.92f)
internal val GoldInk = Color(0xFF2A2208)

/** What texts are drawn with on the night background. */
internal val NightTextColors = ElementColors(
    container = Color.Transparent,
    border = MustardLight,
    accent = MustardLight,
    onAccent = GoldInk,
    ink = NightInk,
    highlight = MustardLight.copy(alpha = 0.25f),
)

/**
 * The bead ring with the pearl in the middle. Mirrors its direction in right-to-left layouts.
 * Beads light up one by one with a soft wave out of the pearl, and reaching [target] bursts into
 * gold sparkles. The dial only draws the count: it reports taps through [onCount] when asked to
 * count by accessibility services, the screen around it handles the touch itself.
 *
 * It is as wide as [modifier] lets it be, up to [maxDiameter]; by default no more than 44% of
 * the window's height, so that the text above it keeps its room.
 */
@Composable
internal fun CounterDial(
    done: Int,
    target: Int,
    complete: Boolean,
    pressed: Boolean,
    onCount: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    maxDiameter: Dp = Dp.Unspecified,
    /** The ring's progress, when it is not [done] over [target] (the daily counter's stages). */
    ringProgress: Float? = null,
) {
    val progress by animateFloatAsState(
        targetValue = ringProgress?.coerceIn(0f, 1f) ?: if (target <= 0) 0f else (done.toFloat() / target).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing),
        label = "dialProgress",
    )
    val lit by animateColorAsState(if (complete) MustardLight else ForestLight, tween(600), label = "dialColor")
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
        uiDigits(done),
        uiDigits(target),
    )
    val countLabel = stringResource(R.string.reader_waiting)
    val windowHeight = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.height.toDp() }

    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val cap = if (maxDiameter.isSpecified) maxDiameter else minOf(330.dp, windowHeight * 0.44f)
        val diameter = minOf(maxWidth, maxHeight, cap)
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
                    drawPath(sparkles, MustardLight.copy(alpha = 1f - t))
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

/** The glowing central bead: green while counting, mustard once the target is reached. */
@Composable
private fun Pearl(done: Int, target: Int, complete: Boolean, modifier: Modifier = Modifier) {
    val light by animateColorAsState(if (complete) Color(0xFFF0DC9A) else Color(0xFF2F7A57), tween(600), label = "pearlLight")
    val deep by animateColorAsState(if (complete) Color(0xFFA88A2E) else Color(0xFF0F3524), tween(600), label = "pearlDeep")
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
                    text = uiDigits(value),
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
                        else stringResource(R.string.counter_of, uiDigits(target)),
                        style = MaterialTheme.typography.titleSmall,
                        color = ink.copy(alpha = 0.75f),
                    )
                }
            }
        }
    }
}

@Composable
internal fun NightButton(
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

/** The reader is always dark: use light navigation bar icons while it is shown. */
@Composable
internal fun LightNavigationBarIcons() {
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
