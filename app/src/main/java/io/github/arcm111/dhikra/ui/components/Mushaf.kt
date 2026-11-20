package io.github.arcm111.dhikra.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import io.github.arcm111.dhikra.ui.theme.ElementColors
import io.github.arcm111.dhikra.ui.theme.MushafFont
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Ornamental end-of-ayah marker: a gold rosette (double ring with eight petals) with the ayah
 * number in its centre. Rendered inline in the Quran text in place of the plain ۝ sign.
 */
@Composable
fun AyahMarker(number: String, colors: ElementColors, numberSize: TextUnit, modifier: Modifier = Modifier) {
    val gold = colors.accent
    Box(
        modifier = modifier
            .fillMaxSize()
            .drawWithCache {
                val ring = 1.1.dp.toPx()
                val petal = 1.2.dp.toPx()
                val inset = petal + ring
                val outerSize = Size(size.width - inset * 2, size.height - inset * 2)
                val innerInset = inset + 2.4.dp.toPx()
                val innerSize = Size(size.width - innerInset * 2, size.height - innerInset * 2)
                val rx = outerSize.width / 2f
                val ry = outerSize.height / 2f
                val cx = size.width / 2f
                val cy = size.height / 2f
                val petals = Path()
                for (i in 0 until 8) {
                    val angle = (i * 45.0 + 22.5) * PI / 180
                    val point = Offset(cx + rx * cos(angle).toFloat(), cy + ry * sin(angle).toFloat())
                    petals.addOval(Rect(point, petal))
                }
                val ringStroke = Stroke(ring)
                val innerStroke = Stroke(0.6.dp.toPx())
                onDrawBehind {
                    drawOval(gold.copy(alpha = 0.12f), Offset(inset, inset), outerSize)
                    drawPath(petals, gold)
                    drawOval(gold, Offset(inset, inset), outerSize, style = ringStroke)
                    drawOval(
                        color = gold.copy(alpha = 0.7f),
                        topLeft = Offset(innerInset, innerInset),
                        size = innerSize,
                        style = innerStroke,
                    )
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = number,
            style = TextStyle(
                fontFamily = MushafFont,
                fontWeight = FontWeight.Bold,
                fontSize = numberSize,
                lineHeight = numberSize,
                lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
                textAlign = TextAlign.Center,
                color = colors.ink,
            ),
            maxLines = 1,
            softWrap = false,
        )
    }
}
