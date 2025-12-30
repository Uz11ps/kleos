package com.kleos.education.ui.language

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.kleos.education.MainActivity
import com.kleos.education.databinding.ActivityLanguageBinding

class LanguageActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLanguageBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        com.kleos.education.ui.language.LocaleManager.applySavedLocale(this)
        super.onCreate(savedInstanceState)
        binding = ActivityLanguageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Анимации появления элементов
        com.kleos.education.ui.utils.AnimationUtils.bounceIn(binding.root.findViewById(com.kleos.education.R.id.btnRu), 600)
        com.kleos.education.ui.utils.AnimationUtils.bounceIn(binding.root.findViewById(com.kleos.education.R.id.btnEn), 700)
        com.kleos.education.ui.utils.AnimationUtils.bounceIn(binding.root.findViewById(com.kleos.education.R.id.btnZh), 800)
        
        // Анимации для кнопок при нажатии
        binding.btnRu.setOnTouchListener { view, motionEvent ->
            when (motionEvent.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    com.kleos.education.ui.utils.AnimationUtils.pressButton(view)
                }
                android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                    com.kleos.education.ui.utils.AnimationUtils.releaseButton(view)
                }
            }
            false
        }
        
        binding.btnEn.setOnTouchListener { view, motionEvent ->
            when (motionEvent.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    com.kleos.education.ui.utils.AnimationUtils.pressButton(view)
                }
                android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                    com.kleos.education.ui.utils.AnimationUtils.releaseButton(view)
                }
            }
            false
        }
        
        binding.btnZh.setOnTouchListener { view, motionEvent ->
            when (motionEvent.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    com.kleos.education.ui.utils.AnimationUtils.pressButton(view)
                }
                android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                    com.kleos.education.ui.utils.AnimationUtils.releaseButton(view)
                }
            }
            false
        }

        binding.btnRu.setOnClickListener { 
            com.kleos.education.ui.utils.AnimationUtils.shake(binding.btnRu)
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                setLangAndStart("ru")
            }, 300)
        }
        binding.btnEn.setOnClickListener { 
            com.kleos.education.ui.utils.AnimationUtils.shake(binding.btnEn)
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                setLangAndStart("en")
            }, 300)
        }
        binding.btnZh.setOnClickListener { 
            com.kleos.education.ui.utils.AnimationUtils.shake(binding.btnZh)
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                setLangAndStart("zh")
            }, 300)
        }
    }

    private fun setLangAndStart(code: String) {
        LocaleManager.setLocale(this, code)
        
        // Проверяем, откуда был открыт LanguageActivity
        val isFromAuth = intent.getBooleanExtra("from_auth", false)
        
        if (isFromAuth) {
            // Если открыт из AuthActivity, возвращаемся обратно
            finish()
            overridePendingTransition(com.kleos.education.R.anim.scale_in, com.kleos.education.R.anim.fade_out)
        } else {
            // Иначе переходим в MainActivity (как было раньше)
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
            finish()
            overridePendingTransition(com.kleos.education.R.anim.scale_in, com.kleos.education.R.anim.fade_out)
        }
    }
}



