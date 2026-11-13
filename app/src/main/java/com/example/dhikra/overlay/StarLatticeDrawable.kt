package com.example.dhikra.overlay

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Drawable
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * The full-screen counter's texture, for the floating counter's box: a lattice of faint
 * eight-pointed stars in staggered rows (as `Modifier.starLattice` draws it), kept inside the
 * box's rounded corners.
 */
class StarLatticeDrawable(
    density: Float,
    color: Int = 0x09FFFFFF,
    private val spacing: Float = 58f * density,
    private val starRadius: Float = 15f * density,
    private val cornerRadius: Float = 18f * density,
) : Drawable() {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1f * density
        this.color = color
    }
    private val stars = Path()
    private val clip = Path()

    override fun onBoundsChange(bounds: Rect) {
        stars.reset()
        val rowHeight = spacing * 0.866f
        var row = 0
        var y = bounds.top.toFloat()
        while (y < bounds.bottom + spacing) {
            var x = bounds.left + if (row % 2 == 0) 0f else spacing / 2f
            while (x < bounds.right + spacing) {
                addStar(x, y)
                x += spacing
            }
            y += rowHeight
            row++
        }
        clip.reset()
        clip.addRoundRect(RectF(bounds), cornerRadius, cornerRadius, Path.Direction.CW)
    }

    private fun addStar(cx: Float, cy: Float) {
        for (i in 0 until 16) {
            val r = if (i % 2 == 0) starRadius else starRadius * 0.765f
            val angle = (i * 22.5 - 90) * PI / 180
            val x = cx + r * cos(angle).toFloat()
            val y = cy + r * sin(angle).toFloat()
            if (i == 0) stars.moveTo(x, y) else stars.lineTo(x, y)
        }
        stars.close()
    }

    override fun draw(canvas: Canvas) {
        canvas.save()
        canvas.clipPath(clip)
        canvas.drawPath(stars, paint)
        canvas.restore()
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}
