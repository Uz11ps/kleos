package com.example.kleos.ui.splash

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import com.example.kleos.databinding.ActivitySplashBinding
import com.example.kleos.ui.onboarding.OnboardingActivity
import com.example.kleos.ui.language.LanguageActivity
import com.example.kleos.ui.language.LocaleManager

class SplashActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Apply saved locale or ask user on first launch
        val prefs = getSharedPreferences("kleos_prefs_lang", MODE_PRIVATE)
        val saved = prefs.getString("app_locale", null)
        if (saved.isNullOrEmpty()) {
            startActivity(Intent(this, LanguageActivity::class.java))
            finish()
            return
        } else {
            LocaleManager.applySavedLocale(this)
        }

        // Стартовая задержка 2 секунды, затем плавный подъём и переход
        binding.logoGroup.postDelayed({
            animateUpAndNavigate()
        }, 2000)
    }

    @Suppress("DEPRECATION")
    private fun animateUpAndNavigate() {
        val targetTranslation = -binding.logoGroup.height.toFloat() - (binding.logoGroup.top.toFloat() / 2f)
        binding.logoGroup.animate()
            .translationY(targetTranslation)
            .alpha(0.9f)
            .setDuration(650)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                startActivity(Intent(this, OnboardingActivity::class.java))
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                // Завершаем, чтобы не возвращаться на сплэш
                finish()
            }
            .start()

        // Лёгкое проявление фона в момент анимации
        binding.splashBackdrop.animate()
            .alpha(1f)
            .setDuration(500)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()
    }
}


