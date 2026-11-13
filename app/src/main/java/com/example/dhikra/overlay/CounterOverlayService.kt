package com.example.dhikra.overlay

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.provider.Settings
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.Choreographer
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import com.example.dhikra.MainActivity
import com.example.dhikra.R
import com.example.dhikra.data.ArabicText
import com.example.dhikra.data.CounterStore
import com.example.dhikra.data.DailyCounter
import com.example.dhikra.data.ProgramRepository
import com.example.dhikra.data.ReadingSpeeds
import com.example.dhikra.ui.AppLanguage
import com.example.dhikra.ui.localized
import com.example.dhikra.ui.uiDigits
import com.example.dhikra.widget.Widgets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.content.SharedPreferences
import com.example.dhikra.data.OverlayFade

/**
 * The floating counter: one daily counter as a small bubble over every app, counted with a tap
 * (see [OverlayCounterView]). A long press expands it into a box with the dhikr and its actions —
 * one step back, reset, reading along, and the full counter in the app. It lives in a foreground
 * service, with a quiet notification, while it is shown; counts go through [CounterStore], so the
 * app and the widget follow them.
 *
 * Reading along works as in the section reader: the repetition is read at the dhikr's learned
 * speed — the bubble's ring filling, the box's words lighting up — and waits for a tap, which
 * counts it, teaches the speed the user's real pace and starts the next.
 */
