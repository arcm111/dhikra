package io.github.arcm111.dhikra.data

/**
 * The sound a reminder plays. [Default] is the phone's notification sound — what every reminder
 * played before sounds could be chosen. Stored as its [key].
 */
sealed interface ReminderSound {
    val key: String

    /** The phone's default notification sound. */
    data object Default : ReminderSound {
        override val key = "default"
    }

    /** No sound. */
    data object Silent : ReminderSound {
        override val key = "silent"
    }

    /** The azan bundled with the app (offered for prayer alerts). */
    data object Azan : ReminderSound {
        override val key = "azan"
    }

    /** A notification sound of the phone, picked by the user ([uri] from Android's sound picker). */
    data class Device(val uri: String) : ReminderSound {
        override val key: String get() = DEVICE_PREFIX + uri
    }

    companion object {
        private const val DEVICE_PREFIX = "device:"

        /** The sound stored as [key]; anything unknown or missing is [Default]. */
        fun fromKey(key: String?): ReminderSound = when {
            key == Silent.key -> Silent
            key == Azan.key -> Azan
            key != null && key.startsWith(DEVICE_PREFIX) && key.length > DEVICE_PREFIX.length ->
                Device(key.removePrefix(DEVICE_PREFIX))
            else -> Default
        }
    }
}

/** The alert at one prayer's time: off by default, and with the default sound when turned on. */
data class PrayerAlert(val enabled: Boolean = false, val sound: ReminderSound = ReminderSound.Default) {

    /** "on|azan", "off|default"... */
    fun encode(): String = (if (enabled) ON else OFF) + SEPARATOR + sound.key

    companion object {
        private const val ON = "on"
        private const val OFF = "off"
        private const val SEPARATOR = "|"

        fun decode(stored: String?): PrayerAlert {
            if (stored == null) return PrayerAlert()
            val enabled = stored.substringBefore(SEPARATOR) == ON
            val sound = ReminderSound.fromKey(stored.substringAfter(SEPARATOR, ReminderSound.Default.key))
            return PrayerAlert(enabled, sound)
        }
    }
}
