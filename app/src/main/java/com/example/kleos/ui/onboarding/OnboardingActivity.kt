package com.example.kleos.ui.onboarding

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.kleos.databinding.ActivityOnboardingBinding
import android.content.Intent
import com.example.kleos.ui.auth.AuthActivity
import com.example.kleos.ui.language.LocaleManager

class OnboardingActivity : AppCompatActivity() {
    private lateinit var binding: ActivityOnboardingBinding

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Отключаем анимации окна
        window.setWindowAnimations(0)
        
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
        
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnContinue.setOnClickListener {
            val intent = Intent(this, AuthActivity::class.java)
            startActivity(intent)
            overridePendingTransition(0, 0)
            finish()
        }
    }
}


