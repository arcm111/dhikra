package com.example.dhikra.ui.prayer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.example.dhikra.R
import com.example.dhikra.data.ArabicText
import com.example.dhikra.data.Prayer
import com.example.dhikra.data.SectionAnchor
import com.example.dhikra.ui.components.FajrIcon
import com.example.dhikra.ui.components.MoonStarIcon
import com.example.dhikra.ui.components.SunIcon
import kotlinx.coroutines.delay
import java.util.Calendar
import java.util.TimeZone

// Prayer bits shared by the prayer-times screen and the day view.

/**
 * The current time as minutes after midnight, updated every minute while shown — in [timeZone]
 * (the prayer city's, so it matches its times) or the phone's when null.
 */
@Composable
internal fun rememberMinuteOfDay(timeZone: String? = null): Int {
    var minute by remember(timeZone) { mutableIntStateOf(minuteOfDay(timeZone)) }
    LaunchedEffect(timeZone) {
        while (true) {
            delay((60 - Calendar.getInstance().get(Calendar.SECOND)) * 1000L)
            minute = minuteOfDay(timeZone)
        }
    }
    return minute
}

internal fun minuteOfDay(timeZone: String? = null): Int =
    (if (timeZone != null) Calendar.getInstance(TimeZone.getTimeZone(timeZone)) else Calendar.getInstance())
        .let { it.get(Calendar.HOUR_OF_DAY) * 60 + it.get(Calendar.MINUTE) }

internal val Prayer.label: Int
    get() = when (this) {
        Prayer.FAJR -> R.string.prayer_fajr
        Prayer.SUNRISE -> R.string.prayer_sunrise
        Prayer.DHUHR -> R.string.prayer_dhuhr
        Prayer.ASR -> R.string.prayer_asr
        Prayer.MAGHRIB -> R.string.prayer_maghrib
        Prayer.ISHA -> R.string.prayer_isha
    }

internal val Prayer.icon: ImageVector
    get() = when (this) {
        Prayer.FAJR, Prayer.SUNRISE, Prayer.MAGHRIB -> FajrIcon
        Prayer.DHUHR, Prayer.ASR -> SunIcon
        Prayer.ISHA -> MoonStarIcon
    }

/** "بعد ١ س ٢٠ د" / "بعد ٢٠ د". */
@Composable
internal fun untilText(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return if (h > 0) {
        stringResource(R.string.prayer_in_hours, ArabicText.toArabicDigits(h), ArabicText.toArabicDigits(m))
    } else {
        stringResource(R.string.prayer_in_minutes, ArabicText.toArabicDigits(m))
    }
}

/** "عند دخول الوقت" / "بعد ١٥ د" / "قبل ١٠ د": a section's time from its prayer's. */
@Composable
internal fun anchorOffsetText(offset: Int): String = when {
    offset > 0 -> stringResource(R.string.anchor_after, ArabicText.toArabicDigits(offset))
    offset < 0 -> stringResource(R.string.anchor_before, ArabicText.toArabicDigits(-offset))
    else -> stringResource(R.string.anchor_at)
}

/** "الفجر · بعد ١٥ د": the prayer a section's time follows. */
@Composable
internal fun anchorLabel(anchor: SectionAnchor): String =
    stringResource(R.string.anchor_label, stringResource(anchor.prayer.label), anchorOffsetText(anchor.offsetMinutes))
