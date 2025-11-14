package com.example.kleos.ui.language

import android.content.Context
import android.content.res.Configuration
import java.util.Locale
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

object LocaleManager {
    private const val PREFS = "kleos_prefs_lang"
    private const val KEY_LANG = "app_locale"

    fun applySavedLocale(context: Context) {
        val code = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_LANG, null)
        if (!code.isNullOrEmpty() && code != currentLocale()) {
            setLocale(context, code)
        }
    }

    fun setLocale(context: Context, code: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_LANG, code).apply()
        // Современный способ: AppCompat per-app locales
        val locales = LocaleListCompat.forLanguageTags(code)
        val current = AppCompatDelegate.getApplicationLocales()
        if (current != locales) {
            AppCompatDelegate.setApplicationLocales(locales)
        }
        // Избегаем тяжёлого updateConfiguration, если локаль не меняется.
        val currentLang = context.resources.configuration.locales.get(0)?.language
        if (currentLang != code) {
            val locale = Locale(code)
            Locale.setDefault(locale)
            val config = Configuration(context.resources.configuration)
            config.setLocale(locale)
            // Выполняем обновление на контексте приложения, чтобы не блокировать конкретную Activity
            context.applicationContext.resources.updateConfiguration(
                config,
                context.applicationContext.resources.displayMetrics
            )
        }
    }

    private fun currentLocale(): String {
        val tags = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        return if (tags.isNullOrEmpty()) Locale.getDefault().language else tags
    }
}


