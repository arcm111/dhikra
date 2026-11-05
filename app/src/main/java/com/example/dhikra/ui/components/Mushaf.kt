package com.example.dhikra.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.example.dhikra.ui.theme.ElementColors
import com.example.dhikra.ui.theme.MushafFont
import com.example.dhikra.ui.theme.ReadingStyles
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Quiet, reverent title of a Quran card: the surah name in gold between two small stars and two
 * hairlines that fade out towards the card edges, with a light caption underneath.
 */
@Composable
fun QuranTitle(
    title: String,
    caption: String,
    colors: ElementColors,
    textScale: Float,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Hairline(colors.accent, towardsStart = true, modifier = Modifier.weight(1f))
            Icon(StarOrnamentIcon, null, tint = colors.accent, modifier = Modifier.padding(horizontal = 8.dp).size(10.dp))
            Text(
                text = title,
                style = ReadingStyles.surahTitle(textScale),
                color = colors.accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Icon(StarOrnamentIcon, null, tint = colors.accent, modifier = Modifier.padding(horizontal = 8.dp).size(10.dp))
            Hairline(colors.accent, towardsStart = false, modifier = Modifier.weight(1f))
        }
        if (caption.isNotBlank()) {
            Text(
                text = caption,
                style = MaterialTheme.typography.labelMedium,
                color = colors.ink.copy(alpha = 0.66f),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

/** A gold line that is solid next to the title and fades out towards the card edge. */
@Composable
private fun Hairline(color: Color, towardsStart: Boolean, modifier: Modifier) {
    // In a row, the first (start-side) line sits on the right in RTL and on the left in LTR.
    val fadesToRight = towardsStart == (LocalLayoutDirection.current == LayoutDirection.Rtl)
    val colors = listOf(Color.Transparent, color.copy(alpha = 0.6f))
    Box(
        modifier
            .height(1.dp)
            .background(Brush.horizontalGradient(if (fadesToRight) colors.reversed() else colors))
    )
}

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
