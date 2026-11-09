package com.example.dhikra.ui

import android.content.Context
import android.content.res.Configuration
import androidx.core.content.edit
import java.util.Locale

/**
 * The language of the app's own interface: Arabic (the default, right-to-left) or English
 * (left-to-right). The content — the Quran, the adhkar and duas, the user's programs — stays as it
 * is written. The choice is the app's own, whatever the device language.
 */
enum class AppLanguage(val tag: String) {
    AR("ar"),
    EN("en");

    val locale: Locale get() = Locale.forLanguageTag(tag)

    val isRtl: Boolean get() = this == AR

    companion object {
        /** The language in use, read by [load] when the app or a reminder starts. */
        @Volatile
        var current: AppLanguage = AR
            internal set // by load / save; tests set it directly

        private const val PREFS = "dhikra"
        private const val KEY = "app_language"

        fun load(context: Context): AppLanguage {
            val tag = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
            return (entries.firstOrNull { it.tag == tag } ?: AR).also { current = it }
        }

        /** Stores [language]; the screen is then recreated to take it on. */
        fun save(context: Context, language: AppLanguage) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit { putString(KEY, language.tag) }
            current = language
        }
    }
}

/** This context with the app's own language (strings, layout direction), see [AppLanguage]. */
fun Context.localized(): Context {
    val language = AppLanguage.load(this)
    val config = Configuration(resources.configuration).apply {
        setLocale(language.locale)
        setLayoutDirection(language.locale)
    }
    return createConfigurationContext(config)
}
