package io.github.arcm111.dhikra.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * A vertical scroll bar for [state], drawn on the end side of the scrolling area — put this
 * modifier before `verticalScroll(state)`. Only for content longer than the area: a faint thumb,
 * as tall as the part in view, with no track. It shows while the content scrolls (and for a moment
 * when it first appears, so a long text is seen to scroll), then fades away.
 */
fun Modifier.verticalScrollbar(
    state: ScrollState,
    thumb: Color,
    width: Dp = 3.dp,
): Modifier = composed {
    var visible by remember { mutableStateOf(true) }
    LaunchedEffect(state.isScrollInProgress) {
        if (state.isScrollInProgress) {
            visible = true
        } else {
            delay(1_200)
            visible = false
        }
    }
    val shown by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(if (visible) 150 else 600),
        label = "scrollbar",
    )
    drawWithContent {
        drawContent()
        val max = state.maxValue
        if (shown == 0f || max <= 0 || max == Int.MAX_VALUE) return@drawWithContent
        val w = width.toPx()
        val viewport = size.height
        val thumbHeight = (viewport * viewport / (viewport + max)).coerceAtLeast(32.dp.toPx()).coerceAtMost(viewport)
        val top = (viewport - thumbHeight) * state.value / max
        val x = if (layoutDirection == LayoutDirection.Rtl) 0f else size.width - w
        drawRoundRect(
            thumb.copy(alpha = thumb.alpha * shown),
            topLeft = Offset(x, top),
            size = Size(w, thumbHeight),
            cornerRadius = CornerRadius(w / 2, w / 2),
        )
    }
}