class CounterOverlayService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var windows: WindowManager
    private lateinit var ui: Context
    private var bubble: OverlayCounterView? = null
    private var bubbleParams: WindowManager.LayoutParams? = null
    private var box: View? = null
    private var boxParams: WindowManager.LayoutParams? = null
    /** The counter at the heart of the box. */
    private var boxDial: OverlayCounterView? = null
    /** Whether the box was moved since it opened (else the bubble comes back where it was). */
    private var boxMoved = false
    private var entryId: String? = null
    private var title: String = ""
    private var text: String = ""
    private var count = 0
    private var snap: ValueAnimator? = null

    // Reading along.
    private var playing = false
    private var awaiting = false
    private var cycleStart = 0L
    private var speed = ProgramRepository.DEFAULT_LETTERS_PER_SECOND
    private var words: List<Word> = emptyList()
    private var shownWord = -2
    private var confirmReset = false
    // Left alone, the bubble fades (as set in the app's settings); not while reading along.
    private var fade = OverlayFade()
    private val dim = Runnable {
        if (fade.enabled && !playing) bubble?.animate()?.alpha(fade.opacity)?.setDuration(400)?.start()
    }
    private val endConfirm = Runnable { setConfirmReset(false) }
    private val ticker = Choreographer.FrameCallback { tick() }

    // The size chosen in the app's settings, followed while the bubble is shown.
    private val sizeListener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
        if (key == KEY_SCALE) resize(prefs.getFloat(KEY_SCALE, 1f))
        if (key in OverlayFade.KEYS) {
            fade = ProgramRepository(applicationContext).loadOverlayFade()
            wake()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windows = getSystemService(WINDOW_SERVICE) as WindowManager
        ui = localized()
        getSharedPreferences(PREFS, MODE_PRIVATE).registerOnSharedPreferenceChangeListener(sizeListener)
        fade = ProgramRepository(applicationContext).loadOverlayFade()
        scope.launch {
            // Counted in the app: show its count.
            CounterStore.changes.filter { it == CounterStore.Source.APP }.collect { reload() }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_CLOSE) {
            stopSelf()
            return START_NOT_STICKY
        }
        val id = intent?.getStringExtra(EXTRA_ENTRY_ID)
        // Android asks for the notification at once; it is filled in once the entry is read.
        startInForeground(notification())
        if (id == null || !canShow(this)) {
            stopSelf()
            return START_NOT_STICKY
        }
        entryId = id
        shownEntryId = id
        scope.launch {
            val entry = withContext(Dispatchers.IO) {
                ProgramRepository(applicationContext).loadWithoutQuranText().library.entry(id)
            }
            if (entry == null) {
                stopSelf()
                return@launch
            }
            title = entry.title.ifBlank { entry.text }
            text = entry.text.ifBlank { entry.title }
            words = Word.split(text)
            count = withContext(Dispatchers.IO) { CounterStore.count(applicationContext, id) }
            speed = withContext(Dispatchers.IO) { ReadingSpeeds.load(applicationContext, ReadingSpeeds.counterKey(id)) }
            if (bubble == null && !showBubble()) {
                stopSelf()
                return@launch
            }
            setPlaying(false)
            collapse()
            render()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        getSharedPreferences(PREFS, MODE_PRIVATE).unregisterOnSharedPreferenceChangeListener(sizeListener)
        scope.cancel()
        snap?.cancel()
        Choreographer.getInstance().removeFrameCallback(ticker)
        removeBox()
        bubble?.let { runCatching { windows.removeView(it) } }
        bubble = null
        if (shownEntryId == entryId) shownEntryId = null
        super.onDestroy()
    }

    // ---------------------------------------------------------------- The bubble

    private fun showBubble(): Boolean {
        val view = OverlayCounterView(
            context = ui,
            onTap = ::countOne,
            onLongPress = ::expand,
            onDrag = ::moveBy,
            onDragEnd = ::dropped,
            onTouched = ::wake,
        )
        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        val metrics = resources.displayMetrics
        view.scale = prefs.getFloat(KEY_SCALE, 1f)
        val size = OverlayCounterView.sizePx(metrics.density, view.scale)
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.LEFT
            // Where it was left last time; at first on the right edge, a third of the way down.
            x = prefs.getInt(KEY_X, metrics.widthPixels - size).coerceIn(0, metrics.widthPixels - size)
            y = prefs.getInt(KEY_Y, metrics.heightPixels / 3).coerceIn(0, metrics.heightPixels - size)
        }
        return runCatching { windows.addView(view, params) }
            .onSuccess {
                bubble = view
                bubbleParams = params
                wake()
            }
            .isSuccess
    }

    /** A tap on the bubble or the box's text: +1; reading along, the repetition read, and the next. */
    private fun countOne() {
        val id = entryId ?: return
        wake()
        if (playing) {
            val elapsed = SystemClock.uptimeMillis() - cycleStart
            ReadingSpeeds.record(this, ReadingSpeeds.counterKey(id), text, elapsed)
            ReadingSpeeds.learn(speed, speed, ArabicText.totalLetters(text), elapsed)?.let { speed = it.item }
            startCycle()
        }
        count += 1
        render()
        CounterStore.add(this, id, 1) { stored -> if (stored != count) { count = stored; render() } }
    }

    private fun undoOne() {
        val id = entryId ?: return
        if (count == 0) return
        count -= 1
        render()
        CounterStore.add(this, id, -1) { stored -> if (stored != count) { count = stored; render() } }
    }

    /** Reset asks once more: the first tap arms it (the icon turns gold) for a few seconds. */
    private fun resetTapped() {
        val id = entryId ?: return
        if (count == 0) return
        if (!confirmReset) {
            setConfirmReset(true)
            return
        }
        setConfirmReset(false)
        box?.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        count = 0
        render()
        CounterStore.reset(this, id)
    }

    private fun setConfirmReset(armed: Boolean) {
        confirmReset = armed
        val button = box?.findViewById<ImageButton>(R.id.overlay_reset) ?: return
        button.removeCallbacks(endConfirm)
        button.setColorFilter(if (armed) OverlayCounterView.GOLD else WHITE)
        button.contentDescription = ui.getString(if (armed) R.string.counter_reset_confirm else R.string.counter_reset)
        if (armed) {
            button.postDelayed(endConfirm, RESET_CONFIRM_MS)
            android.widget.Toast.makeText(ui, R.string.counter_reset_confirm, android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun reload() {
        val id = entryId ?: return
        scope.launch {
            count = withContext(Dispatchers.IO) { CounterStore.count(applicationContext, id) }
            speed = withContext(Dispatchers.IO) { ReadingSpeeds.load(applicationContext, ReadingSpeeds.counterKey(id)) }
            render()
        }
    }

    /** The dials shown: the bubble, and the box's while it is open. */
    private fun dials() = listOfNotNull(bubble, boxDial)

    private fun render() {
        dials().forEach { it.count = count }
        box?.let { view ->
            view.findViewById<View>(R.id.overlay_undo).alpha = if (count > 0) 1f else DISABLED_ALPHA
            view.findViewById<View>(R.id.overlay_reset).alpha = if (count > 0) 1f else DISABLED_ALPHA
        }
        NotificationManagerCompat.from(this).let { manager ->
            if (manager.areNotificationsEnabled()) runCatching { manager.notify(NOTIFICATION_ID, notification()) }
        }
    }

    private fun countLine() = uiDigits("$count/${DailyCounter.target(count)}")

    /** Solid while touched or in use; left alone, it fades after the set time (if fading is on). */
    private fun wake() {
        val view = bubble ?: return
        view.removeCallbacks(dim)
        view.animate().cancel()
        view.alpha = 1f
        if (fade.enabled) view.postDelayed(dim, fade.delaySeconds * 1_000L)
    }

    /** A new size from the settings: against the right side it stays there, elsewhere it stays on screen. */
    private fun resize(scale: Float) {
        val view = bubble ?: return
        val params = bubbleParams ?: return
        val metrics = resources.displayMetrics
        val old = OverlayCounterView.sizePx(metrics.density, view.scale)
        val size = OverlayCounterView.sizePx(metrics.density, scale)
        val atRight = params.x + old >= metrics.widthPixels - 2
        view.scale = scale
        params.x = if (atRight) metrics.widthPixels - size else params.x.coerceIn(0, metrics.widthPixels - size)
        params.y = params.y.coerceIn(0, metrics.heightPixels - size)
        runCatching { windows.updateViewLayout(view, params) }
        getSharedPreferences(PREFS, MODE_PRIVATE).edit {
            putInt(KEY_X, params.x)
            putInt(KEY_Y, params.y)
        }
        wake()
    }

    private fun moveBy(dx: Float, dy: Float) {
        val view = bubble ?: return
        val params = bubbleParams ?: return
        wake()
        snap?.cancel()
        val metrics = resources.displayMetrics
        params.x = (params.x + dx.toInt()).coerceIn(0, metrics.widthPixels - view.width)
        params.y = (params.y + dy.toInt()).coerceIn(0, metrics.heightPixels - view.height)
        runCatching { windows.updateViewLayout(view, params) }
    }

    /**
     * Let go: it stays where it was put, unless that is close to a side of the screen — then it
     * slides against it. Its place is kept for next time.
     */
    private fun dropped() {
        val view = bubble ?: return
        val params = bubbleParams ?: return
        val width = resources.displayMetrics.widthPixels
        val zone = SNAP_ZONE_DP * resources.displayMetrics.density
        val target = when {
            params.x < zone -> 0
            width - (params.x + view.width) < zone -> width - view.width
            else -> params.x
        }
        if (target != params.x) {
            snap = ValueAnimator.ofInt(params.x, target).apply {
                duration = 160
                addUpdateListener {
                    params.x = it.animatedValue as Int
                    runCatching { windows.updateViewLayout(view, params) }
                }
                start()
            }
        }
        getSharedPreferences(PREFS, MODE_PRIVATE).edit {
            putInt(KEY_X, target)
            putInt(KEY_Y, params.y)
        }
        wake()
    }

    // ---------------------------------------------------------------- Reading along

    private fun setPlaying(on: Boolean) {
        playing = on
        Choreographer.getInstance().removeFrameCallback(ticker)
        syncBox()
        if (on) {
            startCycle()
            wake()
        } else {
            awaiting = false
            dials().forEach {
                it.awaiting = false
                it.cycle = null
            }
            wake()
        }
    }

    private fun startCycle() {
        cycleStart = SystemClock.uptimeMillis()
        awaiting = false
        dials().forEach { it.awaiting = false }
        Choreographer.getInstance().removeFrameCallback(ticker)
        Choreographer.getInstance().postFrameCallback(ticker)
    }

    /** One frame of a repetition: how much is read at the learned speed. */
    private fun tick() {
        if (!playing || awaiting) return
        val read = (SystemClock.uptimeMillis() - cycleStart) / 1000f * speed
        val total = words.lastOrNull()?.end ?: 0f
        val progress = if (total > 0f) (read / total).coerceIn(0f, 1f) else 1f
        dials().forEach { it.cycle = progress }
        highlight(words.indexOfFirst { it.end > read }.let { if (it < 0) words.size else it })
        if (progress >= 1f) {
            // Read: it waits for the tap that counts it.
            awaiting = true
            dials().forEach { it.awaiting = true }
            dials().last().performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        } else {
            Choreographer.getInstance().postFrameCallback(ticker)
        }
    }

    /** The box's text with [current] word lit up, those read before it bright, the rest faint; -1 plain. */
    private fun highlight(current: Int) {
        if (current == shownWord && current != -1) return
        shownWord = current
        val view = box?.findViewById<TextView>(R.id.overlay_text) ?: return
        if (current < 0 || words.isEmpty()) {
            view.text = text
            return
        }
        val spans = SpannableString(text)
        words.forEachIndexed { index, word ->
            val color = when {
                index < current -> READ
                index == current -> OverlayCounterView.GOLD
                else -> UNREAD
            }
            spans.setSpan(ForegroundColorSpan(color), word.from, word.to, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        view.text = spans
    }

    // ---------------------------------------------------------------- The box

    /** A long press: the bubble opens into the box, where it stands. */
    @SuppressLint("InflateParams", "ClickableViewAccessibility")
    private fun expand() {
        val view = bubble ?: return
        val anchor = bubbleParams ?: return
        if (box != null) return
        val content = LayoutInflater.from(ui).inflate(R.layout.overlay_counter_box, null)
        content.layoutDirection = if (AppLanguage.current.isRtl) View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR
        // The full-screen counter's texture of faint stars over the box's night green.
        content.background = android.graphics.drawable.LayerDrawable(
            arrayOf(content.background, StarLatticeDrawable(resources.displayMetrics.density))
        )
        // The counter, large in the middle: a tap counts, a drag moves the box, a long press folds it.
        val dial = OverlayCounterView(
            context = ui,
            onTap = ::countOne,
            onLongPress = ::collapse,
            onDrag = ::moveBoxBy,
            onDragEnd = {},
        ).apply {
            scale = BOX_DIAL_SCALE
            disc = false
            count = this@CounterOverlayService.count
        }
        content.findViewById<android.widget.FrameLayout>(R.id.overlay_dial).addView(
            dial,
            android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER,
            ),
        )
        content.findViewById<View>(R.id.overlay_undo).setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            undoOne()
        }
        content.findViewById<View>(R.id.overlay_reset).setOnClickListener { resetTapped() }
        content.findViewById<View>(R.id.overlay_play).setOnClickListener { setPlaying(!playing) }
        // The full counter in the app: the floating one is no longer needed.
        content.findViewById<View>(R.id.overlay_expand).setOnClickListener {
            setPlaying(false)
            openInApp()
            stopSelf()
        }
        content.findViewById<View>(R.id.overlay_close).setOnClickListener { stopSelf() }
        // A drag on the box (its handle, its text, anywhere but a button) moves it; a touch
        // anywhere else on the screen folds it back into the bubble.
        val slop = android.view.ViewConfiguration.get(ui).scaledTouchSlop
        var downX = 0f
        var downY = 0f
        var lastX = 0f
        var lastY = 0f
        var moving = false
        content.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_OUTSIDE -> {
                    collapse()
                    false
                }
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX; downY = event.rawY
                    lastX = downX; lastY = downY
                    moving = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (!moving && kotlin.math.hypot(event.rawX - downX, event.rawY - downY) > slop) moving = true
                    if (moving) {
                        moveBoxBy(event.rawX - lastX, event.rawY - lastY)
                        lastX = event.rawX; lastY = event.rawY
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> true
                else -> false
            }
        }

        val metrics = resources.displayMetrics
        val boxWidth = minOf((BOX_WIDTH_DP * metrics.density).toInt(), metrics.widthPixels - (16 * metrics.density).toInt())
        content.measure(
            View.MeasureSpec.makeMeasureSpec(boxWidth, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
        )
        val onLeft = anchor.x + view.width / 2 < metrics.widthPixels / 2
        val params = WindowManager.LayoutParams(
            boxWidth,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.LEFT
            // Growing from the bubble's corner towards the open side of the screen.
            x = (if (onLeft) anchor.x else anchor.x + view.width - boxWidth).coerceIn(0, metrics.widthPixels - boxWidth)
            y = anchor.y.coerceIn(0, maxOf(0, metrics.heightPixels - content.measuredHeight))
        }
        if (runCatching { windows.addView(content, params) }.isFailure) return
        box = content
        boxParams = params
        boxDial = dial
        boxMoved = false
        content.alpha = 0f
        content.scaleX = 0.9f
        content.scaleY = 0.9f
        content.pivotX = if (onLeft) 0f else boxWidth.toFloat()
        content.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(160).start()
        // The bubble steps aside: hidden, and its window lets touches through while the box is open.
        view.removeCallbacks(dim)
        view.visibility = View.INVISIBLE
        anchor.flags = anchor.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        runCatching { windows.updateViewLayout(view, anchor) }
        syncBox()
        render()
    }

    /** The box as reading along stands: its play icon, and the words read so far. */
    private fun syncBox() {
        box?.findViewById<ImageButton>(R.id.overlay_play)?.apply {
            setImageResource(if (playing) R.drawable.ic_pause else R.drawable.ic_play)
            contentDescription = ui.getString(if (playing) R.string.pause else R.string.play)
        }
        shownWord = -2
        highlight(if (!playing) -1 else if (awaiting) words.size else 0)
    }

    /** The box moves with the finger, staying on the screen. */
    private fun moveBoxBy(dx: Float, dy: Float) {
        val view = box ?: return
        val params = boxParams ?: return
        val metrics = resources.displayMetrics
        params.x = (params.x + dx.toInt()).coerceIn(0, maxOf(0, metrics.widthPixels - view.width))
        params.y = (params.y + dy.toInt()).coerceIn(0, maxOf(0, metrics.heightPixels - view.height))
        runCatching { windows.updateViewLayout(view, params) }
        boxMoved = true
    }

    /**
     * The box folds back into the bubble (reading along goes on there). The bubble comes back
     * where it was — or, when the box was moved, where the box was left: at its corner on the side
     * the box stands.
     */
    private fun collapse() {
        val shown = box ?: return
        val placed = boxParams
        setConfirmReset(false)
        removeBox()
        val view = bubble ?: return
        val params = bubbleParams ?: return
        if (placed != null && boxMoved) {
            val metrics = resources.displayMetrics
            val size = OverlayCounterView.sizePx(metrics.density, view.scale)
            val onLeft = placed.x + shown.width / 2 < metrics.widthPixels / 2
            params.x = (if (onLeft) placed.x else placed.x + shown.width - size).coerceIn(0, metrics.widthPixels - size)
            params.y = placed.y.coerceIn(0, metrics.heightPixels - size)
            getSharedPreferences(PREFS, MODE_PRIVATE).edit {
                putInt(KEY_X, params.x)
                putInt(KEY_Y, params.y)
            }
        }
        params.flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
        runCatching { windows.updateViewLayout(view, params) }
        view.visibility = View.VISIBLE
        wake()
    }

    private fun removeBox() {
        box?.let { runCatching { windows.removeView(it) } }
        box = null
        boxParams = null
        boxDial = null
    }

    private fun openInApp() {
        val id = entryId ?: return
        startActivity(openIntent(id))
    }

    private fun openIntent(id: String?) = Intent(this, MainActivity::class.java)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        .apply { if (id != null) putExtra(Widgets.EXTRA_COUNTER_ID, id) }

    // ---------------------------------------------------------------- The notification

    private fun startInForeground(notification: Notification) {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else 0
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, type)
    }

    private fun notification(): Notification {
        NotificationManagerCompat.from(this).createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_LOW)
                .setName(ui.getString(R.string.overlay_channel))
                .setDescription(ui.getString(R.string.overlay_channel_description))
                .setShowBadge(false)
                .build()
        )
        val open = PendingIntent.getActivity(
            this, 400, openIntent(entryId), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val close = PendingIntent.getService(
            this, 401, Intent(this, CounterOverlayService::class.java).setAction(ACTION_CLOSE),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title.ifBlank { ui.getString(R.string.overlay_channel) })
            .setContentText(if (title.isBlank()) null else countLine())
            .setContentIntent(open)
            .addAction(0, ui.getString(R.string.close), close)
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    /** One word of the dhikr: its place in the text, and the letters read by its end. */
    private data class Word(val from: Int, val to: Int, val end: Float) {
        companion object {
            fun split(text: String): List<Word> {
                var letters = 0f
                return Regex("\\S+").findAll(text).map { match ->
                    letters += ArabicText.totalLetters(match.value)
                    Word(match.range.first, match.range.last + 1, letters)
                }.toList()
            }
        }
    }

    companion object {
        private const val EXTRA_ENTRY_ID = "entry_id"
        private const val ACTION_CLOSE = "com.example.dhikra.overlay.CLOSE"
        private const val CHANNEL_ID = "counter_overlay"
        private const val NOTIFICATION_ID = 7_001
        private const val PREFS = "dhikra"
        private const val KEY_X = "overlay_x"
        private const val KEY_Y = "overlay_y"
        /** The bubble's size, a share of the default; written by the app's settings. */
        const val KEY_SCALE = "overlay_scale"
        private const val DISABLED_ALPHA = 0.35f
        private const val RESET_CONFIRM_MS = 3_000L
        private const val SNAP_ZONE_DP = 40
        private const val BOX_WIDTH_DP = 300
        /** The box's counter: larger than the bubble at its default size, the box's centre. */
        private const val BOX_DIAL_SCALE = 1.45f
        private const val WHITE = 0xEBFFFFFF.toInt()
        private const val READ = 0xEBFFFFFF.toInt()
        private const val UNREAD = 0x80FFFFFF.toInt()

        /** The counter shown floating now, if any. */
        @Volatile
        var shownEntryId: String? = null
            private set

        /** Whether the app may draw over other apps ("Display over other apps"). */
        fun canShow(context: Context): Boolean =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)

        /** Floats the daily counter [entryId] over other apps (replacing one already floating). */
        fun start(context: Context, entryId: String) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, CounterOverlayService::class.java).putExtra(EXTRA_ENTRY_ID, entryId),
            )
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, CounterOverlayService::class.java))
        }

        @Suppress("DEPRECATION")
        private fun overlayType() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }
    }
}
