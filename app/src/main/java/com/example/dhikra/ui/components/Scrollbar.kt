package com.example.dhikra.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * A vertical scroll bar for [state], drawn on the end side of the scrolling area — put this
 * modifier before `verticalScroll(state)`. It shows only when the content is longer than the area,
 * and stays shown, so a long text is seen to scroll: a faint track and a thumb as tall as the part
 * in view.
 */
fun Modifier.verticalScrollbar(
    state: ScrollState,
    thumb: Color,
    track: Color = thumb.copy(alpha = thumb.alpha * 0.25f),
    width: Dp = 3.dp,
): Modifier = drawWithContent {
    drawContent()
    val max = state.maxValue
    if (max <= 0 || max == Int.MAX_VALUE) return@drawWithContent
    val w = width.toPx()
    val viewport = size.height
    val thumbHeight = (viewport * viewport / (viewport + max)).coerceAtLeast(32.dp.toPx()).coerceAtMost(viewport)
    val top = (viewport - thumbHeight) * state.value / max
    val x = if (layoutDirection == LayoutDirection.Rtl) 0f else size.width - w
    val radius = CornerRadius(w / 2, w / 2)
    drawRoundRect(track, topLeft = Offset(x, 0f), size = Size(w, viewport), cornerRadius = radius)
    drawRoundRect(thumb, topLeft = Offset(x, top), size = Size(w, thumbHeight), cornerRadius = radius)
}
