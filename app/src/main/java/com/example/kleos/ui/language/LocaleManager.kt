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
        val code = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_LANG, null)
        if (!code.isNullOrEmpty()) {
            setLocale(context, code)
        }
    }

    fun setLocale(context: Context, code: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_LANG, code).apply()
        // Современный способ: AppCompat per-app locales
        val locales = LocaleListCompat.forLanguageTags(code)
        AppCompatDelegate.setApplicationLocales(locales)
        // На старых местах, где требуется немедленное применение для текущего контекста:
        val locale = Locale(code)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        context.resources.updateConfiguration(config, context.resources.displayMetrics)
    }
}


