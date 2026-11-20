package io.github.arcm111.dhikra.ui.counter

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import io.github.arcm111.dhikra.R
import io.github.arcm111.dhikra.data.DailyCounter
import io.github.arcm111.dhikra.ui.AppLanguage
import io.github.arcm111.dhikra.ui.uiDigits
import java.io.File
import java.util.Date

/**
 * A small 4:3 picture of a daily counter's progress, to share: the dhikr's text, the day's count in
 * a gold ring filling towards its target, the date and the app's name, on the counter's night
 * green. Drawn straight on a bitmap, so it does not depend on the screen.
 */
object CounterShareCard {

    private const val WIDTH = 1200
    private const val HEIGHT = 900
    private const val GOLD = 0xFFE6CF86.toInt()

    /** Draws the card for [text] counted [count] times today, and opens the system's share sheet with it. */
    fun share(context: Context, title: String, text: String, count: Int, nightTop: Int, nightBottom: Int) {
        val bitmap = draw(context, text.ifBlank { title }, count, nightTop, nightBottom)
        val folder = File(context.cacheDir, "shared").apply { mkdirs() }
        val file = File(folder, "counter.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
        val send = Intent(Intent.ACTION_SEND)
            .setType("image/png")
            .putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(
            Intent.createChooser(send, context.getString(R.string.counter_share)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    fun draw(context: Context, text: String, count: Int, nightTop: Int, nightBottom: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val regular = ResourcesCompat.getFont(context, R.font.ibm_plex_sans_arabic_regular) ?: Typeface.DEFAULT
        val bold = ResourcesCompat.getFont(context, R.font.ibm_plex_sans_arabic_bold) ?: Typeface.DEFAULT_BOLD

        // The night green, top to bottom.
        canvas.drawPaint(Paint().apply {
            shader = LinearGradient(0f, 0f, 0f, HEIGHT.toFloat(), nightTop, nightBottom, Shader.TileMode.CLAMP)
        })

        // The dhikr, centred, up to three lines.
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFFFFF.toInt()
            textSize = 50f
            typeface = bold
        }
        val layout = StaticLayout.Builder.obtain(text, 0, text.length, textPaint, WIDTH - 200)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setMaxLines(3)
            .setEllipsize(TextUtils.TruncateAt.END)
            .setLineSpacing(0f, 1.15f)
            .build()
        canvas.save()
        canvas.translate(100f, 70f)
        layout.draw(canvas)
        canvas.restore()

        // The ring: today's count filling towards its target, in gold.
        val target = DailyCounter.target(count)
        val cx = WIDTH / 2f
        val cy = 530f
        val radius = 165f
        val ring = RectF(cx - radius, cy - radius, cx + radius, cy + radius)
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 16f
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawArc(ring, 0f, 360f, false, stroke.apply { color = 0x26FFFFFF })
        canvas.drawArc(ring, -90f, 360f * DailyCounter.stage(count), false, stroke.apply { color = GOLD })

        val countPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFFFFF.toInt()
            textSize = 112f
            typeface = bold
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(uiDigits(count), cx, cy + 30f, countPaint)
        val ofPaint = TextPaint(countPaint).apply {
            textSize = 40f
            typeface = regular
            color = 0xB3FFFFFF.toInt()
        }
        canvas.drawText(context.getString(R.string.counter_of, uiDigits(target)), cx, cy + 92f, ofPaint)

        // The date, and the app's name.
        val footPaint = TextPaint(ofPaint).apply { textSize = 30f; color = 0x99FFFFFF.toInt() }
        canvas.drawText(today(), cx, HEIGHT - 70f, footPaint)
        val namePaint = TextPaint(footPaint).apply { color = GOLD; typeface = bold; textSize = 32f }
        canvas.drawText(context.getString(R.string.app_name), cx, HEIGHT - 28f, namePaint)
        return bitmap
    }

    /** Today's Gregorian and Hijri dates, in the app's language. */
    private fun today(): String = runCatching {
        val arabic = AppLanguage.current == AppLanguage.AR
        val now = Date()
        val gregorian = android.icu.text.DateFormat.getInstanceForSkeleton(
            "dMMMMy", android.icu.util.ULocale(if (arabic) "ar@numbers=arab" else "en"),
        ).format(now)
        val hijri = android.icu.text.DateFormat.getDateInstance(
            android.icu.text.DateFormat.LONG,
            android.icu.util.ULocale(if (arabic) "ar@calendar=islamic-umalqura;numbers=arab" else "en@calendar=islamic-umalqura"),
        ).format(now)
        "$gregorian  ·  $hijri"
    }.getOrDefault("")
}
