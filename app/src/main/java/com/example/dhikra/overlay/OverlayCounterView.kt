package com.example.dhikra.overlay

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.core.content.res.ResourcesCompat
import com.example.dhikra.R
import com.example.dhikra.data.DailyCounter
import com.example.dhikra.ui.AppLanguage
import com.example.dhikra.ui.uiDigits
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * The floating counter's dial, after the app's counter dial: in a dish of the night green curved
 * inward, the misbaha's string — a gold head bead and 33 beads lit in mint one by one, starting again at 1 after each 33 —
 * round a nearly flat green button with today's count. While reading along ([cycle] set) the
 * string fills in gold once per repetition instead, and the bubble pulses when the repetition is
 * read ([awaiting]) until a tap counts it.
 *
 * A tap counts, a drag moves it, a long press expands it. Its size is [scale] times [DIAMETER_DP].
 */
@SuppressLint("ViewConstructor")
class OverlayCounterView(
    context: Context,
    private val onTap: () -> Unit,
    private val onLongPress: () -> Unit,
    private val onDrag: (dx: Float, dy: Float) -> Unit,
    private val onDragEnd: () -> Unit,
    /** A finger came down on it (before it is known to be a tap, a drag or a long press). */
    private val onTouched: () -> Unit = {},
) : View(context) {

    var count: Int = 0
        set(value) {
            // One more: a wave leaves the pearl, as on the app's dial.
            if (value == field + 1) wave.start()
            field = value
            contentDescription = uiDigits("$value/${DailyCounter.target(value)}")
            invalidate()
        }

    /** How far the repetition being read along is (0 to 1); null when not reading along. */
    var cycle: Float? = null
        set(value) {
            if (field == value) return
            field = value
            invalidate()
        }

    /** The repetition is read: the bubble pulses until a tap counts it. */
    var awaiting: Boolean = false
        set(value) {
            if (field == value) return
            field = value
            if (value) pulse.start() else {
                pulse.cancel()
                scaleX = 1f
                scaleY = 1f
            }
        }

    /** Whether the dial sits in its own dish (the bubble), or on the box's ground (no dish, no rim). */
    var disc: Boolean = true
        set(value) {
            field = value
            invalidate()
        }

    /** The bubble's size, as a share of the default ([DIAMETER_DP]); set from the app's settings. */
    var scale: Float = 1f
        set(value) {
            val clamped = value.coerceIn(MIN_SCALE, MAX_SCALE)
            if (field == clamped) return
            field = clamped
            requestLayout()
            invalidate()
        }

    // The pearl gives under the finger, and springs back when let go.
    private var pearlScale = 1f
    private var press: ValueAnimator? = null

    private fun pressTo(target: Float, millis: Long, overshoot: Boolean) {
        press?.cancel()
        press = ValueAnimator.ofFloat(pearlScale, target).apply {
            duration = millis
            interpolator = if (overshoot) OvershootInterpolator(3f) else DecelerateInterpolator()
            addUpdateListener {
                pearlScale = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    /** The wave of a count, from the pearl out to the string (0 to 1; 1 is none). */
    private var waveAt = 1f
    private val wave = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 650
        interpolator = DecelerateInterpolator()
        addUpdateListener {
            waveAt = it.animatedValue as Float
            invalidate()
        }
    }

    private val pulse = ValueAnimator.ofFloat(1f, 1.06f).apply {
        duration = 520
        repeatMode = ValueAnimator.REVERSE
        repeatCount = ValueAnimator.INFINITE
        addUpdateListener {
            val value = it.animatedValue as Float
            scaleX = value
            scaleY = value
        }
    }

    private val density = resources.displayMetrics.density
    private val semibold = ResourcesCompat.getFont(context, R.font.ibm_plex_sans_arabic_semibold) ?: Typeface.DEFAULT_BOLD
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val countText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = WHITE
        typeface = semibold
        textAlign = Paint.Align.CENTER
    }
    private val arc = RectF()

    private val slop = ViewConfiguration.get(context).scaledTouchSlop
    private val longPressTimeout = ViewConfiguration.getLongPressTimeout().toLong()
    private var downX = 0f
    private var downY = 0f
    private var lastX = 0f
    private var lastY = 0f
    private var dragging = false
    private var longPressed = false
    private val longPress = Runnable {
        longPressed = true
        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        onLongPress()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // The dial, and room round it for the pulse to grow into.
        val size = sizePx(density, scale)
        setMeasuredDimension(size, size)
    }

    override fun onDetachedFromWindow() {
        pulse.cancel()
        wave.cancel()
        press?.cancel()
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        val cx = width / 2f
        val cy = height / 2f
        val outer = width / 2f - MARGIN_DP * density
        // Sizes follow the dial's, as the app's dial is drawn at about 330 dp.
        val unit = outer / 50f

        // The dish the button sits in, curved inward: shaded where the light from the upper left
        // cannot reach, lit on the far side, darker towards its lip. Not in the box, whose own
        // ground carries the dial.
        if (disc) {
            paint.color = OPAQUE
            paint.shader = LinearGradient(
                cx - outer, cy - outer, cx + outer, cy + outer,
                DISH_SHADE, DISH_LIT, Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(cx, cy, outer, paint)
            paint.shader = RadialGradient(
                cx, cy, outer, intArrayOf(0x00000000, 0x00000000, 0x47000000), floatArrayOf(0f, 0.72f, 1f), Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(cx, cy, outer, paint)
            paint.shader = null
            stroke.shader = null
            stroke.strokeWidth = 1f * density
            stroke.color = 0x1FFFFFFF
            canvas.drawCircle(cx, cy, outer - stroke.strokeWidth / 2f, stroke)
        }

        // The misbaha's string: a head bead at the top and 33 beads round it, lit one by one;
        // after the 33rd the round starts again at 1 — the count itself goes on in the middle.
        // The arc runs from the head bead to the last bead lit. Mirrored right to left, as the
        // app's dial.
        val ring = outer * 0.84f
        val direction = if (AppLanguage.current.isRtl) -1f else 1f
        val positions = BEADS + 1
        stroke.shader = null
        stroke.strokeWidth = maxOf(0.6f * density, 0.7f * unit)
        stroke.color = 0x17FFFFFF
        canvas.drawCircle(cx, cy, ring, stroke)
        val reading = cycle
        val round = roundOf(count)
        val progress = (reading ?: (round / positions.toFloat())).coerceIn(0f, 1f)
        arc.set(cx - ring, cy - ring, cx + ring, cy + ring)
        stroke.strokeWidth = maxOf(1f * density, 1.2f * unit)
        stroke.strokeCap = Paint.Cap.ROUND
        stroke.color = if (reading != null) GOLD else (MINT and 0xFFFFFF) or 0xBF000000.toInt()
        if (progress > 0f) canvas.drawArc(arc, -90f, 360f * progress * direction, false, stroke)
        if (reading == null) {
            val spacing = (2 * PI * ring / positions).toFloat()
            val bead = (spacing * 0.26f).coerceIn(0.8f * density, 3f * unit)
            for (i in 0 until positions) {
                val angle = -PI / 2 + direction * 2 * PI * i / positions
                val x = cx + ring * cos(angle).toFloat()
                val y = cy + ring * sin(angle).toFloat()
                if (i == 0) {
                    // The head bead, where every round begins: a little larger, in gold.
                    paint.color = GOLD
                    canvas.drawCircle(x, y, bead * 1.3f, paint)
                    continue
                }
                val lit = i <= round
                paint.color = if (lit) MINT else 0x2EFFFFFF
                canvas.drawCircle(x, y, if (lit) bead else bead * 0.78f, paint)
            }
        }

        // The wave of the last count, from the button out to the string.
        if (waveAt < 1f) {
            stroke.strokeWidth = 2f * density
            stroke.color = ((if (reading != null) GOLD else MINT) and 0xFFFFFF) or ((0x99 * (1f - waveAt)).toInt() shl 24)
            canvas.drawCircle(cx, cy, outer * PEARL_RATIO + (ring - outer * PEARL_RATIO) * waveAt, stroke)
        }

        // The button: translucent, the string and the ground showing through it; flat on the
        // bubble, in the box gently lit from the upper left. A faint rim; it gives under the finger.
        canvas.save()
        canvas.scale(pearlScale, pearlScale, cx, cy)
        val pearl = outer * PEARL_RATIO
        val top = cy - pearl
        // A shader is drawn at the paint's own alpha: that alpha is the button's translucency.
        paint.color = if (disc) BUTTON_ALPHA else BOX_BUTTON_ALPHA
        if (disc) {
            paint.shader = RadialGradient(
                cx - pearl * 0.28f, cy - pearl * 0.44f, pearl * 1.8f,
                PEARL_LIGHT, PEARL_DEEP, Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(cx, cy, pearl, paint)
        } else {
            paint.shader = RadialGradient(
                cx - pearl * 0.32f, cy - pearl * 0.4f, pearl * 1.5f,
                intArrayOf(RAISED_LIGHT, RAISED_MID, RAISED_DEEP), floatArrayOf(0f, 0.45f, 1f), Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(cx, cy, pearl, paint)
        }
        paint.shader = null
        stroke.color = RIM_ALPHA
        stroke.strokeWidth = 1f * density
        stroke.shader = LinearGradient(0f, top, 0f, top + 2 * pearl, 0x73FFFFFF, 0x0AFFFFFF, Shader.TileMode.CLAMP)
        canvas.drawCircle(cx, cy, pearl - stroke.strokeWidth / 2f, stroke)
        stroke.shader = null

        // The count alone, centred.
        val digits = uiDigits(count)
        countText.textSize = pearl * when (digits.length) {
            in 0..2 -> 0.7f
            3 -> 0.6f
            4 -> 0.48f
            else -> 0.4f
        }
        val metrics = countText.fontMetrics
        canvas.drawText(digits, cx, cy - (metrics.ascent + metrics.descent) / 2f, countText)
        canvas.restore()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.rawX; downY = event.rawY
                lastX = downX; lastY = downY
                dragging = false
                longPressed = false
                postDelayed(longPress, longPressTimeout)
                pressTo(PRESSED_SCALE, 90, overshoot = false)
                onTouched()
            }
            MotionEvent.ACTION_MOVE -> {
                if (!dragging && !longPressed && hypot(event.rawX - downX, event.rawY - downY) > slop) {
                    dragging = true
                    removeCallbacks(longPress)
                    pressTo(1f, 160, overshoot = false)
                }
                if (dragging) {
                    onDrag(event.rawX - lastX, event.rawY - lastY)
                    lastX = event.rawX; lastY = event.rawY
                }
            }
            MotionEvent.ACTION_UP -> {
                removeCallbacks(longPress)
                if (!dragging) pressTo(1f, 260, overshoot = true)
                when {
                    dragging -> onDragEnd()
                    !longPressed && abs(event.rawX - downX) <= slop && abs(event.rawY - downY) <= slop -> {
                        // Reaching a target is felt more strongly than the other taps.
                        val reaches = count + 1 == DailyCounter.target(count)
                        performHapticFeedback(
                            if (reaches) HapticFeedbackConstants.LONG_PRESS else HapticFeedbackConstants.CLOCK_TICK
                        )
                        onTap()
                    }
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                removeCallbacks(longPress)
                pressTo(1f, 160, overshoot = false)
                if (dragging) onDragEnd()
            }
        }
        return true
    }

    companion object {
        /** The bubble's width and height in px at [scale] (the dial, and the pulse's room round it). */
        fun sizePx(density: Float, scale: Float): Int =
            ((DIAMETER_DP * scale.coerceIn(MIN_SCALE, MAX_SCALE) + 2 * MARGIN_DP) * density).toInt()

        /** The dial's diameter at the default size. */
        const val DIAMETER_DP = 100
        private const val MARGIN_DP = 4
        const val MIN_SCALE = 0.6f
        const val MAX_SCALE = 1.6f
        /** The beads round the string: a round of 33, as on a misbaha. */
        const val BEADS = 33

        /** The beads lit for [count]: 1 to 33, starting again at 1 after each 33 (none at 0). */
        fun roundOf(count: Int): Int = if (count <= 0) 0 else (count - 1) % BEADS + 1
        private const val PRESSED_SCALE = 0.88f
        private const val PEARL_RATIO = 0.58f
        const val NIGHT_TOP = 0xFF20523C.toInt()
        const val NIGHT_BOTTOM = 0xFF0F281D.toInt()
        const val GOLD = 0xFFE6CF86.toInt()
        private const val MINT = 0xFF8CC4A8.toInt()
        private const val PEARL_LIGHT = 0xFF2B6B4C.toInt()
        private const val PEARL_DEEP = 0xFF1A4A34.toInt()
        private const val RAISED_LIGHT = 0xFF4E9F78.toInt()
        private const val RAISED_MID = 0xFF2A6E4E.toInt()
        private const val RAISED_DEEP = 0xFF0E3222.toInt()
        private const val DISH_SHADE = 0xFF0C2118.toInt()
        private const val DISH_LIT = 0xFF26583F.toInt()
        private const val WHITE = 0xFFFFFFFF.toInt()
        private const val OPAQUE = 0xFF000000.toInt()
        /** How much of the button shows: on the bubble, and (a little more, it is lit) in the box. */
        private const val BUTTON_ALPHA = 0x2E000000
        private const val BOX_BUTTON_ALPHA = 0x40000000
        /** The rim's strength, over its own gradient. */
        private const val RIM_ALPHA = 0xBF000000.toInt()
    }
}
