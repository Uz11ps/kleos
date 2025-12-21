package com.example.kleos.ui.splash

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.kleos.databinding.ActivitySplashBinding
import com.example.kleos.ui.onboarding.OnboardingActivity
import com.example.kleos.ui.language.LocaleManager

class SplashActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Применяем сохраненную локаль или используем русскую по умолчанию
        val prefs = getSharedPreferences("kleos_prefs_lang", MODE_PRIVATE)
        val saved = prefs.getString("app_locale", "ru")
        if (saved.isNullOrEmpty()) {
            prefs.edit().putString("app_locale", "ru").apply()
            LocaleManager.applySavedLocale(this)
        } else {
            LocaleManager.applySavedLocale(this)
            // preload server-side translations overrides
            com.example.kleos.ui.language.TranslationManager.initAsync(this, saved)
        }

        // Сразу переходим на страницу onboarding без задержки
        binding.logoGroup.post {
            startActivity(Intent(this, OnboardingActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }
    }
}


