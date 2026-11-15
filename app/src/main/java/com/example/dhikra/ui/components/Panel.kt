package com.example.dhikra.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A list row's piece of a framed panel (the day's prayer panels, the library's lists): the top
 * (rounded above), a middle, the bottom (rounded below), or all of it.
 */
internal enum class Slice { Top, Middle, Bottom, Single }

/** The [Slice] of a row that is the panel's first ([top]) and/or last ([bottom]). */
internal fun slice(top: Boolean, bottom: Boolean) = when {
    top && bottom -> Slice.Single
    top -> Slice.Top
    bottom -> Slice.Bottom
    else -> Slice.Middle
}

/** The program's section tiles' corners, also the panels'. */
internal val PanelRadius = 12.dp

/**
 * Draws a row's [slice] of a panel: its [fill], rounded at the panel's top or bottom, and the
 * panel's outline — only the sides of a middle slice, so the slices join into one panel.
 */
internal fun Modifier.panelSlice(slice: Slice, fill: Color, border: Color, borderWidth: Dp): Modifier = drawBehind {
    val r = PanelRadius.toPx()
    val stroke = borderWidth.toPx()
    val top = slice == Slice.Top || slice == Slice.Single
    val bottom = slice == Slice.Bottom || slice == Slice.Single
    fun corners(rect: Rect) = RoundRect(
        rect = rect,
        topLeft = CornerRadius(if (top) r else 0f),
        topRight = CornerRadius(if (top) r else 0f),
        bottomRight = CornerRadius(if (bottom) r else 0f),
        bottomLeft = CornerRadius(if (bottom) r else 0f),
    )
    drawPath(Path().apply { addRoundRect(corners(Rect(0f, 0f, size.width, size.height))) }, fill)
    // The outline, inset by half its width; an open edge is pushed out of the row and clipped off.
    val inset = stroke / 2f
    clipRect {
        drawPath(
            Path().apply {
                addRoundRect(
                    corners(
                        Rect(inset, if (top) inset else -stroke * 2, size.width - inset, if (bottom) size.height - inset else size.height + stroke * 2)
                    )
                )
            },
            border,
            style = Stroke(stroke),
        )
    }
}
