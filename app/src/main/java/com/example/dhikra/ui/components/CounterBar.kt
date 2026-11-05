package com.example.dhikra.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.dhikra.R
import com.example.dhikra.data.ArabicText
import com.example.dhikra.ui.theme.ElementColors

private val BarHeight = 56.dp

/**
 * Repetition counter shaped as one large pill: the whole bar is the tap target (tap to count,
 * long press to reset). A rounded fill grows from the start side as the count progresses; the
 * label is drawn twice and clipped, so it stays legible both on the track and on the fill.
 *
 * @param pulse gently breathes to invite a tap (used when the reader reached the end of the text).
 */
@Composable
fun CounterBar(
    done: Int,
    target: Int,
    colors: ElementColors,
    onCount: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
    pulse: Boolean = false,
) {
    val complete = target > 0 && done >= target
    val progress by animateFloatAsState(
        targetValue = if (target <= 0) 0f else (done.toFloat() / target).coerceIn(0f, 1f),
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
        label = "counterProgress",
    )
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(if (pressed) 0.97f else 1f, label = "counterPress")
    val pulseScale = remember { Animatable(1f) }
    LaunchedEffect(pulse) {
        if (pulse) {
            while (true) {
                pulseScale.animateTo(1.03f, tween(500))
                pulseScale.animateTo(1f, tween(500))
            }
        } else {
            pulseScale.animateTo(1f, tween(150))
        }
    }

    val description = stringResource(
        R.string.counter_description,
        ArabicText.toArabicDigits(done),
        ArabicText.toArabicDigits(target),
    )
    val state = if (complete) stringResource(R.string.counter_done) else ""
    val countLabel = stringResource(R.string.reader_waiting)
    val resetLabel = stringResource(R.string.counter_reset)

    Box(
        modifier = modifier
            .height(BarHeight)
            .graphicsLayer {
                val scale = pressScale * pulseScale.value
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(colors.accent.copy(alpha = 0.10f))
            .combinedClickable(
                interactionSource = interaction,
                indication = ripple(color = colors.accent),
                onClickLabel = countLabel,
                onLongClickLabel = resetLabel,
                onLongClick = onReset,
                onClick = { if (!complete) onCount() },
            )
            .semantics {
                contentDescription = description
                stateDescription = state
            },
    ) {
        BarContent(done, target, complete, colors.accent)
        BarContent(
            done = done,
            target = target,
            complete = complete,
            color = colors.onAccent,
            modifier = Modifier
                .matchParentSize()
                .drawWithContent {
                    if (progress <= 0f) return@drawWithContent
                    // Never thinner than a circle, so the count always sits in a round "bead".
                    val width = size.height + (size.width - size.height) * progress
                    val left = if (layoutDirection == LayoutDirection.Rtl) size.width - width else 0f
                    val fill = Path().apply {
                        addRoundRect(
                            RoundRect(left, 0f, left + width, size.height, CornerRadius(size.height / 2f))
                        )
                    }
                    clipPath(fill) {
                        drawRect(colors.accent)
                        this@drawWithContent.drawContent()
                    }
                },
        )
    }
}

/** One layer of the bar's label: count in a round "bead" at the start, hint, then the target. */
@Composable
private fun BarContent(
    done: Int,
    target: Int,
    complete: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxSize()
            .clearAndSetSemantics { },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(BarHeight), contentAlignment = Alignment.Center) {
            if (complete) {
                Icon(Icons.Filled.Check, null, tint = color, modifier = Modifier.size(24.dp))
            } else {
                AnimatedContent(
                    targetState = done,
                    transitionSpec = {
                        (slideInVertically { it / 2 } + fadeIn()) togetherWith
                            (slideOutVertically { -it / 2 } + fadeOut())
                    },
                    label = "barCount",
                ) { value ->
                    Text(
                        text = ArabicText.toArabicDigits(value),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = color,
                        maxLines = 1,
                    )
                }
            }
        }
        Text(
            text = stringResource(if (complete) R.string.counter_done else R.string.reader_waiting),
            style = MaterialTheme.typography.labelLarge,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = stringResource(R.string.counter_of, ArabicText.toArabicDigits(target)),
            style = MaterialTheme.typography.labelLarge,
            color = color.copy(alpha = 0.8f),
            maxLines = 1,
        )
        Spacer(Modifier.width(20.dp))
    }
}

/**
 * Full-width pill that marks a single-pass step as done. Filled when it is the current step so
 * the next action is obvious, tonal otherwise.
 */
@Composable
fun DoneBar(colors: ElementColors, emphasized: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (emphasized) colors.accent else colors.accent.copy(alpha = 0.12f),
            contentColor = if (emphasized) colors.onAccent else colors.accent,
        ),
        elevation = null,
        modifier = modifier.fillMaxWidth().height(52.dp),
    ) {
        Icon(Icons.Filled.Check, null, Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.mark_done), style = MaterialTheme.typography.labelLarge)
    }
}
