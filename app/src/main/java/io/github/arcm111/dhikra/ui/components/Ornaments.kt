package io.github.arcm111.dhikra.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Adds an eight-pointed star (the Rub el Hizb ۞ silhouette: two overlapping squares) to the path.
 * [innerRatio] 0.765 gives the exact two-squares outline; smaller values make it spikier.
 */
fun Path.addEightPointStar(center: Offset, radius: Float, innerRatio: Float = 0.765f) {
    for (i in 0 until 16) {
        val r = if (i % 2 == 0) radius else radius * innerRatio
        val angle = (i * 22.5 - 90) * PI / 180
        val x = center.x + r * cos(angle).toFloat()
        val y = center.y + r * sin(angle).toFloat()
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

/** Adds a four-pointed sparkle (✦) to the path. */
fun Path.addSparkle(center: Offset, radius: Float) {
    val waist = radius * 0.28f
    moveTo(center.x, center.y - radius)
    quadraticTo(center.x + waist * 0.35f, center.y - waist * 0.35f, center.x + radius, center.y)
    quadraticTo(center.x + waist * 0.35f, center.y + waist * 0.35f, center.x, center.y + radius)
    quadraticTo(center.x - waist * 0.35f, center.y + waist * 0.35f, center.x - radius, center.y)
    quadraticTo(center.x - waist * 0.35f, center.y - waist * 0.35f, center.x, center.y - radius)
    close()
}

/** Adds a diamond (rhombus) of the given half extents to the path. */
fun Path.addDiamond(center: Offset, halfWidth: Float, halfHeight: Float = halfWidth) {
    moveTo(center.x, center.y - halfHeight)
    lineTo(center.x + halfWidth, center.y)
    lineTo(center.x, center.y + halfHeight)
    lineTo(center.x - halfWidth, center.y)
    close()
}

/**
 * Draws a faint tessellation of eight-pointed star outlines behind the content: the geometric
 * texture used by the branded header and the immersive counter.
 */
fun Modifier.starLattice(color: Color, spacing: Dp = 46.dp, starRadius: Dp = 12.dp): Modifier =
    drawWithCache {
        val step = spacing.toPx()
        val radius = starRadius.toPx()
        val rowHeight = step * 0.866f
        val path = Path()
        var row = 0
        var y = 0f
        while (y < size.height + step) {
            var x = if (row % 2 == 0) 0f else step / 2f
            while (x < size.width + step) {
                path.addEightPointStar(Offset(x, y), radius)
                x += step
            }
            y += rowHeight
            row++
        }
        val stroke = Stroke(width = 1.dp.toPx())
        onDrawBehind { drawPath(path, color, style = stroke) }
    }

/** Small rounded label such as "×١٠٠" or "صحيح". */
@Composable
fun Pill(text: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = color.copy(alpha = 0.10f),
        contentColor = color,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
        )
    }
}
