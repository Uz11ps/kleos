package com.example.kleos

import android.app.Application
import com.example.kleos.ui.language.LocaleManager

class KleosApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Применяем сохранённую локаль один раз на старте приложения,
        // чтобы не делать тяжёлые операции в каждой Activity.
        LocaleManager.applySavedLocale(this)
    }
}



