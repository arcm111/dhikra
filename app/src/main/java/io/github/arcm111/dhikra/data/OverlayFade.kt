package io.github.arcm111.dhikra.data

/**
 * How the floating counter fades while it is left alone: when [enabled], after [delaySeconds]
 * without a touch it turns to [opacity], and a touch makes it solid again. Chosen in the settings;
 * the floating counter follows a change at once.
 */
data class OverlayFade(
    val enabled: Boolean = true,
    val opacity: Float = DEFAULT_OPACITY,
    val delaySeconds: Int = DEFAULT_DELAY_SECONDS,
) {
    companion object {
        const val DEFAULT_OPACITY = 0.6f
        const val DEFAULT_DELAY_SECONDS = 5
        const val MIN_OPACITY = 0.2f
        const val MAX_OPACITY = 0.9f
        const val MIN_DELAY_SECONDS = 2
        const val MAX_DELAY_SECONDS = 15

        const val KEY_ENABLED = "overlay_fade"
        const val KEY_OPACITY = "overlay_fade_opacity"
        const val KEY_DELAY = "overlay_fade_delay"
        val KEYS = setOf(KEY_ENABLED, KEY_OPACITY, KEY_DELAY)
    }
}
